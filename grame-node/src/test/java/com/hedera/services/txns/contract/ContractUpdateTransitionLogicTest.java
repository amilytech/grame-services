package com.grame.services.txns.contract;

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

import com.google.protobuf.StringValue;
import com.grame.services.context.TransactionContext;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.txns.validation.OptionValidator;
import com.grame.services.utils.PlatformTxnAccessor;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.ContractID;
import com.gramegrame.api.proto.java.ContractUpdateTransactionBody;
import com.gramegrame.api.proto.java.Duration;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionID;
import com.gramegrame.api.proto.java.TransactionReceipt;
import com.gramegrame.api.proto.java.TransactionRecord;
import com.swirlds.fcmap.FCMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.AUTORENEW_DURATION_NOT_IN_RANGE;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.CONTRACT_DELETED;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.FAIL_INVALID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_RENEWAL_PERIOD;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_ZERO_BYTE_IN_STRING;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.MEMO_TOO_LONG;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.MODIFYING_IMMUTABLE_CONTRACT;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.SUCCESS;
import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;

public class ContractUpdateTransitionLogicTest {
	final private AccountID proxy = AccountID.newBuilder().setAccountNum(4_321L).build();
	final private AccountID payer = AccountID.newBuilder().setAccountNum(1_234L).build();
	private long customAutoRenewPeriod = 100_001L;
	final private ContractID target = ContractID.newBuilder().setContractNum(9_999L).build();
	final private String memo = "Who, me?";

	private Instant consensusTime;
	private OptionValidator validator;
	private ContractUpdateTransitionLogic.LegacyUpdater delegate;
	private TransactionBody contractUpdateTxn;
	private TransactionContext txnCtx;
	private PlatformTxnAccessor accessor;
	FCMap<MerkleEntityId, MerkleAccount> contracts;
	ContractUpdateTransitionLogic subject;

	@BeforeEach
	private void setup() {
		consensusTime = Instant.now();

		delegate = mock(ContractUpdateTransitionLogic.LegacyUpdater.class);
		txnCtx = mock(TransactionContext.class);
		given(txnCtx.consensusTime()).willReturn(consensusTime);
		accessor = mock(PlatformTxnAccessor.class);
		validator = mock(OptionValidator.class);
		withRubberstampingValidator();

		subject = new ContractUpdateTransitionLogic(delegate, validator, txnCtx, () -> contracts);
	}

	@Test
	public void hasCorrectApplicability() {
		givenValidTxnCtx();

		// expect:
		assertTrue(subject.applicability().test(contractUpdateTxn));
		assertFalse(subject.applicability().test(TransactionBody.getDefaultInstance()));
	}

	@Test
	public void rejectsInvalidMemoInSyntaxCheck() {
		givenValidTxnCtx();
		// and:
		given(validator.memoCheck(any())).willReturn(INVALID_ZERO_BYTE_IN_STRING);

		// expect:
		assertEquals(INVALID_ZERO_BYTE_IN_STRING, subject.syntaxCheck().apply(contractUpdateTxn));
	}

	@Test
	public void capturesUnsuccessfulUpdate() {
		// setup:
		TransactionRecord updateRec = TransactionRecord.newBuilder()
				.setReceipt(TransactionReceipt.newBuilder()
						.setStatus(MODIFYING_IMMUTABLE_CONTRACT)
						.build())
				.build();

		givenValidTxnCtx();
		// and:
		given(delegate.perform(contractUpdateTxn, consensusTime)).willReturn(updateRec);

		// when:
		subject.doStateTransition();

		// then:
		verify(txnCtx).setStatus(MODIFYING_IMMUTABLE_CONTRACT);
	}

	@Test
	public void followsHappyPathWithOverrides() {
		// setup:
		TransactionRecord updateRec = TransactionRecord.newBuilder()
				.setReceipt(TransactionReceipt.newBuilder()
						.setStatus(SUCCESS)
						.build())
				.build();

		givenValidTxnCtx();
		// and:
		given(delegate.perform(contractUpdateTxn, consensusTime)).willReturn(updateRec);

		// when:
		subject.doStateTransition();

		// then:
		verify(txnCtx).setStatus(SUCCESS);
	}

	@Test
	public void acceptsOkSyntax() {
		givenValidTxnCtx();

		// expect:
		assertEquals(OK, subject.syntaxCheck().apply(contractUpdateTxn));
	}

	@Test
	public void acceptsOmittedAutoRenew() {
		givenValidTxnCtx(false);

		// expect:
		assertEquals(OK, subject.syntaxCheck().apply(contractUpdateTxn));
	}

	@Test
	public void rejectsInvalidCid() {
		givenValidTxnCtx();
		// and:
		given(validator.queryableContractStatus(target, contracts)).willReturn(CONTRACT_DELETED);

		// expect:
		assertEquals(CONTRACT_DELETED, subject.syntaxCheck().apply(contractUpdateTxn));
	}

	@Test
	public void rejectsInvalidAutoRenew() {
		// setup:
		customAutoRenewPeriod = -1;

		givenValidTxnCtx();

		// expect:
		assertEquals(INVALID_RENEWAL_PERIOD, subject.syntaxCheck().apply(contractUpdateTxn));
	}

	@Test
	public void rejectsOutOfRangeAutoRenew() {
		givenValidTxnCtx();
		// and:
		given(validator.isValidAutoRenewPeriod(any())).willReturn(false);

		// expect:
		assertEquals(AUTORENEW_DURATION_NOT_IN_RANGE, subject.syntaxCheck().apply(contractUpdateTxn));
	}

	@Test
	public void translatesUnknownException() {
		givenValidTxnCtx();

		given(delegate.perform(any(), any())).willThrow(IllegalStateException.class);

		// when:
		subject.doStateTransition();

		// then:
		verify(txnCtx).setStatus(FAIL_INVALID);
	}

	private void givenValidTxnCtx() {
		givenValidTxnCtx(true);
	}

	private void givenValidTxnCtx(boolean useAutoRenew) {
		Duration autoRenewDuration = Duration.newBuilder().setSeconds(customAutoRenewPeriod).build();
		var op = TransactionBody.newBuilder()
				.setTransactionID(ourTxnId())
				.setContractUpdateInstance(
						ContractUpdateTransactionBody.newBuilder()
								.setMemo(memo)
								.setContractID(target)
								.setProxyAccountID(proxy));
		if (useAutoRenew) {
			op.getContractUpdateInstanceBuilder().setAutoRenewPeriod(autoRenewDuration);
			op.getContractUpdateInstanceBuilder().setMemoWrapper(StringValue.newBuilder().setValue(memo));
		}
		contractUpdateTxn = op.build();
		given(accessor.getTxn()).willReturn(contractUpdateTxn);
		given(txnCtx.accessor()).willReturn(accessor);
	}

	private TransactionID ourTxnId() {
		return TransactionID.newBuilder()
				.setAccountID(payer)
				.setTransactionValidStart(
						Timestamp.newBuilder().setSeconds(consensusTime.getEpochSecond()))
				.build();
	}

	private void withRubberstampingValidator() {
		Duration autoRenewDuration = Duration.newBuilder().setSeconds(customAutoRenewPeriod).build();
		given(validator.queryableContractStatus(target, contracts)).willReturn(OK);
		given(validator.isValidAutoRenewPeriod(autoRenewDuration)).willReturn(true);
		given(validator.memoCheck(memo)).willReturn(OK);
	}
}
