package com.grame.services.context;

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
import com.grame.services.fees.HbarCentExchange;
import com.grame.services.fees.charging.ItemizableFeeCharging;
import com.grame.services.ledger.grameLedger;
import com.grame.services.legacy.core.jproto.JKey;
import com.grame.services.state.expiry.ExpiringEntity;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.state.merkle.MerkleTopic;
import com.grame.services.utils.PlatformTxnAccessor;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.AccountAmount;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.ContractFunctionResult;
import com.gramegrame.api.proto.java.ContractID;
import com.gramegrame.api.proto.java.ExchangeRate;
import com.gramegrame.api.proto.java.ExchangeRateSet;
import com.gramegrame.api.proto.java.FileID;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.ScheduleID;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.TokenID;
import com.gramegrame.api.proto.java.TokenTransferList;
import com.gramegrame.api.proto.java.TopicID;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionID;
import com.gramegrame.api.proto.java.TransactionRecord;
import com.gramegrame.api.proto.java.TransferList;
import com.swirlds.common.Address;
import com.swirlds.common.AddressBook;
import com.swirlds.fcmap.FCMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static com.grame.services.context.AwareTransactionContext.EMPTY_KEY;
import static com.grame.test.utils.IdUtils.asAccount;
import static com.grame.test.utils.IdUtils.asAccountString;
import static com.grame.test.utils.IdUtils.asContract;
import static com.grame.test.utils.IdUtils.asFile;
import static com.grame.test.utils.IdUtils.asSchedule;
import static com.grame.test.utils.IdUtils.asToken;
import static com.grame.test.utils.IdUtils.asTopic;
import static com.grame.test.utils.TxnUtils.withAdjustments;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;

public class AwareTransactionContextTest {
	final TransactionID scheduledTxnId = TransactionID.newBuilder()
			.setAccountID(IdUtils.asAccount("0.0.2"))
			.build();
	private long fee = 123L;
	private long memberId = 3;
	private long anotherMemberId = 4;
	private Instant now = Instant.now();
	private Timestamp timeNow = Timestamp.newBuilder()
			.setSeconds(now.getEpochSecond())
			.setNanos(now.getNano())
			.build();
	private ExchangeRate rateNow = ExchangeRate.newBuilder().setHbarEquiv(1).setCentEquiv(100).build();
	private ExchangeRateSet ratesNow = ExchangeRateSet.newBuilder().setCurrentRate(rateNow).setNextRate(rateNow).build();
	private AccountID payer = asAccount("0.0.2");
	private AccountID node = asAccount("0.0.3");
	private AccountID anotherNodeAccount = asAccount("0.0.4");
	private AccountID funding = asAccount("0.0.98");
	private AccountID created = asAccount("1.0.2");
	private AccountID another = asAccount("1.0.300");
	private TransferList transfers = withAdjustments(payer, -2L, created, 1L, another, 1L);
	private TokenID tokenCreated = asToken("3.0.2");
	private ScheduleID scheduleCreated = asSchedule("0.0.10");
	private TokenTransferList tokenTransfers = TokenTransferList.newBuilder()
			.setToken(tokenCreated)
			.addAllTransfers(withAdjustments(payer, -2L, created, 1L, another, 1L).getAccountAmountsList())
			.build();
	private FileID fileCreated = asFile("2.0.1");
	private ContractID contractCreated = asContract("0.1.2");
	private TopicID topicCreated = asTopic("5.4.3");
	private long txnValidStart = now.getEpochSecond() - 1_234L;
	private grameLedger ledger;
	private ItemizableFeeCharging itemizableFeeCharging;
	private AccountID nodeAccount = asAccount("0.0.3");
	private Address address;
	private Address anotherAddress;
	private AddressBook book;
	private HbarCentExchange exchange;
	private ServicesContext ctx;
	private PlatformTxnAccessor accessor;
	private AwareTransactionContext subject;
	private Transaction signedTxn;
	private TransactionBody txn;
	private TransactionRecord record;
	private ExpiringEntity expiringEntity;
	private String memo = "Hi!";
	private ByteString hash = ByteString.copyFrom("fake hash".getBytes());
	private TransactionID txnId = TransactionID.newBuilder()
					.setTransactionValidStart(Timestamp.newBuilder().setSeconds(txnValidStart))
					.setAccountID(payer)
					.build();
	private ContractFunctionResult result = ContractFunctionResult.newBuilder().setContractID(contractCreated).build();
	JKey payerKey;

