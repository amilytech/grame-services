package com.grame.services.queries.file;

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
import com.gramegrame.api.proto.java.FileGetContentsResponse;
import com.gramegrame.api.proto.java.FileID;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.api.proto.java.Transaction;

import java.util.Optional;

import static com.gramegrame.api.proto.java.grameFunctionality.FileGetContents;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseType.COST_ANSWER;

public class GetFileContentsAnswer implements AnswerService {
	private final OptionValidator validator;

	public GetFileContentsAnswer(OptionValidator validator) {
		this.validator = validator;
	}

	@Override
	public boolean needsAnswerOnlyCost(Query query) {
		return COST_ANSWER == query.getFileGetContents().getHeader().getResponseType();
	}

	@Override
	public boolean requiresNodePayment(Query query) {
		return typicallyRequiresNodePayment(query.getFileGetContents().getHeader().getResponseType());
	}

	@Override
	public Response responseGiven(Query query, StateView view, ResponseCodeEnum validity, long cost) {
		var op = query.getFileGetContents();
		var target = op.getFileID();

		FileGetContentsResponse.Builder response = FileGetContentsResponse.newBuilder();
		ResponseType type = op.getHeader().getResponseType();
		if (validity != OK) {
			response.setHeader(header(validity, type, cost));
			response.setFileContents(from(target, Optional.empty()));
		} else {
			if (type == COST_ANSWER) {
				response.setHeader(costAnswerHeader(OK, cost));
				response.setFileContents(from(target, Optional.empty()));
			} else {
				/* Include cost here to satisfy legacy regression tests. */
				response.setHeader(answerOnlyHeader(OK, cost));
				response.setFileContents(from(target, view.contentsOf(target)));
			}
		}
		return Response.newBuilder()
				.setFileGetContents(response)
				.build();
	}

	private FileGetContentsResponse.FileContents from(FileID id, Optional<byte[]> contents) {
		FileGetContentsResponse.FileContents.Builder wrapper = FileGetContentsResponse.FileContents.newBuilder()
				.setFileID(id);
		contents.ifPresent(d -> wrapper.setContents(ByteString.copyFrom(d)));
		return wrapper.build();
	}

	@Override
	public ResponseCodeEnum checkValidity(Query query, StateView view) {
		var id = query.getFileGetContents().getFileID();

		return validator.queryableFileStatus(id, view);
	}

	@Override
	public grameFunctionality canonicalFunction() {
		return FileGetContents;
	}

	@Override
	public ResponseCodeEnum extractValidityFrom(Response response) {
		return response.getFileGetContents().getHeader().getNodeTransactionPrecheckCode();
	}

	@Override
	public Optional<SignedTxnAccessor> extractPaymentFrom(Query query) {
		Transaction paymentTxn = query.getFileGetContents().getHeader().getPayment();
		return Optional.ofNullable(SignedTxnAccessor.uncheckedFrom(paymentTxn));
	}
}
