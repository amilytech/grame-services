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

import com.grame.services.config.MockEntityNumbers;
import com.grame.services.config.MockGlobalDynamicProps;
import com.grame.services.files.grameFs;
import com.grame.services.keys.grameKeyActivation;
import com.grame.services.keys.KeyActivationCharacteristics;
import com.grame.services.legacy.core.jproto.JKey;
import com.grame.services.legacy.crypto.SignatureStatus;
import com.grame.services.legacy.crypto.SignatureStatusCode;
import com.grame.services.security.ops.SystemOpPolicies;
import com.grame.services.sigs.factories.BodySigningSigFactory;
import com.grame.services.sigs.metadata.SigMetadataLookup;
import com.grame.services.sigs.order.grameSigningOrder;
import com.grame.services.sigs.order.SigningOrderResult;
import com.grame.services.sigs.order.SigningOrderResultFactory;
import com.grame.services.sigs.sourcing.DefaultSigBytesProvider;
import com.grame.services.sigs.sourcing.PubKeyToSigBytes;
import com.grame.services.sigs.verification.SyncVerifier;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.stats.MiscRunningAvgs;
import com.grame.services.stats.MiscSpeedometers;
import com.grame.services.utils.PlatformTxnAccessor;
import com.grame.test.factories.scenarios.TxnHandlingScenario;
import com.grame.test.factories.txns.CryptoCreateFactory;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.TransactionBody;
import com.swirlds.common.crypto.TransactionSignature;
import com.swirlds.common.crypto.VerificationStatus;
import com.swirlds.common.crypto.engine.CryptoEngine;
import com.swirlds.fcmap.FCMap;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.grame.services.keys.DefaultActivationCharacteristics.DEFAULT_ACTIVATION_CHARACTERISTICS;
import static com.grame.services.keys.grameKeyActivation.payerSigIsActive;
import static com.grame.services.security.ops.SystemOpAuthorization.AUTHORIZED;
import static com.grame.services.sigs.grameToPlatformSigOps.PRE_HANDLE_SUMMARY_FACTORY;
import static com.grame.services.sigs.grameToPlatformSigOps.expandIn;
import static com.grame.services.sigs.grameToPlatformSigOps.rationalizeIn;
import static com.grame.services.sigs.Rationalization.IN_HANDLE_SUMMARY_FACTORY;
import static com.grame.services.sigs.metadata.DelegatingSigMetadataLookup.defaultLookupsFor;
import static com.grame.services.sigs.metadata.DelegatingSigMetadataLookup.defaultLookupsPlusAccountRetriesFor;
import static com.grame.test.factories.scenarios.BadPayerScenarios.INVALID_PAYER_ID_SCENARIO;
import static com.grame.test.factories.scenarios.CryptoCreateScenarios.COMPLEX_KEY_ACCOUNT_KT;
import static com.grame.test.factories.scenarios.CryptoCreateScenarios.CRYPTO_CREATE_COMPLEX_PAYER_RECEIVER_SIG_SCENARIO;
import static com.grame.test.factories.scenarios.CryptoCreateScenarios.CRYPTO_CREATE_RECEIVER_SIG_SCENARIO;
import static com.grame.test.factories.scenarios.CryptoCreateScenarios.NEW_ACCOUNT_KT;
import static com.grame.test.factories.scenarios.CryptoUpdateScenarios.CRYPTO_UPDATE_COMPLEX_KEY_ACCOUNT_ADD_NEW_KEY_SCENARIO;
import static com.grame.test.factories.scenarios.CryptoUpdateScenarios.CRYPTO_UPDATE_COMPLEX_KEY_ACCOUNT_SCENARIO;
import static com.grame.test.factories.scenarios.CryptoUpdateScenarios.CRYPTO_UPDATE_MISSING_ACCOUNT_SCENARIO;
import static com.grame.test.factories.sigs.SigWrappers.asKind;
import static com.grame.test.factories.sigs.SigWrappers.asValid;
import static com.grame.test.factories.txns.SignedTxnFactory.DEFAULT_PAYER_KT;
import static com.swirlds.common.crypto.VerificationStatus.INVALID;
import static com.swirlds.common.crypto.VerificationStatus.VALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.mock;

