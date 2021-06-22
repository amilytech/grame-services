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

import com.google.protobuf.ByteString;
import com.grame.services.context.primitives.StateView;
import com.grame.services.queries.contract.GetContractInfoAnswer;
import com.grame.services.usage.contract.ContractGetInfoUsage;
import com.gramegrame.api.proto.java.ContractGetInfoQuery;
import com.gramegrame.api.proto.java.ContractGetInfoResponse;
import com.gramegrame.api.proto.java.ContractID;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.Key;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.QueryHeader;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.api.proto.java.TokenRelationship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.grame.test.utils.IdUtils.asContract;
import static com.gramegrame.api.proto.java.ResponseType.ANSWER_ONLY;
import static com.gramegrame.api.proto.java.ResponseType.COST_ANSWER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.verify;

class GetContractInfoResourceUsageTest {
	String memo = "Stay cold...";
	ContractID target = asContract("0.0.123");
	Key aKey = Key.newBuilder().setEd25519(ByteString.copyFrom("NONSENSE".getBytes())).build();

	StateView view;
	ContractGetInfoResponse.ContractInfo info;

	ContractGetInfoUsage estimator;
	Function<Query, ContractGetInfoUsage> factory;
	FeeData expected;

	Query satisfiableAnswerOnly = contractInfoQuery(target, ANSWER_ONLY);

	GetContractInfoResourceUsage subject;

	@BeforeEach
	private void setup() throws Throwable {
		expected = mock(FeeData.class);
		info = ContractGetInfoResponse.ContractInfo.newBuilder()
				.setAdminKey(aKey)
				.addAllTokenRelationships(List.of(
						TokenRelationship.getDefaultInstance(),
						TokenRelationship.getDefaultInstance(),
						TokenRelationship.getDefaultInstance()))
				.setMemo(memo)
				.build();

		view = mock(StateView.class);

		given(view.infoForContract(target)).willReturn(Optional.of(info));

		estimator = mock(ContractGetInfoUsage.class);
		factory = mock(Function.class);
		given(factory.apply(any())).willReturn(estimator);

		GetContractInfoResourceUsage.factory = factory;

		given(estimator.givenCurrentKey(aKey)).willReturn(estimator);
		given(estimator.givenCurrentMemo(memo)).willReturn(estimator);
		given(estimator.givenCurrentTokenAssocs(3)).willReturn(estimator);
		given(estimator.get()).willReturn(expected);

		subject = new GetContractInfoResourceUsage();
	}

	@Test
	public void recognizesApplicableQuery() {
		// given:
		var applicable = contractInfoQuery(target, COST_ANSWER);
		var inapplicable = Query.getDefaultInstance();

		// expect:
		assertTrue(subject.applicableTo(applicable));
		assertFalse(subject.applicableTo(inapplicable));
	}

	@Test
	public void usesEstimator() {
		// when:
		var usage = subject.usageGiven(contractInfoQuery(target, ANSWER_ONLY), view);

		// then:
		assertEquals(expected, usage);
		// and:
		verify(estimator).givenCurrentKey(aKey);
		verify(estimator).givenCurrentMemo(memo);
		verify(estimator).givenCurrentTokenAssocs(3);
	}

	@Test
	public void setsInfoInQueryCxtIfPresent() {
		// setup:
		var queryCtx = new HashMap<String, Object>();

		// when:
		subject.usageGiven(satisfiableAnswerOnly, view, queryCtx);

		// then:
		assertSame(info, queryCtx.get(GetContractInfoAnswer.CONTRACT_INFO_CTX_KEY));
	}

	@Test
	public void onlySetsContractInfoInQueryCxtIfFound() {
		// setup:
		var queryCtx = new HashMap<String, Object>();

		given(view.infoForContract(target)).willReturn(Optional.empty());

		// when:
		var actual = subject.usageGiven(satisfiableAnswerOnly, view, queryCtx);

		// then:
		assertFalse(queryCtx.containsKey(GetContractInfoAnswer.CONTRACT_INFO_CTX_KEY));
		assertSame(FeeData.getDefaultInstance(), actual);
	}

	private Query contractInfoQuery(ContractID id, ResponseType type) {
		ContractGetInfoQuery.Builder op = ContractGetInfoQuery.newBuilder()
				.setContractID(id)
				.setHeader(QueryHeader.newBuilder().setResponseType(type));
		return Query.newBuilder()
				.setContractGetInfo(op)
				.build();
	}
}
