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

import com.google.protobuf.ByteString;
import com.grame.services.config.MockAccountNumbers;
import com.grame.services.config.MockEntityNumbers;
import com.grame.services.config.MockGlobalDynamicProps;
import com.grame.services.context.ContextPlatformStatus;
import com.grame.services.context.primitives.StateView;
import com.grame.services.context.properties.NodeLocalProperties;
import com.grame.services.context.properties.PropertySource;
import com.grame.services.fees.StandardExemptions;
import com.grame.services.legacy.exception.InvalidAccountIDException;
import com.grame.services.legacy.exception.KeyPrefixMismatchException;
import com.grame.services.legacy.handler.TransactionHandler;
import com.grame.services.legacy.unit.utils.DummyFunctionalityThrottling;
import com.grame.services.legacy.unit.utils.DummyHapiPermissions;
import com.grame.services.queries.validation.QueryFeeCheck;
import com.grame.services.security.ops.SystemOpPolicies;
import com.grame.services.sigs.order.grameSigningOrder;
import com.grame.services.sigs.sourcing.DefaultSigBytesProvider;
import com.grame.services.sigs.utils.PrecheckUtils;
import com.grame.services.sigs.verification.PrecheckKeyReqs;
import com.grame.services.sigs.verification.PrecheckVerifier;
import com.grame.services.sigs.verification.SyncVerifier;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.stats.MiscRunningAvgs;
import com.grame.services.stats.MiscSpeedometers;
import com.grame.services.txns.validation.BasicPrecheck;
import com.grame.services.utils.PlatformTxnAccessor;
import com.grame.test.factories.scenarios.TxnHandlingScenario;
import com.grame.test.mocks.TestContextValidator;
import com.grame.test.mocks.TestFeesFactory;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;
import com.swirlds.common.PlatformStatus;
import com.swirlds.common.crypto.engine.CryptoEngine;
import com.swirlds.fcmap.FCMap;
import org.junit.jupiter.api.Test;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static com.grame.services.security.ops.SystemOpAuthorization.AUTHORIZED;
import static com.grame.services.sigs.metadata.DelegatingSigMetadataLookup.defaultLookupsFor;
import static com.grame.services.sigs.metadata.DelegatingSigMetadataLookup.defaultLookupsPlusAccountRetriesFor;
import static com.grame.test.CiConditions.isInCircleCi;
import static com.grame.test.factories.scenarios.BadPayerScenarios.INVALID_PAYER_ID_SCENARIO;
import static com.grame.test.factories.scenarios.CryptoTransferScenarios.CRYPTO_TRANSFER_RECEIVER_SIG_SCENARIO;
import static com.grame.test.factories.scenarios.CryptoTransferScenarios.QUERY_PAYMENT_INVALID_SENDER_SCENARIO;
import static com.grame.test.factories.scenarios.CryptoTransferScenarios.QUERY_PAYMENT_MISSING_SIGS_SCENARIO;
import static com.grame.test.factories.scenarios.CryptoTransferScenarios.VALID_QUERY_PAYMENT_SCENARIO;
import static com.grame.test.factories.scenarios.SystemDeleteScenarios.AMBIGUOUS_SIG_MAP_SCENARIO;
import static com.grame.test.factories.scenarios.SystemDeleteScenarios.FULL_PAYER_SIGS_VIA_MAP_SCENARIO;
import static com.grame.test.factories.scenarios.SystemDeleteScenarios.INVALID_PAYER_SIGS_VIA_MAP_SCENARIO;
import static com.grame.test.factories.scenarios.SystemDeleteScenarios.MISSING_PAYER_SIGS_VIA_MAP_SCENARIO;
import static com.grame.test.factories.txns.SignedTxnFactory.DEFAULT_NODE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.BDDMockito.anyDouble;
import static org.mockito.BDDMockito.anyInt;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;

public class TxnHandlerVerifySigRegressionTest {
	private PrecheckKeyReqs precheckKeyReqs;
	private PrecheckVerifier precheckVerifier;
	private grameSigningOrder keyOrder;
	private grameSigningOrder retryingKeyOrder;
	private Predicate<TransactionBody> isQueryPayment;
	private PlatformTxnAccessor platformTxn;
	private FCMap<MerkleEntityId, MerkleAccount> accounts;
	private TransactionHandler subject;
	private MiscRunningAvgs runningAvgs;
	private MiscSpeedometers speedometers;

	private SystemOpPolicies mockSystemOpPolicies = new SystemOpPolicies(new MockEntityNumbers());
	private Predicate<TransactionBody> updateAccountSigns = txn ->
			mockSystemOpPolicies.check(txn, grameFunctionality.CryptoUpdate) != AUTHORIZED;
	private BiPredicate<TransactionBody, grameFunctionality> targetWaclSigns = (txn, function) ->
			mockSystemOpPolicies.check(txn, function) != AUTHORIZED;

	@Test
	public void rejectsInvalidTxn() throws Throwable {
		assumeFalse(isInCircleCi);

		// given:
		Transaction invalidSignedTxn = Transaction.newBuilder()
				.setBodyBytes(ByteString.copyFrom("NONSENSE".getBytes())).build();
		var policies = new SystemOpPolicies(new MockEntityNumbers());
		var platformStatus = new ContextPlatformStatus();
		platformStatus.set(PlatformStatus.ACTIVE);
		subject = new TransactionHandler(
				null,
				() -> accounts,
				DEFAULT_NODE,
				null,
				TestFeesFactory.FEES_FACTORY.get(),
				() -> new StateView(StateView.EMPTY_TOPICS_SUPPLIER, () -> accounts, mock(NodeLocalProperties.class), null),
				new BasicPrecheck(TestContextValidator.TEST_VALIDATOR, new MockGlobalDynamicProps()),
				new QueryFeeCheck(() -> accounts),
				new MockAccountNumbers(),
				policies,
				new StandardExemptions(new MockAccountNumbers(), policies),
				platformStatus,
				DummyFunctionalityThrottling.throttlingAlways(false),
				new DummyHapiPermissions());

		// expect:
		assertFalse(subject.verifySignature(invalidSignedTxn));
	}

