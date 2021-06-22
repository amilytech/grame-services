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
import com.grame.services.usage.token.TokenWipeUsage;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.fee.SigValueObj;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

class TokenWipeResourceUsageTest {
	private TokenWipeResourceUsage subject;

	private TransactionBody nonTokenWipeTxn;
	private TransactionBody tokenWipeTxn;

	StateView view;
	int numSigs = 10, sigsSize = 100, numPayerKeys = 3;
	SigValueObj obj = new SigValueObj(numSigs, numPayerKeys, sigsSize);
	SigUsage sigUsage = new SigUsage(numSigs, sigsSize, numPayerKeys);
	FeeData expected;

	TokenWipeUsage usage;
	BiFunction<TransactionBody, SigUsage, TokenWipeUsage> factory;

	@BeforeEach
	private void setup() throws Throwable {
		expected = mock(FeeData.class);
		view = mock(StateView.class);

		tokenWipeTxn = mock(TransactionBody.class);
		given(tokenWipeTxn.hasTokenWipe()).willReturn(true);

		nonTokenWipeTxn = mock(TransactionBody.class);
		given(nonTokenWipeTxn.hasTokenWipe()).willReturn(false);

		factory = (BiFunction<TransactionBody, SigUsage, TokenWipeUsage>)mock(BiFunction.class);
		given(factory.apply(tokenWipeTxn, sigUsage)).willReturn(usage);

		usage = mock(TokenWipeUsage.class);
		given(usage.get()).willReturn(expected);

		TokenWipeResourceUsage.factory = factory;
		given(factory.apply(tokenWipeTxn, sigUsage)).willReturn(usage);

		subject = new TokenWipeResourceUsage();
	}

	@Test
	public void recognizesApplicability() {
		// expect:
		assertTrue(subject.applicableTo(tokenWipeTxn));
		assertFalse(subject.applicableTo(nonTokenWipeTxn));
	}

	@Test
	public void delegatesToCorrectEstimate() throws Exception {
		// expect:
		assertEquals(
				expected,
				subject.usageGiven(tokenWipeTxn, obj, view));
	}
}
