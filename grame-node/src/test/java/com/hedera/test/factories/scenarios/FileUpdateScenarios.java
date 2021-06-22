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

import static com.grame.test.factories.txns.FileUpdateFactory.*;
import static com.grame.test.factories.txns.PlatformTxnFactory.from;

public enum FileUpdateScenarios implements TxnHandlingScenario {
	VANILLA_FILE_UPDATE_SCENARIO {
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedFileUpdate(MISC_FILE_ID).get()
			));
		}
	},
	TREASURY_SYS_FILE_UPDATE_SCENARIO {
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedFileUpdate(SYS_FILE_ID)
							.payer(TREASURY_PAYER_ID)
							.newWaclKt(SIMPLE_NEW_WACL_KT)
							.get()
			));
		}
	},
	MASTER_SYS_FILE_UPDATE_SCENARIO {
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedFileUpdate(SYS_FILE_ID)
							.payer(MASTER_PAYER_ID)
							.newWaclKt(SIMPLE_NEW_WACL_KT)
							.get()
			));
		}
	},
	IMMUTABLE_FILE_UPDATE_SCENARIO {
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedFileUpdate(IMMUTABLE_FILE_ID).get()
			));
		}
	},
	FILE_UPDATE_NEW_WACL_SCENARIO {
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedFileUpdate(MISC_FILE_ID)
							.newWaclKt(SIMPLE_NEW_WACL_KT)
							.get()
			));
		}
	}
}
