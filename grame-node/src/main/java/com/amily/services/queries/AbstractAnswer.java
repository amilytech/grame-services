package com.grame.services.queries;

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
import com.grame.services.utils.SignedTxnAccessor;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.api.proto.java.Transaction;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.gramegrame.api.proto.java.ResponseType.ANSWER_ONLY;
import static com.gramegrame.api.proto.java.ResponseType.COST_ANSWER;

public abstract class AbstractAnswer implements AnswerService {
	private final grameFunctionality function;
	private final Function<Query, Transaction> paymentExtractor;
	private final Function<Query, ResponseType> responseTypeExtractor;
	private final Function<Response, ResponseCodeEnum> statusExtractor;
	private final BiFunction<Query, StateView, ResponseCodeEnum> validityCheck;

	public AbstractAnswer(
			grameFunctionality function,
			Function<Query, Transaction> paymentExtractor,
			Function<Query, ResponseType> responseTypeExtractor,
			Function<Response, ResponseCodeEnum> statusExtractor,
			BiFunction<Query, StateView, ResponseCodeEnum> validityCheck
	) {
		this.function = function;
		this.validityCheck = validityCheck;
		this.statusExtractor = statusExtractor;
		this.paymentExtractor = paymentExtractor;
		this.responseTypeExtractor = responseTypeExtractor;
	}

	@Override
	public boolean needsAnswerOnlyCost(Query query) {
		return COST_ANSWER == responseTypeExtractor.apply(query);
	}

	@Override
	public boolean requiresNodePayment(Query query) {
		return typicallyRequiresNodePayment(responseTypeExtractor.apply(query));
	}

	@Override
	public ResponseCodeEnum checkValidity(Query query, StateView view) {
		return validityCheck.apply(query, view);
	}

	@Override
	public grameFunctionality canonicalFunction() {
		return function;
	}

	@Override
	public ResponseCodeEnum extractValidityFrom(Response response) {
		return statusExtractor.apply(response);
	}

	@Override
	public Optional<SignedTxnAccessor> extractPaymentFrom(Query query) {
		var paymentTxn = paymentExtractor.apply(query);

		return Optional.ofNullable(SignedTxnAccessor.uncheckedFrom(paymentTxn));
	}
}
