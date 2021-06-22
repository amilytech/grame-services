package com.grame.services.legacy.unit.handler;

/*-
 * ‌
 * grame Services Node
 * ​
 * Copyright (C) 2018 - 2021 grame grame, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.grame.services.config.MockGlobalDynamicProps;
import com.grame.services.contracts.sources.LedgerAccountsSource;
import com.grame.services.exceptions.NegativeAccountBalanceException;
import com.grame.services.fees.HbarCentExchange;
import com.grame.services.ledger.grameLedger;
import com.grame.services.ledger.TransactionalLedger;
import com.grame.services.ledger.accounts.FCMapBackingAccounts;
import com.grame.services.ledger.ids.EntityIdSource;
import com.grame.services.ledger.properties.AccountProperty;
import com.grame.services.ledger.properties.ChangeSummaryManager;
import com.grame.services.legacy.TestHelper;
import com.grame.services.legacy.handler.SmartContractRequestHandler;
import com.grame.services.legacy.unit.FCStorageWrapper;
import com.grame.services.legacy.util.SCEncoding;
import com.grame.services.records.AccountRecordsHistorian;
import com.grame.services.state.expiry.ExpiringCreations;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.state.merkle.MerkleBlobMeta;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.state.merkle.MerkleOptionalBlob;
import com.grame.services.state.submerkle.ExchangeRates;
import com.grame.services.state.submerkle.SequenceNumber;
import com.grame.services.store.tokens.TokenStore;
import com.grame.services.utils.EntityIdUtils;
import com.grame.services.utils.MiscUtils;
import com.grame.test.mocks.SolidityLifecycleFactory;
import com.grame.test.mocks.StorageSourceFactory;
import com.grame.test.mocks.TestUsagePricesProvider;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.ContractCallLocalQuery;
import com.gramegrame.api.proto.java.ContractCallLocalResponse;
import com.gramegrame.api.proto.java.ContractID;
import com.gramegrame.api.proto.java.Duration;
import com.gramegrame.api.proto.java.ExchangeRateSet;
import com.gramegrame.api.proto.java.FileID;
import com.gramegrame.api.proto.java.Key;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionRecord;
import com.gramegrame.builder.RequestBuilder;
import com.swirlds.fcmap.FCMap;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.KeyPairGenerator;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.ethereum.core.AccountState;
import org.ethereum.datasource.DbSource;
import org.ethereum.datasource.Source;
import org.ethereum.db.ServicesRepositoryRoot;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author peter
 * @version Junit5 Tests the SmartContractRequestHandler class features for a payable contract
 */

public class SmartContractRequestHandlerPayableTest {

  public static final String PAYABLE_TEST_BIN = "/testfiles/PayTest.bin";
  public static final int DEPOSIT_AMOUNT = 12345;
  public static final long INITIAL_BALANCE = 10_000_000_000L;
  public static final long EXCESSIVE_AMOUNT = INITIAL_BALANCE * 2L; // Too much to deposit or transfer
  // Unused account number 170
  public static final String INVALID_SOLIDITY_ADDRESS = "00000000000000000000000000000000000000aa";
  // Arbitrary account numbers.
  private static final long payerAccount = 787L;
  private static final long nodeAccount = 3L;
  private static final long feeCollAccount = 9876L;
  private static final long receiverAccount = 555L;
  private static final long contractFileNumber = 333L;
  private static final long contractSequenceNumber = 334L;
  SmartContractRequestHandler smartHandler;
  FileServiceHandler fsHandler;
  FCMap<MerkleEntityId, MerkleAccount> fcMap = null;
  FCMapBackingAccounts backingAccounts;
  private FCMap<MerkleBlobMeta, MerkleOptionalBlob> storageMap;
  ServicesRepositoryRoot repository;

  MerkleEntityId payerMerkleEntityId; // fcMap key for payer account
  byte[] payerKeyBytes = null; // Repository key for payer account
  AccountID payerAccountId;
  AccountID nodeAccountId;
  AccountID feeCollAccountId;
  FileID contractFileId;
  BigInteger gasPrice;
  private long selfID = 9870798L;
  private LedgerAccountsSource ledgerSource;
  private FCStorageWrapper storageWrapper;
  grameLedger ledger;

