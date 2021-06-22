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

import com.gramegrame.api.proto.java.CryptoDeleteTransactionBody;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;

import static com.grame.test.utils.IdUtils.asAccount;

public class CryptoDeleteFactory extends SignedTxnFactory<CryptoDeleteFactory> {
	private final String account;
	private final String transfer;

	public CryptoDeleteFactory(String account, String transfer) {
		this.account = account;
		this.transfer = transfer;
	}

	public static CryptoDeleteFactory newSignedCryptoDelete(String account, String transfer) {
		return new CryptoDeleteFactory(account, transfer);
	}

	@Override
	protected CryptoDeleteFactory self() {
		return this;
	}

	@Override
	protected long feeFor(Transaction signedTxn, int numPayerKeys) {
		return 0;
	}

	@Override
	protected void customizeTxn(TransactionBody.Builder txn) {
		CryptoDeleteTransactionBody.Builder op = CryptoDeleteTransactionBody.newBuilder()
				.setTransferAccountID(asAccount(transfer))
				.setDeleteAccountID(asAccount(account));
		txn.setCryptoDelete(op);
	}
}
