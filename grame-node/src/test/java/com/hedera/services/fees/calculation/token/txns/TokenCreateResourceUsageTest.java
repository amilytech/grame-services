package com.grame.services.fees.calculation.token.txns;

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
import com.grame.services.usage.SigUsage;
import com.grame.services.usage.token.TokenCreateUsage;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.*;
import com.gramegrame.fee.SigValueObj;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.verify;

class TokenCreateResourceUsageTest {
	long now = 1_000_000L;
	TransactionBody nonTokenCreateTxn;
	TransactionBody tokenCreateTxn;
	int numSigs = 10, sigsSize = 100, numPayerKeys = 3;
	SigValueObj obj = new SigValueObj(numSigs, numPayerKeys, sigsSize);
	SigUsage sigUsage = new SigUsage(numSigs, sigsSize, numPayerKeys);
	AccountID treasury = IdUtils.asAccount("1.2.3");
	TransactionID txnId = TransactionID.newBuilder()
			.setTransactionValidStart(Timestamp.newBuilder().setSeconds(now))
			.build();

	BiFunction<TransactionBody, SigUsage, TokenCreateUsage> factory;
	FeeData expected;

	StateView view;
	TokenCreateUsage usage;

	TokenCreateResourceUsage subject;

	@BeforeEach
	private void setup() throws Throwable {
		expected = mock(FeeData.class);
		view = mock(StateView.class);

		tokenCreateTxn = mock(TransactionBody.class);
		given(tokenCreateTxn.hasTokenCreation()).willReturn(true);
		var tokenCreation = TokenCreateTransactionBody.newBuilder().setTreasury(treasury).build();
		given(tokenCreateTxn.getTokenCreation()).willReturn(tokenCreation);
		given(tokenCreateTxn.getTransactionID()).willReturn(txnId);

		nonTokenCreateTxn = mock(TransactionBody.class);
		given(nonTokenCreateTxn.hasTokenCreation()).willReturn(false);

		usage = mock(TokenCreateUsage.class);
		given(usage.get()).willReturn(expected);

		factory = (BiFunction<TransactionBody, SigUsage, TokenCreateUsage>)mock(BiFunction.class);
		given(factory.apply(tokenCreateTxn, sigUsage)).willReturn(usage);

		TokenCreateResourceUsage.factory = factory;
		subject = new TokenCreateResourceUsage();
	}

	@Test
	public void recognizesApplicability() {
		// expect:
		assertTrue(subject.applicableTo(tokenCreateTxn));
		assertFalse(subject.applicableTo(nonTokenCreateTxn));
	}

	@Test
	public void delegatesToCorrectEstimate() throws Exception {
		// when:
		var actual = subject.usageGiven(tokenCreateTxn, obj, view);

		// expect:
		assertSame(expected, actual);
	}
}