public class SigOpsRegressionTest {
	private grameFs hfs;
	private MiscRunningAvgs runningAvgs;
	private MiscSpeedometers speedometers;
	private List<TransactionSignature> expectedSigs;
	private SignatureStatus actualStatus;
	private SignatureStatus successStatus;
	private SignatureStatus syncSuccessStatus;
	private SignatureStatus asyncSuccessStatus;
	private SignatureStatus expectedErrorStatus;
	private SignatureStatus sigCreationFailureStatus;
	private PlatformTxnAccessor platformTxn;
	private grameSigningOrder signingOrder;
	private FCMap<MerkleEntityId, MerkleAccount> accounts;

	private SystemOpPolicies mockSystemOpPolicies = new SystemOpPolicies(new MockEntityNumbers());
	private Predicate<TransactionBody> updateAccountSigns = txn ->
			mockSystemOpPolicies.check(txn, grameFunctionality.CryptoUpdate) != AUTHORIZED;
	private BiPredicate<TransactionBody, grameFunctionality> targetWaclSigns = (txn, function) ->
			mockSystemOpPolicies.check(txn, function) != AUTHORIZED;

	public static boolean otherPartySigsAreActive(
			PlatformTxnAccessor accessor,
			grameSigningOrder keyOrder,
			SigningOrderResultFactory<SignatureStatus> summaryFactory
	) {
		return otherPartySigsAreActive(accessor, keyOrder, summaryFactory, DEFAULT_ACTIVATION_CHARACTERISTICS);
	}

	public static boolean otherPartySigsAreActive(
			PlatformTxnAccessor accessor,
			grameSigningOrder keyOrder,
			SigningOrderResultFactory<SignatureStatus> summaryFactory,
			KeyActivationCharacteristics characteristics
	) {
		TransactionBody txn = accessor.getTxn();
		Function<byte[], TransactionSignature> sigsFn = grameKeyActivation.pkToSigMapFrom(accessor.getPlatformTxn().getSignatures());

		SigningOrderResult<SignatureStatus> othersResult = keyOrder.keysForOtherParties(txn, summaryFactory);
		for (JKey otherKey : othersResult.getOrderedKeys()) {
			if (!grameKeyActivation.isActive(otherKey, sigsFn, grameKeyActivation.ONLY_IF_SIG_IS_VALID, characteristics)) {
				return false;
			}
		}
		return true;
	}

	@Test
	public void setsExpectedPlatformSigsForCryptoCreate() throws Throwable {
		// given:
		setupFor(CRYPTO_CREATE_RECEIVER_SIG_SCENARIO);

		// when:
		actualStatus = invokeExpansionScenario();

		// then:
		statusMatches(successStatus);
		assertEquals(expectedSigs, platformTxn.getPlatformTxn().getSignatures());
	}

	@Test
	public void setsExpectedErrorForBadPayer() throws Throwable {
		// given:
		setupFor(INVALID_PAYER_ID_SCENARIO);

		// when:
		actualStatus = invokeExpansionScenario();

		// then:
		statusMatches(expectedErrorStatus);
		assertEquals(expectedSigs, platformTxn.getPlatformTxn().getSignatures());
	}

	@Test
	public void setsExpectedErrorAndSigsForMissingTargetAccount() throws Throwable {
		// given:
		setupFor(CRYPTO_UPDATE_MISSING_ACCOUNT_SCENARIO);

		// when:
		actualStatus = invokeExpansionScenario();

		// then:
		statusMatches(expectedErrorStatus);
		assertEquals(expectedSigs, platformTxn.getPlatformTxn().getSignatures());
	}

	@Test
	public void rationalizesExpectedPlatformSigsForCryptoCreate() throws Throwable {
		// given:
		setupFor(CRYPTO_CREATE_RECEIVER_SIG_SCENARIO);
		// and:
		List<TransactionSignature> expectedSigs = expectedCryptoCreateScenarioSigs();

		// when:
		actualStatus = invokeRationalizationScenario();

		// then:
		statusMatches(syncSuccessStatus);
		assertEquals(expectedSigs, platformTxn.getPlatformTxn().getSignatures());
		// and:
		allVerificationStatusesAre(vs -> !VerificationStatus.UNKNOWN.equals(vs));
	}