  private ServicesRepositoryRoot getLocalRepositoryInstance() {
    DbSource<byte[]> repDBFile = StorageSourceFactory.from(storageMap);
    backingAccounts = new FCMapBackingAccounts(() -> fcMap);
    TransactionalLedger<AccountID, AccountProperty, MerkleAccount> delegate = new TransactionalLedger<>(
            AccountProperty.class,
            () -> new MerkleAccount(),
            backingAccounts,
            new ChangeSummaryManager<>());
    ledger = new grameLedger(
            mock(TokenStore.class),
            mock(EntityIdSource.class),
            mock(ExpiringCreations.class),
            mock(AccountRecordsHistorian.class),
            delegate);
    ledgerSource = new LedgerAccountsSource(ledger, new MockGlobalDynamicProps());
    Source<byte[], AccountState> repDatabase = ledgerSource;
    ServicesRepositoryRoot repository = new ServicesRepositoryRoot(repDatabase, repDBFile);
    return repository;
  }

  @BeforeEach
  public void setUp() throws Exception {
    payerAccountId = RequestBuilder.getAccountIdBuild(payerAccount, 0l, 0l);
    nodeAccountId = RequestBuilder.getAccountIdBuild(nodeAccount, 0l, 0l);
    feeCollAccountId = RequestBuilder.getAccountIdBuild(feeCollAccount, 0l, 0l);
    contractFileId = RequestBuilder.getFileIdBuild(contractFileNumber, 0L, 0L);

    //Init FCMap
    fcMap = new FCMap<>();
    storageMap = new FCMap<>();
    // Create accounts
    createAccount(payerAccountId, INITIAL_BALANCE);
    createAccount(nodeAccountId, INITIAL_BALANCE);
    createAccount(feeCollAccountId, INITIAL_BALANCE);

    repository = getLocalRepositoryInstance();

    gasPrice = new BigInteger("1");

    HbarCentExchange exchange = mock(HbarCentExchange.class);
    long expiryTime = Long.MAX_VALUE;
    ExchangeRateSet rates = RequestBuilder
            .getExchangeRateSetBuilder(
                    1, 12,
                    expiryTime,
                    1, 15,
                    expiryTime);
    given(exchange.activeRates()).willReturn(rates);
    given(exchange.rate(any())).willReturn(rates.getCurrentRate());
    smartHandler = new SmartContractRequestHandler(
            repository,
            ledger,
            () -> fcMap,
            null,
            exchange,
            TestUsagePricesProvider.TEST_USAGE_PRICES,
            () -> repository,
            SolidityLifecycleFactory.newTestInstance(),
            ignore -> true,
            null,
            new MockGlobalDynamicProps());
    storageWrapper = new FCStorageWrapper(storageMap);
    FeeScheduleInterceptor feeScheduleInterceptor = mock(FeeScheduleInterceptor.class);
    fsHandler = new FileServiceHandler(
            storageWrapper,
            feeScheduleInterceptor,
            new ExchangeRates());
    String key = Hex.encodeHexString(EntityIdUtils.asSolidityAddress(0, 0, payerAccount));
    try {
      payerKeyBytes = MiscUtils.commonsHexToBytes(key);
    } catch (DecoderException e) {
      Assert.fail("Failure building solidity key for payer account");
    }
    payerMerkleEntityId = new MerkleEntityId();
    payerMerkleEntityId.setNum(payerAccount);
    payerMerkleEntityId.setRealm(0);
    payerMerkleEntityId.setShard(0);
  }

  @Test
  @DisplayName("01 createContract: Success")
  public void createContractWithAdminKey() {
    KeyPair adminKeyPair = new KeyPairGenerator().generateKeyPair();
    byte[] pubKey = ((EdDSAPublicKey) adminKeyPair.getPublic()).getAbyte();
    Key adminPubKey = Key.newBuilder().setEd25519(ByteString.copyFrom(pubKey)).build();
    byte[] contractBytes = createFile(PAYABLE_TEST_BIN, contractFileId);
    TransactionBody body = getCreateTransactionBody(0L, 250000L, adminPubKey);

    Instant consensusTime = new Date().toInstant();
    SequenceNumber seqNumber = new SequenceNumber(contractSequenceNumber);
    ledger.begin();
    TransactionRecord record = smartHandler.createContract(body, consensusTime, contractBytes, seqNumber);
    ledger.commit();

    Assert.assertNotNull(record);
    Assert.assertNotNull(record.getTransactionID());
    Assert.assertNotNull(record.getReceipt());
    Assert.assertEquals(ResponseCodeEnum.SUCCESS, record.getReceipt().getStatus());
    Assert.assertEquals(contractSequenceNumber, record.getReceipt().getContractID().getContractNum());
    Assert.assertTrue(record.hasContractCreateResult());

    ContractID newContractId = record.getReceipt().getContractID();
    checkContractArtifactsExist(newContractId);
  }