	@BeforeEach
	private void setup() {
		address = mock(Address.class);
		given(address.getMemo()).willReturn(asAccountString(nodeAccount));
		anotherAddress = mock(Address.class);
		given(anotherAddress.getMemo()).willReturn(asAccountString(anotherNodeAccount));
		book = mock(AddressBook.class);
		given(book.getAddress(memberId)).willReturn(address);
		given(book.getAddress(anotherMemberId)).willReturn(anotherAddress);

		ledger = mock(grameLedger.class);
		given(ledger.netTransfersInTxn()).willReturn(transfers);
		given(ledger.netTokenTransfersInTxn()).willReturn(List.of(tokenTransfers));

		exchange = mock(HbarCentExchange.class);
		given(exchange.activeRates()).willReturn(ratesNow);

		itemizableFeeCharging = mock(ItemizableFeeCharging.class);

		payerKey = mock(JKey.class);
		MerkleAccount payerAccount = mock(MerkleAccount.class);
		given(payerAccount.getKey()).willReturn(payerKey);
		FCMap<MerkleEntityId, MerkleAccount> accounts = mock(FCMap.class);
		given(accounts.get(MerkleEntityId.fromAccountId(payer))).willReturn(payerAccount);

		ctx = mock(ServicesContext.class);
		given(ctx.exchange()).willReturn(exchange);
		given(ctx.ledger()).willReturn(ledger);
		given(ctx.accounts()).willReturn(accounts);
		given(ctx.charging()).willReturn(itemizableFeeCharging);
		given(ctx.accounts()).willReturn(accounts);
		given(ctx.addressBook()).willReturn(book);

		txn = mock(TransactionBody.class);
		given(txn.getMemo()).willReturn(memo);
		signedTxn = mock(Transaction.class);
		given(signedTxn.toByteArray()).willReturn(memo.getBytes());
		accessor = mock(PlatformTxnAccessor.class);
		given(accessor.getTxnId()).willReturn(txnId);
		given(accessor.getTxn()).willReturn(txn);
		given(accessor.getBackwardCompatibleSignedTxn()).willReturn(signedTxn);
		given(accessor.getPayer()).willReturn(payer);
		given(accessor.getHash()).willReturn(hash);

		expiringEntity = mock(ExpiringEntity.class);

		subject = new AwareTransactionContext(ctx);
		subject.resetFor(accessor, now, memberId);
	}

	@Test
	public void throwsOnUpdateIfNoRecordSoFar() {
		// expect:
		assertThrows(
				IllegalStateException.class,
				() -> subject.updatedRecordGiven(withAdjustments(payer, -100, funding, 50, another, 50)));
	}

	@Test
	public void updatesAsExpectedIfRecordSoFar() {
		// setup:
		subject.recordSoFar = mock(TransactionRecord.Builder.class);
		subject.hasComputedRecordSoFar = true;
		// and:
		var expected = mock(TransactionRecord.class);

		// given:
		given(itemizableFeeCharging.totalFeesChargedToPayer()).willReturn(123L);
		var xfers = withAdjustments(payer, -100, funding, 50, another, 50);
		// and:
		given(subject.recordSoFar.build()).willReturn(expected);
		given(subject.recordSoFar.setTransferList(xfers)).willReturn(subject.recordSoFar);

		// when:
		var actual = subject.updatedRecordGiven(xfers);

		// then:
		verify(subject.recordSoFar).setTransferList(xfers);
		verify(subject.recordSoFar).setTransactionFee(123L);
		// and:
		assertSame(expected, actual);
	}

	@Test
	public void throwsIseIfNoPayerActive() {
		// expect:
		assertThrows(IllegalStateException.class, () -> subject.activePayer());
	}

	@Test
	public void returnsPayerIfSigActive() {
		// given:
		subject.payerSigIsKnownActive();

		// expect:
		assertEquals(payer, subject.activePayer());
	}

	@Test
	public void returnsEmptyKeyIfNoPayerActive() {
		// expect:
		assertEquals(EMPTY_KEY, subject.activePayerKey());
	}

	@Test
	public void getsPayerKeyIfSigActive() {
		// given:
		subject.payerSigIsKnownActive();

		// then:
		assertEquals(payerKey, subject.activePayerKey());
	}

	@Test
	public void getsExpectedNodeAccount() {
		// expect:
		assertEquals(nodeAccount, subject.submittingNodeAccount());
	}

	@Test
	public void failsHardForMissingMemberAccount() {
		given(book.getAddress(memberId)).willReturn(null);

		// expect:
		assertThrows(IllegalStateException.class, () -> subject.submittingNodeAccount());
	}

	@Test
	public void resetsRecordSoFar() {
		// given:
		subject.recordSoFar = mock(TransactionRecord.Builder.class);

		// when:
		subject.resetFor(accessor, now, anotherMemberId);

		// then:
		verify(subject.recordSoFar).clear();
	}

