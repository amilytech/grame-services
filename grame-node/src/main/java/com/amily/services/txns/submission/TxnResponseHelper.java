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

import com.grame.services.queries.answering.QueryResponseHelper;
import com.grame.services.stats.HapiOpCounters;
import com.grame.services.txns.SubmissionFlow;
import com.grame.services.utils.SignedTxnAccessor;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionResponse;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.FAIL_INVALID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;

public class TxnResponseHelper {
	private static final Logger log = LogManager.getLogger(QueryResponseHelper.class);

	static final TransactionResponse FAIL_INVALID_RESPONSE = TransactionResponse.newBuilder()
			.setNodeTransactionPrecheckCode(FAIL_INVALID)
			.build();

	private final SubmissionFlow submissionFlow;
	private final HapiOpCounters opCounters;

	public TxnResponseHelper(SubmissionFlow submissionFlow, HapiOpCounters opCounters) {
		this.opCounters = opCounters;
		this.submissionFlow = submissionFlow;
	}

	public void submit(
			Transaction signedTxn,
			StreamObserver<TransactionResponse> observer,
			grameFunctionality statedFunction
	) {
		respondWithMetrics(
				signedTxn,
				observer,
				() -> opCounters.countReceived(statedFunction),
				() -> opCounters.countSubmitted(statedFunction));
	}

	private void respondWithMetrics(
			Transaction signedTxn,
			StreamObserver<TransactionResponse> observer,
			Runnable incReceivedCount,
			Runnable incSubmittedCount
	) {
		incReceivedCount.run();
		TransactionResponse response;

		try {
			response = submissionFlow.submit(signedTxn);
		} catch (Exception surprising) {
			SignedTxnAccessor accessor = SignedTxnAccessor.uncheckedFrom(signedTxn);
			log.warn("Submission flow unable to submit {}!", accessor.getSignedTxn4Log(), surprising);
			response = FAIL_INVALID_RESPONSE;
		}

		observer.onNext(response);
		observer.onCompleted();

		if (response.getNodeTransactionPrecheckCode() == OK) {
			incSubmittedCount.run();
		}
	}
}
