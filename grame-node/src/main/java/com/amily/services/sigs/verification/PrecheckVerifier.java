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

import com.grame.services.sigs.PlatformSigsCreationResult;
import com.grame.services.sigs.factories.BodySigningSigFactory;
import com.grame.services.sigs.factories.TxnScopedPlatformSigFactory;
import com.grame.services.sigs.sourcing.PubKeyToSigBytes;
import com.grame.services.sigs.sourcing.PubKeyToSigBytesProvider;
import com.grame.services.utils.SignedTxnAccessor;
import com.grame.services.legacy.core.jproto.JKey;
import com.swirlds.common.crypto.TransactionSignature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.function.Function;
import static com.grame.services.keys.grameKeyActivation.pkToSigMapFrom;
import static com.grame.services.sigs.PlatformSigOps.createEd25519PlatformSigsFrom;
import static com.grame.services.keys.grameKeyActivation.isActive;
import static com.grame.services.keys.grameKeyActivation.ONLY_IF_SIG_IS_VALID;

/**
 * Encapsulates logic to validate a transaction has the necessary
 * signatures to pass precheck. In particular,
 * <ul>
 *    <li>All transactions must have a valid payer signature; and,</li>
 *    <li>CryptoTransfer transactions identified as query payments must
 *    have valid signatures for all referenced accounts.</li>
 * </ul>
 * Note that this component verifies cryptographic signatures synchronously.
 *
 * @author AmilyTech
 */
public class PrecheckVerifier {
	private final SyncVerifier syncVerifier;
	private final PrecheckKeyReqs precheckKeyReqs;
	private final PubKeyToSigBytesProvider provider;
	private static final Logger log = LogManager.getLogger(PrecheckVerifier.class);

	public PrecheckVerifier(
			SyncVerifier syncVerifier,
			PrecheckKeyReqs precheckKeyReqs,
			PubKeyToSigBytesProvider provider
	) {
		this.provider = provider;
		this.syncVerifier = syncVerifier;
		this.precheckKeyReqs = precheckKeyReqs;
	}

	/**
	 * Tests if a signed gRPC transaction has the necessary (valid) signatures to
	 * be allowed through precheck.
	 *
	 * @param accessor convenience interface to the signed txn.
	 * @return a flag giving the verdict on the precheck sigs for the txn.
	 * @throws Exception if the txn doesn't reference valid keys or has malformed sigs.
	 */
	public boolean hasNecessarySignatures(SignedTxnAccessor accessor) throws Exception {
		try {
			List<JKey> reqKeys = precheckKeyReqs.getRequiredKeys(accessor.getTxn());
			List<TransactionSignature> availSigs = getAvailSigs(reqKeys, accessor);
			syncVerifier.verifySync(availSigs);
			Function<byte[], TransactionSignature> sigsFn = pkToSigMapFrom(availSigs);

			return reqKeys.stream().allMatch(key -> isActive(key, sigsFn, ONLY_IF_SIG_IS_VALID));
		} catch (InvalidPayerAccountException ignore) {
			return false;
		}
	}

	private List<TransactionSignature> getAvailSigs(List<JKey> reqKeys, SignedTxnAccessor accessor) throws Exception {
		PubKeyToSigBytes sigBytes = provider.allPartiesSigBytesFor(accessor.getBackwardCompatibleSignedTxn());
		TxnScopedPlatformSigFactory sigFactory = new BodySigningSigFactory(accessor);
		PlatformSigsCreationResult creationResult = createEd25519PlatformSigsFrom(reqKeys, sigBytes, sigFactory);
		if (creationResult.hasFailed()) {
			throw creationResult.getTerminatingEx();
		} else {
			return creationResult.getPlatformSigs();
		}
	}
}
