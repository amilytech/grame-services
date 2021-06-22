package com.grame.services.queries.answering;

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

import static com.gramegrame.api.proto.java.grameFunctionality.CryptoGetStakers;

import com.grame.services.context.primitives.StateView;
import com.grame.services.fees.FeeCalculator;
import com.grame.services.fees.calculation.UsagePricesProvider;
import com.grame.services.queries.AnswerService;
import com.grame.services.throttling.FunctionalityThrottling;
import com.grame.services.txns.submission.PlatformSubmissionManager;
import com.grame.services.utils.SignedTxnAccessor;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionID;
import com.gramegrame.fee.FeeObject;
import com.grame.services.context.domain.process.TxnValidityAndFeeReq;
import com.grame.services.legacy.handler.TransactionHandler;
import com.swirlds.common.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Supplier;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.*;
import static com.gramegrame.api.proto.java.ResponseType.ANSWER_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.*;
import static com.grame.test.utils.IdUtils.*;

class StakedAnswerFlowTest {
	Timestamp at = Timestamp.newBuilder().setSeconds(1_234_567L).build();
	grameFunctionality function = grameFunctionality.ConsensusGetTopicInfo;

	TransactionID userTxnId = TransactionID.newBuilder()
			.setAccountID(asAccount("0.0.1002"))
			.setTransactionValidStart(at)
			.build();
	Transaction userTxn = Transaction.newBuilder()
			.setBodyBytes(TransactionBody.newBuilder().setTransactionID(userTxnId).build().toByteString())
			.build();
	SignedTxnAccessor userAccessor = SignedTxnAccessor.uncheckedFrom(userTxn);

	TransactionID masterTxnId = TransactionID.newBuilder()
			.setAccountID(asAccount("0.0.50"))
			.setTransactionValidStart(at)
			.build();
	Transaction masterTxn = Transaction.newBuilder()
			.setBodyBytes(TransactionBody.newBuilder().setTransactionID(masterTxnId).build().toByteString())
			.build();
	SignedTxnAccessor masterAccessor = SignedTxnAccessor.uncheckedFrom(masterTxn);

	FeeObject costs = new FeeObject(1L, 2L, 3L);
	FeeObject zeroCosts = new FeeObject(0L, 0L, 0L);

	FeeCalculator fees;
	TransactionHandler legacyHandler;
	StateView view;
	Supplier<StateView> stateViews;
	UsagePricesProvider resourceCosts;
	FunctionalityThrottling throttles;
	PlatformSubmissionManager submissionManager;

	Query query = Query.getDefaultInstance();
	FeeData usagePrices;
	Response response;
	AnswerService service;

	StakedAnswerFlow subject;

	@BeforeEach
	private void setup() {
		fees = mock(FeeCalculator.class);
		view = mock(StateView.class);
		throttles = mock(FunctionalityThrottling.class);
		legacyHandler = mock(TransactionHandler.class);
		stateViews = () -> view;
		resourceCosts = mock(UsagePricesProvider.class);
		usagePrices = mock(FeeData.class);
		submissionManager = mock(PlatformSubmissionManager.class);

		service = mock(AnswerService.class);
		response = mock(Response.class);

		subject = new StakedAnswerFlow(fees, legacyHandler, stateViews, resourceCosts, throttles, submissionManager);
	}

	@Test
	public void doesntThrottleExemptAccounts() {
		// setup:
		Response wrongResponse = mock(Response.class);

		given(throttles.shouldThrottle(CryptoGetStakers)).willReturn(true);
		given(service.checkValidity(query, view)).willReturn(OK);
		given(legacyHandler.validateQuery(query, false)).willReturn(OK);
		given(service.extractPaymentFrom(query)).willReturn(Optional.of(masterAccessor));
		given(service.canonicalFunction()).willReturn(CryptoGetStakers);
		given(resourceCosts.pricesGiven(CryptoGetStakers, at)).willReturn(usagePrices);
		given(service.requiresNodePayment(query)).willReturn(false);
		given(service.needsAnswerOnlyCost(query)).willReturn(false);
		// and:
		given(service.responseGiven(query, view, BUSY)).willReturn(wrongResponse);
		given(service.responseGiven(
				argThat(query::equals),
				argThat(view::equals),
				argThat(OK::equals),
				longThat(l -> l == 0),
				anyMap())).willReturn(response);

		// when:
		Response actual = subject.satisfyUsing(service, query);

		// then:
		assertEquals(response, actual);
		verify(throttles, never()).shouldThrottle(CryptoGetStakers);
	}


	@Test
	public void throttlesIfAppropriate() {
		given(service.canonicalFunction()).willReturn(function);
		given(throttles.shouldThrottle(function)).willReturn(true);
		given(service.responseGiven(query, view, BUSY)).willReturn(response);

		// when:
		Response actual = subject.satisfyUsing(service, query);

		// then:
		assertEquals(response, actual);
		verify(throttles).shouldThrottle(function);
	}

