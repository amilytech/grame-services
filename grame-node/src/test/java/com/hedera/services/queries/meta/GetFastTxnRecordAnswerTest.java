package com.grame.services.queries.meta;

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

import com.grame.services.context.primitives.StateView;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.QueryHeader;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.ResponseHeader;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.api.proto.java.TransactionGetFastRecordQuery;
import com.gramegrame.api.proto.java.TransactionGetFastRecordResponse;
import org.junit.jupiter.api.Test;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.*;
import static com.gramegrame.api.proto.java.ResponseType.ANSWER_ONLY;
import static com.gramegrame.api.proto.java.ResponseType.COST_ANSWER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.NOT_SUPPORTED;


class GetFastTxnRecordAnswerTest {
	GetFastTxnRecordAnswer subject = new GetFastTxnRecordAnswer();
	Query query = Query.getDefaultInstance();

	@Test
	public void neverDoesOrNeedsAnything() {
		// expect:
		assertFalse(subject.needsAnswerOnlyCost(query));
		assertFalse(subject.requiresNodePayment(query));
		assertFalse(subject.extractPaymentFrom(query).isPresent());
	}

	@Test
	public void extractsValidity() {
		// given:
		Response response = Response.newBuilder()
				.setTransactionGetFastRecord(TransactionGetFastRecordResponse.newBuilder()
						.setHeader(ResponseHeader.newBuilder().setNodeTransactionPrecheckCode(FAIL_FEE)))
				.build();

		// expect:
		assertEquals(FAIL_FEE, subject.extractValidityFrom(response));
	}

	@Test
	public void respectsTypeOfUnsupportedQuery() {
		// given:
		Query costAnswer = getFastTxnRecordQuery(COST_ANSWER);
		Query answerOnly = getFastTxnRecordQuery(ANSWER_ONLY);

		// when:
		Response costAnswerResponse = subject.responseGiven(costAnswer, StateView.EMPTY_VIEW, OK, 0L);
		Response answerOnlyResponse = subject.responseGiven(answerOnly, StateView.EMPTY_VIEW, OK, 0L);

		// then:
		assertEquals(COST_ANSWER, costAnswerResponse.getTransactionGetFastRecord().getHeader().getResponseType());
		assertEquals(ANSWER_ONLY, answerOnlyResponse.getTransactionGetFastRecord().getHeader().getResponseType());
		// and:
		assertEquals(NOT_SUPPORTED, subject.extractValidityFrom(costAnswerResponse));
		assertEquals(NOT_SUPPORTED, subject.extractValidityFrom(answerOnlyResponse));
	}

	@Test
	public void alwaysUnsupported() {
		// expect:
		assertEquals(NOT_SUPPORTED, subject.checkValidity(query, StateView.EMPTY_VIEW));
	}

	@Test
	public void recognizesFunction() {
		// expect:
		assertEquals(grameFunctionality.TransactionGetRecord, subject.canonicalFunction());
	}

	private Query getFastTxnRecordQuery(ResponseType type) {
		TransactionGetFastRecordQuery.Builder op = TransactionGetFastRecordQuery.newBuilder();
		op.setHeader(QueryHeader.newBuilder().setResponseType(type));
		return Query.newBuilder()
				.setTransactionGetFastRecord(op)
				.build();
	}
}
