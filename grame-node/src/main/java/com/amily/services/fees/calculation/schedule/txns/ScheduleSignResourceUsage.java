package com.grame.services.fees.calculation.schedule.txns;

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

import com.grame.services.context.primitives.StateView;
import com.grame.services.context.properties.GlobalDynamicProperties;
import com.grame.services.fees.calculation.TxnResourceUsageEstimator;
import com.grame.services.usage.SigUsage;
import com.grame.services.usage.schedule.ScheduleOpsUsage;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.exception.InvalidTxBodyException;
import com.gramegrame.fee.SigValueObj;


public class ScheduleSignResourceUsage implements TxnResourceUsageEstimator {
    private final ScheduleOpsUsage scheduleOpsUsage;
    private final GlobalDynamicProperties properties;

    public ScheduleSignResourceUsage(
            ScheduleOpsUsage scheduleOpsUsage,
            GlobalDynamicProperties properties
    ) {
        this.scheduleOpsUsage = scheduleOpsUsage;
        this.properties = properties;
    }

    @Override
    public boolean applicableTo(TransactionBody txn) {
        return txn.hasScheduleSign();
    }

    @Override
    public FeeData usageGiven(TransactionBody txn, SigValueObj svo, StateView view) throws InvalidTxBodyException {
    	var op = txn.getScheduleSign();
        var sigUsage = new SigUsage(svo.getTotalSigCount(), svo.getSignatureSize(), svo.getPayerAcctSigCount());

        var optionalInfo = view.infoForSchedule(op.getScheduleID());
        if (optionalInfo.isPresent()) {
            var info = optionalInfo.get();
            return scheduleOpsUsage.scheduleSignUsage(txn, sigUsage, info.getExpirationTime().getSeconds());
        } else {
            long latestExpiry = txn.getTransactionID().getTransactionValidStart().getSeconds()
                    + properties.scheduledTxExpiryTimeSecs();
            return scheduleOpsUsage.scheduleSignUsage(txn, sigUsage, latestExpiry);
        }
    }
}
