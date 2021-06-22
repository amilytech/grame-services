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
import com.grame.services.fees.calculation.QueryResourceUsageEstimator;
import com.grame.services.usage.crypto.CryptoGetInfoUsage;
import com.grame.services.usage.crypto.CryptoOpsUsage;
import com.grame.services.usage.crypto.ExtantCryptoContext;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.ResponseType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Function;

import static com.grame.services.state.merkle.MerkleEntityId.fromAccountId;
import static com.grame.services.utils.MiscUtils.asKeyUnchecked;

public class GetAccountInfoResourceUsage implements QueryResourceUsageEstimator {
	private static final Logger log = LogManager.getLogger(GetAccountInfoResourceUsage.class);

	private final CryptoOpsUsage cryptoOpsUsage;

	public GetAccountInfoResourceUsage(CryptoOpsUsage cryptoOpsUsage) {
		this.cryptoOpsUsage = cryptoOpsUsage;
	}

	@Override
	public boolean applicableTo(Query query) {
		return query.hasCryptoGetInfo();
	}

	@Override
	public FeeData usageGiven(Query query, StateView view) {
		return usageGivenType(query, view, query.getCryptoGetInfo().getHeader().getResponseType());
	}

	@Override
	public FeeData usageGivenType(Query query, StateView view, ResponseType type) {
		var op = query.getCryptoGetInfo();

		var tgt = op.getAccountID();
		var info = view.infoForAccount(tgt);
		/* Given the test in {@code GetAccountInfoAnswer.checkValidity}, this can only be empty
		 * under the extraordinary circumstance that the desired account expired during the query
		 * answer flow (which will now fail downstream with an appropriate status code); so
		 * just return the default {@code FeeData} here. */
		if (info.isEmpty()) {
			return FeeData.getDefaultInstance();
		}
		var details = info.get();
		var ctx = ExtantCryptoContext.newBuilder()
				.setCurrentKey(details.getKey())
				.setCurrentMemo(details.getMemo())
				.setCurrentExpiry(details.getExpirationTime().getSeconds())
				.setCurrentlyHasProxy(details.hasProxyAccountID())
				.setCurrentNumTokenRels(details.getTokenRelationshipsCount())
				.build();
		return cryptoOpsUsage.cryptoInfoUsage(query, ctx);
	}
}
