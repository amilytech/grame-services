package com.grame.services.fees.calculation.token.txns;

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
import com.grame.services.usage.token.TokenUpdateUsage;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.TokenInfo;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.exception.InvalidTxBodyException;
import com.gramegrame.fee.SigValueObj;

import java.util.function.BiFunction;

import static com.grame.services.fees.calculation.token.queries.GetTokenInfoResourceUsage.ifPresent;

public class TokenUpdateResourceUsage implements TxnResourceUsageEstimator {
	static BiFunction<TransactionBody, SigUsage, TokenUpdateUsage> factory = TokenUpdateUsage::newEstimate;

	@Override
	public boolean applicableTo(TransactionBody txn) {
		return txn.hasTokenUpdate();
	}

	@Override
	public FeeData usageGiven(TransactionBody txn, SigValueObj svo, StateView view) throws InvalidTxBodyException {
		var op = txn.getTokenUpdate();
		var sigUsage = new SigUsage(svo.getTotalSigCount(), svo.getSignatureSize(), svo.getPayerAcctSigCount());
		var optionalInfo = view.infoForToken(op.getToken());
		if (optionalInfo.isPresent()) {
			var info = optionalInfo.get();
			var estimate = factory.apply(txn, sigUsage)
					.givenCurrentExpiry(info.getExpiry().getSeconds())
					.givenCurrentAdminKey(ifPresent(info, TokenInfo::hasAdminKey, TokenInfo::getAdminKey))
					.givenCurrentFreezeKey(ifPresent(info, TokenInfo::hasFreezeKey, TokenInfo::getFreezeKey))
					.givenCurrentWipeKey(ifPresent(info, TokenInfo::hasWipeKey, TokenInfo::getWipeKey))
					.givenCurrentSupplyKey(ifPresent(info, TokenInfo::hasSupplyKey, TokenInfo::getSupplyKey))
					.givenCurrentKycKey(ifPresent(info, TokenInfo::hasKycKey, TokenInfo::getKycKey))
					.givenCurrentMemo(info.getMemo())
					.givenCurrentName(info.getName())
					.givenCurrentSymbol(info.getSymbol());
			if (info.hasAutoRenewAccount()) {
				estimate.givenCurrentlyUsingAutoRenewAccount();
			}
			return estimate.get();
		} else {
			return FeeData.getDefaultInstance();
		}
	}
}
