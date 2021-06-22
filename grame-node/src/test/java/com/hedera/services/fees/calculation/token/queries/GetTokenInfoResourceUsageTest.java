package com.grame.services.fees.calculation.token.queries;

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
import com.grame.services.queries.token.GetTokenInfoAnswer;
import com.grame.services.usage.token.TokenGetInfoUsage;
import com.grame.test.factories.scenarios.TxnHandlingScenario;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.QueryHeader;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.api.proto.java.TokenGetInfoQuery;
import com.gramegrame.api.proto.java.TokenID;
import com.gramegrame.api.proto.java.TokenInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;

import static com.grame.services.queries.token.GetTokenInfoAnswer.TOKEN_INFO_CTX_KEY;
import static com.gramegrame.api.proto.java.ResponseType.ANSWER_ONLY;
import static com.gramegrame.api.proto.java.ResponseType.COST_ANSWER;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.*;

class GetTokenInfoResourceUsageTest {
	String memo = "22 a million";
	String symbol = "HEYMAOK";
	String name = "IsItReallyOk";
	TokenID target = IdUtils.asToken("0.0.123");
	FeeData expected;

	TokenGetInfoUsage estimator;
	Function<Query, TokenGetInfoUsage> factory;

	StateView view;
	TokenInfo info = TokenInfo.newBuilder()
			.setAdminKey(TxnHandlingScenario.TOKEN_ADMIN_KT.asKey())
			.setFreezeKey(TxnHandlingScenario.TOKEN_FREEZE_KT.asKey())
			.setWipeKey(TxnHandlingScenario.TOKEN_WIPE_KT.asKey())
			.setSupplyKey(TxnHandlingScenario.TOKEN_SUPPLY_KT.asKey())
			.setKycKey(TxnHandlingScenario.TOKEN_KYC_KT.asKey())
			.setSymbol(symbol)
			.setName(name)
			.setMemo(memo)
			.setAutoRenewAccount(IdUtils.asAccount("1.2.3"))
			.build();

	Query satisfiableAnswerOnly = tokenInfoQuery(target, ANSWER_ONLY);

	GetTokenInfoResourceUsage subject;

	@BeforeEach
	private void setup() throws Throwable {
		expected = mock(FeeData.class);
		view = mock(StateView.class);
		estimator = mock(TokenGetInfoUsage.class);
		factory = mock(Function.class);
		given(factory.apply(any())).willReturn(estimator);

		GetTokenInfoResourceUsage.factory = factory;

		given(estimator.givenCurrentAdminKey(any())).willReturn(estimator);
		given(estimator.givenCurrentWipeKey(any())).willReturn(estimator);
		given(estimator.givenCurrentKycKey(any())).willReturn(estimator);
		given(estimator.givenCurrentSupplyKey(any())).willReturn(estimator);
		given(estimator.givenCurrentFreezeKey(any())).willReturn(estimator);
		given(estimator.givenCurrentSymbol(any())).willReturn(estimator);
		given(estimator.givenCurrentName(any())).willReturn(estimator);
		given(estimator.givenCurrentMemo(any())).willReturn(estimator);
		given(estimator.givenCurrentlyUsingAutoRenewAccount()).willReturn(estimator);
		given(estimator.get()).willReturn(expected);

		given(view.infoForToken(target)).willReturn(Optional.of(info));

		subject = new GetTokenInfoResourceUsage();
	}

	@Test
	public void recognizesApplicableQuery() {
		// given:
		var applicable = tokenInfoQuery(target, COST_ANSWER);
		var inapplicable = Query.getDefaultInstance();

		// expect:
		assertTrue(subject.applicableTo(applicable));
		assertFalse(subject.applicableTo(inapplicable));
	}

	@Test
	public void setsInfoInQueryCxtIfPresent() {
		// setup:
		var queryCtx = new HashMap<String, Object>();

		// when:
		var usage = subject.usageGiven(satisfiableAnswerOnly, view, queryCtx);

		// then:
		assertSame(info, queryCtx.get(TOKEN_INFO_CTX_KEY));
		assertSame(expected, usage);
		// and:
		verify(estimator).givenCurrentAdminKey(Optional.of(TxnHandlingScenario.TOKEN_ADMIN_KT.asKey()));
		verify(estimator).givenCurrentWipeKey(Optional.of(TxnHandlingScenario.TOKEN_WIPE_KT.asKey()));
		verify(estimator).givenCurrentKycKey(Optional.of(TxnHandlingScenario.TOKEN_KYC_KT.asKey()));
		verify(estimator).givenCurrentSupplyKey(Optional.of(TxnHandlingScenario.TOKEN_SUPPLY_KT.asKey()));
		verify(estimator).givenCurrentFreezeKey(Optional.of(TxnHandlingScenario.TOKEN_FREEZE_KT.asKey()));
		verify(estimator).givenCurrentSymbol(symbol);
		verify(estimator).givenCurrentName(name);
		verify(estimator).givenCurrentlyUsingAutoRenewAccount();
		verify(estimator).givenCurrentMemo(memo);
	}

	@Test
	public void onlySetsTokenInfoInQueryCxtIfFound() {
		// setup:
		var queryCtx = new HashMap<String, Object>();

		given(view.infoForToken(target)).willReturn(Optional.empty());

		// when:
		var usage = subject.usageGiven(satisfiableAnswerOnly, view, queryCtx);

		// then:
		assertFalse(queryCtx.containsKey(GetTokenInfoAnswer.TOKEN_INFO_CTX_KEY));
		// and:
		assertSame(FeeData.getDefaultInstance(), usage);
	}

	private Query tokenInfoQuery(TokenID id, ResponseType type) {
		TokenGetInfoQuery.Builder op = TokenGetInfoQuery.newBuilder()
				.setToken(id)
				.setHeader(QueryHeader.newBuilder().setResponseType(type));
		return Query.newBuilder()
				.setTokenGetInfo(op)
				.build();
	}
}
