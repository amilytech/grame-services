package com.grame.services.stats;

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

import com.swirlds.common.StatEntry;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CounterFactoryTest {
	CounterFactory subject = new CounterFactory() { };

	@Test
	public void constructsExpectedEntry() {
		// setup:
		var name = "MyOp";
		var desc = "Happy thoughts";
		Supplier<Object> pretend = () -> 123;

		// when:
		StatEntry counter = subject.from(name, desc, pretend);

		// then:
		assertEquals("app", counter.category);
		assertEquals(name, counter.name);
		assertEquals(desc, counter.desc);
		assertEquals("%d", counter.format);
		assertNull(counter.buffered);
		assertNull(counter.init);
		assertSame(pretend, counter.supplier);
	}
}
