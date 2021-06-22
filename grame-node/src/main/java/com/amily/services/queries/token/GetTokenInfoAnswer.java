package com.grame.services.queries.token;

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
import com.grame.services.queries.AnswerService;
import com.grame.services.txns.validation.OptionValidator;
import com.grame.services.utils.SignedTxnAccessor;
import com.gramegrame.api.proto.java.TokenGetInfoQuery;
import com.gramegrame.api.proto.java.TokenGetInfoResponse;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.TokenInfo;

import java.util.Map;
import java.util.Optional;

import static com.grame.services.utils.SignedTxnAccessor.uncheckedFrom;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenGetInfo;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TOKEN_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseType.COST_ANSWER;

public class GetTokenInfoAnswer implements AnswerService {
	public static final String TOKEN_INFO_CTX_KEY = GetTokenInfoAnswer.class.getSimpleName() + "_tokenInfo";

	@Override
	public boolean needsAnswerOnlyCost(Query query) {
		return COST_ANSWER == query.getTokenGetInfo().getHeader().getResponseType();
	}

	@Override
	public boolean requiresNodePayment(Query query) {
		return typicallyRequiresNodePayment(query.getTokenGetInfo().getHeader().getResponseType());
	}

	@Override
	public Response responseGiven(Query query, StateView view, ResponseCodeEnum validity, long cost) {
		return responseFor(query, view, validity, cost, NO_QUERY_CTX);
	}

	@Override
	public Response responseGiven(
			Query query,
			StateView view,
			ResponseCodeEnum validity,
			long cost,
			Map<String, Object> queryCtx
	) {
		return responseFor(query, view, validity, cost, Optional.of(queryCtx));
	}

	@Override
	public ResponseCodeEnum checkValidity(Query query, StateView view) {
		var token = query.getTokenGetInfo().getToken();

		return view.tokenExists(token) ? OK : INVALID_TOKEN_ID;
	}

	@Override
	public grameFunctionality canonicalFunction() {
		return TokenGetInfo;
	}

	@Override
	public ResponseCodeEnum extractValidityFrom(Response response) {
		return response.getTokenGetInfo().getHeader().getNodeTransactionPrecheckCode();
	}

	@Override
	public Optional<SignedTxnAccessor> extractPaymentFrom(Query query) {
		var paymentTxn = query.getTokenGetInfo().getHeader().getPayment();
		return Optional.ofNullable(uncheckedFrom(paymentTxn));
	}

	private Response responseFor(
			Query query,
			StateView view,
			ResponseCodeEnum validity,
			long cost,
			Optional<Map<String, Object>> queryCtx
	) {
		var op = query.getTokenGetInfo();
		var response = TokenGetInfoResponse.newBuilder();

		var type = op.getHeader().getResponseType();
		if (validity != OK) {
			response.setHeader(header(validity, type, cost));
		} else {
			if (type == COST_ANSWER) {
				response.setHeader(costAnswerHeader(OK, cost));
			} else {
				setAnswerOnly(response, view, op, cost, queryCtx);
			}
		}

		return Response.newBuilder()
				.setTokenGetInfo(response)
				.build();
	}

	@SuppressWarnings("unchecked")
	private void setAnswerOnly(
			TokenGetInfoResponse.Builder response,
			StateView view,
			TokenGetInfoQuery op,
			long cost,
			Optional<Map<String, Object>> queryCtx
	) {
		if (queryCtx.isPresent()) {
			var ctx = queryCtx.get();
			if (!ctx.containsKey(TOKEN_INFO_CTX_KEY)) {
				response.setHeader(answerOnlyHeader(INVALID_TOKEN_ID));
			} else {
				response.setHeader(answerOnlyHeader(OK, cost));
				response.setTokenInfo((TokenInfo)ctx.get(TOKEN_INFO_CTX_KEY));
			}
		} else {
			var info = view.infoForToken(op.getToken());
			if (info.isEmpty()) {
				response.setHeader(answerOnlyHeader(INVALID_TOKEN_ID));
			} else {
				response.setHeader(answerOnlyHeader(OK, cost));
				response.setTokenInfo(info.get());
			}
		}
	}
}