	@Test
	public void validatesMetaAsExpected() {
		given(legacyHandler.validateQuery(query, false)).willReturn(ACCOUNT_IS_NOT_GENESIS_ACCOUNT);
		given(service.responseGiven(query, view, ACCOUNT_IS_NOT_GENESIS_ACCOUNT)).willReturn(response);

		// when:
		Response actual = subject.satisfyUsing(service, query);

		// then:
		assertEquals(response, actual);
		verify(service).requiresNodePayment(query);
	}

	@Test
	public void validatesSpecificAfterMetaOk() {
		given(legacyHandler.validateQuery(query, false)).willReturn(OK);
		given(service.checkValidity(query, view)).willReturn(INVALID_ACCOUNT_ID);
		given(service.responseGiven(query, view, INVALID_ACCOUNT_ID)).willReturn(response);

		// when:
		Response actual = subject.satisfyUsing(service, query);

		// then:
		assertEquals(response, actual);
		verify(service).requiresNodePayment(query);
	}

	@Test
	public void figuresResponseWhenNoNodePaymentNoAnswerOnlyCostRequired() {
		given(legacyHandler.validateQuery(query, false)).willReturn(OK);
		given(service.checkValidity(query, view)).willReturn(OK);
		given(service.extractPaymentFrom(query)).willReturn(Optional.of(userAccessor));
		given(service.canonicalFunction()).willReturn(CryptoGetStakers);
		given(resourceCosts.pricesGiven(CryptoGetStakers, at)).willReturn(usagePrices);
		given(service.requiresNodePayment(query)).willReturn(false);
		given(service.needsAnswerOnlyCost(query)).willReturn(false);
		given(service.responseGiven(
				argThat(query::equals),
				argThat(view::equals),
				argThat(OK::equals),
				longThat(l -> l == 0),
				anyMap())).willReturn(response);

		// when:
		Response actual = subject.satisfyUsing(service, query);

		// then:
		assertEquals(response, actual);
		verify(service, times(2)).requiresNodePayment(query);
		verify(service).needsAnswerOnlyCost(query);
		verifyNoInteractions(fees);
	}

	@Test
	public void figuresResponseWhenNoNodePaymentButAnswerOnlyCostRequired() {
		given(legacyHandler.validateQuery(query, false)).willReturn(OK);
		given(service.checkValidity(query, view)).willReturn(OK);
		given(service.extractPaymentFrom(query)).willReturn(Optional.of(userAccessor));
		given(service.canonicalFunction()).willReturn(CryptoGetStakers);
		given(resourceCosts.pricesGiven(CryptoGetStakers, at)).willReturn(usagePrices);
		given(service.requiresNodePayment(query)).willReturn(false);
		given(service.needsAnswerOnlyCost(query)).willReturn(true);
		given(fees.estimatePayment(query, usagePrices, view, at, ANSWER_ONLY)).willReturn(costs);
		given(service.responseGiven(
				argThat(query::equals),
				argThat(view::equals),
				argThat(OK::equals),
				longThat(l -> l == 6),
				any())).willReturn(response);

		// when:
		Response actual = subject.satisfyUsing(service, query);

		// then:
		assertEquals(response, actual);
		verify(service, times(2)).requiresNodePayment(query);
		verify(service).needsAnswerOnlyCost(query);
	}

	@Test
	public void figuresResponseWhenNodePaymentButNoAnswerOnlyCostRequired() throws Exception {
		given(legacyHandler.validateQuery(query, true)).willReturn(OK);
		given(service.checkValidity(query, view)).willReturn(OK);
		given(service.extractPaymentFrom(query)).willReturn(Optional.of(userAccessor));
		given(service.canonicalFunction()).willReturn(CryptoGetStakers);
		given(resourceCosts.pricesGiven(CryptoGetStakers, at)).willReturn(usagePrices);
		given(service.requiresNodePayment(query)).willReturn(true);
		given(service.needsAnswerOnlyCost(query)).willReturn(false);
		given(fees.computePayment(
				argThat(query::equals),
				argThat(usagePrices::equals),
				argThat(view::equals),
				argThat(at::equals),
				any())).willReturn(costs);
		given(legacyHandler.validateTransactionPreConsensus(userTxn, true))
				.willReturn(new TxnValidityAndFeeReq(INVALID_ACCOUNT_ID));
		given(service.responseGiven(query, view, INVALID_ACCOUNT_ID, 6)).willReturn(response);

		// when:
		Response actual = subject.satisfyUsing(service, query);

		// then:
		assertEquals(response, actual);
		verify(service, times(2)).requiresNodePayment(query);
		verify(submissionManager, never()).trySubmission(any());
	}

