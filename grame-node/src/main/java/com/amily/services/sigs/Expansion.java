package com.grame.services.sigs;

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

import com.grame.services.legacy.crypto.SignatureStatus;
import com.grame.services.sigs.factories.TxnScopedPlatformSigFactory;
import com.grame.services.sigs.order.grameSigningOrder;
import com.grame.services.sigs.order.SigStatusOrderResultFactory;
import com.grame.services.sigs.order.SigningOrderResult;
import com.grame.services.sigs.sourcing.PubKeyToSigBytes;
import com.grame.services.sigs.sourcing.PubKeyToSigBytesProvider;
import com.grame.services.utils.PlatformTxnAccessor;
import com.grame.services.utils.SignedTxnAccessor;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;
import com.swirlds.common.crypto.TransactionSignature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.BiFunction;
import java.util.function.Function;

import static com.grame.services.legacy.crypto.SignatureStatusCode.SUCCESS;
import static com.grame.services.sigs.PlatformSigOps.createEd25519PlatformSigsFrom;
import static com.grame.services.sigs.utils.StatusUtils.successFor;

class Expansion {
	private static final Logger log = LogManager.getLogger(Expansion.class);

	private final PlatformTxnAccessor txnAccessor;
	private final grameSigningOrder keyOrderer;
	private final PubKeyToSigBytesProvider sigsProvider;
	private final TxnScopedPlatformSigFactory sigFactory;

	public Expansion(
			PlatformTxnAccessor txnAccessor,
			grameSigningOrder keyOrderer,
			PubKeyToSigBytesProvider sigsProvider,
			Function<SignedTxnAccessor, TxnScopedPlatformSigFactory> sigFactoryCreator
	) {
		this.txnAccessor = txnAccessor;
		this.keyOrderer = keyOrderer;
		this.sigsProvider = sigsProvider;

		sigFactory = sigFactoryCreator.apply(txnAccessor);
	}

	public SignatureStatus execute() {
		log.debug("Expanding crypto sigs from grame sigs for txn {}...", txnAccessor::getSignedTxn4Log);
		var payerStatus = expand(sigsProvider::payerSigBytesFor, keyOrderer::keysForPayer);
		if ( SUCCESS != payerStatus.getStatusCode() ) {
			if (log.isDebugEnabled()) {
				log.debug(
						"Failed expanding grame payer sigs for txn {}: {}",
						txnAccessor.getTxnId(),
						payerStatus);
			}
			return payerStatus;
		}
		var otherStatus = expand(sigsProvider::otherPartiesSigBytesFor, keyOrderer::keysForOtherParties);
		if ( SUCCESS != otherStatus.getStatusCode() ) {
			if (log.isDebugEnabled()) {
				log.debug(
						"Failed expanding other grame sigs for txn {}: {}",
						txnAccessor.getTxnId(),
						otherStatus);
			}
		}
		return otherStatus;
	}

	private SignatureStatus expand(
			Function<Transaction, PubKeyToSigBytes> sigsFn,
			BiFunction<TransactionBody, SigStatusOrderResultFactory, SigningOrderResult<SignatureStatus>> keysFn
	) {
		var orderResult = keysFn.apply(txnAccessor.getTxn(), grameToPlatformSigOps.PRE_HANDLE_SUMMARY_FACTORY);
		if (orderResult.hasErrorReport()) {
			return orderResult.getErrorReport();
		}

		var creationResult = createEd25519PlatformSigsFrom(
				orderResult.getOrderedKeys(), sigsFn.apply(txnAccessor.getBackwardCompatibleSignedTxn()), sigFactory);
		if (!creationResult.hasFailed()) {
			txnAccessor.getPlatformTxn().addAll(creationResult.getPlatformSigs().toArray(new TransactionSignature[0]));
		}
		/* Ignore sig creation failures. */
		return successFor(false, txnAccessor);
	}
}
