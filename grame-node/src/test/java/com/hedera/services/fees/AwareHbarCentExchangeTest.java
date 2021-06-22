package com.grame.services.fees;

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

import com.grame.services.context.TransactionContext;
import com.grame.services.utils.PlatformTxnAccessor;
import com.gramegrame.api.proto.java.ExchangeRate;
import com.gramegrame.api.proto.java.ExchangeRateSet;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.TimestampSeconds;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

class AwareHbarCentExchangeTest {
	long validStartTime = 1_234_567L;
	Timestamp validStart = Timestamp.newBuilder()
		.setSeconds(validStartTime).build();
	TransactionBody txn = TransactionBody.newBuilder()
			.setTransactionID(TransactionID.newBuilder()
					.setTransactionValidStart(validStart)
					.build())
			.build();

	ExchangeRate current, next;
	ExchangeRateSet rates;
	PlatformTxnAccessor accessor;
	TransactionContext txnCtx;

	AwareHbarCentExchange subject;

	@BeforeEach
	public void setUp() throws Exception {
		next = mock(ExchangeRate.class);
		current = mock(ExchangeRate.class);

		rates = mock(ExchangeRateSet.class);
		given(rates.getCurrentRate()).willReturn(current);
		given(rates.getNextRate()).willReturn(next);

		txnCtx = mock(TransactionContext.class);
		accessor = mock(PlatformTxnAccessor.class);

		given(txnCtx.accessor()).willReturn(accessor);
		given(accessor.getTxn()).willReturn(txn);

		subject = new AwareHbarCentExchange(txnCtx);
		subject.updateRates(rates);
	}

	@Test
	public void updatesWork() {
		// expect:
		assertSame(rates, subject.rates);
		// and:
		assertSame(rates, subject.activeRates());
	}

	@Test
	public void returnsCurrentRateIfNotExpired() {
		given(current.getExpirationTime())
				.willReturn(TimestampSeconds.newBuilder().setSeconds(validStartTime + 1).build());

		// when:
		var ratesNow = subject.rate(Timestamp.newBuilder().setSeconds(validStartTime).build());

		// then:
		assertSame(current, ratesNow);
	}

	@Test
	public void returnsNextRateIfNotExpired() {
		given(current.getExpirationTime())
				.willReturn(TimestampSeconds.newBuilder().setSeconds(validStartTime - 1).build());

		// when:
		var ratesNow = subject.rate(Timestamp.newBuilder().setSeconds(validStartTime).build());

		// then:
		assertSame(next, ratesNow);
	}

	@Test
	public void usesValidStartToGetRate() {
		given(current.getExpirationTime())
				.willReturn(TimestampSeconds.newBuilder().setSeconds(validStartTime + 1).build());

		// when:
		var activeRates = subject.activeRate();

		// then:
		assertSame(current, activeRates);
	}
}
