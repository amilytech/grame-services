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

import static com.grame.test.factories.txns.TokenWipeFactory.newSignedTokenWipe;
import static com.grame.test.factories.txns.PlatformTxnFactory.from;

public enum TokenWipeScenarios implements TxnHandlingScenario {
	VALID_WIPE_WITH_EXTANT_TOKEN {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenWipe()
							.wiping(KNOWN_TOKEN_WITH_WIPE, MISC_ACCOUNT)
							.nonPayerKts(TOKEN_WIPE_KT)
							.get()
			));
		}
	},
	WIPE_WITH_MISSING_TOKEN {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenWipe()
							.wiping(UNKNOWN_TOKEN, MISC_ACCOUNT)
							.get()
			));
		}
	},
	WIPE_FOR_TOKEN_WITHOUT_KEY {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenWipe()
							.wiping(KNOWN_TOKEN_NO_SPECIAL_KEYS, MISC_ACCOUNT)
							.nonPayerKts(TOKEN_KYC_KT)
							.get()
			));
		}
	},
}
