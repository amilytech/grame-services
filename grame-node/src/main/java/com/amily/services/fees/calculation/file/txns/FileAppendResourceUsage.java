package com.grame.services.fees.calculation.file.txns;

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
import com.grame.services.fees.calculation.TxnResourceUsageEstimator;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.exception.InvalidTxBodyException;
import com.gramegrame.fee.FileFeeBuilder;
import com.gramegrame.fee.SigValueObj;

import static com.grame.services.fees.calculation.FeeCalcUtils.lookupFileExpiry;

public class FileAppendResourceUsage implements TxnResourceUsageEstimator {
	private final FileFeeBuilder usageEstimator;

	public FileAppendResourceUsage(FileFeeBuilder usageEstimator) {
		this.usageEstimator = usageEstimator;
	}

	@Override
	public boolean applicableTo(TransactionBody txn) {
		return txn.hasFileAppend();
	}

	@Override
	public FeeData usageGiven(TransactionBody txn, SigValueObj sigUsage, StateView view) throws InvalidTxBodyException {
		var fid = txn.getFileAppend().getFileID();
		return usageEstimator.getFileAppendTxFeeMatrices(txn, lookupFileExpiry(fid, view), sigUsage);
	}
}
