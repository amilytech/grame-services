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
import com.gramegrame.api.proto.java.ResponseHeader;
import com.gramegrame.api.proto.java.ResponseType;

import java.util.Map;
import java.util.Optional;

import static com.gramegrame.api.proto.java.ResponseType.ANSWER_ONLY;
import static com.gramegrame.api.proto.java.ResponseType.ANSWER_STATE_PROOF;
import static com.gramegrame.api.proto.java.ResponseType.COST_ANSWER;

public interface AnswerService {
	Optional<Map<String, Object>> NO_QUERY_CTX = Optional.empty();

	boolean needsAnswerOnlyCost(Query query);
	boolean requiresNodePayment(Query query);
	Response responseGiven(Query query, StateView view, ResponseCodeEnum validity, long cost);
	default Response responseGiven(
			Query query,
			StateView view,
			ResponseCodeEnum validity,
			long cost,
			Map<String, Object> queryCtx
	) {
		return responseGiven(query, view, validity, cost);
	}
	ResponseCodeEnum checkValidity(Query query, StateView view);
	grameFunctionality canonicalFunction();
	ResponseCodeEnum extractValidityFrom(Response response);
	Optional<SignedTxnAccessor> extractPaymentFrom(Query query);

	default Response responseGiven(Query query, StateView view, ResponseCodeEnum validity) {
		return responseGiven(query, view, validity, 0L);
	}

	default ResponseHeader answerOnlyHeader(ResponseCodeEnum status) {
		return header(status, ANSWER_ONLY, 0);
	}

	default ResponseHeader answerOnlyHeader(ResponseCodeEnum status, long cost) {
		return header(status, ANSWER_ONLY, cost);
	}

	default ResponseHeader costAnswerHeader(ResponseCodeEnum status, long cost) {
		return header(status, COST_ANSWER, cost);
	}

	default ResponseHeader header(ResponseCodeEnum status, ResponseType type, long cost) {
		return ResponseHeader.newBuilder()
				.setNodeTransactionPrecheckCode(status)
				.setResponseType(type)
				.setCost(cost)
				.build();
	}

	default boolean typicallyRequiresNodePayment(ResponseType type) {
		return type == ANSWER_ONLY || type == ANSWER_STATE_PROOF;
	}
}
