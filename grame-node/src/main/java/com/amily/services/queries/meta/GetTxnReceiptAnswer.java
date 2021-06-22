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
import com.grame.services.records.RecordCache;
import com.grame.services.utils.SignedTxnAccessor;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.TransactionGetReceiptQuery;
import com.gramegrame.api.proto.java.TransactionGetReceiptResponse;
import com.gramegrame.api.proto.java.TransactionID;

import java.util.Optional;

import static com.gramegrame.api.proto.java.grameFunctionality.TransactionGetReceipt;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TRANSACTION_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.RECEIPT_NOT_FOUND;

public class GetTxnReceiptAnswer implements AnswerService {
	private final TransactionID DEFAULT_TXN_ID = TransactionID.getDefaultInstance();
	private final RecordCache recordCache;

	public GetTxnReceiptAnswer(RecordCache recordCache) {
		this.recordCache = recordCache;
	}

	@Override
	public boolean needsAnswerOnlyCost(Query query) {
		return false;
	}

	@Override
	public boolean requiresNodePayment(Query query) {
		return false;
	}

	@Override
	public Response responseGiven(Query query, StateView view, ResponseCodeEnum validity, long cost) {
		TransactionGetReceiptQuery op = query.getTransactionGetReceipt();
		TransactionGetReceiptResponse.Builder opResponse = TransactionGetReceiptResponse.newBuilder();

		if (validity == OK) {
			var txnId = op.getTransactionID();
			var receipt = recordCache.getPriorityReceipt(txnId);
			if (receipt == null) {
				validity = RECEIPT_NOT_FOUND;
			} else {
				opResponse.setReceipt(receipt);
				if (op.getIncludeDuplicates()) {
					opResponse.addAllDuplicateTransactionReceipts(recordCache.getDuplicateReceipts(txnId));
				}
			}
		}
		opResponse.setHeader(answerOnlyHeader(validity));

		return Response.newBuilder()
				.setTransactionGetReceipt(opResponse)
				.build();
	}

	@Override
	public ResponseCodeEnum checkValidity(Query query, StateView view) {
		boolean isOk = (!DEFAULT_TXN_ID.equals(query.getTransactionGetReceipt().getTransactionID()));

		return isOk ? OK : INVALID_TRANSACTION_ID;
	}

	@Override
	public ResponseCodeEnum extractValidityFrom(Response response) {
		return response.getTransactionGetReceipt().getHeader().getNodeTransactionPrecheckCode();
	}

	@Override
	public grameFunctionality canonicalFunction() {
		return TransactionGetReceipt;
	}

	@Override
	public Optional<SignedTxnAccessor> extractPaymentFrom(Query query) {
		return Optional.empty();
	}
}
