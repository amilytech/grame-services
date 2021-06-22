package com.grame.services.fees.calculation.contract.txns;

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
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.exception.InvalidTxBodyException;
import com.gramegrame.fee.SigValueObj;
import com.gramegrame.fee.SmartContractFeeBuilder;
import com.grame.services.state.merkle.MerkleEntityId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.grame.services.fees.calculation.FeeCalcUtils.lookupAccountExpiry;
import static com.grame.services.state.merkle.MerkleEntityId.fromContractId;

public class ContractUpdateResourceUsage implements TxnResourceUsageEstimator {
	private static final Logger log = LogManager.getLogger(ContractUpdateResourceUsage.class);

	private final SmartContractFeeBuilder usageEstimator;

	public ContractUpdateResourceUsage(SmartContractFeeBuilder usageEstimator) {
		this.usageEstimator = usageEstimator;
	}

	@Override
	public boolean applicableTo(TransactionBody txn) {
		return txn.hasContractUpdateInstance();
	}

	@Override
	public FeeData usageGiven(TransactionBody txn, SigValueObj sigUsage, StateView view) throws InvalidTxBodyException {
		try {
			MerkleEntityId id = fromContractId(txn.getContractUpdateInstance().getContractID());
			Timestamp expiry = lookupAccountExpiry(id, view.accounts());
			return usageEstimator.getContractUpdateTxFeeMatrices(txn, expiry, sigUsage);
		} catch (Exception e) {
			log.debug("Unable to deduce ContractUpdate usage for {}, using defaults", txn.getTransactionID(), e);
			return FeeData.getDefaultInstance();
		}
	}
}
