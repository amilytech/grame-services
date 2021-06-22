package com.grame.services.queries.answering;

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
import com.grame.services.legacy.handler.TransactionHandler;
import com.grame.services.queries.AnswerService;
import com.grame.services.throttling.FunctionalityThrottling;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static com.gramegrame.api.proto.java.grameFunctionality.CryptoGetStakers;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.ACCOUNT_IS_NOT_GENESIS_ACCOUNT;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.BUSY;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_ACCOUNT_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;

class ZeroStakeAnswerFlowTest {
	grameFunctionality function = grameFunctionality.ConsensusGetTopicInfo;


	TransactionHandler legacyHandler;
	StateView view;
	Supplier<StateView> stateViews;
	FunctionalityThrottling throttles;

	Query query = Query.getDefaultInstance();
	Response response;
	AnswerService service;

	ZeroStakeAnswerFlow subject;

	@BeforeEach
	private void setup() {
		view = mock(StateView.class);
		throttles = mock(FunctionalityThrottling.class);
		legacyHandler = mock(TransactionHandler.class);
		stateViews = () -> view;

		service = mock(AnswerService.class);
		response = mock(Response.class);

		subject = new ZeroStakeAnswerFlow(legacyHandler, stateViews, throttles);
	}

	@Test
	public void validatesMetaAsExpected() {
		given(legacyHandler.validateQuery(query, false)).willReturn(ACCOUNT_IS_NOT_GENESIS_ACCOUNT);
		given(service.responseGiven(query, view, ACCOUNT_IS_NOT_GENESIS_ACCOUNT)).willReturn(response);

		// when:
		Response actual = subject.satisfyUsing(service, query);

		// then:
		assertEquals(response, actual);
	}

	@Test
	public void validatesSpecificAfterMetaOk() {
		given(legacyHandler.validateQuery(query, false)).willReturn(OK);
		given(service.checkValidity(query, view)).willReturn(INVALID_ACCOUNT_ID);
		given(service.responseGiven(query, view, INVALID_ACCOUNT_ID)).willReturn(response);

		// when:
		Response actual = subject.satisfyUsing(service, query);

		// then:
		assertEquals(response, actual);
	}

	@Test
	public void throttlesIfAppropriate() {
		given(service.canonicalFunction()).willReturn(function);
		given(throttles.shouldThrottle(function)).willReturn(true);
		given(service.responseGiven(query, view, BUSY)).willReturn(response);

		// when:
		Response actual = subject.satisfyUsing(service, query);

		// then:
		assertEquals(response, actual);
		verify(throttles).shouldThrottle(function);
	}

	@Test
	public void throttlesEvenAllegedSuperusers() {
		given(throttles.shouldThrottle(CryptoGetStakers)).willReturn(true);
		given(service.checkValidity(query, view)).willReturn(OK);
		given(legacyHandler.validateQuery(query, false)).willReturn(OK);
		given(service.canonicalFunction()).willReturn(CryptoGetStakers);
		// and:
		given(service.responseGiven(query, view, BUSY)).willReturn(response);

		// when:
		Response actual = subject.satisfyUsing(service, query);

		// then:
		assertEquals(response, actual);
	}

	@Test
	public void submitsIfShouldntThrottle() {
		given(throttles.shouldThrottle(CryptoGetStakers)).willReturn(false);
		given(service.checkValidity(query, view)).willReturn(OK);
		given(legacyHandler.validateQuery(query, false)).willReturn(OK);
		given(service.canonicalFunction()).willReturn(CryptoGetStakers);
		// and:
		given(service.responseGiven(query, view, OK)).willReturn(response);

		// when:
		Response actual = subject.satisfyUsing(service, query);

		// then:
		assertEquals(response, actual);
	}
}
