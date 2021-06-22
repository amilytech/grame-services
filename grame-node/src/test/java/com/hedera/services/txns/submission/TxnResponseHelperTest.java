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

import com.grame.services.stats.HapiOpCounters;
import com.grame.services.txns.SubmissionFlow;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static com.gramegrame.api.proto.java.grameFunctionality.CryptoTransfer;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.inOrder;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;

class TxnResponseHelperTest {
	Transaction txn = Transaction.getDefaultInstance();
	TransactionResponse okResponse;
	TransactionResponse notOkResponse;

	SubmissionFlow submissionFlow;
	HapiOpCounters opCounters;
	StreamObserver<TransactionResponse> observer;
	TxnResponseHelper subject;

	@BeforeEach
	private void setup() {
		submissionFlow = mock(SubmissionFlow.class);
		opCounters = mock(HapiOpCounters.class);
		observer = mock(StreamObserver.class);
		okResponse = mock(TransactionResponse.class);
		given(okResponse.getNodeTransactionPrecheckCode()).willReturn(OK);
		notOkResponse = mock(TransactionResponse.class);

		subject = new TxnResponseHelper(submissionFlow, opCounters);
	}

	@Test
	public void helpsWithSubmitHappyPath() {
		// setup:
		InOrder inOrder = inOrder(submissionFlow, opCounters, observer);

		given(submissionFlow.submit(txn)).willReturn(okResponse);

		// when:
		subject.submit(txn, observer, CryptoTransfer);

		// then:
		inOrder.verify(opCounters).countReceived(CryptoTransfer);
		inOrder.verify(submissionFlow).submit(txn);
		inOrder.verify(observer).onNext(okResponse);
		inOrder.verify(observer).onCompleted();
		inOrder.verify(opCounters).countSubmitted(CryptoTransfer);
	}

	@Test
	public void helpsWithSubmitUnhappyPath() {
		// setup:
		InOrder inOrder = inOrder(submissionFlow, opCounters, observer);

		given(submissionFlow.submit(txn)).willReturn(notOkResponse);

		// when:
		subject.submit(txn, observer, CryptoTransfer);

		// then:
		inOrder.verify(opCounters).countReceived(CryptoTransfer);
		inOrder.verify(submissionFlow).submit(txn);
		inOrder.verify(observer).onNext(notOkResponse);
		inOrder.verify(observer).onCompleted();
		inOrder.verify(opCounters, never()).countSubmitted(CryptoTransfer);
	}
}
