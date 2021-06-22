package com.grame.services.queries.contract;

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

import com.google.protobuf.ByteString;
import com.grame.services.context.primitives.StateView;
import com.grame.services.queries.AnswerService;
import com.grame.services.txns.validation.OptionValidator;
import com.grame.services.utils.SignedTxnAccessor;
import com.gramegrame.api.proto.java.ContractGetBytecodeResponse;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.ResponseCodeEnum;

import java.util.Optional;

import static com.grame.services.utils.SignedTxnAccessor.uncheckedFrom;
import static com.gramegrame.api.proto.java.grameFunctionality.ContractGetBytecode;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseType.COST_ANSWER;

public class GetBytecodeAnswer implements AnswerService {
	private static final byte[] EMPTY_BYTECODE = new byte[0];

	private final OptionValidator validator;

	public GetBytecodeAnswer(OptionValidator validator) {
		this.validator = validator;
	}

	@Override
	public boolean needsAnswerOnlyCost(Query query) {
		return COST_ANSWER == query.getContractGetBytecode().getHeader().getResponseType();
	}

	@Override
	public boolean requiresNodePayment(Query query) {
		return typicallyRequiresNodePayment(query.getContractGetBytecode().getHeader().getResponseType());
	}

	@Override
	public Response responseGiven(Query query, StateView view, ResponseCodeEnum validity, long cost) {
		var op = query.getContractGetBytecode();
		var target = op.getContractID();

		var response = ContractGetBytecodeResponse.newBuilder();
		var type = op.getHeader().getResponseType();
		if (validity != OK) {
			response.setHeader(header(validity, type, cost));
			response.setBytecode(ByteString.copyFrom(EMPTY_BYTECODE));
		} else {
			if (type == COST_ANSWER) {
				response.setHeader(costAnswerHeader(OK, cost));
				response.setBytecode(ByteString.copyFrom(EMPTY_BYTECODE));
			} else {
				/* Include cost here to satisfy legacy regression tests. */
				response.setHeader(answerOnlyHeader(OK, cost));
				response.setBytecode(ByteString.copyFrom(view.bytecodeOf(target).orElse(EMPTY_BYTECODE)));
			}
		}
		return Response.newBuilder()
				.setContractGetBytecodeResponse(response)
				.build();
	}

	@Override
	public ResponseCodeEnum checkValidity(Query query, StateView view) {
		var id = query.getContractGetBytecode().getContractID();

		return validator.queryableContractStatus(id, view.contracts());
	}

	@Override
	public grameFunctionality canonicalFunction() {
		return ContractGetBytecode;
	}

	@Override
	public ResponseCodeEnum extractValidityFrom(Response response) {
		return response.getContractGetBytecodeResponse().getHeader().getNodeTransactionPrecheckCode();
	}

	@Override
	public Optional<SignedTxnAccessor> extractPaymentFrom(Query query) {
		var paymentTxn = query.getContractGetBytecode().getHeader().getPayment();
		return Optional.ofNullable(uncheckedFrom(paymentTxn));
	}
}
