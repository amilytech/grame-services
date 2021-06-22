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

import com.grame.services.context.primitives.StateView;
import com.grame.services.fees.FeeCalculator;
import com.grame.services.fees.calculation.UsagePricesProvider;
import com.grame.services.queries.AnswerFlow;
import com.grame.services.queries.AnswerService;
import com.grame.services.throttling.FunctionalityThrottling;
import com.grame.services.txns.submission.PlatformSubmissionManager;
import com.grame.services.utils.SignedTxnAccessor;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.fee.FeeObject;
import com.grame.services.legacy.handler.TransactionHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.grame.services.utils.SignedTxnAccessor.uncheckedFrom;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.BUSY;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseType.ANSWER_ONLY;
import static com.grame.services.legacy.handler.TransactionHandler.IS_THROTTLE_EXEMPT;

public class StakedAnswerFlow implements AnswerFlow {
	private static final Logger log = LogManager.getLogger(StakedAnswerFlow.class);

	private static final SignedTxnAccessor defaultAccessor = uncheckedFrom(Transaction.getDefaultInstance());

	private final FeeCalculator fees;
	private final TransactionHandler legacyHandler;
	private final Supplier<StateView> stateViews;
	private final UsagePricesProvider resourceCosts;
	private final FunctionalityThrottling throttles;
	private final PlatformSubmissionManager submissionManager;

	public StakedAnswerFlow(
			FeeCalculator fees,
			TransactionHandler legacyHandler,
			Supplier<StateView> stateViews,
			UsagePricesProvider resourceCosts,
			FunctionalityThrottling throttles,
			PlatformSubmissionManager submissionManager
	) {
		this.fees = fees;
		this.throttles = throttles;
		this.stateViews = stateViews;
		this.legacyHandler = legacyHandler;
		this.resourceCosts = resourceCosts;
		this.submissionManager = submissionManager;
	}

	@Override
	public Response satisfyUsing(AnswerService service, Query query) {
		StateView view = stateViews.get();
		SignedTxnAccessor accessor = service.extractPaymentFrom(query).orElse(defaultAccessor);

		if (shouldThrottle(service, accessor)) {
			return service.responseGiven(query, view, BUSY);
		}

		ResponseCodeEnum validity = legacyHandler.validateQuery(query, service.requiresNodePayment(query));
		if (validity == OK) {
			validity = service.checkValidity(query, view);
		}
		if (validity != OK) {
			return service.responseGiven(query, view, validity);
		}

		Timestamp at = accessor.getTxnId().getTransactionValidStart();
		FeeData usagePrices = resourceCosts.pricesGiven(service.canonicalFunction(), at);

		long cost = 0L;
		Map<String, Object> queryCtx = new HashMap<>();
		if (service.requiresNodePayment(query)) {
			cost = totalOf(fees.computePayment(query, usagePrices, view, at, queryCtx));
			validity = validatePayment(cost, accessor);
			if (validity != OK) {
				return service.responseGiven(query, view, validity, cost);
			}
			validity = submissionManager.trySubmission(accessor);
			if (validity != OK) {
				return service.responseGiven(query, view, validity, cost);
			}
		}

		if (service.needsAnswerOnlyCost(query)) {
			cost = totalOf(fees.estimatePayment(query, usagePrices, view, at, ANSWER_ONLY));
		}

		return service.responseGiven(query, view, OK, cost, queryCtx);
	}

	private boolean shouldThrottle(AnswerService service, SignedTxnAccessor paymentAccessor) {
		if (IS_THROTTLE_EXEMPT.test(paymentAccessor.getPayer())) {
			return false;
		} else {
			return throttles.shouldThrottle(service.canonicalFunction());
		}
	}

	private long totalOf(FeeObject costs) {
		return costs.getNetworkFee() + costs.getServiceFee() + costs.getNodeFee();
	}

	private ResponseCodeEnum validatePayment(long requiredPayment, SignedTxnAccessor accessor) {
		if (requiredPayment > 0) {
			ResponseCodeEnum validity =
					legacyHandler.validateTransactionPreConsensus(accessor.getBackwardCompatibleSignedTxn(), true)
							.getValidity();
			if (validity == OK) {
				validity = legacyHandler.nodePaymentValidity(accessor.getBackwardCompatibleSignedTxn(), requiredPayment);
			}
			return validity;
		} else {
			return OK;
		}
	}
}
