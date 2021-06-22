package com.grame.services.queries.file;

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
import com.grame.test.factories.scenarios.TxnHandlingScenario;
import com.gramegrame.api.proto.java.FileGetInfoQuery;
import com.gramegrame.api.proto.java.FileGetInfoResponse;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.QueryHeader;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.grame.test.factories.scenarios.TxnHandlingScenario.COMPLEX_KEY_ACCOUNT_KT;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.*;
import static com.gramegrame.api.proto.java.ResponseType.ANSWER_ONLY;
import static com.gramegrame.api.proto.java.ResponseType.COST_ANSWER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.*;
import static com.grame.test.utils.IdUtils.*;
import static com.grame.test.utils.TxnUtils.*;

class GetFileInfoAnswerTest {
	int size = 1_234;
	long expiry = 2_000_000L;
	private String node = "0.0.3";
	private String payer = "0.0.12345";
	private String target = "0.0.123";
	private Transaction paymentTxn;
	private long fee = 1_234L;
	FileGetInfoResponse.FileInfo expected;

	OptionValidator optionValidator;
	StateView view;

	GetFileInfoAnswer subject;

	@BeforeEach
	private void setup() throws Throwable {
		expected = FileGetInfoResponse.FileInfo.newBuilder()
				.setDeleted(false)
				.setExpirationTime(Timestamp.newBuilder().setSeconds(expiry))
				.setFileID(asFile(target))
				.setSize(size)
				.setKeys(TxnHandlingScenario.MISC_FILE_WACL_KT.asKey().getKeyList())
				.build();

		view = mock(StateView.class);
		optionValidator = mock(OptionValidator.class);

		subject = new GetFileInfoAnswer(optionValidator);
	}

	@Test
	public void usesValidator() throws Throwable {
		// setup:
		Query query = validQuery(COST_ANSWER, fee, target);

		given(optionValidator.queryableFileStatus(asFile(target), view)).willReturn(FILE_DELETED);

		// when:
		ResponseCodeEnum validity = subject.checkValidity(query, view);

		// then:
		assertEquals(FILE_DELETED, validity);
		// and:
		verify(optionValidator).queryableFileStatus(any(), any());
	}

	@Test
	public void requiresAnswerOnlyCostAsExpected() throws Throwable {
		// expect:
		assertTrue(subject.needsAnswerOnlyCost(validQuery(COST_ANSWER, 0, target)));
		assertFalse(subject.needsAnswerOnlyCost(validQuery(ANSWER_ONLY, 0, target)));
	}

	@Test
	public void requiresAnswerOnlyPayment() throws Throwable {
		// expect:
		assertFalse(subject.requiresNodePayment(validQuery(COST_ANSWER, 0, target)));
		assertTrue(subject.requiresNodePayment(validQuery(ANSWER_ONLY, 0, target)));
	}

	@Test
	public void getsExpectedPayment() throws Throwable {
		// given:
		Query query = validQuery(COST_ANSWER, fee, target);

		// expect:
		assertEquals(paymentTxn, subject.extractPaymentFrom(query).get().getBackwardCompatibleSignedTxn());
	}

	@Test
	public void getsValidity() {
		// given:
		Response response = Response.newBuilder().setFileGetInfo(
				FileGetInfoResponse.newBuilder()
						.setHeader(subject.answerOnlyHeader(RESULT_SIZE_LIMIT_EXCEEDED))).build();

		// expect:
		assertEquals(RESULT_SIZE_LIMIT_EXCEEDED, subject.extractValidityFrom(response));
	}

	@Test
	public void recognizesFunction() {
		// expect:
		assertEquals(grameFunctionality.FileGetInfo, subject.canonicalFunction());
	}

	@Test
	public void getsTheInfo() throws Throwable {
		// setup:
		Query query = validQuery(ANSWER_ONLY, fee, target);

		given(view.infoForFile(asFile(target))).willReturn(Optional.of(expected));

		// when:
		Response response = subject.responseGiven(query, view, OK, fee);

		// then:
		assertTrue(response.hasFileGetInfo());
		assertTrue(response.getFileGetInfo().hasHeader(), "Missing response header!");
		assertEquals(OK, response.getFileGetInfo().getHeader().getNodeTransactionPrecheckCode());
		assertEquals(ANSWER_ONLY, response.getFileGetInfo().getHeader().getResponseType());
		assertEquals(fee, response.getFileGetInfo().getHeader().getCost());
		// and:
		var actual = response.getFileGetInfo().getFileInfo();
		assertEquals(expected, actual);
	}

	@Test
	public void getsCostAnswerResponse() throws Throwable {
		// setup:
		Query query = validQuery(COST_ANSWER, fee, target);

		// when:
		Response response = subject.responseGiven(query, view, OK, fee);

		// then:
		assertTrue(response.hasFileGetInfo());
		assertEquals(OK, response.getFileGetInfo().getHeader().getNodeTransactionPrecheckCode());
		assertEquals(COST_ANSWER, response.getFileGetInfo().getHeader().getResponseType());
		assertEquals(fee, response.getFileGetInfo().getHeader().getCost());
	}

	@Test
	public void getsInvalidResponse() throws Throwable {
		// setup:
		Query query = validQuery(COST_ANSWER, fee, target);

		// when:
		Response response = subject.responseGiven(query, view, FILE_DELETED, fee);

		// then:
		assertTrue(response.hasFileGetInfo());
		assertEquals(FILE_DELETED, response.getFileGetInfo().getHeader().getNodeTransactionPrecheckCode());
		assertEquals(COST_ANSWER, response.getFileGetInfo().getHeader().getResponseType());
		assertEquals(fee, response.getFileGetInfo().getHeader().getCost());
	}

	private Query validQuery(ResponseType type, long payment, String idLit) throws Throwable {
		this.paymentTxn = payerSponsoredTransfer(payer, COMPLEX_KEY_ACCOUNT_KT, node, payment);
		QueryHeader.Builder header = QueryHeader.newBuilder()
				.setPayment(this.paymentTxn)
				.setResponseType(type);
		FileGetInfoQuery.Builder op = FileGetInfoQuery.newBuilder()
				.setHeader(header)
				.setFileID(asFile(idLit));
		return Query.newBuilder().setFileGetInfo(op).build();
	}
}
