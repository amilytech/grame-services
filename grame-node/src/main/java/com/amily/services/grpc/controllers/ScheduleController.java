package com.grame.services.grpc.controllers;

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

import com.grame.services.queries.answering.QueryResponseHelper;
import com.grame.services.queries.schedule.ScheduleAnswers;
import com.grame.services.txns.submission.TxnResponseHelper;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionResponse;
import com.gramegrame.service.proto.java.ScheduleServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.gramegrame.api.proto.java.grameFunctionality.ScheduleCreate;
import static com.gramegrame.api.proto.java.grameFunctionality.ScheduleDelete;
import static com.gramegrame.api.proto.java.grameFunctionality.ScheduleGetInfo;
import static com.gramegrame.api.proto.java.grameFunctionality.ScheduleSign;

public class ScheduleController extends ScheduleServiceGrpc.ScheduleServiceImplBase {
    private static final Logger log = LogManager.getLogger(ScheduleController.class);

    private final ScheduleAnswers scheduleAnswers;
    private final TxnResponseHelper txnHelper;
    private final QueryResponseHelper queryHelper;

    public ScheduleController(
            ScheduleAnswers scheduleAnswers,
            TxnResponseHelper txnHelper,
            QueryResponseHelper queryHelper
    ) {
        this.txnHelper = txnHelper;
        this.queryHelper = queryHelper;
        this.scheduleAnswers = scheduleAnswers;
    }

    @Override
    public void createSchedule(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
        txnHelper.submit(signedTxn, observer, ScheduleCreate);
    }

    @Override
    public void signSchedule(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
        txnHelper.submit(signedTxn, observer, ScheduleSign);
    }

    @Override
    public void deleteSchedule(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
        txnHelper.submit(signedTxn, observer, ScheduleDelete);
    }

    @Override
    public void getScheduleInfo(Query query, StreamObserver<Response> observer) {
        queryHelper.answer(query, observer, scheduleAnswers.getScheduleInfo(), ScheduleGetInfo);
    }
}