	@Test
	public void resetsEverythingElse() {
		// given:
		subject.addNonThresholdFeeChargedToPayer(1_234L);
		subject.setCallResult(result);
		subject.setStatus(ResponseCodeEnum.SUCCESS);
		subject.setCreated(contractCreated);
		subject.payerSigIsKnownActive();
		subject.hasComputedRecordSoFar = true;
		// and:
		assertEquals(memberId, subject.submittingSwirldsMember());
		assertEquals(nodeAccount, subject.submittingNodeAccount());

		// when:
		subject.resetFor(accessor, now, anotherMemberId);
		assertFalse(subject.hasComputedRecordSoFar);
		// and:
		record = subject.recordSoFar();

		// then:
		assertEquals(ResponseCodeEnum.UNKNOWN, record.getReceipt().getStatus());
		assertFalse(record.getReceipt().hasContractID());
		assertEquals(0, record.getTransactionFee());
		assertFalse(record.hasContractCallResult());
		assertFalse(subject.isPayerSigKnownActive());
		assertTrue(subject.hasComputedRecordSoFar);
		assertEquals(anotherNodeAccount, subject.submittingNodeAccount());
		assertEquals(anotherMemberId, subject.submittingSwirldsMember());
		// and:
		verify(itemizableFeeCharging).resetFor(accessor, anotherNodeAccount);
	}

	@Test
	public void effectivePayerIsSubmittingNodeIfNotVerified() {
		// expect:
		assertEquals(nodeAccount, subject.effectivePayer());
	}

	@Test
	public void effectivePayerIsActiveIfVerified() {
		// given:
		subject.payerSigIsKnownActive();

		// expect:
		assertEquals(payer, subject.effectivePayer());
	}

	@Test
	public void getsItemizedRepr() {
		// setup:
		TransferList canonicalAdjustments =
				withAdjustments(payer, -2100, node, 100, funding, 1000, another, 1000);
		TransferList itemizedFees =
				withAdjustments(funding, 900, payer, -900, node, 100, payer, -100);
		// and:
		TransferList desiredRepr = itemizedFees.toBuilder()
				.addAccountAmounts(AccountAmount.newBuilder().setAccountID(payer).setAmount(-1100))
				.addAccountAmounts(AccountAmount.newBuilder().setAccountID(funding).setAmount(100))
				.addAccountAmounts(AccountAmount.newBuilder().setAccountID(another).setAmount(1000))
				.build();

		given(ledger.netTransfersInTxn()).willReturn(canonicalAdjustments);
		given(itemizableFeeCharging.itemizedFees()).willReturn(itemizedFees);

		// when:
		TransferList repr = subject.itemizedRepresentation();

		// then:
		assertEquals(desiredRepr, repr);
	}

	@Test
	public void usesChargingToSetTransactionFee() {
		long std = 1_234L;
		long other = 4_321L;
		given(itemizableFeeCharging.totalFeesChargedToPayer()).willReturn(std);

		// when:
		subject.addNonThresholdFeeChargedToPayer(other);
		record = subject.recordSoFar();

		// then:
		assertEquals(std + other, record.getTransactionFee());
	}

	@Test
	public void usesTokenTransfersToSetApropos() {
		// when:
		record = subject.recordSoFar();

		// then:
		assertEquals(tokenTransfers, record.getTokenTransferLists(0));
	}

	@Test
	public void configuresCallResult() {
		// when:
		subject.setCallResult(result);
		record = subject.recordSoFar();

		// expect:
		assertEquals(result, record.getContractCallResult());
	}

	@Test
	public void configuresCreateResult() {
		// when:
		subject.setCreateResult(result);
		record = subject.recordSoFar();

		// expect:
		assertEquals(result, record.getContractCreateResult());
	}

	@Test
	public void hasTransferList() {
		// expect:
		assertEquals(transfers, subject.recordSoFar().getTransferList());
	}

	@Test
	public void hasExpectedCopyFields() {
		// when:
		TransactionRecord record = subject.recordSoFar();

		// expect:
		assertEquals(memo, record.getMemo());
		assertEquals(hash, record.getTransactionHash());
		assertEquals(txnId, record.getTransactionID());
		assertEquals(timeNow, record.getConsensusTimestamp());
	}

	@Test
	public void hasExpectedPrimitives() {
		// expect:
		assertEquals(accessor, subject.accessor());
		assertEquals(now, subject.consensusTime());
		assertEquals(ResponseCodeEnum.UNKNOWN, subject.status());
	}

	@Test
	public void hasExpectedStatus() {
		// when:
		subject.setStatus(ResponseCodeEnum.INVALID_PAYER_SIGNATURE);

		// then:
		assertEquals(ResponseCodeEnum.INVALID_PAYER_SIGNATURE, subject.status());
	}

