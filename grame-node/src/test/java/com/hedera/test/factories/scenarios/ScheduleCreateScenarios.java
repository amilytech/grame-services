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

import com.google.protobuf.ByteString;
import com.grame.services.utils.PlatformTxnAccessor;
import com.gramegrame.api.proto.java.Transaction;

import static com.grame.test.factories.txns.PlatformTxnFactory.from;
import static com.grame.test.factories.txns.ScheduleCreateFactory.newSignedScheduleCreate;
import static com.grame.test.factories.txns.CryptoTransferFactory.newSignedCryptoTransfer;
import static com.grame.test.factories.txns.TinyBarsFromTo.tinyBarsFromTo;
import static com.grame.test.factories.txns.ScheduleSignFactory.newSignedScheduleSign;

public enum ScheduleCreateScenarios implements TxnHandlingScenario {
    SCHEDULE_CREATE_NESTED_SCHEDULE_SIGN {
        public PlatformTxnAccessor platformTxn() throws Throwable {
            return new PlatformTxnAccessor(from(
                    newSignedScheduleCreate()
                            .missingAdmin()
                            .creating(newSignedScheduleSign().signing(KNOWN_SCHEDULE_IMMUTABLE).get())
                            .get()
            ));
        }
    },
    SCHEDULE_CREATE_XFER_NO_ADMIN {
        public PlatformTxnAccessor platformTxn() throws Throwable {
            return new PlatformTxnAccessor(from(
                    newSignedScheduleCreate()
                            .missingAdmin()
                            .creating(newSignedCryptoTransfer()
                                    .skipPayerSig()
                                    .transfers(tinyBarsFromTo(MISC_ACCOUNT_ID, RECEIVER_SIG_ID, 1_000L))
                                    .get())
                            .get()
            ));
        }
    },
    SCHEDULE_CREATE_INVALID_XFER {
        public PlatformTxnAccessor platformTxn() throws Throwable {
            return new PlatformTxnAccessor(from(
                    newSignedScheduleCreate()
                            .missingAdmin()
                            .creating(newSignedCryptoTransfer()
                                    .skipPayerSig()
                                    .transfers(tinyBarsFromTo(MISSING_ACCOUNT_ID, RECEIVER_SIG_ID, 1_000L))
                                    .get())
                            .get()
            ));
        }
    },
    SCHEDULE_CREATE_XFER_WITH_ADMIN {
        public PlatformTxnAccessor platformTxn() throws Throwable {
            return new PlatformTxnAccessor(from(
                    newSignedScheduleCreate()
                            .creating(newSignedCryptoTransfer()
                                    .skipPayerSig()
                                    .transfers(tinyBarsFromTo(MISC_ACCOUNT_ID, RECEIVER_SIG_ID, 1_000L))
                                    .get())
                            .get()
            ));
        }
    },
    SCHEDULE_CREATE_XFER_WITH_ADMIN_AND_PAYER {
        public PlatformTxnAccessor platformTxn() throws Throwable {
            return new PlatformTxnAccessor(from(
                    newSignedScheduleCreate()
							.designatingPayer(DILIGENT_SIGNING_PAYER)
                            .creating(newSignedCryptoTransfer()
                                    .skipPayerSig()
                                    .transfers(tinyBarsFromTo(MISC_ACCOUNT_ID, RECEIVER_SIG_ID, 1_000L))
                                    .get())
                            .get()
            ));
        }
    },
    SCHEDULE_CREATE_XFER_WITH_MISSING_PAYER {
        public PlatformTxnAccessor platformTxn() throws Throwable {
            return new PlatformTxnAccessor(from(
                    newSignedScheduleCreate()
                            .missingAdmin()
                            .designatingPayer(MISSING_ACCOUNT)
                            .creating(newSignedCryptoTransfer()
                                    .skipPayerSig()
                                    .transfers(tinyBarsFromTo(MISC_ACCOUNT_ID, RECEIVER_SIG_ID, 1_000L))
                                    .get())
                            .get()
            ));
        }
    },
}
