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
import static com.grame.test.factories.txns.TokenUpdateFactory.newSignedTokenUpdate;

public enum TokenUpdateScenarios implements TxnHandlingScenario {
	UPDATE_WITH_NO_KEYS_AFFECTED {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenUpdate()
							.updating(KNOWN_TOKEN_NO_SPECIAL_KEYS)
							.get()
			));
		}
	},
	UPDATE_REPLACING_TREASURY {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenUpdate()
							.updating(KNOWN_TOKEN_NO_SPECIAL_KEYS)
							.newTreasury(TOKEN_TREASURY)
							.get()
			));
		}
	},
	UPDATE_REPLACING_WITH_MISSING_TREASURY {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenUpdate()
							.updating(KNOWN_TOKEN_NO_SPECIAL_KEYS)
							.newTreasury(MISSING_ACCOUNT)
							.get()
			));
		}
	},
	UPDATE_REPLACING_ADMIN_KEY {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenUpdate()
							.updating(KNOWN_TOKEN_NO_SPECIAL_KEYS)
							.newAdmin(TOKEN_REPLACE_KT)
							.get()
			));
		}
	},
	UPDATE_WITH_SUPPLY_KEYED_TOKEN {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenUpdate()
							.updating(KNOWN_TOKEN_WITH_SUPPLY)
							.replacingSupply()
							.get()
			));
		}
	},
	UPDATE_WITH_KYC_KEYED_TOKEN {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenUpdate()
							.updating(KNOWN_TOKEN_WITH_KYC)
							.replacingKyc()
							.get()
			));
		}
	},
	UPDATE_WITH_FREEZE_KEYED_TOKEN {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenUpdate()
							.updating(KNOWN_TOKEN_WITH_FREEZE)
							.replacingFreeze()
							.get()
			));
		}
	},
	UPDATE_WITH_WIPE_KEYED_TOKEN {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenUpdate()
							.updating(KNOWN_TOKEN_WITH_WIPE)
							.replacingWipe()
							.get()
			));
		}
	},
	UPDATE_WITH_MISSING_TOKEN {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenUpdate()
							.updating(UNKNOWN_TOKEN)
							.get()
			));
		}
	},
	UPDATE_WITH_MISSING_TOKEN_ADMIN_KEY {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenUpdate()
							.updating(KNOWN_TOKEN_IMMUTABLE)
							.get()
			));
		}
	},
	TOKEN_UPDATE_WITH_NEW_AUTO_RENEW_ACCOUNT {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenUpdate()
							.updating(KNOWN_TOKEN_NO_SPECIAL_KEYS)
							.newAutoRenew(MISC_ACCOUNT)
							.get()
			));
		}
	},
	TOKEN_UPDATE_WITH_MISSING_AUTO_RENEW_ACCOUNT {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedTokenUpdate()
							.updating(KNOWN_TOKEN_NO_SPECIAL_KEYS)
							.newAutoRenew(MISSING_ACCOUNT)
							.get()
			));
		}
	}
}
