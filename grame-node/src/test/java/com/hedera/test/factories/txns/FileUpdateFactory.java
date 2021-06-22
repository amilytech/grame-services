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

import com.grame.test.factories.keys.KeyFactory;
import com.grame.test.factories.keys.KeyTree;
import com.gramegrame.api.proto.java.CryptoUpdateTransactionBody;
import com.gramegrame.api.proto.java.FileUpdateTransactionBody;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;
import static com.grame.test.utils.IdUtils.asFile;

import java.util.Optional;

public class FileUpdateFactory extends SignedTxnFactory<FileUpdateFactory> {
	private final String file;
	private Optional<KeyTree> newWaclKt = Optional.empty();

	public FileUpdateFactory(String file) {
		this.file = file;
	}
	public static FileUpdateFactory newSignedFileUpdate(String file) {
		return new FileUpdateFactory(file);
	}

	@Override
	protected FileUpdateFactory self() {
		return this;
	}

	@Override
	protected long feeFor(Transaction signedTxn, int numPayerKeys) {
		return 0;
	}

	@Override
	protected void customizeTxn(TransactionBody.Builder txn) {
		FileUpdateTransactionBody.Builder op = FileUpdateTransactionBody.newBuilder()
				.setFileID(asFile(file));
		newWaclKt.ifPresent(kt -> op.setKeys(kt.asKey().getKeyList()));
		txn.setFileUpdate(op);
	}

	public FileUpdateFactory newWaclKt(KeyTree newWaclKt) {
		this.newWaclKt = Optional.of(newWaclKt);
		return this;
	}
}
