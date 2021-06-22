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

import com.grame.services.legacy.core.jproto.JKeyList;
import com.grame.services.legacy.core.jproto.JThresholdKey;

import static com.grame.services.legacy.core.jproto.JKey.equalUpToDecodability;

public class RevocationServiceCharacteristics {
	public static KeyActivationCharacteristics forTopLevelFile(JKeyList wacl) {
		return new KeyActivationCharacteristics() {
			@Override
			public int sigsNeededForList(JKeyList l) {
				return equalUpToDecodability(l, wacl) ? 1 : l.getKeysList().size();
			}

			@Override
			public int sigsNeededForThreshold(JThresholdKey t) {
				return t.getThreshold();
			}
		};
	}
}
