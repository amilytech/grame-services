package com.grame.services.queries.schedule;

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
import com.grame.services.txns.validation.OptionValidator;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.QueryHeader;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.api.proto.java.ScheduleGetInfoQuery;
import com.gramegrame.api.proto.java.ScheduleGetInfoResponse;
import com.gramegrame.api.proto.java.ScheduleID;
import com.gramegrame.api.proto.java.ScheduleInfo;
import com.gramegrame.api.proto.java.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.grame.test.factories.scenarios.TxnHandlingScenario.COMPLEX_KEY_ACCOUNT_KT;
import static com.grame.test.utils.IdUtils.asAccount;
import static com.grame.test.utils.IdUtils.asSchedule;
import static com.grame.test.utils.TxnUtils.payerSponsoredTransfer;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_SCHEDULE_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.RESULT_SIZE_LIMIT_EXCEEDED;
import static com.gramegrame.api.proto.java.ResponseType.ANSWER_ONLY;
import static com.gramegrame.api.proto.java.ResponseType.COST_ANSWER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class GetScheduleInfoAnswerTest {
    private Transaction paymentTxn;
    private String node = "0.0.3";
    private String payer = "0.0.12345";
    private AccountID payerAccount = asAccount(payer);
    private AccountID creatorAccount = asAccount("0.0.12346");
    private ScheduleID scheduleID = asSchedule("1.2.3");
    private long fee = 1_234L;

    StateView view;
    OptionValidator optionValidator;

    ScheduleInfo info;

    GetScheduleInfoAnswer subject;

    @BeforeEach
    public void setup() {
        info = ScheduleInfo.newBuilder()
                .setScheduleID(scheduleID)
                .setPayerAccountID(payerAccount)
                .setCreatorAccountID(creatorAccount)
                .build();

        view = mock(StateView.class);
        optionValidator = mock(OptionValidator.class);

        subject = new GetScheduleInfoAnswer();
    }

    @Test
    public void getsTheInfo() throws Throwable {
        // setup:
        Query query = validQuery(ANSWER_ONLY, fee, scheduleID);

        given(view.infoForSchedule(scheduleID)).willReturn(Optional.of(info));

        // when:
        Response response = subject.responseGiven(query, view, OK, fee);

        // then:
        assertTrue(response.hasScheduleGetInfo());
        assertTrue(response.getScheduleGetInfo().hasHeader(), "Missing response header!");
        assertEquals(OK, response.getScheduleGetInfo().getHeader().getNodeTransactionPrecheckCode());
        assertEquals(ANSWER_ONLY, response.getScheduleGetInfo().getHeader().getResponseType());
        assertEquals(fee, response.getScheduleGetInfo().getHeader().getCost());
        // and:
        var actual = response.getScheduleGetInfo().getScheduleInfo();
        assertEquals(info, actual);
    }

    @Test
    public void getsInfoFromCtxWhenAvailable() throws Throwable {
        // setup:
        Query sensibleQuery = validQuery(ANSWER_ONLY, 5L, scheduleID);
        Map<String, Object> ctx = new HashMap<>();

        // given:
        ctx.put(GetScheduleInfoAnswer.SCHEDULE_INFO_CTX_KEY, info);

        // when:
        Response response = subject.responseGiven(sensibleQuery, view, OK, 0L, ctx);

        // then:
        var opResponse = response.getScheduleGetInfo();
        assertTrue(opResponse.hasHeader(), "Missing response header!");
        assertEquals(OK, opResponse.getHeader().getNodeTransactionPrecheckCode());
        assertSame(info, opResponse.getScheduleInfo());
        // and:
        verify(view, never()).infoForSchedule(any());
    }

    @Test
    public void recognizesMissingInfoWhenNoCtxGiven() throws Throwable {
        // setup:
        Query sensibleQuery = validQuery(ANSWER_ONLY, 5L, scheduleID);

        given(view.infoForSchedule(scheduleID)).willReturn(Optional.empty());

        // when:
        Response response = subject.responseGiven(sensibleQuery, view, OK, 0L);

        // then:
        ScheduleGetInfoResponse opResponse = response.getScheduleGetInfo();
        assertTrue(opResponse.hasHeader(), "Missing response header!");
        assertEquals(INVALID_SCHEDULE_ID, opResponse.getHeader().getNodeTransactionPrecheckCode());
    }

    @Test
    public void recognizesMissingInfoWhenCtxGiven() throws Throwable {
        // setup:
        Query sensibleQuery = validQuery(ANSWER_ONLY, 5L, scheduleID);

        // when:
        Response response = subject.responseGiven(sensibleQuery, view, OK, 0L, Collections.emptyMap());

        // then:
        ScheduleGetInfoResponse opResponse = response.getScheduleGetInfo();
        assertTrue(opResponse.hasHeader(), "Missing response header!");
        assertEquals(INVALID_SCHEDULE_ID, opResponse.getHeader().getNodeTransactionPrecheckCode());
        verify(view, never()).infoForSchedule(any());
    }

    @Test
    public void getsCostAnswerResponse() throws Throwable {
        // setup:
        Query query = validQuery(COST_ANSWER, fee, scheduleID);

        // when:
        Response response = subject.responseGiven(query, view, OK, fee);

        // then:
        assertTrue(response.hasScheduleGetInfo());
        assertEquals(OK, response.getScheduleGetInfo().getHeader().getNodeTransactionPrecheckCode());
        assertEquals(COST_ANSWER, response.getScheduleGetInfo().getHeader().getResponseType());
        assertEquals(fee, response.getScheduleGetInfo().getHeader().getCost());
    }

    @Test
    public void getsInvalidResponse() throws Throwable {
        // setup:
        Query query = validQuery(COST_ANSWER, fee, scheduleID);

        // when:
        Response response = subject.responseGiven(query, view, INVALID_SCHEDULE_ID, fee);

        // then:
        assertTrue(response.hasScheduleGetInfo());
        assertEquals(
                INVALID_SCHEDULE_ID,
                response.getScheduleGetInfo().getHeader().getNodeTransactionPrecheckCode());
        assertEquals(COST_ANSWER, response.getScheduleGetInfo().getHeader().getResponseType());
        assertEquals(fee, response.getScheduleGetInfo().getHeader().getCost());
    }

    @Test
    public void recognizesFunction() {
        // expect:
        assertEquals(grameFunctionality.ScheduleGetInfo, subject.canonicalFunction());
    }

    @Test
    public void requiresAnswerOnlyPayment() throws Throwable {
        // expect:
        assertFalse(subject.requiresNodePayment(validQuery(COST_ANSWER, 0, scheduleID)));
        assertTrue(subject.requiresNodePayment(validQuery(ANSWER_ONLY, 0, scheduleID)));
    }

    @Test
    public void requiresAnswerOnlyCostAsExpected() throws Throwable {
        // expect:
        assertTrue(subject.needsAnswerOnlyCost(validQuery(COST_ANSWER, 0, scheduleID)));
        assertFalse(subject.needsAnswerOnlyCost(validQuery(ANSWER_ONLY, 0, scheduleID)));
    }

    @Test
    public void getsValidity() {
        // given:
        Response response = Response.newBuilder().setScheduleGetInfo(
                ScheduleGetInfoResponse.newBuilder()
                        .setHeader(subject.answerOnlyHeader(RESULT_SIZE_LIMIT_EXCEEDED))).build();

        // expect:
        assertEquals(RESULT_SIZE_LIMIT_EXCEEDED, subject.extractValidityFrom(response));
    }

    @Test
    public void usesViewToValidate() throws Throwable {
        // setup:
        Query query = validQuery(COST_ANSWER, fee, scheduleID);

        given(view.scheduleExists(scheduleID)).willReturn(false);

        // when:
        ResponseCodeEnum validity = subject.checkValidity(query, view);

        // then:
        assertEquals(INVALID_SCHEDULE_ID, validity);
    }

    @Test
    public void getsExpectedPayment() throws Throwable {
        // given:
        Query query = validQuery(COST_ANSWER, fee, scheduleID);

        // expect:
        assertEquals(paymentTxn, subject.extractPaymentFrom(query).get().getBackwardCompatibleSignedTxn());
    }

    private Query validQuery(ResponseType type, long payment, ScheduleID id) throws Throwable {
        this.paymentTxn = payerSponsoredTransfer(payer, COMPLEX_KEY_ACCOUNT_KT, node, payment);
        QueryHeader.Builder header = QueryHeader.newBuilder()
                .setPayment(this.paymentTxn)
                .setResponseType(type);
        ScheduleGetInfoQuery.Builder op = ScheduleGetInfoQuery.newBuilder()
                .setHeader(header)
                .setScheduleID(id);
        return Query.newBuilder().setScheduleGetInfo(op).build();
    }
}
