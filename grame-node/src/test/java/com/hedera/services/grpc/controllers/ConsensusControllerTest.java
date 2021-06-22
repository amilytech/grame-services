package com.grame.services.grpc.controllers;

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
import com.grame.services.queries.consensus.HcsAnswers;
import com.grame.services.txns.submission.TxnResponseHelper;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusCreateTopic;
import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusDeleteTopic;
import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusSubmitMessage;
import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusUpdateTopic;
import static org.mockito.BDDMockito.*;

class ConsensusControllerTest {
	Query query = Query.getDefaultInstance();
	Transaction txn = Transaction.getDefaultInstance();
	HcsAnswers hcsAnswers;
	TxnResponseHelper txnResponseHelper;
	QueryResponseHelper queryResponseHelper;
	StreamObserver<Response> queryObserver;
	StreamObserver<TransactionResponse> txnObserver;

	ConsensusController subject;

	@BeforeEach
	private void setup() {
		txnObserver = mock(StreamObserver.class);
		queryObserver = mock(StreamObserver.class);

		hcsAnswers = mock(HcsAnswers.class);
		txnResponseHelper = mock(TxnResponseHelper.class);
		queryResponseHelper = mock(QueryResponseHelper.class);

		subject = new ConsensusController(hcsAnswers, txnResponseHelper, queryResponseHelper);
	}

	@Test
	public void forwardsTopicInfoAsExpected() {
		// when:
		subject.getTopicInfo(query, queryObserver);

		// expect:
		verify(hcsAnswers).topicInfo();
		verify(queryResponseHelper).answer(query, queryObserver, null, grameFunctionality.ConsensusGetTopicInfo);
	}

	@Test
	public void forwardsCreateAsExpected() {
		// when:
		subject.createTopic(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, ConsensusCreateTopic);
	}

	@Test
	public void forwardsDeleteAsExpected() {
		// when:
		subject.deleteTopic(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, ConsensusDeleteTopic);
	}

	@Test
	public void forwardsUpdateAsExpected() {
		// when:
		subject.updateTopic(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, ConsensusUpdateTopic);
	}

	@Test
	public void forwardsSubmitAsExpected() {
		// when:
		subject.submitMessage(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, ConsensusSubmitMessage);
	}
}
