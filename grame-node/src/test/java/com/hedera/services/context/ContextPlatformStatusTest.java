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

import com.grame.services.context.ContextPlatformStatus;
import org.junit.jupiter.api.Test;

import static com.swirlds.common.PlatformStatus.MAINTENANCE;
import static com.swirlds.common.PlatformStatus.STARTING_UP;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ContextPlatformStatusTest {
	ContextPlatformStatus subject = new ContextPlatformStatus();

	@Test
	public void beginsAsStartingUp() {
		// expect:
		assertEquals(STARTING_UP, subject.get());
	}

	@Test
	public void setterWorks() {
		// when:
		subject.set(MAINTENANCE);

		// then:
		assertEquals(MAINTENANCE, subject.get());
	}
}
