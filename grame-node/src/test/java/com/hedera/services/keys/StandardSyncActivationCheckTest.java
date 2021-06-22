package com.grame.services.keys;

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

import com.grame.services.legacy.core.jproto.JKey;
import com.grame.services.sigs.PlatformSigsCreationResult;
import com.grame.services.sigs.PlatformSigsFactory;
import com.grame.services.sigs.factories.TxnScopedPlatformSigFactory;
import com.grame.services.sigs.sourcing.PubKeyToSigBytes;
import com.grame.services.sigs.verification.SyncVerifier;
import com.grame.services.utils.PlatformTxnAccessor;
import com.grame.services.utils.TxnAccessor;
import com.grame.test.factories.scenarios.TxnHandlingScenario;
import com.gramegrame.api.proto.java.Transaction;
import com.swirlds.common.crypto.TransactionSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static com.grame.services.keys.StandardSyncActivationCheck.allKeysAreActive;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;

class StandardSyncActivationCheckTest {
	JKey key;
	Transaction signedTxn;
	List<TransactionSignature> sigs;
	PubKeyToSigBytes sigBytes;
	PlatformSigsCreationResult result;

	SyncVerifier syncVerifier;
	PlatformTxnAccessor accessor;
	PlatformSigsFactory sigsFactory;
	TxnScopedPlatformSigFactory scopedSig;
	Function<byte[], TransactionSignature> sigsFn;
	Function<Transaction, PubKeyToSigBytes> sigBytesProvider;
	Function<TxnAccessor, TxnScopedPlatformSigFactory> scopedSigProvider;
	BiPredicate<JKey, Function<byte[], TransactionSignature>> isActive;
	Function<List<TransactionSignature>, Function<byte[], TransactionSignature>> sigsFnProvider;

	@BeforeEach
	private void setup() throws Exception {
		sigs = mock(List.class);
		key = TxnHandlingScenario.MISC_TOPIC_ADMIN_KT.asJKey();
		sigBytes = mock(PubKeyToSigBytes.class);
		signedTxn = mock(Transaction.class);

		sigsFn = mock(Function.class);
		result = mock(PlatformSigsCreationResult.class);
		accessor = mock(PlatformTxnAccessor.class);
		given(accessor.getTxnBytes()).willReturn("Goodness".getBytes());
		given(accessor.getBackwardCompatibleSignedTxn()).willReturn(signedTxn);
		isActive = mock(BiPredicate.class);
		syncVerifier = mock(SyncVerifier.class);
		sigBytesProvider = mock(Function.class);
		given(sigBytesProvider.apply(signedTxn)).willReturn(sigBytes);
		sigsFnProvider = mock(Function.class);
		given(sigsFnProvider.apply(sigs)).willReturn(sigsFn);
		scopedSig = mock(TxnScopedPlatformSigFactory.class);
		scopedSigProvider = mock(Function.class);
		given(scopedSigProvider.apply(any())).willReturn(scopedSig);
		sigsFactory = mock(PlatformSigsFactory.class);
		given(sigsFactory.createEd25519From(List.of(key), sigBytes, scopedSig)).willReturn(result);
	}

	@Test
	public void happyPathFlows() {
		given(result.hasFailed()).willReturn(false);
		given(result.getPlatformSigs()).willReturn(sigs);
		given(isActive.test(any(), any())).willReturn(true);

		// when:
		boolean flag = allKeysAreActive(
				List.of(key),
				syncVerifier,
				accessor,
				sigsFactory,
				sigBytesProvider,
				scopedSigProvider,
				isActive,
				sigsFnProvider);

		// then:
		verify(isActive).test(key, sigsFn);
		// and:
		assertTrue(flag);
	}

	@Test
	public void failsOnInActive() {
		given(result.hasFailed()).willReturn(false);
		given(result.getPlatformSigs()).willReturn(sigs);
		given(isActive.test(any(), any())).willReturn(false);

		// when:
		boolean flag = allKeysAreActive(
				List.of(key),
				syncVerifier,
				accessor,
				sigsFactory,
				sigBytesProvider,
				scopedSigProvider,
				isActive,
				sigsFnProvider);

		// then:
		verify(isActive).test(key, sigsFn);
		// and:
		assertFalse(flag);
	}

	@Test
	public void shortCircuitsOnCreationFailure() {
		given(result.hasFailed()).willReturn(true);

		// when:
		boolean flag = allKeysAreActive(
				List.of(key),
				syncVerifier,
				accessor,
				sigsFactory,
				sigBytesProvider,
				scopedSigProvider,
				isActive,
				sigsFnProvider);

		// then:
		assertFalse(flag);
	}
}
