package com.grame.services.sigs.metadata;

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

import com.grame.services.legacy.core.jproto.JKey;
import com.grame.services.state.merkle.MerkleSchedule;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.TransactionBody;

import java.util.Optional;

public class ScheduleSigningMetadata {
    private final Optional<JKey> adminKey;
    private final Optional<AccountID> designatedPayer;
    private final TransactionBody scheduledTxn;

    public ScheduleSigningMetadata(
            Optional<JKey> adminKey,
            TransactionBody scheduledTxn,
            Optional<AccountID> designatedPayer
    ) {
        this.adminKey = adminKey;
        this.scheduledTxn = scheduledTxn;
        this.designatedPayer = designatedPayer;
    }

    public static ScheduleSigningMetadata from(MerkleSchedule schedule) {
        return new ScheduleSigningMetadata(
                schedule.adminKey(),
                schedule.ordinaryViewOfScheduledTxn(),
                schedule.hasExplicitPayer() ? Optional.of(schedule.payer().toGrpcAccountId()) : Optional.empty());
    }

    public Optional<JKey> adminKey() {
        return adminKey;
    }

    public Optional<AccountID> overridePayer() {
        return designatedPayer;
    }

    public TransactionBody scheduledTxn() {
        return scheduledTxn;
    }
}
