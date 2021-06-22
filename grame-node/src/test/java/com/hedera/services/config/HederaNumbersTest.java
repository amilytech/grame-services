package com.grame.services.config;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

class grameNumbersTest {
	PropertySource properties;
	grameNumbers subject;

	@BeforeEach
	private void setup() {
		properties = mock(PropertySource.class);
		given(properties.getLongProperty("grame.numReservedSystemEntities")).willReturn(123L);
		given(properties.getLongProperty("grame.shard")).willReturn(1L);
		given(properties.getLongProperty("grame.realm")).willReturn(2L);

		subject = new grameNumbers(properties);
	}

	@Test
	public void hasExpectedNumbers() {
		// expect:
		assertEquals(1L, subject.shard());
		assertEquals(2L, subject.realm());
		assertEquals(123L, subject.numReservedSystemEntities());
	}
}
