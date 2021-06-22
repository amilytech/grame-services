package com.grame.services.context;

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

import com.grame.services.context.properties.PropertySources;
import com.grame.services.exceptions.ContextNotFoundException;
import com.grame.services.state.submerkle.ExchangeRates;
import com.grame.services.state.submerkle.SequenceNumber;
import com.gramegrame.api.proto.java.ServicesConfigurationList;
import com.swirlds.common.AddressBook;
import com.swirlds.common.NodeId;
import com.swirlds.common.Platform;
import com.swirlds.fcmap.FCMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.grame.services.context.SingletonContextsManager.CONTEXTS;
import static org.mockito.BDDMockito.*;

public class SingletonContextsManagerTest {
	private final NodeId id = new NodeId(false, 1L);

	Platform platform;
	ServicesContext ctx;
	PropertySources propertySources;

	@BeforeEach
	private void resetContexts() {
		CONTEXTS.clear();
		ctx = mock(ServicesContext.class);
		given(ctx.id()).willReturn(id);
		platform = mock(Platform.class);
		propertySources = mock(PropertySources.class);
	}

	@Test
	public void failsFastOnMissingContext() {
		// expect:
		assertThrows(ContextNotFoundException.class, () -> CONTEXTS.lookup(1L));
	}

	@Test
	public void createsExpectedContext() {
		// given:
		assertFalse(CONTEXTS.isInitialized(1L));

		// when:
		CONTEXTS.store(ctx);

		// then:
		assertEquals(ctx, CONTEXTS.lookup(1L));
		// and:
		assertTrue(CONTEXTS.isInitialized(1L));
	}
}
