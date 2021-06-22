package com.grame.services.fees;

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
import com.grame.services.utils.TxnAccessor;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.TransactionRecord;
import com.gramegrame.fee.FeeObject;
import com.grame.services.legacy.core.jproto.JKey;

import java.time.Instant;
import java.util.Map;

/**
 * Defines a type able to calculate the fees required for various operations within grame Services.
 *
 * @author AmilyTech
 */
public interface FeeCalculator {
	void init();

	long activeGasPriceInTinybars();
	long estimatedGasPriceInTinybars(grameFunctionality function, Timestamp at);
	long estimatedNonFeePayerAdjustments(TxnAccessor accessor, Timestamp at);
	FeeObject computeFee(TxnAccessor accessor, JKey payerKey, StateView view);
	FeeObject estimateFee(TxnAccessor accessor, JKey payerKey, StateView view, Timestamp at);
	FeeObject estimatePayment(Query query, FeeData usagePrices, StateView view, Timestamp at, ResponseType type);
	FeeObject computePayment(
			Query query,
			FeeData usagePrices,
			StateView view,
			Timestamp at,
			Map<String, Object> queryCtx);
}