	@Test
	public void figuresResponseWhenZeroNodePaymentButNoAnswerOnlyCostRequired() {
		given(legacyHandler.validateQuery(query, true)).willReturn(OK);
		given(service.checkValidity(query, view)).willReturn(OK);
		given(service.extractPaymentFrom(query)).willReturn(Optional.of(userAccessor));
		given(service.canonicalFunction()).willReturn(CryptoGetStakers);
		given(resourceCosts.pricesGiven(CryptoGetStakers, at)).willReturn(usagePrices);
		given(service.requiresNodePayment(query)).willReturn(true);
		given(service.needsAnswerOnlyCost(query)).willReturn(false);
		given(fees.computePayment(
				argThat(query::equals),
				argThat(usagePrices::equals),
				argThat(view::equals),
				argThat(at::equals),
				any())).willReturn(zeroCosts);
		given(legacyHandler.validateTransactionPreConsensus(userTxn, true))
				.willReturn(new TxnValidityAndFeeReq(INVALID_ACCOUNT_ID));
		given(service.responseGiven(query, view, INVALID_ACCOUNT_ID, 6)).willReturn(response);

		// when:
		Response actual = subject.satisfyUsing(service, query);

		// then:
		verify(service, times(2)).requiresNodePayment(query);
		verify(legacyHandler, never()).validateTransactionPreConsensus(any(), anyBoolean());
	}

	@Test
	public void validatesFullyWhenNodePaymentButNoAnswerOnlyCostRequired() throws Exception {
		given(legacyHandler.validateQuery(query, true)).willReturn(OK);
		given(service.checkValidity(query, view)).willReturn(OK);
		given(service.extractPaymentFrom(query)).willReturn(Optional.of(userAccessor));
		given(service.canonicalFunction()).willReturn(CryptoGetStakers);
		given(resourceCosts.pricesGiven(CryptoGetStakers, at)).willReturn(usagePrices);
		given(service.requiresNodePayment(query)).willReturn(true);
		given(service.needsAnswerOnlyCost(query)).willReturn(false);
		given(fees.computePayment(
				argThat(query::equals),
				argThat(usagePrices::equals),
				argThat(view::equals),
				argThat(at::equals),
				any())).willReturn(costs);
		given(legacyHandler.validateTransactionPreConsensus(userTxn, true))
				.willReturn(new TxnValidityAndFeeReq(OK));
		given(legacyHandler.nodePaymentValidity(userTxn, 6)).willReturn(INSUFFICIENT_PAYER_BALANCE);
		given(service.responseGiven(query, view, INSUFFICIENT_PAYER_BALANCE, 6)).willReturn(response);

		// when:
		Response actual = subject.satisfyUsing(service, query);

		// then:
		assertEquals(response, actual);
		verify(service, times(2)).requiresNodePayment(query);
		verify(submissionManager, never()).trySubmission(any());
	}

	@Test
	public void submitsWhenAppropriate() throws Exception {
		given(legacyHandler.validateQuery(query, true)).willReturn(OK);
		given(service.checkValidity(query, view)).willReturn(OK);
		given(service.extractPaymentFrom(query)).willReturn(Optional.of(userAccessor));
		given(service.canonicalFunction()).willReturn(CryptoGetStakers);
		given(resourceCosts.pricesGiven(CryptoGetStakers, at)).willReturn(usagePrices);
		given(service.requiresNodePayment(query)).willReturn(true);
		given(service.needsAnswerOnlyCost(query)).willReturn(false);
		given(fees.computePayment(
				argThat(query::equals),
				argThat(usagePrices::equals),
				argThat(view::equals),
				argThat(at::equals),
				any())).willReturn(costs);
		given(legacyHandler.validateTransactionPreConsensus(userTxn, true))
				.willReturn(new TxnValidityAndFeeReq(OK));
		given(legacyHandler.nodePaymentValidity(userTxn, 6)).willReturn(OK);
		given(service.responseGiven(
				argThat(query::equals),
				argThat(view::equals),
				argThat(OK::equals),
				longThat(l -> l == 6),
				any())).willReturn(response);
		given(submissionManager.trySubmission(any())).willReturn(OK);

		// when:
		Response actual = subject.satisfyUsing(service, query);

		// then:
		assertEquals(response, actual);
		verify(service, times(2)).requiresNodePayment(query);
	}

	@Test
	public void recoversFromPtce() throws Exception {
		given(legacyHandler.validateQuery(query, true)).willReturn(OK);
		given(service.checkValidity(query, view)).willReturn(OK);
		given(service.extractPaymentFrom(query)).willReturn(Optional.of(userAccessor));
		given(service.canonicalFunction()).willReturn(CryptoGetStakers);
		given(resourceCosts.pricesGiven(CryptoGetStakers, at)).willReturn(usagePrices);
		given(service.requiresNodePayment(query)).willReturn(true);
		given(service.needsAnswerOnlyCost(query)).willReturn(false);
		given(fees.computePayment(
				argThat(query::equals),
				argThat(usagePrices::equals),
				argThat(view::equals),
				argThat(at::equals),
				any())).willReturn(costs);
		given(legacyHandler.validateTransactionPreConsensus(userTxn, true))
				.willReturn(new TxnValidityAndFeeReq(OK));
		given(legacyHandler.nodePaymentValidity(userTxn, 6)).willReturn(OK);
		given(submissionManager.trySubmission(any())).willReturn(PLATFORM_TRANSACTION_NOT_CREATED);
		given(service.responseGiven(query, view, PLATFORM_TRANSACTION_NOT_CREATED, 6)).willReturn(response);

		// when:
		Response actual = subject.satisfyUsing(service, query);

		// then:
		assertEquals(response, actual);
		verify(service, times(2)).requiresNodePayment(query);
		verify(submissionManager).trySubmission(any());
	}
}
