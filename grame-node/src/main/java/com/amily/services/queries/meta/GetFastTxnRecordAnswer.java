package com.grame.services.queries.meta;

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
import com.grame.services.utils.SignedTxnAccessor;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.api.proto.java.TransactionGetFastRecordQuery;
import com.gramegrame.api.proto.java.TransactionGetFastRecordResponse;

import java.util.Optional;
import static com.gramegrame.api.proto.java.grameFunctionality.TransactionGetRecord;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.NOT_SUPPORTED;
import static com.gramegrame.api.proto.java.ResponseType.COST_ANSWER;

public class GetFastTxnRecordAnswer implements AnswerService {
	@Override
	public Response responseGiven(Query query, StateView view, ResponseCodeEnum validity, long cost) {
		TransactionGetFastRecordQuery op = query.getTransactionGetFastRecord();
		ResponseType type = op.getHeader().getResponseType();

		TransactionGetFastRecordResponse.Builder response = TransactionGetFastRecordResponse.newBuilder();
		if (type == COST_ANSWER) {
			response.setHeader(costAnswerHeader(NOT_SUPPORTED, 0L));
		} else {
			response.setHeader(answerOnlyHeader(NOT_SUPPORTED));
		}
		return Response.newBuilder()
				.setTransactionGetFastRecord(response)
				.build();
	}

	@Override
	public ResponseCodeEnum extractValidityFrom(Response response) {
		return response.getTransactionGetFastRecord().getHeader().getNodeTransactionPrecheckCode();
	}

	@Override
	public ResponseCodeEnum checkValidity(Query query, StateView view) {
		return NOT_SUPPORTED;
	}

	@Override
	public grameFunctionality canonicalFunction() {
		return TransactionGetRecord;
	}

	@Override
	public Optional<SignedTxnAccessor> extractPaymentFrom(Query query) {
		return Optional.empty();
	}

	@Override
	public boolean needsAnswerOnlyCost(Query query) {
		return false;
	}

	@Override
	public boolean requiresNodePayment(Query query) {
		return false;
	}
}
