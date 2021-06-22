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
import com.grame.services.legacy.crypto.SignatureStatusCode;
import com.grame.services.sigs.factories.TxnScopedPlatformSigFactory;
import com.grame.services.sigs.order.grameSigningOrder;
import com.grame.services.sigs.order.SigStatusOrderResultFactory;
import com.grame.services.sigs.order.SigningOrderResult;
import com.grame.services.sigs.sourcing.PubKeyToSigBytes;
import com.grame.services.sigs.sourcing.PubKeyToSigBytesProvider;
import com.grame.services.sigs.verification.SyncVerifier;
import com.grame.services.utils.TxnAccessor;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;
import com.swirlds.common.crypto.TransactionSignature;
import com.swirlds.common.crypto.VerificationStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.grame.services.legacy.crypto.SignatureStatusCode.SUCCESS;
import static com.grame.services.sigs.PlatformSigOps.createEd25519PlatformSigsFrom;
import static com.grame.services.sigs.factories.PlatformSigFactory.allVaryingMaterialEquals;
import static com.grame.services.sigs.utils.StatusUtils.successFor;

public class Rationalization {
	private static final Logger log = LogManager.getLogger(Rationalization.class);

	public final static SigStatusOrderResultFactory IN_HANDLE_SUMMARY_FACTORY =
			new SigStatusOrderResultFactory(true);

	private final SyncVerifier syncVerifier;
	private final List<TransactionSignature> txnSigs;
	private final TxnAccessor txnAccessor;
	private final grameSigningOrder keyOrderer;
	private final PubKeyToSigBytesProvider sigsProvider;
	private final TxnScopedPlatformSigFactory sigFactory;

	public Rationalization(
			TxnAccessor txnAccessor,
			SyncVerifier syncVerifier,
			grameSigningOrder keyOrderer,
			PubKeyToSigBytesProvider sigsProvider,
			Function<TxnAccessor, TxnScopedPlatformSigFactory> sigFactoryCreator
	) {
		this.txnAccessor = txnAccessor;
		this.syncVerifier = syncVerifier;
		this.keyOrderer = keyOrderer;
		this.sigsProvider = sigsProvider;

		txnSigs = txnAccessor.getPlatformTxn().getSignatures();
		sigFactory = sigFactoryCreator.apply(txnAccessor);
	}

	public SignatureStatus execute() {
		log.debug("Rationalizing crypto sigs with grame sigs for txn {}...", txnAccessor::getSignedTxn4Log);
		List<TransactionSignature> realPayerSigs = new ArrayList<>(), realOtherPartySigs = new ArrayList<>();

		var payerStatus = expandIn(
				realPayerSigs, sigsProvider::payerSigBytesFor, keyOrderer::keysForPayer);
		if (!SUCCESS.equals(payerStatus.getStatusCode())) {
			if (log.isDebugEnabled()) {
				log.debug("Failed rationalizing payer sigs, txn {}: {}", txnAccessor.getTxnId(), payerStatus);
			}
			return payerStatus;
		}
		var otherPartiesStatus = expandIn(
				realOtherPartySigs, sigsProvider::otherPartiesSigBytesFor, keyOrderer::keysForOtherParties);
		if (!SUCCESS.equals(otherPartiesStatus.getStatusCode())) {
			if (log.isDebugEnabled()) {
				log.debug("Failed rationalizing other sigs, txn {}: {}", txnAccessor.getTxnId(), otherPartiesStatus);
			}
			return otherPartiesStatus;
		}

		var rationalizedPayerSigs = rationalize(realPayerSigs, 0);
		var rationalizedOtherPartySigs = rationalize(realOtherPartySigs, realPayerSigs.size());

		if (rationalizedPayerSigs == realPayerSigs || rationalizedOtherPartySigs == realOtherPartySigs) {
			txnAccessor.getPlatformTxn().clear();
			txnAccessor.getPlatformTxn().addAll(rationalizedPayerSigs.toArray(new TransactionSignature[0]));
			txnAccessor.getPlatformTxn().addAll(rationalizedOtherPartySigs.toArray(new TransactionSignature[0]));
			log.warn("Verified crypto sigs synchronously for txn {}", txnAccessor.getSignedTxn4Log());
			return syncSuccess();
		}

		return asyncSuccess();
	}

	private List<TransactionSignature> rationalize(List<TransactionSignature> realSigs, int startingAt) {
		try {
			var candidateSigs = txnSigs.subList(startingAt, startingAt + realSigs.size());
			if (allVaryingMaterialEquals(candidateSigs, realSigs) && allStatusesAreKnown(candidateSigs)) {
				return candidateSigs;
			}
		} catch (IndexOutOfBoundsException ignore) {
		}
		syncVerifier.verifySync(realSigs);
		return realSigs;
	}

	private boolean allStatusesAreKnown(List<TransactionSignature> sigs) {
		return sigs.stream().map(TransactionSignature::getSignatureStatus).noneMatch(VerificationStatus.UNKNOWN::equals);
	}

	private SignatureStatus expandIn(
			List<TransactionSignature> target,
			Function<Transaction, PubKeyToSigBytes> sigsFn,
			BiFunction<TransactionBody, SigStatusOrderResultFactory, SigningOrderResult<SignatureStatus>> keysFn
	) {
		SigningOrderResult<SignatureStatus> orderResult =
				keysFn.apply(txnAccessor.getTxn(), IN_HANDLE_SUMMARY_FACTORY);
		if (orderResult.hasErrorReport()) {
			return orderResult.getErrorReport();
		}
		PlatformSigsCreationResult creationResult = createEd25519PlatformSigsFrom(
				orderResult.getOrderedKeys(), sigsFn.apply(txnAccessor.getBackwardCompatibleSignedTxn()), sigFactory);
		if (creationResult.hasFailed()) {
			return creationResult.asSignatureStatus(true, txnAccessor.getTxnId());
		}
		target.addAll(creationResult.getPlatformSigs());
		return successFor(true, txnAccessor);
	}

	private SignatureStatus syncSuccess() {
		return success(SignatureStatusCode.SUCCESS_VERIFY_SYNC);
	}

	private SignatureStatus asyncSuccess() {
		return success(SignatureStatusCode.SUCCESS_VERIFY_ASYNC);
	}

	private SignatureStatus success(SignatureStatusCode code) {
		return new SignatureStatus(
				code, ResponseCodeEnum.OK,
				true, txnAccessor.getTxn().getTransactionID(),
				null, null, null, null);
	}
}
