package com.grame.services.records;

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

import com.grame.services.context.properties.PropertySource;
import com.gramegrame.api.proto.java.TransactionID;
import com.gramegrame.api.proto.java.TransactionRecord;
import org.junit.jupiter.api.Test;

import static com.grame.services.utils.SleepingPause.SLEEPING_PAUSE;
import static com.grame.test.utils.IdUtils.asAccount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

class RecordCacheFactoryTest {
	private TransactionID txnIdA = TransactionID.newBuilder()
			.setAccountID(asAccount("0.0.2"))
			.build();
	private TransactionID txnIdB = TransactionID.newBuilder()
			.setAccountID(asAccount("2.2.0"))
			.build();

	private PropertySource properties;
	private RecordCacheFactory subject;

	@Test
	public void hasExpectedExpiry() {
		// setup:
		properties = mock(PropertySource.class);
		subject = new RecordCacheFactory(properties);

		given(properties.getIntProperty("cache.records.ttl")).willReturn(1);

		// when:
		var cache = subject.getRecordCache();
		cache.put(txnIdA, RecordCache.MARKER);

		// then:
		assertEquals(RecordCache.MARKER, cache.getIfPresent(txnIdA));
		assertNull(cache.getIfPresent(txnIdB));
		SLEEPING_PAUSE.forMs(500L);
		assertEquals(RecordCache.MARKER, cache.getIfPresent(txnIdA));
		SLEEPING_PAUSE.forMs(500L);
		assertNull(cache.getIfPresent(txnIdA));
	}

}
