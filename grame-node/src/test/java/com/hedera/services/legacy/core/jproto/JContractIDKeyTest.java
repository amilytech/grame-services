package com.grame.services.legacy.core.jproto;

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

import com.gramegrame.api.proto.java.ContractID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JContractIDKeyTest {
	@Test
	public void zeroContractIDKeyTest() {
		JContractIDKey key = new JContractIDKey(ContractID.newBuilder().build());
		assertTrue(key.isEmpty());
		assertFalse(key.isValid());
	}

	@Test
	public void nonZeroContractIDKeyTest() {
		JContractIDKey key = new JContractIDKey(ContractID.newBuilder().setContractNum(1L).build());
		assertFalse(key.isEmpty());
		assertTrue(key.isValid());
	}

	@Test
	public void scheduleOpsAsExpected() {
		var subject = new JContractIDKey(ContractID.newBuilder().setContractNum(1L).build());
		assertFalse(subject.isForScheduledTxn());
		subject.setForScheduledTxn(true);
		assertTrue(subject.isForScheduledTxn());
	}
}