  private TransactionBody getCallTransactionBody(ContractID newContractId,
      ByteString functionData, long gas, long value) {
    Timestamp startTime = RequestBuilder
        .getTimestamp(Instant.now(Clock.systemUTC()));
    Duration transactionDuration = RequestBuilder.getDuration(100);

    Transaction txn = RequestBuilder.getContractCallRequest(payerAccount, 0L, 0L,
        nodeAccount, 0L, 0L,
        100L /* fee */, startTime,
        transactionDuration, gas, newContractId,
        functionData, value);

    TransactionBody body = null;
    try {
      body = com.grame.services.legacy.proto.utils.CommonUtils.extractTransactionBody(txn);
    } catch (InvalidProtocolBufferException e) {
      Assert.fail("Error calling contract: parsing transaction body");
    }
    return body;
  }

  @Test
  @DisplayName("02 ContractDepositCall: Success")
  public void contractDepositCall() {
    // Create the contract
    byte[] contractBytes = createFile(PAYABLE_TEST_BIN, contractFileId);
    TransactionBody body = getCreateTransactionBody();
    Instant consensusTime = new Date().toInstant();
    SequenceNumber seqNumber = new SequenceNumber(contractSequenceNumber);
    ledger.begin();
    TransactionRecord record = smartHandler.createContract(body, consensusTime, contractBytes, seqNumber);
    ledger.commit();
    ContractID newContractId = record.getReceipt().getContractID();

    // Call the contract to deposit value
    ByteString dataToSet = ByteString.copyFrom(SCEncoding.encodeDeposit(DEPOSIT_AMOUNT));
    body = getCallTransactionBody(newContractId, dataToSet, 250000L, DEPOSIT_AMOUNT);
    consensusTime = new Date().toInstant();
    seqNumber.getAndIncrement();
    ledger.begin();
    record = smartHandler.contractCall(body, consensusTime, seqNumber);
    ledger.commit();

    Assert.assertNotNull(record);
    Assert.assertNotNull(record.getTransactionID());
    Assert.assertNotNull(record.getReceipt());
    Assert.assertEquals(ResponseCodeEnum.SUCCESS, record.getReceipt().getStatus());
    Assert.assertEquals(contractSequenceNumber, record.getReceipt().getContractID().getContractNum());
  }

  private Query getCallLocalQuery(ContractID newContractId, ByteString functionData, long gas) {
    Transaction transferTransaction = TestHelper.createTransferUnsigned(payerAccountId,
        feeCollAccountId, payerAccountId, nodeAccountId, 100000L /* amount */);

    return RequestBuilder.getContractCallLocalQuery(newContractId, gas,
        functionData, 0L /* value */, 5000L /* maxResultSize */,
        transferTransaction, ResponseType.ANSWER_ONLY);
  }

  @Test
  @DisplayName("03 ContractDepositCall: Mismatched values")
  public void contractDepositCallMismatch() {
    // Create the contract
    byte[] contractBytes = createFile(PAYABLE_TEST_BIN, contractFileId);
    TransactionBody body = getCreateTransactionBody();
    Instant consensusTime = new Date().toInstant();
    SequenceNumber seqNumber = new SequenceNumber(contractSequenceNumber);
    ledger.begin();
    TransactionRecord record = smartHandler.createContract(body, consensusTime, contractBytes, seqNumber);
    ledger.commit();
    ContractID newContractId = record.getReceipt().getContractID();

    // Call the contract to deposit value
    // Fails when passed parameter doesn't match value sent. This is an attribute of this particular
    // function, not all payable functions.
    ByteString dataToSet = ByteString.copyFrom(SCEncoding.encodeDeposit(DEPOSIT_AMOUNT + 1));
    body = getCallTransactionBody(newContractId, dataToSet, 250000L, DEPOSIT_AMOUNT);
    consensusTime = new Date().toInstant();
    seqNumber.getAndIncrement();
    ledger.begin();
    record = smartHandler.contractCall(body, consensusTime, seqNumber);
    ledger.commit();

    Assert.assertNotNull(record);
    Assert.assertNotNull(record.getTransactionID());
    Assert.assertNotNull(record.getReceipt());
    Assert.assertEquals(ResponseCodeEnum.CONTRACT_REVERT_EXECUTED, record.getReceipt().getStatus());
    Assert.assertEquals(contractSequenceNumber, record.getReceipt().getContractID().getContractNum());
  }

