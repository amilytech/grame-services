package com.grame.services.queries.token;

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
import com.grame.services.txns.validation.OptionValidator;
import com.gramegrame.api.proto.java.TokenGetInfoQuery;
import com.gramegrame.api.proto.java.TokenGetInfoResponse;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.QueryHeader;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.api.proto.java.TokenID;
import com.gramegrame.api.proto.java.TokenInfo;
import com.gramegrame.api.proto.java.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.grame.test.factories.scenarios.TxnHandlingScenario.COMPLEX_KEY_ACCOUNT_KT;
import static com.grame.test.utils.IdUtils.asToken;
import static com.grame.test.utils.TxnUtils.payerSponsoredTransfer;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TOKEN_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.RESULT_SIZE_LIMIT_EXCEEDED;
import static com.gramegrame.api.proto.java.ResponseType.ANSWER_ONLY;
import static com.gramegrame.api.proto.java.ResponseType.COST_ANSWER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.never;

class GetTokenInfoAnswerTest {
	private Transaction paymentTxn;
	private String node = "0.0.3";
	private String payer = "0.0.12345";
	private TokenID tokenId = asToken("1.2.3");
	private long fee = 1_234L;

	StateView view;
	OptionValidator optionValidator;

	TokenInfo info;

	GetTokenInfoAnswer subject;

	@BeforeEach
	public void setup() {
		info = TokenInfo.newBuilder()
				.setTokenId(tokenId)
				.setAdminKey(COMPLEX_KEY_ACCOUNT_KT.asKey())
				.build();

		view = mock(StateView.class);
		optionValidator = mock(OptionValidator.class);

		subject = new GetTokenInfoAnswer();
	}

	@Test
	public void getsTheInfo() throws Throwable {
		// setup:
		Query query = validQuery(ANSWER_ONLY, fee, tokenId);

		given(view.infoForToken(tokenId)).willReturn(Optional.of(info));

		// when:
		Response response = subject.responseGiven(query, view, OK, fee);

		// then:
		assertTrue(response.hasTokenGetInfo());
		assertTrue(response.getTokenGetInfo().hasHeader(), "Missing response header!");
		assertEquals(OK, response.getTokenGetInfo().getHeader().getNodeTransactionPrecheckCode());
		assertEquals(ANSWER_ONLY, response.getTokenGetInfo().getHeader().getResponseType());
		assertEquals(fee, response.getTokenGetInfo().getHeader().getCost());
		// and:
		var actual = response.getTokenGetInfo().getTokenInfo();
		assertEquals(info, actual);
	}

	@Test
	public void getsInfoFromCtxWhenAvailable() throws Throwable {
		// setup:
		Query sensibleQuery = validQuery(ANSWER_ONLY, 5L, tokenId);
		Map<String, Object> ctx = new HashMap<>();

		// given:
		ctx.put(GetTokenInfoAnswer.TOKEN_INFO_CTX_KEY, info);

		// when:
		Response response = subject.responseGiven(sensibleQuery, view, OK, 0L, ctx);

		// then:
		var opResponse = response.getTokenGetInfo();
		assertTrue(opResponse.hasHeader(), "Missing response header!");
		assertEquals(OK, opResponse.getHeader().getNodeTransactionPrecheckCode());
		assertSame(info, opResponse.getTokenInfo());
		// and:
		verify(view, never()).infoForToken(any());
	}

	@Test
	public void recognizesMissingInfoWhenNoCtxGiven() throws Throwable {
		// setup:
		Query sensibleQuery = validQuery(ANSWER_ONLY, 5L, tokenId);

		given(view.infoForToken(tokenId)).willReturn(Optional.empty());

		// when:
		Response response = subject.responseGiven(sensibleQuery, view, OK, 0L);

		// then:
		TokenGetInfoResponse opResponse = response.getTokenGetInfo();
		assertTrue(opResponse.hasHeader(), "Missing response header!");
		assertEquals(INVALID_TOKEN_ID, opResponse.getHeader().getNodeTransactionPrecheckCode());
	}

