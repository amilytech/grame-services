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

import com.grame.test.factories.keys.KeyTree;
import com.gramegrame.api.proto.java.CryptoUpdateTransactionBody;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;

import java.util.Optional;

import static com.grame.test.utils.IdUtils.asAccount;

public class CryptoUpdateFactory extends SignedTxnFactory<CryptoUpdateFactory> {
	private final String account;
	private Optional<KeyTree> newAccountKt = Optional.empty();

	public CryptoUpdateFactory(String account) {
		this.account = account;
	}
	public static CryptoUpdateFactory newSignedCryptoUpdate(String account) {
		return new CryptoUpdateFactory(account);
	}

	@Override
	protected CryptoUpdateFactory self() {
		return this;
	}

	@Override
	protected long feeFor(Transaction signedTxn, int numPayerKeys) {
		return 0;
	}

	@Override
	protected void customizeTxn(TransactionBody.Builder txn) {
		CryptoUpdateTransactionBody.Builder op = CryptoUpdateTransactionBody.newBuilder()
				.setAccountIDToUpdate(asAccount(account));
		newAccountKt.ifPresent(kt -> op.setKey(kt.asKey(keyFactory)));
		txn.setCryptoUpdateAccount(op);
	}

	public CryptoUpdateFactory newAccountKt(KeyTree newAccountKt) {
		this.newAccountKt = Optional.of(newAccountKt);
		return this;
	}
}
