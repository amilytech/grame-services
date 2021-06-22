package com.grame.services.fees.calculation.crypto.queries;

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
import com.grame.services.fees.calculation.FeeCalcUtils;
import com.grame.services.fees.calculation.QueryResourceUsageEstimator;
import com.grame.services.queries.answering.AnswerFunctions;
import com.grame.services.records.RecordCache;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.api.proto.java.TransactionRecord;
import com.gramegrame.fee.CryptoFeeBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;

import static com.grame.services.queries.AnswerService.NO_QUERY_CTX;
import static com.grame.services.queries.meta.GetTxnRecordAnswer.DUPLICATE_RECORDS_CTX_KEY;
import static com.grame.services.queries.meta.GetTxnRecordAnswer.PRIORITY_RECORD_CTX_KEY;

public class GetTxnRecordResourceUsage implements QueryResourceUsageEstimator {
	private static final Logger log = LogManager.getLogger(GetTxnRecordResourceUsage.class);

	public static final TransactionRecord MISSING_RECORD_STANDIN = TransactionRecord.getDefaultInstance();

	private final RecordCache recordCache;
	private final AnswerFunctions answerFunctions;
	private final CryptoFeeBuilder usageEstimator;

	static BinaryOperator<FeeData> sumFn = FeeCalcUtils::sumOfUsages;

	public GetTxnRecordResourceUsage(
			RecordCache recordCache,
			AnswerFunctions answerFunctions,
			CryptoFeeBuilder usageEstimator
	) {
		this.recordCache = recordCache;
		this.usageEstimator = usageEstimator;
		this.answerFunctions = answerFunctions;
	}

	@Override
	public boolean applicableTo(Query query) {
		return query.hasTransactionGetRecord();
	}

	@Override
	public FeeData usageGiven(Query query, StateView view) {
		return usageFor(query, view, query.getTransactionGetRecord().getHeader().getResponseType(), NO_QUERY_CTX);
	}

	@Override
	public FeeData usageGiven(Query query, StateView view, Map<String, Object> queryCtx) {
		return usageFor(
				query,
				view,
				query.getTransactionGetRecord().getHeader().getResponseType(),
				Optional.of(queryCtx));
	}

	@Override
	public FeeData usageGivenType(Query query, StateView view, ResponseType type) {
		return usageFor(query, view, type, NO_QUERY_CTX);
	}

	private FeeData usageFor(Query query, StateView view, ResponseType type, Optional<Map<String, Object>> queryCtx) {
		var record = answerFunctions.txnRecord(recordCache, view, query).orElse(MISSING_RECORD_STANDIN);
		var usages = usageEstimator.getTransactionRecordQueryFeeMatrices(record, type);
		if (record != MISSING_RECORD_STANDIN) {
			queryCtx.ifPresent(ctx -> ctx.put(PRIORITY_RECORD_CTX_KEY, record));
			var op = query.getTransactionGetRecord();
			if (op.getIncludeDuplicates()) {
				var duplicateRecords = recordCache.getDuplicateRecords(op.getTransactionID());
				queryCtx.ifPresent(ctx -> ctx.put(DUPLICATE_RECORDS_CTX_KEY, duplicateRecords));
				for (TransactionRecord duplicate : duplicateRecords) {
					var duplicateUsage = usageEstimator.getTransactionRecordQueryFeeMatrices(duplicate, type);
					usages = sumFn.apply(usages, duplicateUsage);
				}
			}
		}
		return usages;
	}
}
