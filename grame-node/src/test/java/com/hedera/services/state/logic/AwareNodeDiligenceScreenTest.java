package com.grame.services.state.logic;

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

import com.google.protobuf.InvalidProtocolBufferException;
import com.grame.services.context.TransactionContext;
import com.grame.services.ledger.accounts.BackingStore;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.txns.validation.OptionValidator;
import com.grame.services.utils.SignedTxnAccessor;
import com.grame.services.utils.TxnAccessor;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.Duration;
import com.gramegrame.api.proto.java.SignedTransaction;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.grame.services.state.logic.AwareNodeDiligenceScreen.MISSING_NODE_LOG_TPL;
import static com.grame.services.state.logic.AwareNodeDiligenceScreen.WRONG_NODE_LOG_TPL;
import static com.grame.services.txns.diligence.DuplicateClassification.BELIEVED_UNIQUE;
import static com.grame.services.txns.diligence.DuplicateClassification.NODE_DUPLICATE;
import static com.grame.services.utils.EntityIdUtils.readableId;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.DUPLICATE_TRANSACTION;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_NODE_ACCOUNT;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_PAYER_SIGNATURE;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TRANSACTION_DURATION;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_ZERO_BYTE_IN_STRING;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.TRANSACTION_EXPIRED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.*;

import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class AwareNodeDiligenceScreenTest {
	long submittingMember = 2L;
	String pretendMemo = "ignored";
	Instant consensusTime = Instant.ofEpochSecond(1_234_567L);
	AccountID aNodeAccount = IdUtils.asAccount("0.0.3");
	AccountID bNodeAccount = IdUtils.asAccount("0.0.4");
	TxnAccessor accessor;
	Duration validDuration = Duration.newBuilder().setSeconds(1_234_567L).build();

	@Mock
	Logger mockLog;
	@Mock
	TransactionContext txnCtx;
	@Mock
	OptionValidator validator;
	@Mock
	BackingStore<AccountID, MerkleAccount> backingAccounts;

	AwareNodeDiligenceScreen subject;

	@BeforeEach
	void setUp() {
		subject = new AwareNodeDiligenceScreen(validator, txnCtx, backingAccounts);

		AwareNodeDiligenceScreen.log = mockLog;
	}

	@Test
	void flagsMissingNodeAccount() throws InvalidProtocolBufferException {
		givenHandleCtx(aNodeAccount, aNodeAccount);
		given(txnCtx.submittingSwirldsMember()).willReturn(submittingMember);
		given(backingAccounts.contains(aNodeAccount)).willReturn(false);

		// then:
		assertTrue(subject.nodeIgnoredDueDiligence(BELIEVED_UNIQUE));
		// and:
		verify(txnCtx).setStatus(INVALID_NODE_ACCOUNT);
		// and:
		verify(mockLog).warn(
				MISSING_NODE_LOG_TPL,
				readableId(aNodeAccount),
				submittingMember,
				readableId(aNodeAccount),
				accessor.getSignedTxn4Log());

	}

	@Test
	void flagsNodeSubmittingTxnWithDiffNodeAccountId() throws InvalidProtocolBufferException {
		givenHandleCtx(bNodeAccount, aNodeAccount);
		given(txnCtx.submittingSwirldsMember()).willReturn(submittingMember);
		given(backingAccounts.contains(aNodeAccount)).willReturn(true);

		// then:
		assertTrue(subject.nodeIgnoredDueDiligence(BELIEVED_UNIQUE));
		// and:
		verify(txnCtx).setStatus(INVALID_NODE_ACCOUNT);
		// and:
		verify(mockLog).warn(
				WRONG_NODE_LOG_TPL,
				readableId(bNodeAccount),
				submittingMember,
				readableId(aNodeAccount),
				accessor.getSignedTxn4Log());

	}

	@Test
	void flagsInvalidPayerSig() throws InvalidProtocolBufferException {
		givenHandleCtx(aNodeAccount, aNodeAccount);
		given(backingAccounts.contains(aNodeAccount)).willReturn(true);
		given(txnCtx.isPayerSigKnownActive()).willReturn(false);

		// then:
		assertTrue(subject.nodeIgnoredDueDiligence(BELIEVED_UNIQUE));
		// and:
		verify(txnCtx).setStatus(INVALID_PAYER_SIGNATURE);
	}

	@Test
	void flagsNodeDuplicate() throws InvalidProtocolBufferException {
		givenHandleCtx(aNodeAccount, aNodeAccount);
		given(backingAccounts.contains(aNodeAccount)).willReturn(true);
		given(txnCtx.isPayerSigKnownActive()).willReturn(true);

		// then:
		assertTrue(subject.nodeIgnoredDueDiligence(NODE_DUPLICATE));
		// and:
		verify(txnCtx).setStatus(DUPLICATE_TRANSACTION);
	}

	@Test
	void flagsInvalidDuration() throws InvalidProtocolBufferException {
		givenHandleCtx(aNodeAccount, aNodeAccount);
		given(backingAccounts.contains(aNodeAccount)).willReturn(true);
		given(txnCtx.isPayerSigKnownActive()).willReturn(true);
		given(validator.isValidTxnDuration(validDuration.getSeconds())).willReturn(false);

		// then:
		assertTrue(subject.nodeIgnoredDueDiligence(BELIEVED_UNIQUE));
		// and:
		verify(txnCtx).setStatus(INVALID_TRANSACTION_DURATION);
	}

	@Test
	void flagsInvalidChronology() throws InvalidProtocolBufferException {
		givenHandleCtx(aNodeAccount, aNodeAccount);
		given(backingAccounts.contains(aNodeAccount)).willReturn(true);
		given(txnCtx.isPayerSigKnownActive()).willReturn(true);
		given(validator.isValidTxnDuration(validDuration.getSeconds())).willReturn(true);
		given(validator.chronologyStatus(accessor, consensusTime)).willReturn(TRANSACTION_EXPIRED);
		given(txnCtx.consensusTime()).willReturn(consensusTime);

		// then:
		assertTrue(subject.nodeIgnoredDueDiligence(BELIEVED_UNIQUE));
		// and:
		verify(txnCtx).setStatus(TRANSACTION_EXPIRED);
	}

	@Test
	void flagsInvalidMemo() throws InvalidProtocolBufferException {
		givenHandleCtx(aNodeAccount, aNodeAccount);
		given(backingAccounts.contains(aNodeAccount)).willReturn(true);
		given(txnCtx.isPayerSigKnownActive()).willReturn(true);
		given(validator.isValidTxnDuration(validDuration.getSeconds())).willReturn(true);
		given(validator.chronologyStatus(accessor, consensusTime)).willReturn(OK);
		given(txnCtx.consensusTime()).willReturn(consensusTime);
		given(validator.memoCheck(pretendMemo)).willReturn(INVALID_ZERO_BYTE_IN_STRING);

		// then:
		assertTrue(subject.nodeIgnoredDueDiligence(BELIEVED_UNIQUE));
		// and:
		verify(txnCtx).setStatus(INVALID_ZERO_BYTE_IN_STRING);
	}

	@Test
	void doesntFlagWithAllOk() throws InvalidProtocolBufferException {
		givenHandleCtx(aNodeAccount, aNodeAccount);
		given(backingAccounts.contains(aNodeAccount)).willReturn(true);
		given(txnCtx.isPayerSigKnownActive()).willReturn(true);
		given(validator.isValidTxnDuration(validDuration.getSeconds())).willReturn(true);
		given(validator.chronologyStatus(accessor, consensusTime)).willReturn(OK);
		given(txnCtx.consensusTime()).willReturn(consensusTime);
		given(validator.memoCheck(pretendMemo)).willReturn(OK);

		// then:
		assertFalse(subject.nodeIgnoredDueDiligence(BELIEVED_UNIQUE));
		// and:
		verify(txnCtx, never()).setStatus(any());
	}

	private void givenHandleCtx(
			AccountID submittingNodeAccount,
			AccountID designatedNodeAccount
	) throws InvalidProtocolBufferException {
		given(txnCtx.submittingNodeAccount()).willReturn(submittingNodeAccount);
		accessor = accessorWith(designatedNodeAccount);
		given(txnCtx.accessor()).willReturn(accessor);
	}

	private TxnAccessor accessorWith(AccountID designatedNodeAccount) throws InvalidProtocolBufferException {
		var bodyBytes = TransactionBody.newBuilder()
				.setMemo(pretendMemo)
				.setTransactionValidDuration(validDuration)
				.setNodeAccountID(designatedNodeAccount)
				.build()
				.toByteString();
		var signedTxn = Transaction.newBuilder()
				.setSignedTransactionBytes(SignedTransaction.newBuilder()
						.setBodyBytes(bodyBytes)
						.build()
						.toByteString())
				.build();
		return new SignedTxnAccessor(signedTxn);
	}
}
