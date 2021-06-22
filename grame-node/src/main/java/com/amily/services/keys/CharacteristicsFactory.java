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

import com.grame.services.files.grameFs;
import com.grame.services.legacy.core.jproto.JKeyList;
import com.gramegrame.api.proto.java.TransactionBody;

import java.util.function.Function;

import static com.grame.services.keys.DefaultActivationCharacteristics.DEFAULT_ACTIVATION_CHARACTERISTICS;

public class CharacteristicsFactory {
	static Function<JKeyList, KeyActivationCharacteristics>	revocationServiceCharacteristicsFn =
			RevocationServiceCharacteristics::forTopLevelFile;

	private final grameFs hfs;

	public CharacteristicsFactory(grameFs hfs) {
		this.hfs = hfs;
	}

	@SuppressWarnings("unchecked")
	public KeyActivationCharacteristics inferredFor(TransactionBody txn) {
		if (!txn.hasFileDelete() || !txn.getFileDelete().hasFileID()) {
			return DEFAULT_ACTIVATION_CHARACTERISTICS;
		} else {
			var target = txn.getFileDelete().getFileID();
			if (hfs.exists(target)) {
				var wacl = hfs.getattr(target).getWacl();
				return revocationServiceCharacteristicsFn.apply((JKeyList)wacl);
			} else {
				return DEFAULT_ACTIVATION_CHARACTERISTICS;
			}
		}
	}
}
