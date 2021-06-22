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
import com.grame.services.context.properties.ActiveVersions;
import com.grame.services.context.properties.PropertySource;
import com.grame.services.context.properties.SemanticVersions;
import com.grame.services.queries.token.GetTokenInfoAnswer;
import com.gramegrame.api.proto.java.NetworkGetVersionInfoQuery;
import com.gramegrame.api.proto.java.NetworkGetVersionInfoResponse;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.QueryHeader;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.api.proto.java.SemanticVersion;
import com.gramegrame.api.proto.java.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.grame.test.factories.scenarios.TxnHandlingScenario.COMPLEX_KEY_ACCOUNT_KT;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.*;
import static com.gramegrame.api.proto.java.ResponseType.ANSWER_ONLY;
import static com.gramegrame.api.proto.java.ResponseType.COST_ANSWER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.*;
import static com.grame.test.utils.TxnUtils.*;

class GetVersionInfoAnswerTest {
	private String node = "0.0.3";
	private long fee = 1_234L;
	private String payer = "0.0.12345";
	private Transaction paymentTxn;
	StateView view;

	SemanticVersion expectedVersions = SemanticVersion.newBuilder()
			.setMajor(0)
			.setMinor(4)
			.setPatch(0)
			.build();
	SemanticVersions semanticVersions;
	GetVersionInfoAnswer subject;

	@BeforeEach
	private void setup() throws Throwable {
		view = mock(StateView.class);
		semanticVersions = mock(SemanticVersions.class);
		given(semanticVersions.getDeployed())
				.willReturn(Optional.of(new ActiveVersions(expectedVersions, expectedVersions)));

		subject = new GetVersionInfoAnswer(semanticVersions);
	}

	@Test
	public void getsInvalidResponse() throws Throwable {
		// setup:
		Query query = validQuery(COST_ANSWER, fee);

		// when:
		Response response = subject.responseGiven(query, view, FAIL_INVALID, fee);

		// then:
		assertTrue(response.hasNetworkGetVersionInfo());
		assertEquals(FAIL_INVALID, response.getNetworkGetVersionInfo().getHeader().getNodeTransactionPrecheckCode());
		assertEquals(COST_ANSWER, response.getNetworkGetVersionInfo().getHeader().getResponseType());
		assertEquals(fee, response.getNetworkGetVersionInfo().getHeader().getCost());
	}

	@Test
	public void getsCostAnswerResponse() throws Throwable {
		// setup:
		Query query = validQuery(COST_ANSWER, fee);

		// when:
		Response response = subject.responseGiven(query, view, OK, fee);

		// then:
		assertTrue(response.hasNetworkGetVersionInfo());
		NetworkGetVersionInfoResponse opResponse = response.getNetworkGetVersionInfo();
		assertEquals(OK, opResponse.getHeader().getNodeTransactionPrecheckCode());
		assertEquals(COST_ANSWER, opResponse.getHeader().getResponseType());
		assertEquals(fee, opResponse.getHeader().getCost());
	}

	@Test
	public void complainsWhenVersionInfoAvailable() throws Throwable {
		// setup:
		Query sensibleQuery = validQuery(ANSWER_ONLY, 5L);

		given(semanticVersions.getDeployed()).willReturn(Optional.empty());

		// given:
		assertEquals(FAIL_INVALID, subject.checkValidity(sensibleQuery, view));
	}

	@Test
	public void getsVersionInfoWhenAvailable() throws Throwable {
		// setup:
		Query sensibleQuery = validQuery(ANSWER_ONLY, 5L);

		// given:
		assertEquals(OK, subject.checkValidity(sensibleQuery, view));

		// when:
		Response response = subject.responseGiven(sensibleQuery, view, OK, 0L);

		// then:
		var opResponse = response.getNetworkGetVersionInfo();
		assertTrue(opResponse.hasHeader(), "Missing response header!");
		assertEquals(OK, opResponse.getHeader().getNodeTransactionPrecheckCode());
		assertEquals(expectedVersions, opResponse.getgrameServicesVersion());
		assertEquals(expectedVersions, opResponse.getHapiProtoVersion());
	}

	@Test
	public void respectsMetaValidity() throws Throwable {
		// given:
		Query sensibleQuery = validQuery(ANSWER_ONLY, 5L);

		// when:
		Response response = subject.responseGiven(sensibleQuery, view, INVALID_TRANSACTION, 0L);

		// then:
		var opResponse = response.getNetworkGetVersionInfo();
		assertEquals(INVALID_TRANSACTION, opResponse.getHeader().getNodeTransactionPrecheckCode());
	}

	private Query validQuery(ResponseType type, long payment) throws Throwable {
		this.paymentTxn = payerSponsoredTransfer(payer, COMPLEX_KEY_ACCOUNT_KT, node, payment);

		QueryHeader.Builder header = QueryHeader.newBuilder()
				.setPayment(this.paymentTxn)
				.setResponseType(type);
		NetworkGetVersionInfoQuery.Builder op = NetworkGetVersionInfoQuery.newBuilder()
				.setHeader(header);
		return Query.newBuilder().setNetworkGetVersionInfo(op).build();
	}
}
