package com.grame.services.fees.calculation.contract.queries;

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
import com.grame.services.queries.contract.GetContractRecordsAnswer;
import com.gramegrame.api.proto.java.ContractGetRecordsQuery;
import com.gramegrame.api.proto.java.ContractID;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.QueryHeader;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.fee.SmartContractFeeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.grame.test.utils.IdUtils.asContract;
import static com.gramegrame.api.proto.java.ResponseType.ANSWER_ONLY;
import static com.gramegrame.api.proto.java.ResponseType.COST_ANSWER;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

class GetContractRecordsResourceUsageTest {
	StateView view;
	SmartContractFeeBuilder usageEstimator;
	GetContractRecordsResourceUsage subject;
	String a = "0.0.1234";

	@BeforeEach
	private void setup() throws Throwable {
		usageEstimator = mock(SmartContractFeeBuilder.class);
		view = new StateView(StateView.EMPTY_TOPICS_SUPPLIER, () -> null, null, null);

		subject = new GetContractRecordsResourceUsage(usageEstimator);
	}

	@Test
	public void invokesEstimatorAsExpectedForType() {
		// setup:
		FeeData costAnswerUsage = mock(FeeData.class);
		FeeData answerOnlyUsage = mock(FeeData.class);

		// given:
		Query answerOnlyQuery = accountRecordsQuery(a, ANSWER_ONLY);
		Query costAnswerQuery = accountRecordsQuery(a, COST_ANSWER);
		given(usageEstimator.getContractRecordsQueryFeeMatrices(
				GetContractRecordsAnswer.GUARANTEED_EMPTY_PAYER_RECORDS,
				COST_ANSWER)).willReturn(costAnswerUsage);
		given(usageEstimator.getContractRecordsQueryFeeMatrices(
				GetContractRecordsAnswer.GUARANTEED_EMPTY_PAYER_RECORDS,
				ANSWER_ONLY)).willReturn(answerOnlyUsage);

		// when:
		FeeData costAnswerEstimate = subject.usageGiven(costAnswerQuery, view);
		FeeData answerOnlyEstimate = subject.usageGiven(answerOnlyQuery, view);

		// then:
		assertTrue(costAnswerEstimate == costAnswerUsage);
		assertTrue(answerOnlyEstimate == answerOnlyUsage);
	}


	@Test
	public void recognizesApplicableQuery() {
		// given:
		Query accountRecordsQuery = accountRecordsQuery(a, COST_ANSWER);
		Query nonContractRecordsQuery = nonContractRecordsQuery();

		// expect:
		assertTrue(subject.applicableTo(accountRecordsQuery));
		assertFalse(subject.applicableTo(nonContractRecordsQuery));
	}

	private Query accountRecordsQuery(String target, ResponseType type) {
		ContractID id = asContract(target);
		ContractGetRecordsQuery.Builder op = ContractGetRecordsQuery.newBuilder()
				.setContractID(id)
				.setHeader(QueryHeader.newBuilder().setResponseType(type));
		return Query.newBuilder()
				.setContractGetRecords(op)
				.build();
	}

	private Query nonContractRecordsQuery() {
		return Query.newBuilder().build();
	}
}