	@Test
	public void recognizesMissingInfoWhenCtxGiven() throws Throwable {
		// setup:
		Query sensibleQuery = validQuery(ANSWER_ONLY, 5L, tokenId);

		// when:
		Response response = subject.responseGiven(sensibleQuery, view, OK, 0L, Collections.emptyMap());

		// then:
		TokenGetInfoResponse opResponse = response.getTokenGetInfo();
		assertTrue(opResponse.hasHeader(), "Missing response header!");
		assertEquals(INVALID_TOKEN_ID, opResponse.getHeader().getNodeTransactionPrecheckCode());
		verify(view, never()).infoForToken(any());
	}

	@Test
	public void getsCostAnswerResponse() throws Throwable {
		// setup:
		Query query = validQuery(COST_ANSWER, fee, tokenId);

		// when:
		Response response = subject.responseGiven(query, view, OK, fee);

		// then:
		assertTrue(response.hasTokenGetInfo());
		assertEquals(OK, response.getTokenGetInfo().getHeader().getNodeTransactionPrecheckCode());
		assertEquals(COST_ANSWER, response.getTokenGetInfo().getHeader().getResponseType());
		assertEquals(fee, response.getTokenGetInfo().getHeader().getCost());
	}

	@Test
	public void getsInvalidResponse() throws Throwable {
		// setup:
		Query query = validQuery(COST_ANSWER, fee, tokenId);

		// when:
		Response response = subject.responseGiven(query, view, INVALID_TOKEN_ID, fee);

		// then:
		assertTrue(response.hasTokenGetInfo());
		assertEquals(
				INVALID_TOKEN_ID,
				response.getTokenGetInfo().getHeader().getNodeTransactionPrecheckCode());
		assertEquals(COST_ANSWER, response.getTokenGetInfo().getHeader().getResponseType());
		assertEquals(fee, response.getTokenGetInfo().getHeader().getCost());
	}

	@Test
	public void recognizesFunction() {
		// expect:
		assertEquals(grameFunctionality.TokenGetInfo, subject.canonicalFunction());
	}

	@Test
	public void requiresAnswerOnlyPayment() throws Throwable {
		// expect:
		assertFalse(subject.requiresNodePayment(validQuery(COST_ANSWER, 0, tokenId)));
		assertTrue(subject.requiresNodePayment(validQuery(ANSWER_ONLY, 0, tokenId)));
	}

	@Test
	public void requiresAnswerOnlyCostAsExpected() throws Throwable {
		// expect:
		assertTrue(subject.needsAnswerOnlyCost(validQuery(COST_ANSWER, 0, tokenId)));
		assertFalse(subject.needsAnswerOnlyCost(validQuery(ANSWER_ONLY, 0, tokenId)));
	}

	@Test
	public void getsValidity() {
		// given:
		Response response = Response.newBuilder().setTokenGetInfo(
				TokenGetInfoResponse.newBuilder()
						.setHeader(subject.answerOnlyHeader(RESULT_SIZE_LIMIT_EXCEEDED))).build();

		// expect:
		assertEquals(RESULT_SIZE_LIMIT_EXCEEDED, subject.extractValidityFrom(response));
	}

	@Test
	public void usesViewToValidate() throws Throwable {
		// setup:
		Query query = validQuery(COST_ANSWER, fee, tokenId);

		given(view.tokenExists(tokenId)).willReturn(false);

		// when:
		ResponseCodeEnum validity = subject.checkValidity(query, view);

		// then:
		assertEquals(INVALID_TOKEN_ID, validity);
	}

	@Test
	public void getsExpectedPayment() throws Throwable {
		// given:
		Query query = validQuery(COST_ANSWER, fee, tokenId);

		// expect:
		assertEquals(paymentTxn, subject.extractPaymentFrom(query).get().getBackwardCompatibleSignedTxn());
	}

	private Query validQuery(ResponseType type, long payment, TokenID id) throws Throwable {
		this.paymentTxn = payerSponsoredTransfer(payer, COMPLEX_KEY_ACCOUNT_KT, node, payment);
		QueryHeader.Builder header = QueryHeader.newBuilder()
				.setPayment(this.paymentTxn)
				.setResponseType(type);
		TokenGetInfoQuery.Builder op = TokenGetInfoQuery.newBuilder()
				.setHeader(header)
				.setToken(id);
		return Query.newBuilder().setTokenGetInfo(op).build();
	}
}
