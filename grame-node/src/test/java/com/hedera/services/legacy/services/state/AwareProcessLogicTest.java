package com.grame.services.legacy.services.state;

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
import com.grame.services.context.ServicesContext;
import com.grame.services.context.TransactionContext;
import com.grame.services.fees.FeeCalculator;
import com.grame.services.fees.charging.TxnFeeChargingPolicy;
import com.grame.services.files.grameFs;
import com.grame.services.ledger.grameLedger;
import com.grame.services.ledger.accounts.BackingStore;
import com.grame.services.legacy.handler.SmartContractRequestHandler;
import com.grame.services.records.AccountRecordsHistorian;
import com.grame.services.records.TxnIdRecentHistory;
import com.grame.services.security.ops.SystemOpAuthorization;
import com.grame.services.security.ops.SystemOpPolicies;
import com.grame.services.sigs.order.grameSigningOrder;
import com.grame.services.sigs.order.SigningOrderResult;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.stats.MiscRunningAvgs;
import com.grame.services.stats.MiscSpeedometers;
import com.grame.services.stream.RecordStreamManager;
import com.grame.services.stream.RecordStreamObject;
import com.grame.services.txns.TransitionLogicLookup;
import com.grame.services.txns.validation.OptionValidator;
import com.grame.services.utils.PlatformTxnAccessor;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.SignedTransaction;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.Duration;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionID;
import com.gramegrame.api.proto.java.TransactionRecord;
import com.swirlds.common.Address;
import com.swirlds.common.AddressBook;
import com.swirlds.common.Transaction;
import com.swirlds.common.crypto.RunningHash;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.grame.services.txns.diligence.DuplicateClassification.BELIEVED_UNIQUE;
import static org.mockito.BDDMockito.*;

class AwareProcessLogicTest {
	Logger mockLog;
	Transaction platformTxn;
	AddressBook book;
	ServicesContext ctx;
	TransactionContext txnCtx;
	TransactionBody txnBody;
	TransactionBody nonMockTxnBody;
	SmartContractRequestHandler contracts;
	grameFs hfs;

	AwareProcessLogic subject;

	@BeforeEach
	public void setup() {
		final Transaction txn = mock(Transaction.class);
		final PlatformTxnAccessor txnAccessor = mock(PlatformTxnAccessor.class);
		final grameLedger ledger = mock(grameLedger.class);
		final AccountRecordsHistorian historian = mock(AccountRecordsHistorian.class);
		final grameSigningOrder keyOrder = mock(grameSigningOrder.class);
		final SigningOrderResult orderResult = mock(SigningOrderResult.class);
		final MiscRunningAvgs runningAvgs = mock(MiscRunningAvgs.class);
		final MiscSpeedometers speedometers = mock(MiscSpeedometers.class);
		final FeeCalculator fees = mock(FeeCalculator.class);
		final TxnIdRecentHistory recentHistory = mock(TxnIdRecentHistory.class);
		final Map<TransactionID, TxnIdRecentHistory> histories = mock(Map.class);
		final BackingStore<AccountID, MerkleAccount> backingAccounts = mock(BackingStore.class);
		final AccountID accountID = mock(AccountID.class);
		final OptionValidator validator = mock(OptionValidator.class);
		final TxnFeeChargingPolicy policy = mock(TxnFeeChargingPolicy.class);
		final SystemOpPolicies policies = mock(SystemOpPolicies.class);
		final TransitionLogicLookup lookup = mock(TransitionLogicLookup.class);
		hfs = mock(grameFs.class);

		given(histories.get(any())).willReturn(recentHistory);

		txnCtx = mock(TransactionContext.class);
		ctx = mock(ServicesContext.class);
		txnBody = mock(TransactionBody.class);
		contracts = mock(SmartContractRequestHandler.class);
		mockLog = mock(Logger.class);
		nonMockTxnBody = TransactionBody.newBuilder()
				.setTransactionID(TransactionID.newBuilder()
								.setAccountID(IdUtils.asAccount("0.0.2"))).build();
		platformTxn = new Transaction(com.gramegrame.api.proto.java.Transaction.newBuilder()
				.setBodyBytes(nonMockTxnBody.toByteString())
				.build().toByteArray());

		AwareProcessLogic.log = mockLog;

		var zeroStakeAddress = mock(Address.class);
		given(zeroStakeAddress.getStake()).willReturn(0L);
		var stakedAddress = mock(Address.class);
		given(stakedAddress.getStake()).willReturn(1L);
		book = mock(AddressBook.class);
		given(book.getAddress(1)).willReturn(stakedAddress);
		given(book.getAddress(666L)).willReturn(zeroStakeAddress);
		given(ctx.addressBook()).willReturn(book);
		given(ctx.ledger()).willReturn(ledger);
		given(ctx.txnCtx()).willReturn(txnCtx);
		given(ctx.recordsHistorian()).willReturn(historian);
		given(ctx.backedKeyOrder()).willReturn(keyOrder);
		given(ctx.runningAvgs()).willReturn(runningAvgs);
		given(ctx.speedometers()).willReturn(speedometers);
		given(ctx.fees()).willReturn(fees);
		given(ctx.txnHistories()).willReturn(histories);
		given(ctx.backingAccounts()).willReturn(backingAccounts);
		given(ctx.validator()).willReturn(validator);
		given(ctx.txnChargingPolicy()).willReturn(policy);
		given(ctx.systemOpPolicies()).willReturn(policies);
		given(ctx.transitionLogic()).willReturn(lookup);
		given(ctx.hfs()).willReturn(hfs);
		given(ctx.contracts()).willReturn(contracts);

		given(txnCtx.accessor()).willReturn(txnAccessor);
		given(txnCtx.submittingNodeAccount()).willReturn(accountID);
		given(txnCtx.isPayerSigKnownActive()).willReturn(true);
		given(txnAccessor.getPlatformTxn()).willReturn(txn);

		given(txn.getSignatures()).willReturn(Collections.emptyList());
		given(keyOrder.keysForPayer(any(), any())).willReturn(orderResult);
		given(keyOrder.keysForOtherParties(any(), any())).willReturn(orderResult);

		final com.gramegrame.api.proto.java.Transaction signedTxn = mock(com.gramegrame.api.proto.java.Transaction.class);
		final TransactionID txnId = mock(TransactionID.class);

		given(txnAccessor.getBackwardCompatibleSignedTxn()).willReturn(signedTxn);
		given(signedTxn.getSignedTransactionBytes()).willReturn(ByteString.EMPTY);
		given(txnAccessor.getTxn()).willReturn(txnBody);
		given(txnBody.getTransactionID()).willReturn(txnId);
		given(txnBody.getTransactionValidDuration()).willReturn(Duration.getDefaultInstance());

		given(recentHistory.currentDuplicityFor(anyLong())).willReturn(BELIEVED_UNIQUE);
		given(backingAccounts.contains(any())).willReturn(true);

		given(validator.isValidTxnDuration(anyLong())).willReturn(true);
		given(validator.chronologyStatus(any(), any())).willReturn(ResponseCodeEnum.OK);
		given(validator.isValidAutoRenewPeriod(any())).willReturn(true);

		given(txnBody.getNodeAccountID()).willReturn(accountID);
		given(policy.apply(any(), any())).willReturn(ResponseCodeEnum.OK);
		given(policies.check(any())).willReturn(SystemOpAuthorization.AUTHORIZED);
		given(lookup.lookupFor(any(), any())).willReturn(Optional.empty());
		given(hfs.exists(any())).willReturn(true);

		subject = new AwareProcessLogic(ctx);
	}

