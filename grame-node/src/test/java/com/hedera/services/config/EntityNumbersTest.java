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

import static org.junit.jupiter.api.Assertions.*;

import com.grame.test.utils.IdUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EntityNumbersTest {
	FileNumbers fileNumbers;
	grameNumbers grameNumbers;
	AccountNumbers accountNumbers;

	EntityNumbers subject;

	@BeforeEach
	public void setup() {
		fileNumbers = new MockFileNumbers();
		grameNumbers = new MockgrameNumbers();
		accountNumbers = new MockAccountNumbers();

		subject = new EntityNumbers(fileNumbers, grameNumbers, accountNumbers);
	}

	@Test
	public void hasExpectedMembers() {
		// expect:
		assertSame(fileNumbers, subject.files());
		assertSame(accountNumbers, subject.accounts());
		assertSame(grameNumbers, subject.all());
	}

	@Test
	public void recognizesSystemEntities() {
		// given:
		var sysFile = IdUtils.asFile("0.0.1000");
		var civFile = IdUtils.asFile("0.0.1001");
		var nonsenseFile = IdUtils.asFile("0.0.0");
		// and:
		var sysAccount = IdUtils.asAccount("0.0.1000");
		var civAccount = IdUtils.asAccount("0.0.1001");
		var nonsenseAccount = IdUtils.asAccount("0.0.0");
		// and:
		var sysContract = IdUtils.asContract("0.0.1000");
		var civContract = IdUtils.asContract("0.0.1001");
		var nonsenseContract = IdUtils.asContract("0.0.0");

		// expect:
		assertTrue(subject.isSystemFile(sysFile));
		assertFalse(subject.isSystemFile(civFile));
		assertTrue(subject.isSystemAccount(sysAccount));
		assertFalse(subject.isSystemAccount(civAccount));
		assertTrue(subject.isSystemContract(sysContract));
		assertFalse(subject.isSystemContract(civContract));
		// and:
		assertFalse(subject.isSystemAccount(nonsenseAccount));
		assertFalse(subject.isSystemFile(nonsenseFile));
		assertFalse(subject.isSystemContract(nonsenseContract));
	}
}