	@Test
	public void acceptsValidNonCryptoTransferPayerSig() throws Throwable {
		assumeFalse(isInCircleCi);

		// given:
		setupFor(FULL_PAYER_SIGS_VIA_MAP_SCENARIO);

		// expect:
		assertTrue(subject.verifySignature(platformTxn.getBackwardCompatibleSignedTxn()));
	}

	@Test
	public void rejectsIncompleteNonCryptoTransferPayerSig() throws Throwable {
		assumeFalse(isInCircleCi);

		// given:
		setupFor(MISSING_PAYER_SIGS_VIA_MAP_SCENARIO);

		// expect:
		assertFalse(subject.verifySignature(platformTxn.getBackwardCompatibleSignedTxn()));
	}

	@Test
	public void rejectsInvalidNonCryptoTransferPayerSig() throws Throwable {
		assumeFalse(isInCircleCi);

		// given:
		setupFor(INVALID_PAYER_SIGS_VIA_MAP_SCENARIO);

		// expect:
		assertFalse(subject.verifySignature(platformTxn.getBackwardCompatibleSignedTxn()));
	}

	@Test
	public void acceptsNonQueryPaymentTransfer() throws Throwable {
		assumeFalse(isInCircleCi);

		// given:
		setupFor(CRYPTO_TRANSFER_RECEIVER_SIG_SCENARIO);

		// expect:
		assertTrue(subject.verifySignature(platformTxn.getBackwardCompatibleSignedTxn()));
	}

	@Test
	public void acceptsQueryPaymentTransfer() throws Throwable {
		assumeFalse(isInCircleCi);

		// given:
		setupFor(VALID_QUERY_PAYMENT_SCENARIO);

		// expect:
		assertTrue(subject.verifySignature(platformTxn.getBackwardCompatibleSignedTxn()));
	}

	@Test
	public void rejectsInvalidPayerAccount() throws Throwable {
		assumeFalse(isInCircleCi);

		// given:
		setupFor(INVALID_PAYER_ID_SCENARIO);

		// expect:
		assertFalse(subject.verifySignature(platformTxn.getBackwardCompatibleSignedTxn()));
	}

	@Test
	public void throwsOnInvalidSenderAccount() throws Throwable {
		assumeFalse(isInCircleCi);

		// given:
		setupFor(QUERY_PAYMENT_INVALID_SENDER_SCENARIO);

		// expect:
		assertThrows(InvalidAccountIDException.class,
				() -> subject.verifySignature(platformTxn.getBackwardCompatibleSignedTxn()));
		verify(runningAvgs).recordAccountLookupRetries(anyInt());
		verify(runningAvgs).recordAccountRetryWaitMs(anyDouble());
		verify(speedometers).cycleAccountLookupRetries();
	}

	@Test
	public void throwsOnInvalidSigMap() throws Throwable {
		assumeFalse(isInCircleCi);

		// given:
		setupFor(AMBIGUOUS_SIG_MAP_SCENARIO);

		// expect:
		assertThrows(KeyPrefixMismatchException.class,
				() -> subject.verifySignature(platformTxn.getBackwardCompatibleSignedTxn()));
	}

	@Test
	public void rejectsQueryPaymentTransferWithMissingSigs() throws Throwable {
		assumeFalse(isInCircleCi);

		// given:
		setupFor(QUERY_PAYMENT_MISSING_SIGS_SCENARIO);

		// expect:
		assertFalse(subject.verifySignature(platformTxn.getBackwardCompatibleSignedTxn()));
	}

	private void setupFor(TxnHandlingScenario scenario)	throws Throwable {
		final int MN = 10;
		accounts = scenario.accounts();
		platformTxn = scenario.platformTxn();
		runningAvgs = mock(MiscRunningAvgs.class);
		speedometers = mock(MiscSpeedometers.class);
		keyOrder = new grameSigningOrder(
				new MockEntityNumbers(),
				defaultLookupsFor(null, () -> accounts, () -> null, ref -> null, ref -> null),
				updateAccountSigns,
				targetWaclSigns,
				new MockGlobalDynamicProps());
		retryingKeyOrder =
				new grameSigningOrder(
						new MockEntityNumbers(),
						defaultLookupsPlusAccountRetriesFor(
								null, () -> accounts, () -> null, ref -> null, ref -> null,
								MN, MN, runningAvgs, speedometers),
						updateAccountSigns,
						targetWaclSigns,
						new MockGlobalDynamicProps());
		isQueryPayment = PrecheckUtils.queryPaymentTestFor(DEFAULT_NODE);
		SyncVerifier syncVerifier = new CryptoEngine()::verifySync;
		precheckKeyReqs = new PrecheckKeyReqs(keyOrder, retryingKeyOrder, isQueryPayment);
		precheckVerifier = new PrecheckVerifier(syncVerifier, precheckKeyReqs, DefaultSigBytesProvider.DEFAULT_SIG_BYTES);

		var policies = new SystemOpPolicies(new MockEntityNumbers());
		var platformStatus = new ContextPlatformStatus();
		platformStatus.set(PlatformStatus.ACTIVE);
		subject = new TransactionHandler(
				null,
				precheckVerifier,
				() -> accounts,
				DEFAULT_NODE,
				new MockAccountNumbers(),
				policies,
				new StandardExemptions(new MockAccountNumbers(), policies),
				platformStatus,
				new DummyHapiPermissions());
	}
}