	@AfterEach
	public void cleanup() {
		AwareProcessLogic.log = LogManager.getLogger(AwareProcessLogic.class);
	}

	@Test
	public void shortCircuitsWithWarningOnZeroStakeSubmission() {
		// setup:
		var now = Instant.now();
		var then = now.minusMillis(1L);

		given(ctx.consensusTimeOfLastHandledTxn()).willReturn(then);

		// when:
		subject.incorporateConsensusTxn(platformTxn, now, 666);

		// then:
		verify(mockLog).warn(argThat((String s) -> s.startsWith("Ignoring a transaction submitted by zero-stake")));
	}

	@Test
	public void shortCircuitsWithErrorOnNonIncreasingConsensusTime() {
		// setup:
		var now = Instant.now();

		given(ctx.consensusTimeOfLastHandledTxn()).willReturn(now);

		// when:
		subject.incorporateConsensusTxn(platformTxn, now,1);

		// then:
		verify(mockLog).error(argThat((String s) -> s.startsWith("Catastrophic invariant failure!")));
	}

	@Test
	public void shortCircuitsWithWarningOnZeroStakeSignedTxnSubmission() {
		// setup:
		var now = Instant.now();
		var then = now.minusMillis(1L);
		SignedTransaction signedTxn = SignedTransaction.newBuilder().setBodyBytes(nonMockTxnBody.toByteString()).build();
		Transaction platformSignedTxn = new Transaction(com.gramegrame.api.proto.java.Transaction.newBuilder().
				setSignedTransactionBytes(signedTxn.toByteString()).build().toByteArray());

		given(ctx.consensusTimeOfLastHandledTxn()).willReturn(then);

		// when:
		subject.incorporateConsensusTxn(platformSignedTxn, now, 666);

		// then:
		verify(mockLog).warn(argThat((String s) -> s.startsWith("Ignoring a transaction submitted by zero-stake")));
	}

	@Test
	public void addForStreamingTest() {
		//setup:
		RecordStreamManager recordStreamManager = mock(RecordStreamManager.class);
		when(ctx.recordStreamManager()).thenReturn(recordStreamManager);

		//when:
		subject.addForStreaming(mock(com.gramegrame.api.proto.java.Transaction.class),
				mock(TransactionRecord.class), Instant.now());
		//then:
		verify(ctx).updateRecordRunningHash(any(RunningHash.class));
		verify(recordStreamManager).addRecordStreamObject(any(RecordStreamObject.class));
	}
}
