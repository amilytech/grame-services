package com.grame.services.legacy.unit;

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

import com.grame.services.config.MockAccountNumbers;
import com.grame.services.config.MockEntityNumbers;
import com.grame.services.config.MockGlobalDynamicProps;
import com.grame.services.context.ContextPlatformStatus;
import com.grame.services.fees.StandardExemptions;
import com.grame.services.legacy.handler.TransactionHandler;
import com.grame.services.legacy.unit.utils.DummyHapiPermissions;
import com.grame.services.records.RecordCache;
import com.grame.services.security.ops.SystemOpPolicies;
import com.grame.services.sigs.verification.PrecheckVerifier;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.state.merkle.MerkleBlobMeta;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.state.merkle.MerkleOptionalBlob;
import com.grame.services.txns.validation.BasicPrecheck;
import com.grame.services.utils.MiscUtils;
import com.grame.test.mocks.TestContextValidator;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.Duration;
import com.gramegrame.api.proto.java.ExchangeRateSet;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.api.proto.java.SignatureList;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.builder.RequestBuilder;
import com.gramegrame.builder.TransactionSigner;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.swirlds.common.PlatformStatus;
import com.swirlds.common.internal.SettingsCommon;
import com.swirlds.fcmap.FCMap;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.KeyPairGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

@TestInstance(Lifecycle.PER_CLASS)
class QueryValidationTest {
	@BeforeAll
	public static void setupAll() {
		SettingsCommon.transactionMaxBytes = 1_234_567;
	}

	long payerAccountInitialBalance = 100000;
	private FCMap<MerkleEntityId, MerkleAccount> map = new FCMap<>();
	private AccountID nodeAccount =
			AccountID.newBuilder().setAccountNum(3).setRealmNum(0).setShardNum(0).build();
	private AccountID payerAccount =
			AccountID.newBuilder().setAccountNum(300).setRealmNum(0).setShardNum(0).build();
	private AccountID negetiveAccountNo =
			AccountID.newBuilder().setAccountNum(-1111).setRealmNum(0).setShardNum(0).build();
	private AccountID lowBalanceAccount = AccountID.newBuilder().setAccountNum(3001).setRealmNum(0)
			.setShardNum(0).build();
	private KeyPair payerKeyGenerated = new KeyPairGenerator().generateKeyPair();
	private TransactionHandler transactionHandler;

	private Transaction createQueryHeaderTransfer(AccountID payer) throws Exception {
		Timestamp timestamp = RequestBuilder.getTimestamp(Instant.now());
		Duration transactionDuration = RequestBuilder.getDuration(30);

		SignatureList sigList = SignatureList.getDefaultInstance();
		Transaction transferTx = RequestBuilder.getCryptoTransferRequest(payer.getAccountNum(),
				payer.getRealmNum(), payer.getShardNum(), nodeAccount.getAccountNum(),
				nodeAccount.getRealmNum(), nodeAccount.getShardNum(), 100, timestamp, transactionDuration,
				false, "test", payer.getAccountNum(), -100l, nodeAccount.getAccountNum(), 100l);
		List<PrivateKey> privateKeyList = new ArrayList<>();
		List<PublicKey> pubKeyList = new ArrayList<>();
		PrivateKey genPrivKey = payerKeyGenerated.getPrivate();
		PublicKey genPubKey = payerKeyGenerated.getPublic();
		privateKeyList.add(genPrivKey);
		privateKeyList.add(genPrivKey);
		pubKeyList.add(genPubKey);
		pubKeyList.add(genPubKey);
		transferTx = TransactionSigner.signTransactionWithSignatureMap(transferTx, privateKeyList, pubKeyList);
		return transferTx;
	}

	@BeforeAll
	void initializeState() throws Exception {
		PrecheckVerifier precheckVerifier = mock(PrecheckVerifier.class);
		given(precheckVerifier.hasNecessarySignatures(any())).willReturn(true);
		var policies = new SystemOpPolicies(new MockEntityNumbers());
		var platformStatus = new ContextPlatformStatus();
		platformStatus.set(PlatformStatus.ACTIVE);
		transactionHandler = new TransactionHandler(
				mock(RecordCache.class),
				precheckVerifier,
				() -> map,
				nodeAccount,
				new MockAccountNumbers(),
				policies,
				new StandardExemptions(new MockAccountNumbers(), policies),
				platformStatus,
				new DummyHapiPermissions());
		transactionHandler.setBasicPrecheck(
				new BasicPrecheck(TestContextValidator.TEST_VALIDATOR, new MockGlobalDynamicProps()));
		byte[] pubKey = ((EdDSAPublicKey) payerKeyGenerated.getPublic()).getAbyte();
		onboardAccount(payerAccount, pubKey, payerAccountInitialBalance);
		onboardAccount(lowBalanceAccount, pubKey, 100L);

		GlobalFlag.getInstance().setExchangeRateSet(getDefaultExchangeRateSet());
	}

	private static ExchangeRateSet getDefaultExchangeRateSet() {
		long expiryTime = Long.MAX_VALUE;
		return RequestBuilder.getExchangeRateSetBuilder(1, 1, expiryTime, 1, 1, expiryTime);
	}

	private void onboardAccount(AccountID account, byte[] publicKey, long initialBalance)
			throws Exception {
		NodeAccountsCreation.insertAccount(initialBalance, MiscUtils.commonsBytesToHex(publicKey), account, map
		);
	}

	@Test
	void testValidateGetInfoQuery_validateQuery() throws Exception {
		Transaction transferTransaction = createQueryHeaderTransfer(payerAccount);
		Query cryptoGetInfoQuery = RequestBuilder.getCryptoGetInfoQuery(payerAccount,
				transferTransaction, ResponseType.ANSWER_ONLY);

		ResponseCodeEnum result = transactionHandler.validateQuery(cryptoGetInfoQuery, true);
		assertEquals(OK, result);
	}

	@Test
	void testValidateGetInfoQuery_validateQuery_negative() throws Exception {
		transactionHandler.setHapiOpPermissions(new DummyHapiPermissions(ResponseCodeEnum.NOT_SUPPORTED));
		Transaction transferTransaction = createQueryHeaderTransfer(negetiveAccountNo);
		Query cryptoGetInfoQuery = RequestBuilder.getCryptoGetInfoQuery(negetiveAccountNo,
				transferTransaction, ResponseType.ANSWER_ONLY);

		ResponseCodeEnum result = transactionHandler.validateQuery(cryptoGetInfoQuery, true);
		assertEquals(ResponseCodeEnum.NOT_SUPPORTED, result);
		transactionHandler.setHapiOpPermissions(new DummyHapiPermissions());
	}
}