	@Test
	public void rubberstampsCorrectPlatformSigsForCryptoCreate() throws Throwable {
		// given:
		setupFor(CRYPTO_CREATE_RECEIVER_SIG_SCENARIO);
		// and:
		List<TransactionSignature> expectedSigs = expectedCryptoCreateScenarioSigs();
		platformTxn.getPlatformTxn().addAll(asValid(expectedSigs).toArray(new TransactionSignature[0]));

		// when:
		actualStatus = invokeRationalizationScenario();

		// then:
		statusMatches(asyncSuccessStatus);
		assertEquals(expectedSigs, platformTxn.getPlatformTxn().getSignatures());
		// and:
		allVerificationStatusesAre(vs -> VerificationStatus.VALID.equals(vs));
	}

	@Test
	public void validatesComplexPayerSigActivation() throws Throwable {
		// given:
		setupFor(CRYPTO_CREATE_COMPLEX_PAYER_RECEIVER_SIG_SCENARIO);
		// and:
		List<TransactionSignature> unknownSigs = PlatformSigOps.createEd25519PlatformSigsFrom(
				List.of(COMPLEX_KEY_ACCOUNT_KT.asJKey(), CryptoCreateFactory.DEFAULT_ACCOUNT_KT.asJKey()),
				PubKeyToSigBytes.from(platformTxn.getBackwardCompatibleSignedTxn().getSigMap()),
				new BodySigningSigFactory(platformTxn)
		).getPlatformSigs();
		List<TransactionSignature> knownSigs = asKind(List.of(
				new AbstractMap.SimpleEntry<>(unknownSigs.get(0), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(1), INVALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(2), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(3), INVALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(4), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(5), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(6), INVALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(7), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(8), VALID)));

		// expect:
		assertTrue(invokePayerSigActivationScenario(knownSigs));
	}

	@Test
	public void deniesInactiveComplexPayerSig() throws Throwable {
		// given:
		setupFor(CRYPTO_CREATE_COMPLEX_PAYER_RECEIVER_SIG_SCENARIO);
		// and:
		List<TransactionSignature> unknownSigs = PlatformSigOps.createEd25519PlatformSigsFrom(
				List.of(COMPLEX_KEY_ACCOUNT_KT.asJKey(), CryptoCreateFactory.DEFAULT_ACCOUNT_KT.asJKey()),
				PubKeyToSigBytes.from(platformTxn.getBackwardCompatibleSignedTxn().getSigMap()),
				new BodySigningSigFactory(platformTxn)
		).getPlatformSigs();
		List<TransactionSignature> knownSigs = asKind(List.of(
				new AbstractMap.SimpleEntry<>(unknownSigs.get(0), INVALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(1), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(2), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(3), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(4), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(5), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(6), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(7), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(8), VALID)));

		// expect:
		assertFalse(invokePayerSigActivationScenario(knownSigs));
	}

	@Test
	public void validatesComplexOtherPartySigActivation() throws Throwable {
		// given:
		setupFor(CRYPTO_UPDATE_COMPLEX_KEY_ACCOUNT_SCENARIO);
		// and:
		List<TransactionSignature> unknownSigs = PlatformSigOps.createEd25519PlatformSigsFrom(
				List.of(DEFAULT_PAYER_KT.asJKey(), COMPLEX_KEY_ACCOUNT_KT.asJKey()),
				PubKeyToSigBytes.from(platformTxn.getBackwardCompatibleSignedTxn().getSigMap()),
				new BodySigningSigFactory(platformTxn)
		).getPlatformSigs();
		List<TransactionSignature> knownSigs = asKind(List.of(
				new AbstractMap.SimpleEntry<>(unknownSigs.get(0), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(1), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(2), INVALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(3), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(4), INVALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(5), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(6), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(7), INVALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(8), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(9), VALID)));

		// expect:
		assertTrue(invokeOtherPartySigActivationScenario(knownSigs));
	}

	@Test
	public void deniesInactiveComplexOtherPartySig() throws Throwable {
		// given:
		setupFor(CRYPTO_UPDATE_COMPLEX_KEY_ACCOUNT_SCENARIO);
		// and:
		List<TransactionSignature> unknownSigs = PlatformSigOps.createEd25519PlatformSigsFrom(
				List.of(DEFAULT_PAYER_KT.asJKey(), COMPLEX_KEY_ACCOUNT_KT.asJKey()),
				PubKeyToSigBytes.from(platformTxn.getBackwardCompatibleSignedTxn().getSigMap()),
				new BodySigningSigFactory(platformTxn)
		).getPlatformSigs();
		List<TransactionSignature> knownSigs = asKind(List.of(
				new AbstractMap.SimpleEntry<>(unknownSigs.get(0), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(1), INVALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(2), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(3), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(4), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(5), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(6), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(7), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(8), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(9), VALID)));

		// expect:
		assertFalse(invokeOtherPartySigActivationScenario(knownSigs));
	}