  @Test
  @DisplayName("05 ContractDepositCall: value more than payer has")
  public void contractDepositCallTooMuch() {
    // Create the contract
    byte[] contractBytes = createFile(PAYABLE_TEST_BIN, contractFileId);
    TransactionBody body = getCreateTransactionBody();
    Instant consensusTime = new Date().toInstant();
    SequenceNumber seqNumber = new SequenceNumber(contractSequenceNumber);
    ledger.begin();
    TransactionRecord record = smartHandler.createContract(body, consensusTime, contractBytes, seqNumber);
    ledger.commit();
    ContractID newContractId = record.getReceipt().getContractID();

    // Call the contract to deposit value
    // System does not allow negative values.
    ByteString dataToSet = ByteString.copyFrom(SCEncoding.encodeDeposit(EXCESSIVE_AMOUNT));
    body = getCallTransactionBody(newContractId, dataToSet, 250000L, EXCESSIVE_AMOUNT);
    consensusTime = new Date().toInstant();
    seqNumber.getAndIncrement();
    ledger.begin();
    record = smartHandler.contractCall(body, consensusTime, seqNumber);
    ledger.commit();

    Assert.assertNotNull(record);
    Assert.assertNotNull(record.getTransactionID());
    Assert.assertNotNull(record.getReceipt());
    Assert.assertEquals(ResponseCodeEnum.INSUFFICIENT_PAYER_BALANCE, record.getReceipt().getStatus());
    Assert.assertEquals(contractSequenceNumber, record.getReceipt().getContractID().getContractNum());
  }

  @Test
  @DisplayName("08 ContractGetBalanceCall: Success")
  public void contractGetBalanceCall() throws Exception {
    // Create the contract
    byte[] contractBytes = createFile(PAYABLE_TEST_BIN, contractFileId);
    TransactionBody body = getCreateTransactionBody();
    Instant consensusTime = new Date().toInstant();
    SequenceNumber seqNumber = new SequenceNumber(contractSequenceNumber);
    ledger.begin();
    TransactionRecord record = smartHandler.createContract(body, consensusTime, contractBytes, seqNumber);
    ledger.commit();
    ContractID newContractId = record.getReceipt().getContractID();

    // Call the contract to deposit value
    ByteString dataToSet = ByteString.copyFrom(SCEncoding.encodeDeposit(DEPOSIT_AMOUNT));
    body = getCallTransactionBody(newContractId, dataToSet, 250000L, DEPOSIT_AMOUNT);
    consensusTime = new Date().toInstant();
    seqNumber.getAndIncrement();
    ledger.begin();
    record = smartHandler.contractCall(body, consensusTime, seqNumber);
    ledger.commit();

    // Call the contract to get the balance
    ByteString dataToGet = ByteString.copyFrom(SCEncoding.encodeGetBalance());
    ContractCallLocalQuery cCLQuery = getCallLocalQuery(newContractId, dataToGet, 250000L)
        .getContractCallLocal();
    seqNumber.getAndIncrement();
    ContractCallLocalResponse response = smartHandler.contractCallLocal(cCLQuery, System.currentTimeMillis());
    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getFunctionResult().getContractCallResult());

