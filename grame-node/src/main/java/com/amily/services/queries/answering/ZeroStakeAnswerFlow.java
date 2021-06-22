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
import com.grame.services.legacy.handler.TransactionHandler;
import com.grame.services.queries.AnswerFlow;
import com.grame.services.queries.AnswerService;
import com.grame.services.throttling.FunctionalityThrottling;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.BUSY;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;

public class ZeroStakeAnswerFlow implements AnswerFlow {
	private static final Logger log = LogManager.getLogger(ZeroStakeAnswerFlow.class);

	private final TransactionHandler legacyHandler;
	private final Supplier<StateView> stateViews;
	private final FunctionalityThrottling throttles;

	public ZeroStakeAnswerFlow(
			TransactionHandler legacyHandler,
			Supplier<StateView> stateViews,
			FunctionalityThrottling throttles
	) {
		this.legacyHandler = legacyHandler;
		this.stateViews = stateViews;
		this.throttles = throttles;
	}

	@Override
	public Response satisfyUsing(AnswerService service, Query query) {
		var view = stateViews.get();

		if (throttles.shouldThrottle(service.canonicalFunction())) {
			return service.responseGiven(query, view, BUSY);
		}

		var validity = legacyHandler.validateQuery(query, false);
		if (validity == OK) {
			validity = service.checkValidity(query, view);
		}

		return service.responseGiven(query, view, validity);
	}
}
