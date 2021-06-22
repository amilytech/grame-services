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

import com.gramegrame.api.proto.java.TokenFreezeAccountTransactionBody;
import com.gramegrame.api.proto.java.TokenID;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;

public class TokenFreezeFactory extends SignedTxnFactory<TokenFreezeFactory> {
	private TokenFreezeFactory() {}

	private TokenID id;

	public static TokenFreezeFactory newSignedTokenFreeze() {
		return new TokenFreezeFactory();
	}

	public TokenFreezeFactory freezing(TokenID id) {
		this.id = id;
		return this;
	}

	@Override
	protected TokenFreezeFactory self() {
		return this;
	}

	@Override
	protected long feeFor(Transaction signedTxn, int numPayerKeys) {
		return 0;
	}

	@Override
	protected void customizeTxn(TransactionBody.Builder txn) {
		var op = TokenFreezeAccountTransactionBody.newBuilder()
				.setToken(id);
		txn.setTokenFreeze(op);
	}
}
