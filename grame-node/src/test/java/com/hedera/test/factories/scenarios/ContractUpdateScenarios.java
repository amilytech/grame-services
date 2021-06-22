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

import static com.grame.test.factories.txns.ContractUpdateFactory.*;
import static com.grame.test.factories.txns.PlatformTxnFactory.from;

public enum ContractUpdateScenarios implements TxnHandlingScenario {
	CONTRACT_UPDATE_EXPIRATION_ONLY_SCENARIO {
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedContractUpdate(MISC_CONTRACT_ID).newExpiration(DEFAULT_EXPIRY).get()
			));
		}
	},
	CONTRACT_UPDATE_EXPIRATION_PLUS_NEW_ADMIN_KEY_SCENARIO {
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedContractUpdate(MISC_CONTRACT_ID)
							.newAdminKt(SIMPLE_NEW_ADMIN_KT)
							.newExpiration(DEFAULT_EXPIRY)
							.get()
			));
		}
	},
	CONTRACT_UPDATE_EXPIRATION_PLUS_NEW_DEPRECATED_CID_ADMIN_KEY_SCENARIO {
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedContractUpdate(MISC_CONTRACT_ID)
							.newDeprecatedAdminKey(true)
							.newExpiration(DEFAULT_EXPIRY)
							.get()
			));
		}
	},
	CONTRACT_UPDATE_EXPIRATION_PLUS_NEW_PROXY_SCENARIO {
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedContractUpdate(MISC_CONTRACT_ID)
							.newProxyAccount(MISC_ACCOUNT_ID)
							.newExpiration(DEFAULT_EXPIRY)
							.get()
			));
		}
	},
	CONTRACT_UPDATE_EXPIRATION_PLUS_NEW_AUTORENEW_SCENARIO {
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedContractUpdate(MISC_CONTRACT_ID)
							.newAutoRenewPeriod(DEFAULT_PERIOD)
							.newExpiration(DEFAULT_EXPIRY)
							.get()
			));
		}
	},
	CONTRACT_UPDATE_EXPIRATION_PLUS_NEW_FILE_SCENARIO {
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedContractUpdate(MISC_CONTRACT_ID)
							.newFile(MISC_FILE_ID)
							.newExpiration(DEFAULT_EXPIRY)
							.get()
			));
		}
	},
	CONTRACT_UPDATE_EXPIRATION_PLUS_NEW_MEMO {
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedContractUpdate(MISC_CONTRACT_ID)
							.newMemo(DEFAULT_MEMO)
							.newExpiration(DEFAULT_EXPIRY)
							.get()
			));
		}
	},
	CONTRACT_UPDATE_WITH_NEW_ADMIN_KEY {
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedContractUpdate(MISC_CONTRACT_ID).newAdminKt(SIMPLE_NEW_ADMIN_KT).get()
			));
		}
	}
}
