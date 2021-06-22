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

import com.grame.services.config.MockGlobalDynamicProps;
import com.grame.services.context.primitives.StateView;
import com.grame.services.usage.SigUsage;
import com.grame.services.usage.schedule.ScheduleOpsUsage;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.ScheduleDeleteTransactionBody;
import com.gramegrame.api.proto.java.ScheduleID;
import com.gramegrame.api.proto.java.ScheduleInfo;
import com.gramegrame.api.proto.java.ScheduleSignTransactionBody;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionID;
import com.gramegrame.fee.SigValueObj;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ScheduleDeleteResourceUsageTest {
    TransactionID scheduledTxnId = TransactionID.newBuilder()
            .setScheduled(true)
            .setAccountID(IdUtils.asAccount("0.0.2"))
            .build();

    ScheduleDeleteResourceUsage subject;

    StateView view;
    ScheduleOpsUsage scheduleOpsUsage;

    long expiry = 1_234_567L;
    int numSigs = 10, sigsSize = 100, numPayerKeys = 3;
    ScheduleID target = IdUtils.asSchedule("0.0.123");
    SigValueObj obj = new SigValueObj(numSigs, numPayerKeys, sigsSize);
    SigUsage sigUsage = new SigUsage(numSigs, sigsSize, numPayerKeys);
    FeeData expected;

    TransactionBody nonScheduleDeleteTxn;
    TransactionBody scheduleDeleteTxn;

    ScheduleInfo info = ScheduleInfo.newBuilder()
            .setScheduledTransactionID(scheduledTxnId)
            .setExpirationTime(Timestamp.newBuilder().setSeconds(expiry))
            .build();

    @BeforeEach
    private void setup() {
        expected = mock(FeeData.class);
        view = mock(StateView.class);
        scheduleDeleteTxn = mock(TransactionBody.class);
        given(scheduleDeleteTxn.hasScheduleDelete()).willReturn(true);
        given(scheduleDeleteTxn.getScheduleDelete())
                .willReturn(ScheduleDeleteTransactionBody.newBuilder()
                        .setScheduleID(target)
                        .build());

        nonScheduleDeleteTxn = mock(TransactionBody.class);
        given(nonScheduleDeleteTxn.hasScheduleDelete()).willReturn(false);

        scheduleOpsUsage = mock(ScheduleOpsUsage.class);
        given(scheduleOpsUsage.scheduleDeleteUsage(scheduleDeleteTxn, sigUsage, expiry)).willReturn(expected);

        given(view.infoForSchedule(target)).willReturn(Optional.of(info));

        subject = new ScheduleDeleteResourceUsage(scheduleOpsUsage, new MockGlobalDynamicProps());
    }

    @Test
    public void recognizesApplicableQuery() {
        // expect:
        assertTrue(subject.applicableTo(scheduleDeleteTxn));
        assertFalse(subject.applicableTo(nonScheduleDeleteTxn));
    }

    @Test
    public void delegatesToCorrectEstimate() throws Exception {
        // expect:
        assertEquals(expected, subject.usageGiven(scheduleDeleteTxn, obj, view));
    }

    @Test
    public void returnsDefaultIfInfoMissing() throws Exception {
        // setup:
        long start = 1_234_567L;
        TransactionID txnId = TransactionID.newBuilder()
                .setTransactionValidStart(Timestamp.newBuilder()
                        .setSeconds(start))
                .build();
        given(scheduleDeleteTxn.getTransactionID()).willReturn(txnId);
        given(view.infoForSchedule(target)).willReturn(Optional.empty());
        given(scheduleOpsUsage.scheduleDeleteUsage(scheduleDeleteTxn, sigUsage, start + 1800))
                .willReturn(expected);

        // expect:
        assertEquals(expected, subject.usageGiven(scheduleDeleteTxn, obj, view));
        // and:
        verify(view).infoForSchedule(target);
    }
}
