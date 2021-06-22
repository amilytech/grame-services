package com.grame.services.txns.submission;

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

import static com.gramegrame.api.proto.java.grameFunctionality.CryptoTransfer;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INSUFFICIENT_PAYER_BALANCE;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_ACCOUNT_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_NODE_ACCOUNT;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TRANSACTION_BODY;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.NOT_SUPPORTED;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.PLATFORM_TRANSACTION_NOT_CREATED;
import static com.grame.services.context.ServicesNodeType.*;

import com.google.protobuf.ByteString;
import com.grame.services.legacy.proto.utils.CommonUtils;
import com.grame.services.txns.TransitionLogic;
import com.grame.services.txns.TransitionLogicLookup;
import com.gramegrame.api.proto.java.CryptoTransferTransactionBody;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.SignedTransaction;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionID;
import com.gramegrame.api.proto.java.TransactionResponse;
import com.grame.services.context.domain.process.TxnValidityAndFeeReq;
import com.grame.services.legacy.handler.TransactionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.grame.test.utils.IdUtils.asAccount;
import static org.mockito.BDDMockito.*;

class TxnHandlerSubmissionFlowTest {
	private static final byte[] NONSENSE = "Jabberwocky".getBytes();

	long feeRequired = 1_234L;
	TransactionID txnId = TransactionID.newBuilder().setAccountID(asAccount("0.0.2")).build();
	Transaction signedTxn = Transaction.newBuilder()
			.setBodyBytes(TransactionBody.newBuilder()
					.setCryptoTransfer(CryptoTransferTransactionBody.getDefaultInstance())
					.setTransactionID(txnId)
					.build().toByteString())
			.build();
	TxnValidityAndFeeReq okMeta = new TxnValidityAndFeeReq(OK);

	private TransitionLogic logic;
	private TransactionHandler txnHandler;
	private TransitionLogicLookup logicLookup;
	private PlatformSubmissionManager submissionManager;
	private Function<TransactionBody, ResponseCodeEnum> syntaxCheck;

	private TxnHandlerSubmissionFlow subject;

	// Create second transaction using signedTransactionBytes
	SignedTransaction newSignedTxn = SignedTransaction.newBuilder().
			setBodyBytes(TransactionBody.newBuilder()
					.setCryptoTransfer(CryptoTransferTransactionBody.getDefaultInstance()).
							setTransactionID(txnId).build().toByteString()).
			build();

	Transaction newTxn = Transaction.newBuilder()
			.setSignedTransactionBytes(newSignedTxn.toByteString())
			.build();

	@BeforeEach
	private void setup() throws Exception {
		logic = mock(TransitionLogic.class);
		txnHandler = mock(TransactionHandler.class);
		syntaxCheck = mock(Function.class);
		given(logic.syntaxCheck()).willReturn(syntaxCheck);
		logicLookup = mock(TransitionLogicLookup.class);
		given(logicLookup.lookupFor(CryptoTransfer, CommonUtils.extractTransactionBody(signedTxn))).willReturn(Optional.of(logic));
		submissionManager = mock(PlatformSubmissionManager.class);

		subject = new TxnHandlerSubmissionFlow(STAKED_NODE, txnHandler, logicLookup, submissionManager);

		given(logicLookup.lookupFor(CryptoTransfer, CommonUtils.extractTransactionBody(newTxn))).willReturn(Optional.of(logic));
	}

	@Test
	public void rejectsAllTxnsOnZeroStakeNode() {
		// given:
		subject = new TxnHandlerSubmissionFlow(ZERO_STAKE_NODE, txnHandler, logicLookup, submissionManager);

		// when:
		TransactionResponse response = subject.submit(Transaction.getDefaultInstance());

		// then:
		assertEquals(INVALID_NODE_ACCOUNT, response.getNodeTransactionPrecheckCode());
	}

