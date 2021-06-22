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
import com.grame.services.utils.SignedTxnAccessor;
import com.grame.test.factories.txns.ScheduleUtils;
import com.gramegrame.api.proto.java.ScheduleCreateTransactionBody;
import com.gramegrame.api.proto.java.TransactionBody;

import static com.grame.test.factories.txns.CryptoTransferFactory.newSignedCryptoTransfer;
import static com.grame.test.factories.txns.PlatformTxnFactory.from;
import static com.grame.test.factories.txns.ScheduleSignFactory.newSignedScheduleSign;
import static com.grame.test.factories.txns.TinyBarsFromTo.tinyBarsFromTo;

public enum ScheduleSignScenarios implements TxnHandlingScenario {
	SCHEDULE_SIGN_MISSING_SCHEDULE {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedScheduleSign()
							.signing(UNKNOWN_SCHEDULE)
							.get()
			));
		}
	},
	SCHEDULE_SIGN_KNOWN_SCHEDULE {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedScheduleSign()
							.signing(KNOWN_SCHEDULE_WITH_ADMIN)
							.get()
			));
		}

		@Override
		public byte[] extantSchedulingBodyBytes() throws Throwable {
			var accessor = new SignedTxnAccessor(newSignedCryptoTransfer()
					.sansTxnId()
					.transfers(tinyBarsFromTo(MISC_ACCOUNT_ID, RECEIVER_SIG_ID, 1))
					.get());
			var scheduled = ScheduleUtils.fromOrdinary(accessor.getTxn());
			return TransactionBody.newBuilder()
					.setScheduleCreate(ScheduleCreateTransactionBody.newBuilder()
							.setScheduledTransactionBody(scheduled))
					.build()
					.toByteArray();
		}
	},
	SCHEDULE_SIGN_KNOWN_SCHEDULE_WITH_PAYER {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedScheduleSign()
							.signing(KNOWN_SCHEDULE_WITH_EXPLICIT_PAYER)
							.get()
			));
		}

		@Override
		public byte[] extantSchedulingBodyBytes() throws Throwable {
			var accessor = new SignedTxnAccessor(newSignedCryptoTransfer()
					.sansTxnId()
					.transfers(tinyBarsFromTo(MISC_ACCOUNT_ID, RECEIVER_SIG_ID, 1))
					.get());
			var scheduled = ScheduleUtils.fromOrdinary(accessor.getTxn());
			return TransactionBody.newBuilder()
					.setScheduleCreate(ScheduleCreateTransactionBody.newBuilder()
							.setScheduledTransactionBody(scheduled))
					.build()
					.toByteArray();
		}
	},
	SCHEDULE_SIGN_KNOWN_SCHEDULE_WITH_NOW_INVALID_PAYER {
		@Override
		public PlatformTxnAccessor platformTxn() throws Throwable {
			return new PlatformTxnAccessor(from(
					newSignedScheduleSign()
							.signing(KNOWN_SCHEDULE_WITH_NOW_INVALID_PAYER)
							.get()
			));
		}
	}
}
