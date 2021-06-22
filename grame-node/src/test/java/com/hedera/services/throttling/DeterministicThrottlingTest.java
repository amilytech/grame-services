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

import com.grame.services.throttles.BucketThrottle;
import com.grame.services.throttles.DeterministicThrottle;
import com.grame.test.utils.SerdeUtils;
import com.gramegrame.api.proto.java.grameFunctionality;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import static com.gramegrame.api.proto.java.grameFunctionality.ContractCall;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoTransfer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class DeterministicThrottlingTest {
	int n = 2;
	int aTps = 6, bTps = 12;
	DeterministicThrottle a = DeterministicThrottle.withTps(aTps);
	DeterministicThrottle b = DeterministicThrottle.withTps(bTps);
	Instant consensusNow = Instant.ofEpochSecond(1_234_567L, 123);

	DeterministicThrottling subject;

	@Mock
	ThrottleReqsManager manager;

	@BeforeEach
	void setUp() {
		subject = new DeterministicThrottling(() -> n);
	}

	@Test
	void managerBehavesAsExpectedForMultiBucketOp() throws IOException {
		// setup:
		var defs = SerdeUtils.pojoDefs("bootstrap/throttles.json");

		// when:
		subject.rebuildFor(defs);
		// and:
		var ans = subject.shouldThrottle(ContractCall, consensusNow);
		var throttlesNow = subject.activeThrottlesFor(ContractCall);
		// and:
		var aNow = throttlesNow.get(0);
		var bNow = throttlesNow.get(1);

		// then:
		assertFalse(ans);
		assertEquals( 2500 * BucketThrottle.capacityUnitsPerTxn(), aNow.used());
		assertEquals( BucketThrottle.capacityUnitsPerTxn(), bNow.used());
	}

	@Test
	void logsAsExpected() throws IOException {
		// setup:
		var defs = SerdeUtils.pojoDefs("bootstrap/throttles.json");
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		var desired = "Resolved throttles (after splitting capacity 2 ways) - \n" +
				"  ContractCall: min{6.00 tps (A), 5.00 tps (B)}\n" +
				"  CryptoCreate: min{5000.00 tps (A), 1.00 tps (C)}\n" +
				"  CryptoGetAccountBalance: min{500000.00 tps (D)}\n" +
				"  CryptoTransfer: min{5000.00 tps (A)}\n" +
				"  TokenAssociateToAccount: min{50.00 tps (C)}\n" +
				"  TokenCreate: min{50.00 tps (C)}\n" +
				"  TokenMint: min{1500.00 tps (A)}\n" +
				"  TransactionGetReceipt: min{500000.00 tps (D)}";

		var mockLog = mock(Logger.class);

		// when:
		subject.rebuildFor(defs);
		// and:
		subject.logResolvedDefinitions(mockLog);

		// then:
		verify(mockLog).info(captor.capture());
		assertEquals(desired, captor.getValue());
	}

	@Test
	void constructsExpectedBucketsFromTestResource() throws IOException {
		// setup:
		var defs = SerdeUtils.pojoDefs("bootstrap/throttles.json");
		// and:
		var expected = List.of(
				DeterministicThrottle.withMtpsAndBurstPeriod(15_000_000, 2),
				DeterministicThrottle.withMtpsAndBurstPeriod(5_000, 2),
				DeterministicThrottle.withMtpsAndBurstPeriod(50_000, 3),
				DeterministicThrottle.withMtpsAndBurstPeriod(500_000_000, 4));

		// when:
		subject.rebuildFor(defs);
		// and:
		var rebuilt = subject.allActiveThrottles();

		// then:
		assertEquals(expected, rebuilt);
	}

	@Test
	public void alwaysRejectsIfNoThrottle() {
		// expect:
		assertTrue(subject.shouldThrottle(ContractCall, consensusNow));
	}

	@Test
	void returnsNoActiveThrottlesForUnconfiguredOp() {
		Assertions.assertSame(Collections.emptyList(), subject.activeThrottlesFor(ContractCall));
	}

	@Test
	public void shouldAllowWithEnoughCapacity() {
		// setup:
		subject.functionReqs = reqsManager();

		given(manager.allReqsMetAt(consensusNow)).willReturn(true);

		// then:
		assertFalse(subject.shouldThrottle(CryptoTransfer, consensusNow));
	}

	@Test
	void requiresExplicitTimestamp() {
		// expect:
		assertThrows(UnsupportedOperationException.class, () -> subject.shouldThrottle(CryptoTransfer));
	}

	private EnumMap<grameFunctionality, ThrottleReqsManager> reqsManager() {
		EnumMap<grameFunctionality, ThrottleReqsManager> opsManagers = new EnumMap<>(grameFunctionality.class);
		opsManagers.put(CryptoTransfer, manager);
		return opsManagers;
	}
}
