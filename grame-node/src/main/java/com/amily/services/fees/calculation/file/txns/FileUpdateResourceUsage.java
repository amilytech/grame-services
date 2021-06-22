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
import com.grame.services.usage.SigUsage;
import com.grame.services.usage.file.ExtantFileContext;
import com.grame.services.usage.file.FileOpsUsage;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.KeyList;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.fee.SigValueObj;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.grame.services.state.merkle.MerkleAccountState.DEFAULT_MEMO;

public class FileUpdateResourceUsage implements TxnResourceUsageEstimator {
	public static final Logger log = LogManager.getLogger(FileUpdateResourceUsage.class);

	private final FileOpsUsage fileOpsUsage;

	public FileUpdateResourceUsage(FileOpsUsage fileOpsUsage) {
		this.fileOpsUsage = fileOpsUsage;
	}

	@Override
	public boolean applicableTo(TransactionBody txn) {
		return txn.hasFileUpdate();
	}

	@Override
	public FeeData usageGiven(TransactionBody txn, SigValueObj svo, StateView view) {
		var op = txn.getFileUpdate();
		var sigUsage = new SigUsage(svo.getTotalSigCount(), svo.getSignatureSize(), svo.getPayerAcctSigCount());
		var info = view.infoForFile(op.getFileID());
		if (info.isPresent()) {
			var details = info.get();
			var ctx = ExtantFileContext.newBuilder()
					.setCurrentSize(details.getSize())
					.setCurrentWacl(details.getKeys())
					.setCurrentMemo(details.getMemo())
					.setCurrentExpiry(details.getExpirationTime().getSeconds())
					.build();
			return fileOpsUsage.fileUpdateUsage(txn, sigUsage, ctx);
		} else {
			long now = txn.getTransactionID().getTransactionValidStart().getSeconds();
			return fileOpsUsage.fileUpdateUsage(txn, sigUsage, missingCtx(now));
		}
	}

	static ExtantFileContext missingCtx(long now) {
		return ExtantFileContext.newBuilder()
				.setCurrentExpiry(now)
				.setCurrentMemo(DEFAULT_MEMO)
				.setCurrentWacl(KeyList.getDefaultInstance())
				.setCurrentSize(0)
				.build();
	}
}