	@Test
	public void rejectsNonsenseTransaction() {
		// given:
		Transaction signedNonsenseTxn = Transaction.newBuilder()
				.setBodyBytes(ByteString.copyFrom(NONSENSE))
				.build();

		// when:
		TransactionResponse response = subject.submit(signedNonsenseTxn);

		// then:
		assertEquals(INVALID_TRANSACTION_BODY, response.getNodeTransactionPrecheckCode());
	}

	@Test
	public void shortCircuitsOnInvalidMeta() {
		// setup:
		TxnValidityAndFeeReq metaValidity = new TxnValidityAndFeeReq(INSUFFICIENT_PAYER_BALANCE, feeRequired);

		given(txnHandler.validateTransactionPreConsensus(signedTxn, false)).willReturn(metaValidity);

		// when:
		TransactionResponse response = subject.submit(signedTxn);

		// then:
		assertEquals(INSUFFICIENT_PAYER_BALANCE, response.getNodeTransactionPrecheckCode());
		assertEquals(feeRequired, response.getCost());
	}

	@Test
	public void rejectsInvalidSyntax() {
		given(txnHandler.validateTransactionPreConsensus(signedTxn, false)).willReturn(okMeta);
		given(syntaxCheck.apply(any())).willReturn(INVALID_ACCOUNT_ID);

		// when:
		TransactionResponse response = subject.submit(signedTxn);

		// then:
		assertEquals(INVALID_ACCOUNT_ID, response.getNodeTransactionPrecheckCode());
	}

	@Test
	public void catchesPlatformCreateEx() throws Exception {
		given(txnHandler.validateTransactionPreConsensus(signedTxn, false)).willReturn(okMeta);
		given(syntaxCheck.apply(any())).willReturn(OK);
		given(submissionManager.trySubmission(any())).willReturn(PLATFORM_TRANSACTION_NOT_CREATED);

		// when:
		TransactionResponse response = subject.submit(signedTxn);

		// then:
		assertEquals(PLATFORM_TRANSACTION_NOT_CREATED, response.getNodeTransactionPrecheckCode());
	}

	@Test
	public void followsHappyPathToOk() throws Exception {
		given(txnHandler.validateTransactionPreConsensus(signedTxn, false)).willReturn(okMeta);
		given(syntaxCheck.apply(any())).willReturn(OK);
		given(submissionManager.trySubmission(any())).willReturn(OK);

		// when:
		TransactionResponse response = subject.submit(signedTxn);

		// then:
		assertEquals(OK, response.getNodeTransactionPrecheckCode());
	}

	@Test
	public void usesFallbackSyntaxCheckIfNotSupported() throws Exception {
		given(txnHandler.validateTransactionPreConsensus(signedTxn, false)).willReturn(okMeta);
		given(logicLookup.lookupFor(any(), any())).willReturn(Optional.empty());

		// when:
		TransactionResponse response = subject.submit(signedTxn);

		// then:
		assertEquals(NOT_SUPPORTED, response.getNodeTransactionPrecheckCode());
	}

	@Test
	public void shortCircuitsSignedTxnOnInvalidMeta() {
		// setup:
		TxnValidityAndFeeReq metaValidity = new TxnValidityAndFeeReq(INSUFFICIENT_PAYER_BALANCE, feeRequired);

		given(txnHandler.validateTransactionPreConsensus(newTxn, false)).willReturn(metaValidity);

		// when:
		TransactionResponse response = subject.submit(newTxn);

		// then:
		assertEquals(INSUFFICIENT_PAYER_BALANCE, response.getNodeTransactionPrecheckCode());
		assertEquals(feeRequired, response.getCost());
	}

	@Test
	public void followsSignedTxnHappyPathToOk() throws Exception {
		given(txnHandler.validateTransactionPreConsensus(newTxn, false)).willReturn(okMeta);
		given(syntaxCheck.apply(any())).willReturn(OK);
		given(submissionManager.trySubmission(any())).willReturn(OK);

		// when:
		TransactionResponse response = subject.submit(newTxn);

		// then:
		assertEquals(OK, response.getNodeTransactionPrecheckCode());
	}
}
