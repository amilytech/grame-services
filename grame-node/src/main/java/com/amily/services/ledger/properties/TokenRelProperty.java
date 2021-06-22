package com.grame.services.ledger.properties;

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

import com.grame.services.state.merkle.MerkleTokenRelStatus;

import java.util.function.BiConsumer;
import java.util.function.Function;

public enum TokenRelProperty implements BeanProperty<MerkleTokenRelStatus> {
	TOKEN_BALANCE {
		@Override
		public BiConsumer<MerkleTokenRelStatus, Object> setter() {
			return (a, l) -> a.setBalance((long)l);
		}

		@Override
		public Function<MerkleTokenRelStatus, Object> getter() {
			return MerkleTokenRelStatus::getBalance;
		}
	},
	IS_FROZEN {
		@Override
		public BiConsumer<MerkleTokenRelStatus, Object> setter() {
			return (a, f) -> a.setFrozen((boolean)f);
		}

		@Override
		public Function<MerkleTokenRelStatus, Object> getter() {
			return MerkleTokenRelStatus::isFrozen;
		}
	},
	IS_KYC_GRANTED {
		@Override
		public BiConsumer<MerkleTokenRelStatus, Object> setter() {
			return (a, f) -> a.setKycGranted((boolean)f);
		}

		@Override
		public Function<MerkleTokenRelStatus, Object> getter() {
			return MerkleTokenRelStatus::isKycGranted;
		}
	}
}