    byte[] callResults = response.getFunctionResult().getContractCallResult().toByteArray();
    Assert.assertNotNull(callResults);
    Assert.assertTrue(callResults.length > 0);
    int retVal = SCEncoding.decodeGetBalanceResult(callResults);
    Assert.assertEquals(DEPOSIT_AMOUNT, retVal);
  }

  @Test
  @DisplayName("10 ContractSendFundsCall: Success")
  public void contractSendFundsCall() throws Exception {
    // Create the contract
    byte[] contractBytes = createFile(PAYABLE_TEST_BIN, contractFileId);
    TransactionBody body = getCreateTransactionBody();
    Instant consensusTime = new Date().toInstant();
    SequenceNumber seqNumber = new SequenceNumber(contractSequenceNumber);
    ledger.begin();
    TransactionRecord record = smartHandler.createContract(body, consensusTime, contractBytes, seqNumber);
    ledger.commit();
    ContractID newContractId = record.getReceipt().getContractID();

    // Call the contract to deposit value
    ByteString dataToSet = ByteString.copyFrom(SCEncoding.encodeDeposit(DEPOSIT_AMOUNT));
    body = getCallTransactionBody(newContractId, dataToSet, 250000L, DEPOSIT_AMOUNT);
    consensusTime = new Date().toInstant();
    seqNumber.getAndIncrement();
    ledger.begin();
    record = smartHandler.contractCall(body, consensusTime, seqNumber);
    ledger.commit();

    // Create a receiver account
    AccountID receiverAccountId = RequestBuilder.getAccountIdBuild(receiverAccount, 0l, 0l);
    createAccount(receiverAccountId, INITIAL_BALANCE);
    String receiverSolidityAddr = Hex.encodeHexString(EntityIdUtils.asSolidityAddress(0, 0, receiverAccount));

    // Save the "before" balances
    long receiverBefore = getBalance(receiverAccountId);
    long contractBefore = getBalance(newContractId);
    long totalBefore = getTotalBalance();

    // Call the contract to transfer funds
    int transferAmount = DEPOSIT_AMOUNT / 2;
    ByteString dataToSend = ByteString.copyFrom(SCEncoding.encodeSendFunds(receiverSolidityAddr, transferAmount));
    body = getCallTransactionBody(newContractId, dataToSend, 250000L, 0L);
    consensusTime = new Date().toInstant();
    seqNumber.getAndIncrement();
    ledger.begin();
    record = smartHandler.contractCall(body, consensusTime, seqNumber);
    ledger.commit();
    Assert.assertNotNull(record);
    Assert.assertNotNull(record.getTransactionID());
    Assert.assertNotNull(record.getReceipt());
    Assert.assertEquals(ResponseCodeEnum.SUCCESS, record.getReceipt().getStatus());
    Assert.assertEquals(contractSequenceNumber, record.getReceipt().getContractID().getContractNum());

    long receiverAfter = getBalance(receiverAccountId);
    long contractAfter = getBalance(newContractId);
    long totalAfter = getTotalBalance();

    // Do the after balances match expected values?
    Assert.assertEquals(receiverBefore + transferAmount, receiverAfter);
    Assert.assertEquals(contractBefore - transferAmount, contractAfter);
    Assert.assertEquals(totalBefore, totalAfter);
  }

  @Test
  @DisplayName("11 ContractSendFundsCall: Invalid receiver address")
  public void contractSendFundsCallInvalidReceiver() throws Exception {
    // Create the contract
    byte[] contractBytes = createFile(PAYABLE_TEST_BIN, contractFileId);
    TransactionBody body = getCreateTransactionBody();
    Instant consensusTime = new Date().toInstant();
    SequenceNumber seqNumber = new SequenceNumber(contractSequenceNumber);
    ledger.begin();
    TransactionRecord record = smartHandler.createContract(body, consensusTime, contractBytes, seqNumber);
    ledger.commit();
    ContractID newContractId = record.getReceipt().getContractID();

    // Call the contract to deposit value
    ByteString dataToSet = ByteString.copyFrom(SCEncoding.encodeDeposit(DEPOSIT_AMOUNT));
    body = getCallTransactionBody(newContractId, dataToSet, 250000L, DEPOSIT_AMOUNT);
    consensusTime = new Date().toInstant();
    seqNumber.getAndIncrement();
    ledger.begin();
    record = smartHandler.contractCall(body, consensusTime, seqNumber);
    ledger.commit();

    // Save the "before" balances
    long contractBefore = getBalance(newContractId);
    long totalBefore = getTotalBalance();

    // Call the contract to transfer funds
    int transferAmount = DEPOSIT_AMOUNT / 2;
    ByteString dataToSend = ByteString.copyFrom(SCEncoding.encodeSendFunds(INVALID_SOLIDITY_ADDRESS, transferAmount));
    body = getCallTransactionBody(newContractId, dataToSend, 250000L, 0L);
    consensusTime = new Date().toInstant();
    seqNumber.getAndIncrement();
    ledger.begin();
    record = smartHandler.contractCall(body, consensusTime, seqNumber);
    ledger.commit();
    Assert.assertNotNull(record);
    Assert.assertNotNull(record.getTransactionID());
    Assert.assertNotNull(record.getReceipt());
    //invalid address should cause an exception
    Assert.assertEquals(ResponseCodeEnum.INVALID_SOLIDITY_ADDRESS, record.getReceipt().getStatus());
    Assert.assertEquals(contractSequenceNumber, record.getReceipt().getContractID().getContractNum());

    long contractAfter = getBalance(newContractId);
    long totalAfter = getTotalBalance();

    // Do the after balances match expected values?
    Assert.assertEquals(contractBefore, contractAfter);
    Assert.assertEquals(totalBefore, totalAfter);
  }

  @Test
  @DisplayName("12 ContractSendFundsCall: Value more than contract has")
  public void contractSendFundsCallTooMuch() throws Exception {
    // Create the contract
    byte[] contractBytes = createFile(PAYABLE_TEST_BIN, contractFileId);
    TransactionBody body = getCreateTransactionBody();
    Instant consensusTime = new Date().toInstant();
    SequenceNumber seqNumber = new SequenceNumber(contractSequenceNumber);
    ledger.begin();
    TransactionRecord record = smartHandler.createContract(body, consensusTime, contractBytes, seqNumber);
    ledger.commit();
    ContractID newContractId = record.getReceipt().getContractID();

    // Call the contract to deposit value
    ByteString dataToSet = ByteString.copyFrom(SCEncoding.encodeDeposit(EXCESSIVE_AMOUNT));
    body = getCallTransactionBody(newContractId, dataToSet, 250000L, EXCESSIVE_AMOUNT);
    consensusTime = new Date().toInstant();
    seqNumber.getAndIncrement();
    ledger.begin();
    record = smartHandler.contractCall(body, consensusTime, seqNumber);
    ledger.commit();

    // Create a receiver account
    AccountID receiverAccountId = RequestBuilder.getAccountIdBuild(receiverAccount, 0l, 0l);
    createAccount(receiverAccountId, INITIAL_BALANCE);
    String receiverSolidityAddr = Hex.encodeHexString(EntityIdUtils.asSolidityAddress(0, 0, receiverAccount));

    // Save the "before" balances
    long receiverBefore = getBalance(receiverAccountId);
    long contractBefore = getBalance(newContractId);
    long totalBefore = getTotalBalance();

    // Call the contract to transfer funds
    int transferAmount = DEPOSIT_AMOUNT / 2;
    ByteString dataToSend = ByteString.copyFrom(SCEncoding.encodeSendFunds(receiverSolidityAddr, transferAmount));
    body = getCallTransactionBody(newContractId, dataToSend, 250000L, 0L);
    consensusTime = new Date().toInstant();
    seqNumber.getAndIncrement();
    ledger.begin();
    record = smartHandler.contractCall(body, consensusTime, seqNumber);
    ledger.commit();
    Assert.assertNotNull(record);
    Assert.assertNotNull(record.getTransactionID());
    Assert.assertNotNull(record.getReceipt());
    Assert.assertEquals(ResponseCodeEnum.CONTRACT_REVERT_EXECUTED, record.getReceipt().getStatus());
    Assert.assertEquals(contractSequenceNumber, record.getReceipt().getContractID().getContractNum());

    long receiverAfter = getBalance(receiverAccountId);
    long contractAfter = getBalance(newContractId);
    long totalAfter = getTotalBalance();

    // Do the after balances match expected values?
    Assert.assertEquals(receiverBefore, receiverAfter);
    Assert.assertEquals(contractBefore, contractAfter);
    Assert.assertEquals(totalBefore, totalAfter);
  }

  @Test
  @DisplayName("15 ContractGetBalanceOfCall: Success")
  public void contractGetBalanceOfCall() throws Exception {
    // Create the contract
    byte[] contractBytes = createFile(PAYABLE_TEST_BIN, contractFileId);
    TransactionBody body = getCreateTransactionBody();
    Instant consensusTime = new Date().toInstant();
    SequenceNumber seqNumber = new SequenceNumber(contractSequenceNumber);
    ledger.begin();
    TransactionRecord record = smartHandler.createContract(body, consensusTime, contractBytes, seqNumber);
    ledger.commit();
    ContractID newContractId = record.getReceipt().getContractID();

    // Create a new account
    AccountID receiverAccountId = RequestBuilder.getAccountIdBuild(receiverAccount, 0l, 0l);
    createAccount(receiverAccountId, INITIAL_BALANCE);
    String receiverSolidityAddr = Hex.encodeHexString(EntityIdUtils.asSolidityAddress(0, 0, receiverAccount));

    // Call the contract to get the balance
    ByteString dataToGet = ByteString.copyFrom(SCEncoding.encodeGetBalanceOf(receiverSolidityAddr));
    ContractCallLocalQuery cCLQuery = getCallLocalQuery(newContractId, dataToGet, 250000L).getContractCallLocal();
    seqNumber.getAndIncrement();
    ContractCallLocalResponse response = smartHandler.contractCallLocal(cCLQuery, System.currentTimeMillis());
    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getFunctionResult().getContractCallResult());

    byte[] callResults = response.getFunctionResult().getContractCallResult().toByteArray();
    Assert.assertNotNull(callResults);
    Assert.assertTrue(callResults.length > 0);
    long retVal = SCEncoding.decodeGetBalanceOfResult(callResults);
    Assert.assertEquals(INITIAL_BALANCE, retVal);
  }

  @Test
  @DisplayName("16 ContractGetBalanceOfCall: Invalid account address")
  public void contractGetBalanceOfCallInvalidAccount() throws Exception {
    // Create the contract
    byte[] contractBytes = createFile(PAYABLE_TEST_BIN, contractFileId);
    TransactionBody body = getCreateTransactionBody();
    Instant consensusTime = new Date().toInstant();
    SequenceNumber seqNumber = new SequenceNumber(contractSequenceNumber);
    ledger.begin();
    TransactionRecord record = smartHandler.createContract(body, consensusTime, contractBytes, seqNumber);
    ledger.commit();
    ContractID newContractId = record.getReceipt().getContractID();

    // Call the contract to get the balance
    // Note that this returns zero for an invalid account address.
    ByteString dataToGet = ByteString.copyFrom(SCEncoding.encodeGetBalanceOf(INVALID_SOLIDITY_ADDRESS));
    ContractCallLocalQuery cCLQuery = getCallLocalQuery(newContractId, dataToGet, 250000L).getContractCallLocal();
    seqNumber.getAndIncrement();
    ContractCallLocalResponse response = smartHandler.contractCallLocal(cCLQuery, System.currentTimeMillis());
    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getFunctionResult().getContractCallResult());

    byte[] callResults = response.getFunctionResult().getContractCallResult().toByteArray();
    Assert.assertNotNull(callResults);
    Assert.assertTrue(callResults.length > 0);
    long retVal = SCEncoding.decodeGetBalanceOfResult(callResults);
    Assert.assertEquals(0, retVal);
  }

  private long getBalance(AccountID accountId) {
    MerkleEntityId mk = new MerkleEntityId();
    mk.setNum(accountId.getAccountNum());
    mk.setRealm(0);
    mk.setShard(0);

    MerkleAccount mv = fcMap.get(mk);
    if (mv == null) {
      return 0;
    } else {
      return mv.getBalance();
    }
  }

  private long getBalance(ContractID contractId) {
    MerkleEntityId mk = new MerkleEntityId();
    mk.setNum(contractId.getContractNum());
    mk.setRealm(0);
    mk.setShard(0);

    MerkleAccount mv = fcMap.get(mk);
    if (mv == null) {
      return 0;
    } else {
      return mv.getBalance();
    }
  }

  private long getTotalBalance() {
    long total = 0L;
    for (MerkleAccount val : fcMap.values()) {
      total += val.getBalance();
    }
    return total;
  }


  @AfterEach
  public void tearDown() throws Exception {
    try {

      repository.close();
    } catch (Throwable tx) {
      //do nothing now.
    } finally {
      repository = null;

    }
  }

  private void createAccount(AccountID payerAccount, long balance)
          throws NegativeAccountBalanceException {
    MerkleEntityId mk = new MerkleEntityId();
    mk.setNum(payerAccount.getAccountNum());
    mk.setRealm(0);
    MerkleAccount mv = new MerkleAccount();
    mv.setBalance(balance);
    if (backingAccounts != null) {
      backingAccounts.put(payerAccount, mv);
    } else {
      fcMap.put(mk, mv);
    }
  }

  private byte[] createFile(String filePath, FileID fileId) {
    InputStream fis = SmartContractRequestHandlerPayableTest.class.getResourceAsStream(filePath);
    byte[] fileBytes = null;
    try {
      fileBytes = fis.readAllBytes();
    } catch (IOException e) {
      Assert.fail("Error creating file: reading contract file " + filePath);
    }
    ByteString fileData = ByteString.copyFrom(fileBytes);

    Timestamp startTime = RequestBuilder
            .getTimestamp(Instant.now(Clock.systemUTC()));
    Timestamp expTime = RequestBuilder
            .getTimestamp(Instant.now(Clock.systemUTC()).plusSeconds(130));
    Duration transactionDuration = RequestBuilder.getDuration(100);
    boolean generateRecord = true;
    String memo = "SmartContractFile";

    Transaction txn = RequestBuilder.getFileCreateBuilder(payerAccount, 0L, 0L,
            nodeAccount, 0L, 0L,
            100L, startTime, transactionDuration, generateRecord,
            memo, fileData, expTime, Collections.emptyList());

    TransactionBody body = null;
    try {
      body = com.grame.services.legacy.proto.utils.CommonUtils.extractTransactionBody(txn);
    } catch (InvalidProtocolBufferException e) {
      Assert.fail("Error creating file: parsing transaction body");
    }

    Instant consensusTime = new Date().toInstant();
    TransactionRecord record = fsHandler.createFile(body, consensusTime, fileId, selfID);

    Assert.assertNotNull(record);
    Assert.assertNotNull(record.getTransactionID());
    Assert.assertNotNull(record.getReceipt());
    Assert.assertEquals(ResponseCodeEnum.SUCCESS, record.getReceipt().getStatus());
    Assert.assertEquals(fileId.getFileNum(), record.getReceipt().getFileID().getFileNum());
    return fileBytes;
  }


  private TransactionBody getCreateTransactionBody() {
    return getCreateTransactionBody(0L, 250000L, null);
  }

  private TransactionBody getCreateTransactionBody(long initialBalance, long gas, Key adminKey) {
    Timestamp startTime = RequestBuilder
            .getTimestamp(Instant.now(Clock.systemUTC()));
    Duration transactionDuration = RequestBuilder.getDuration(100);
    Duration renewalDuration = RequestBuilder.getDuration(3600 * 24);
    boolean generateRecord = true;
    String memo = "SmartContract";
    String sCMemo = "SmartContractMemo";

    Transaction txn = RequestBuilder.getCreateContractRequest(payerAccount, 0L, 0L,
            nodeAccount, 0L, 0L,
            100L, startTime, transactionDuration, generateRecord,
            memo, gas, contractFileId, ByteString.EMPTY, initialBalance,
            renewalDuration, sCMemo, adminKey);

    TransactionBody body = null;
    try {
      body = com.grame.services.legacy.proto.utils.CommonUtils.extractTransactionBody(txn);
    } catch (InvalidProtocolBufferException e) {
      Assert.fail("Error creating contract: parsing transaction body");
    }
    return body;
  }

  private void checkContractArtifactsExist(ContractID contractId) {
    MerkleEntityId mk = new MerkleEntityId();
    mk.setNum(contractId.getContractNum());
    mk.setRealm(contractId.getRealmNum());
    mk.setShard(contractId.getShardNum());
    MerkleAccount mv = fcMap.get(mk);
    Assert.assertNotNull(mv);
    Assert.assertNotNull(mv.getKey());
    Assert.assertNotNull(mv.getKey());
    long mapValueExpiration = mv.getExpiry();
    Assert.assertNotEquals(0, mapValueExpiration);
    String bytesPath = String.format("/%d/s%d", contractId.getRealmNum(), contractId.getContractNum());
    Assert.assertTrue(storageWrapper.fileExists(bytesPath));
    String sCMetaDataPath = String
            .format("/%d/m%d", contractId.getRealmNum(), contractId.getContractNum());
    Assert.assertFalse(storageWrapper.fileExists(sCMetaDataPath));
    String sCAdminKeyPath = String.format("/%d/a%d", contractId.getRealmNum(), contractId.getContractNum());
    Assert.assertFalse(storageWrapper.fileExists(sCAdminKeyPath));
  }

}
