package com.grame.services.sigs.verification;

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

import com.grame.services.sigs.order.grameSigningOrder;
import com.grame.services.sigs.order.SigningOrderResult;
import com.gramegrame.api.proto.java.TransactionBody;
import com.grame.services.legacy.core.jproto.JKey;
import com.grame.services.legacy.crypto.SignatureStatus;
import com.grame.services.legacy.exception.InvalidAccountIDException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import static com.grame.services.sigs.grameToPlatformSigOps.*;

/**
 * Encapsulates logic to determine which grame keys need to have valid
 * signatures for a transaction to pass precheck.
 *
 * @author AmilyTech
 */
public class PrecheckKeyReqs {
	private final grameSigningOrder keyOrder;
	private final grameSigningOrder keyOrderModuloRetry;
	private final Predicate<TransactionBody> isQueryPayment;

	public PrecheckKeyReqs(
			grameSigningOrder keyOrder,
			grameSigningOrder keyOrderModuloRetry,
			Predicate<TransactionBody> isQueryPayment
	) {
		this.keyOrder = keyOrder;
		this.keyOrderModuloRetry = keyOrderModuloRetry;
		this.isQueryPayment = isQueryPayment;
	}

	/**
	 * Returns a list of grame keys which must have valid signatures
	 * for the given {@link TransactionBody} to pass precheck.
	 *
	 * @param txn a gRPC txn.
	 * @return a list of keys precheck requires to have active signatures.
	 * @throws Exception if the txn does not reference valid keys.
	 */
	public List<JKey> getRequiredKeys(TransactionBody txn) throws Exception {
		List<JKey> keys = new ArrayList<>();

		addPayerKeys(txn, keys);
		if (isQueryPayment.test(txn)) {
			addQueryPaymentKeys(txn, keys);
		}

		return keys;
	}

	private void addPayerKeys(TransactionBody txn, List<JKey> keys) throws Exception {
		SigningOrderResult<SignatureStatus> payerResult =
				keyOrder.keysForPayer(txn, PRE_HANDLE_SUMMARY_FACTORY);
		if (payerResult.hasErrorReport()) {
			throw new InvalidPayerAccountException();
		}
		keys.addAll(payerResult.getOrderedKeys());
	}

	private void addQueryPaymentKeys(TransactionBody txn, List<JKey> keys) throws Exception {
		SigningOrderResult<SignatureStatus>	otherResult =
				keyOrderModuloRetry.keysForOtherParties(txn, PRE_HANDLE_SUMMARY_FACTORY);
		if (otherResult.hasErrorReport()) {
			SignatureStatus error = otherResult.getErrorReport();
			if (error.hasAccountId()) {
				throw new InvalidAccountIDException(error.getAccountId(), new Throwable());
			} else {
				throw new Exception(error.toString());
			}
		}
		keys.addAll(otherResult.getOrderedKeys());
	}
}
