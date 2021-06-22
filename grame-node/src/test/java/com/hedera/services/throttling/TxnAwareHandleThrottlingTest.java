package com.grame.services.throttling;

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
import com.grame.services.sysfiles.domain.throttling.ThrottleDefinitions;
import com.grame.services.throttles.DeterministicThrottle;
import com.gramegrame.api.proto.java.grameFunctionality;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static com.gramegrame.api.proto.java.grameFunctionality.Freeze;
import static org.mockito.BDDMockito.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TxnAwareHandleThrottlingTest {
	Instant consensusTime = Instant.ofEpochSecond(1_234_567L, 123);

	@Mock
	TimedFunctionalityThrottling delegate;
	@Mock
	TransactionContext txnCtx;

	TxnAwareHandleThrottling subject;

	@BeforeEach
	void setUp() {
		subject = new TxnAwareHandleThrottling(txnCtx, delegate);
	}

	@Test
	void delegatesThrottlingDecisionsWithConsensusTime() {
		given(txnCtx.consensusTime()).willReturn(consensusTime);
		given(delegate.shouldThrottle(grameFunctionality.CryptoTransfer, consensusTime)).willReturn(true);

		// expect:
		assertTrue(subject.shouldThrottle(grameFunctionality.CryptoTransfer));
		// and:
		verify(delegate).shouldThrottle(grameFunctionality.CryptoTransfer, consensusTime);
	}

	@Test
	void otherMethodsPassThrough() {
		// setup:
		ThrottleDefinitions defs = new ThrottleDefinitions();
		List<DeterministicThrottle> whatever = List.of(DeterministicThrottle.withTps(1));

		given(delegate.allActiveThrottles()).willReturn(whatever);
		given(delegate.activeThrottlesFor(grameFunctionality.CryptoTransfer)).willReturn(whatever);

		// when:
		var all = subject.allActiveThrottles();
		var onlyXfer = subject.activeThrottlesFor(grameFunctionality.CryptoTransfer);
		subject.rebuildFor(defs);

		// then:
		verify(delegate).rebuildFor(defs);
		assertSame(whatever, all);
		assertSame(whatever, onlyXfer);
	}
}
