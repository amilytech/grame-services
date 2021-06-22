package com.grame.services.keys;

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

import com.grame.test.factories.scenarios.TxnHandlingScenario;
import com.grame.services.legacy.core.jproto.JKeyList;
import com.grame.services.legacy.core.jproto.JThresholdKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.grame.services.keys.DefaultActivationCharacteristics.DEFAULT_ACTIVATION_CHARACTERISTICS;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultActivationCharacteristicsTest {
	JKeyList l;
	JThresholdKey t;

	KeyActivationCharacteristics subject = DEFAULT_ACTIVATION_CHARACTERISTICS;

	@BeforeEach
	private void setup() throws Exception {
		l = TxnHandlingScenario.MISC_FILE_WACL_KT.asJKey().getKeyList();
		t = TxnHandlingScenario.LONG_THRESHOLD_KT.asJKey().getThresholdKey();
	}

	@Test
	public void defaultsToListLength() {
		// expect:
		assertEquals(l.getKeysList().size(), subject.sigsNeededForList(l));
	}

	@Test
	public void defaultsToThresholdReq() {
		// expect:
		assertEquals(t.getThresholdKey().getThreshold(), subject.sigsNeededForThreshold(t));
	}
}
