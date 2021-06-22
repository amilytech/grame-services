package com.grame.test.factories.txns;

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

import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.AccountAmount;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.TokenAssociateTransactionBody;
import com.gramegrame.api.proto.java.TokenID;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class TokenAssociateFactory extends SignedTxnFactory<TokenAssociateFactory> {
	Map<TokenID, List<AccountAmount>> adjustments = new HashMap<>();

	private AccountID target;
	private List<TokenID> associations = new ArrayList<>();

	private TokenAssociateFactory() {
	}

	public static TokenAssociateFactory newSignedTokenAssociate() {
		return new TokenAssociateFactory();
	}

	public TokenAssociateFactory targeting(AccountID target) {
		this.target = target;
		return this;
	}

	public TokenAssociateFactory associating(TokenID token) {
		associations.add(token);
		return this;
	}

	@Override
	protected TokenAssociateFactory self() {
		return this;
	}

	@Override
	protected long feeFor(Transaction signedTxn, int numPayerKeys) {
		return 0;
	}

	@Override
	protected void customizeTxn(TransactionBody.Builder txn) {
		txn.setTokenAssociate(TokenAssociateTransactionBody.newBuilder()
				.setAccount(target)
				.addAllTokens(associations))
				.build();
	}
}