	@Test
	public void hasExpectedRecordStatus() {
		// when:
		subject.setStatus(ResponseCodeEnum.INVALID_PAYER_SIGNATURE);
		record = subject.recordSoFar();

		// then:
		assertEquals(ResponseCodeEnum.INVALID_PAYER_SIGNATURE, record.getReceipt().getStatus());
	}

	@Test
	public void getsExpectedReceiptForAccountCreation() {
		// when:
		subject.setCreated(created);
		record = subject.recordSoFar();

		// then:
		assertEquals(ratesNow, record.getReceipt().getExchangeRate());
		assertEquals(created, record.getReceipt().getAccountID());
	}

	@Test
	public void getsExpectedReceiptForTokenCreation() {
		// when:
		subject.setCreated(tokenCreated);
		record = subject.recordSoFar();

		// then:
		assertEquals(ratesNow, record.getReceipt().getExchangeRate());
		assertEquals(tokenCreated, record.getReceipt().getTokenID());
	}

	@Test
	public void getsExpectedReceiptForTokenMintBurnWipe() {
		// when:
		final var newTotalSupply = 1000L;
		subject.setNewTotalSupply(newTotalSupply);
		record = subject.recordSoFar();

		// then:
		assertEquals(ratesNow, record.getReceipt().getExchangeRate());
		assertEquals(newTotalSupply, record.getReceipt().getNewTotalSupply());
	}



	@Test
	public void getsExpectedReceiptForFileCreation() {
		// when:
		subject.setCreated(fileCreated);
		record = subject.recordSoFar();

		// then:
		assertEquals(ratesNow, record.getReceipt().getExchangeRate());
		assertEquals(fileCreated, record.getReceipt().getFileID());
	}

	@Test
	public void getsExpectedReceiptForContractCreation() {
		// when:
		subject.setCreated(contractCreated);
		record = subject.recordSoFar();

		// then:
		assertEquals(ratesNow, record.getReceipt().getExchangeRate());
		assertEquals(contractCreated, record.getReceipt().getContractID());
	}

	@Test
	public void getsExpectedReceiptForTopicCreation() {
		// when:
		subject.setCreated(topicCreated);
		record = subject.recordSoFar();

		// then:
		assertEquals(ratesNow, record.getReceipt().getExchangeRate());
		assertEquals(topicCreated, record.getReceipt().getTopicID());
	}

	@Test
	public void getsExpectedReceiptForSubmitMessage() {
		var sequenceNumber = 1000L;
		var runningHash = new byte[11];

		// when:
		subject.setTopicRunningHash(runningHash, sequenceNumber);
		record = subject.recordSoFar();

		// then:
		assertEquals(ratesNow, record.getReceipt().getExchangeRate());
		assertArrayEquals(runningHash, record.getReceipt().getTopicRunningHash().toByteArray());
		assertEquals(sequenceNumber, record.getReceipt().getTopicSequenceNumber());
		assertEquals(MerkleTopic.RUNNING_HASH_VERSION, record.getReceipt().getTopicRunningHashVersion());
	}

	@Test
	public void getsExpectedReceiptForSuccessfulScheduleOps() {
		// when:
		subject.setCreated(scheduleCreated);
		subject.setScheduledTxnId(scheduledTxnId);
		// and:
		record = subject.recordSoFar();

		// then:
		assertEquals(scheduleCreated, record.getReceipt().getScheduleID());
		assertEquals(scheduledTxnId, record.getReceipt().getScheduledTransactionID());
	}

	@Test
	public void startsWithoutKnownValidPayerSig() {
		// expect:
		assertFalse(subject.isPayerSigKnownActive());
	}

	@Test
	public void setsSigToKnownValid() {
		// given:
		subject.payerSigIsKnownActive();

		// expect:
		assertTrue(subject.isPayerSigKnownActive());
	}

	@Test
	public void triggersTxn() {
		// when:
		subject.trigger(accessor);
		// then:
		assertEquals(subject.triggeredTxn(), accessor);
	}

	@Test
	public void getsExpectedRecordForTriggeredTxn() {
		// given:
		given(accessor.getScheduleRef()).willReturn(scheduleCreated);
		given(accessor.isTriggeredTxn()).willReturn(true);

		// when:
		record = subject.recordSoFar();

		// then:
		assertEquals(scheduleCreated, record.getScheduleRef());
	}

	@Test
	public void addsExpiringEntities() {
		// given:
		var expected = Collections.singletonList(expiringEntity);
		// when:
		subject.addExpiringEntities(expected);

		// then:
		assertEquals(subject.expiringEntities(), expected);
	}

	@Test
	public void throwsIfAccessorIsAlreadyTriggered() {
		// given:
		given(accessor.getScheduleRef()).willReturn(scheduleCreated);
		given(accessor.isTriggeredTxn()).willReturn(true);

		// when:
		assertThrows(IllegalStateException.class, () -> subject.trigger(accessor));
	}
}
