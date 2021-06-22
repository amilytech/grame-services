package com.grame.test.factories.txns;

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

import com.gramegrame.api.proto.java.ScheduleID;
import com.gramegrame.api.proto.java.ScheduleSignTransactionBody;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;

public class ScheduleSignFactory extends SignedTxnFactory<ScheduleSignFactory> {
    private ScheduleSignFactory() {}

    private ScheduleID id;

    public static ScheduleSignFactory newSignedScheduleSign() {
        return new ScheduleSignFactory();
    }

    public ScheduleSignFactory signing(ScheduleID id) {
        this.id = id;
        return this;
    }

    @Override
    protected ScheduleSignFactory self() {
        return this;
    }

    @Override
    protected long feeFor(Transaction signedTxn, int numPayerKeys) {
        return 0;
    }

    @Override
    protected void customizeTxn(TransactionBody.Builder txn) {
        var op = ScheduleSignTransactionBody.newBuilder()
                .setScheduleID(id);
        txn.setScheduleSign(op);
    }
}