	@Test
	public void deniesSecondInactiveComplexOtherPartySig() throws Throwable {
		// given:
		setupFor(CRYPTO_UPDATE_COMPLEX_KEY_ACCOUNT_ADD_NEW_KEY_SCENARIO);
		// and:
		List<TransactionSignature> unknownSigs = PlatformSigOps.createEd25519PlatformSigsFrom(
				List.of(DEFAULT_PAYER_KT.asJKey(), COMPLEX_KEY_ACCOUNT_KT.asJKey(), NEW_ACCOUNT_KT.asJKey()),
				PubKeyToSigBytes.from(platformTxn.getBackwardCompatibleSignedTxn().getSigMap()),
				new BodySigningSigFactory(platformTxn)
		).getPlatformSigs();
		List<TransactionSignature> knownSigs = asKind(List.of(
				new AbstractMap.SimpleEntry<>(unknownSigs.get(0), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(1), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(2), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(3), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(4), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(5), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(6), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(7), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(8), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(9), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(10), VALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(11), INVALID),
				new AbstractMap.SimpleEntry<>(unknownSigs.get(12), INVALID)
		));

		// expect:
		assertFalse(invokeOtherPartySigActivationScenario(knownSigs));
	}

	private List<TransactionSignature> expectedCryptoCreateScenarioSigs() throws Throwable {
		return PlatformSigOps.createEd25519PlatformSigsFrom(
				List.of(
						DEFAULT_PAYER_KT.asJKey(),
						CryptoCreateFactory.DEFAULT_ACCOUNT_KT.asJKey()),
				PubKeyToSigBytes.from(platformTxn.getBackwardCompatibleSignedTxn().getSigMap()),
				new BodySigningSigFactory(platformTxn)
		).getPlatformSigs();
	}

	private boolean allVerificationStatusesAre(Predicate<VerificationStatus> statusPred) {
		return platformTxn.getPlatformTxn().getSignatures().stream()
				.map(TransactionSignature::getSignatureStatus)
				.allMatch(statusPred);
	}

	private void statusMatches(SignatureStatus expectedStatus) {
		assertEquals(expectedStatus.toLogMessage(), actualStatus.toLogMessage());
	}

	private boolean invokePayerSigActivationScenario(List<TransactionSignature> knownSigs) {
		platformTxn.getPlatformTxn().clear();
		platformTxn.getPlatformTxn().addAll(knownSigs.toArray(new TransactionSignature[0]));
		grameSigningOrder keysOrder = new grameSigningOrder(
				new MockEntityNumbers(),
				defaultLookupsFor(null, () -> accounts, () -> null, ref -> null, ref -> null),
				updateAccountSigns,
				targetWaclSigns,
				new MockGlobalDynamicProps());

		return payerSigIsActive(platformTxn, keysOrder, IN_HANDLE_SUMMARY_FACTORY);
	}

	private boolean invokeOtherPartySigActivationScenario(List<TransactionSignature> knownSigs) {
		platformTxn.getPlatformTxn().clear();
		platformTxn.getPlatformTxn().addAll(knownSigs.toArray(new TransactionSignature[0]));
		grameSigningOrder keysOrder = new grameSigningOrder(
				new MockEntityNumbers(),
				defaultLookupsFor(hfs, () -> accounts, null, ref -> null, ref -> null),
				updateAccountSigns,
				targetWaclSigns,
				new MockGlobalDynamicProps());

		return otherPartySigsAreActive(platformTxn, keysOrder, IN_HANDLE_SUMMARY_FACTORY);
	}

