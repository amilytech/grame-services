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
import com.grame.services.queries.meta.MetaAnswers;
import com.grame.services.txns.submission.TxnResponseHelper;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.BDDMockito.*;
import static com.grame.services.grpc.controllers.NetworkController.*;

class NetworkControllerTest {
	Query query = Query.getDefaultInstance();
	Transaction txn = Transaction.getDefaultInstance();
	MetaAnswers answers;
	TxnResponseHelper txnResponseHelper;
	QueryResponseHelper queryResponseHelper;
	StreamObserver<Response> queryObserver;
	StreamObserver<TransactionResponse> txnObserver;

	NetworkController subject;

	@BeforeEach
	private void setup() {
		answers = mock(MetaAnswers.class);
		txnObserver = mock(StreamObserver.class);
		queryObserver = mock(StreamObserver.class);

		txnResponseHelper = mock(TxnResponseHelper.class);
		queryResponseHelper = mock(QueryResponseHelper.class);

		subject = new NetworkController(answers, txnResponseHelper, queryResponseHelper);
	}

	@Test
	public void forwardsVersionInfoAsExpected() {
		// when:
		subject.getVersionInfo(query, queryObserver);

		// expect:
		verify(answers).getVersionInfo();
		verify(queryResponseHelper).answer(query, queryObserver, null, grameFunctionality.GetVersionInfo);
	}

	@Test
	public void forwardsUncheckedSubmitAsExpected() {
		// when:
		subject.uncheckedSubmit(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, grameFunctionality.UncheckedSubmit);
	}
}
