package com.grame.services.context.properties;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.grame.services.context.properties.Profile.DEV;
import static com.grame.services.context.properties.Profile.PROD;
import static com.grame.services.context.properties.Profile.TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class NodeLocalPropertiesTest {
	PropertySource properties;

	NodeLocalProperties subject;

	private static final Profile[] LEGACY_ENV_ORDER = { DEV, PROD, TEST };

	@BeforeEach
	public void setup() {
		properties = mock(PropertySource.class);
	}

	@Test
	public void constructsAsExpected() {
		givenPropsWithSeed(1);

		// when:
		subject = new NodeLocalProperties(properties);

		// expect:
		assertEquals(1, subject.port());
		assertEquals(2, subject.tlsPort());
		assertEquals(3, subject.precheckLookupRetries());
		assertEquals(4, subject.precheckLookupRetryBackoffMs());
		assertEquals(TEST, subject.activeProfile());
		assertEquals(6L, subject.statsHapiOpsSpeedometerUpdateIntervalMs());
		assertEquals(7.0, subject.statsSpeedometerHalfLifeSecs());
		assertEquals(8.0, subject.statsRunningAvgHalfLifeSecs());
		assertEquals(logDir(9), subject.recordLogDir());
		assertEquals(10L, subject.recordLogPeriod());
		assertTrue(subject.isRecordStreamEnabled());
		assertEquals(12, subject.recordStreamQueueCapacity());
		assertEquals(13, subject.queryBlobLookupRetries());
		assertEquals(14L, subject.nettyProdKeepAliveTime());
		assertEquals("grame1.crt", subject.nettyTlsCrtPath());
		assertEquals("grame2.key", subject.nettyTlsKeyPath());
		assertEquals(15L, subject.nettyProdKeepAliveTimeout());
		assertEquals(16L, subject.nettyMaxConnectionAge());
		assertEquals(17L, subject.nettyMaxConnectionAgeGrace());
		assertEquals(18L, subject.nettyMaxConnectionIdle());
		assertEquals(19, subject.nettyMaxConcurrentCalls());
		assertEquals(20, subject.nettyFlowControlWindow());
		assertEquals("0.0.4", subject.devListeningAccount());
		assertFalse(subject.devOnlyDefaultNodeListens());
		assertEquals("B", subject.accountsExportPath());
		assertFalse(subject.exportAccountsOnStartup());
		assertEquals(Profile.PROD, subject.nettyMode());
	}

	@Test
	public void reloadWorksAsExpected() {
		givenPropsWithSeed(2);

		// when:
		subject = new NodeLocalProperties(properties);

		// expect:
		assertEquals(2, subject.port());
		assertEquals(3, subject.tlsPort());
		assertEquals(4, subject.precheckLookupRetries());
		assertEquals(5, subject.precheckLookupRetryBackoffMs());
		assertEquals(DEV, subject.activeProfile());
		assertEquals(7L, subject.statsHapiOpsSpeedometerUpdateIntervalMs());
		assertEquals(8.0, subject.statsSpeedometerHalfLifeSecs());
		assertEquals(9.0, subject.statsRunningAvgHalfLifeSecs());
		assertEquals(logDir(10), subject.recordLogDir());
		assertEquals(11L, subject.recordLogPeriod());
		assertFalse(subject.isRecordStreamEnabled());
		assertEquals(13, subject.recordStreamQueueCapacity());
		assertEquals(14, subject.queryBlobLookupRetries());
		assertEquals(15L, subject.nettyProdKeepAliveTime());
		assertEquals("grame2.crt", subject.nettyTlsCrtPath());
		assertEquals("grame3.key", subject.nettyTlsKeyPath());
		assertEquals(16L, subject.nettyProdKeepAliveTimeout());
		assertEquals(17L, subject.nettyMaxConnectionAge());
		assertEquals(18L, subject.nettyMaxConnectionAgeGrace());
		assertEquals(19L, subject.nettyMaxConnectionIdle());
		assertEquals(20, subject.nettyMaxConcurrentCalls());
		assertEquals(21, subject.nettyFlowControlWindow());
		assertEquals("0.0.3", subject.devListeningAccount());
		assertTrue(subject.devOnlyDefaultNodeListens());
		assertEquals("A", subject.accountsExportPath());
		assertTrue(subject.exportAccountsOnStartup());
		assertEquals(Profile.TEST, subject.nettyMode());
	}

	private void givenPropsWithSeed(int i) {
		given(properties.getIntProperty("grpc.port")).willReturn(i);
		given(properties.getIntProperty("grpc.tlsPort")).willReturn(i + 1);
		given(properties.getIntProperty("precheck.account.maxLookupRetries")).willReturn(i + 2);
		given(properties.getIntProperty("precheck.account.lookupRetryBackoffIncrementMs")).willReturn(i + 3);
		given(properties.getProfileProperty("grame.profiles.active")).willReturn(LEGACY_ENV_ORDER[(i + 4) % 3]);
		given(properties.getLongProperty("stats.hapiOps.speedometerUpdateIntervalMs")).willReturn(i + 5L);
		given(properties.getDoubleProperty("stats.speedometerHalfLifeSecs")).willReturn(i + 6.0);
		given(properties.getDoubleProperty("stats.runningAvgHalfLifeSecs")).willReturn(i + 7.0);
		given(properties.getStringProperty("grame.recordStream.logDir")).willReturn(logDir(i + 8));
		given(properties.getLongProperty("grame.recordStream.logPeriod")).willReturn(i + 9L);
		given(properties.getBooleanProperty("grame.recordStream.isEnabled")).willReturn(i % 2 == 1);
		given(properties.getIntProperty("grame.recordStream.queueCapacity")).willReturn(i + 11);
		given(properties.getIntProperty("queries.blob.lookupRetries")).willReturn(i + 12);
		given(properties.getLongProperty("netty.prod.keepAliveTime")).willReturn(i + 13L);
		given(properties.getStringProperty("netty.tlsCrt.path")).willReturn("grame" + i + ".crt");
		given(properties.getStringProperty("netty.tlsKey.path")).willReturn("grame" + (i + 1) + ".key");
		given(properties.getLongProperty("netty.prod.keepAliveTimeout")).willReturn(i + 14L);
		given(properties.getLongProperty("netty.prod.maxConnectionAge")).willReturn(i + 15L);
		given(properties.getLongProperty("netty.prod.maxConnectionAgeGrace")).willReturn(i + 16L);
		given(properties.getLongProperty("netty.prod.maxConnectionIdle")).willReturn(i + 17L);
		given(properties.getIntProperty("netty.prod.maxConcurrentCalls")).willReturn(i + 18);
		given(properties.getIntProperty("netty.prod.flowControlWindow")).willReturn(i + 19);
		given(properties.getStringProperty("dev.defaultListeningNodeAccount"))
				.willReturn(i % 2 == 0 ? "0.0.3" : "0.0.4");
		given(properties.getBooleanProperty("dev.onlyDefaultNodeListens")).willReturn(i % 2 == 0);
		given(properties.getStringProperty("grame.accountsExportPath")).willReturn(i % 2 == 0 ? "A" : "B");
		given(properties.getBooleanProperty("grame.exportAccountsOnStartup")).willReturn(i % 2 == 0);
		given(properties.getProfileProperty("netty.mode")).willReturn(LEGACY_ENV_ORDER[(i + 21) % 3]);
	}

	static String logDir(int num) {
		return "myRecords/dir" + num;
	}
}
