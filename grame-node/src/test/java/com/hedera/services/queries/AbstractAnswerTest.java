package com.grame.services.queries;

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
import com.grame.test.factories.txns.CryptoTransferFactory;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.api.proto.java.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;
import java.util.function.Function;

import static com.grame.test.factories.txns.TinyBarsFromTo.tinyBarsFromTo;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.FILE_DELETED;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.RESULT_SIZE_LIMIT_EXCEEDED;
import static com.gramegrame.api.proto.java.ResponseType.ANSWER_ONLY;
import static com.gramegrame.api.proto.java.ResponseType.COST_ANSWER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

class AbstractAnswerTest {
	Query query;
	Response response;
	StateView view;
	Transaction payment;

	grameFunctionality function;
	Function<Query, Transaction> paymentExtractor;
	Function<Query, ResponseType> responseTypeExtractor;
	Function<Response, ResponseCodeEnum> statusExtractor;
	BiFunction<Query, StateView, ResponseCodeEnum> validityCheck;

	AbstractAnswer subject;

	@BeforeEach
	private void setup() throws Throwable {
		query = mock(Query.class);
		view = mock(StateView.class);
		response = mock(Response.class);
		payment = CryptoTransferFactory
				.newSignedCryptoTransfer()
				.transfers(tinyBarsFromTo("0.0.2", "0.0.3", 1_234L))
				.get();

		function = grameFunctionality.GetVersionInfo;
		statusExtractor = mock(Function.class);
		paymentExtractor = mock(Function.class);
		responseTypeExtractor = mock(Function.class);
		validityCheck = mock(BiFunction.class);

		subject = new AbstractAnswer(
				function,
				paymentExtractor,
				responseTypeExtractor,
				statusExtractor,
				validityCheck
		) {
			@Override
			public Response responseGiven(Query query, StateView view, ResponseCodeEnum validity, long cost) {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Test
	public void usesValidator() throws Throwable {
		given(validityCheck.apply(query, view)).willReturn(FILE_DELETED);

		// when:
		ResponseCodeEnum validity = subject.checkValidity(query, view);

		// then:
		assertEquals(FILE_DELETED, validity);
	}

	@Test
	public void requiresAnswerOnlyCostAsExpected() throws Throwable {
		given(responseTypeExtractor.apply(query)).willReturn(COST_ANSWER);

		// expect:
		assertTrue(subject.needsAnswerOnlyCost(query));

		// and given:
		given(responseTypeExtractor.apply(query)).willReturn(ANSWER_ONLY);

		// expect:
		assertFalse(subject.needsAnswerOnlyCost(query));
	}

	@Test
	public void requiresAnswerOnlyPayment() throws Throwable {
		// given:
		given(responseTypeExtractor.apply(query)).willReturn(COST_ANSWER);

		// expect:
		assertFalse(subject.requiresNodePayment(query));

		given(responseTypeExtractor.apply(query)).willReturn(ANSWER_ONLY);

		// expect:
		assertTrue(subject.requiresNodePayment(query));
	}

	@Test
	public void recognizesFunction() {
		// expect:
		assertEquals(grameFunctionality.GetVersionInfo, subject.canonicalFunction());
	}

	@Test
	public void getsValidity() {
		given(statusExtractor.apply(response)).willReturn(RESULT_SIZE_LIMIT_EXCEEDED);

		// when:
		var status = subject.extractValidityFrom(response);

		// expect:
		assertEquals(RESULT_SIZE_LIMIT_EXCEEDED, status);
	}

	@Test
	public void getsExpectedPayment() {
		given(paymentExtractor.apply(query)).willReturn(payment);

		// expect:
		assertEquals(payment, subject.extractPaymentFrom(query).get().getBackwardCompatibleSignedTxn());
	}
}
