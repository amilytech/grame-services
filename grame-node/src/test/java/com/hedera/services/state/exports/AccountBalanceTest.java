package com.grame.services.state.exports;

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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountBalanceTest {
	@Test
	public void comparesAllIdParts() {
		// setup:
		var a = new AccountBalance(5, 4, 3, 5);
		var b = new AccountBalance(15, 20, 1, 25);
		var c = new AccountBalance(5, 20, 4, 25);
		var d = new AccountBalance(5, 4, 3, 25);

		// expect:
		assertTrue(a.compareTo(b) < 0);
		assertTrue(a.compareTo(c) < 0);
		assertTrue(a.compareTo(d) == 0);
	}

	@Test
	public void beanWorks() {
		// setup:
		var a = new AccountBalance(5, 4, 3, 6);

		// expect:
		assertEquals(5, a.getShard());
		assertEquals(4, a.getRealm());
		assertEquals(3, a.getNum());
		assertEquals(6, a.getBalance());
	}
}
