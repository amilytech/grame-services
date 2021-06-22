package com.grame.test.factories.scenarios;

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

import com.grame.services.utils.PlatformTxnAccessor;

import static com.grame.test.factories.txns.PlatformTxnFactory.from;
import static com.grame.test.factories.txns.TokenBurnFactory.newSignedTokenBurn;

public enum TokenBurnScenarios implements TxnHandlingScenario {
	BURN_WITH_SUPPLY_KEYED_TOKEN {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenBurn()
							.burning(KNOWN_TOKEN_WITH_SUPPLY)
							.nonPayerKts(TOKEN_SUPPLY_KT)
							.get()
			));
		}
	},
	BURN_WITH_MISSING_TOKEN {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenBurn()
							.burning(UNKNOWN_TOKEN)
							.get()
			));
		}
	},
	BURN_FOR_TOKEN_WITHOUT_SUPPLY {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenBurn()
							.burning(KNOWN_TOKEN_NO_SPECIAL_KEYS)
							.get()
			));
		}
	},
}