	private SignatureStatus invokeExpansionScenario() {
		int MAGIC_NUMBER = 10;
		SigMetadataLookup sigMetaLookups =
				defaultLookupsPlusAccountRetriesFor(
						hfs, () -> accounts, () -> null, ref -> null, ref -> null, MAGIC_NUMBER, MAGIC_NUMBER,
						runningAvgs, speedometers);
		grameSigningOrder keyOrder = new grameSigningOrder(
				new MockEntityNumbers(),
				sigMetaLookups,
				updateAccountSigns,
				targetWaclSigns,
				new MockGlobalDynamicProps());

		return expandIn(platformTxn, keyOrder, DefaultSigBytesProvider.DEFAULT_SIG_BYTES, BodySigningSigFactory::new);
	}

	private SignatureStatus invokeRationalizationScenario() throws Exception {
		SyncVerifier syncVerifier = new CryptoEngine()::verifySync;
		SigMetadataLookup sigMetaLookups = defaultLookupsFor(hfs, () -> accounts, () -> null, ref -> null, ref -> null);
		grameSigningOrder keyOrder = new grameSigningOrder(
				new MockEntityNumbers(),
				sigMetaLookups,
				updateAccountSigns,
				targetWaclSigns,
				new MockGlobalDynamicProps());

		return rationalizeIn(
				platformTxn,
				syncVerifier,
				keyOrder,
				DefaultSigBytesProvider.DEFAULT_SIG_BYTES,
				BodySigningSigFactory::new);
	}

	private void setupFor(TxnHandlingScenario scenario) throws Throwable {
		hfs = scenario.hfs();
		runningAvgs = mock(MiscRunningAvgs.class);
		speedometers = mock(MiscSpeedometers.class);
		accounts = scenario.accounts();
		platformTxn = scenario.platformTxn();

		expectedErrorStatus = null;

		signingOrder = new grameSigningOrder(
				new MockEntityNumbers(),
				defaultLookupsFor(hfs, () -> accounts, () -> null, ref -> null, ref -> null),
				updateAccountSigns,
				targetWaclSigns,
				new MockGlobalDynamicProps());
		SigningOrderResult<SignatureStatus> payerKeys =
				signingOrder.keysForPayer(platformTxn.getTxn(), PRE_HANDLE_SUMMARY_FACTORY);
		expectedSigs = new ArrayList<>();
		if (payerKeys.hasErrorReport()) {
			expectedErrorStatus = payerKeys.getErrorReport();
		} else {
			PlatformSigsCreationResult payerResult = PlatformSigOps.createEd25519PlatformSigsFrom(
					payerKeys.getOrderedKeys(),
					PubKeyToSigBytes.forPayer(platformTxn.getBackwardCompatibleSignedTxn()),
					new BodySigningSigFactory(platformTxn)
			);
			expectedSigs.addAll(payerResult.getPlatformSigs());
			SigningOrderResult<SignatureStatus> otherKeys =
					signingOrder.keysForOtherParties(platformTxn.getTxn(), PRE_HANDLE_SUMMARY_FACTORY);
			if (otherKeys.hasErrorReport()) {
				expectedErrorStatus = otherKeys.getErrorReport();
			} else {
				PlatformSigsCreationResult otherResult = PlatformSigOps.createEd25519PlatformSigsFrom(
						otherKeys.getOrderedKeys(),
						PubKeyToSigBytes.forOtherParties(platformTxn.getBackwardCompatibleSignedTxn()),
						new BodySigningSigFactory(platformTxn)
				);
				if (!otherResult.hasFailed()) {
					expectedSigs.addAll(otherResult.getPlatformSigs());
				}
			}
		}
		successStatus = new SignatureStatus(
				SignatureStatusCode.SUCCESS, ResponseCodeEnum.OK,
				false, platformTxn.getTxn().getTransactionID(),
				null, null, null, null);
		syncSuccessStatus = new SignatureStatus(
				SignatureStatusCode.SUCCESS_VERIFY_SYNC, ResponseCodeEnum.OK,
				true, platformTxn.getTxn().getTransactionID(),
				null, null, null, null);
		asyncSuccessStatus = new SignatureStatus(
				SignatureStatusCode.SUCCESS_VERIFY_ASYNC, ResponseCodeEnum.OK,
				true, platformTxn.getTxn().getTransactionID(),
				null, null, null, null);
		sigCreationFailureStatus = new SignatureStatus(
				SignatureStatusCode.KEY_COUNT_MISMATCH, ResponseCodeEnum.INVALID_SIGNATURE_COUNT_MISMATCHING_KEY,
				true, platformTxn.getTxn().getTransactionID(),
				null, null, null, null);
	}
}
