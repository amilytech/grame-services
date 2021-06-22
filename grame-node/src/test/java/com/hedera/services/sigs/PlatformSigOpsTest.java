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
import com.grame.services.legacy.core.jproto.JEd25519Key;
import com.grame.services.sigs.factories.TxnScopedPlatformSigFactory;
import com.grame.services.sigs.sourcing.PubKeyToSigBytes;
import com.grame.test.factories.keys.KeyTree;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.TransactionID;
import com.grame.services.legacy.core.jproto.JKey;
import com.grame.services.legacy.crypto.SignatureStatus;
import com.grame.services.legacy.crypto.SignatureStatusCode;
import com.grame.services.legacy.exception.KeyPrefixMismatchException;
import com.grame.services.legacy.exception.KeySignatureCountMismatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.grame.test.factories.keys.NodeFactory.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static com.grame.services.sigs.PlatformSigOps.createEd25519PlatformSigsFrom;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PlatformSigOpsTest {
	private final byte[] EMPTY_SIG = new byte[0];
	private final byte[] MOCK_SIG = "FIRST".getBytes();
	private final byte[][] MORE_MOCK_SIGS = new byte[][] {
		"SECOND".getBytes(), "THIRD".getBytes(), "FOURTH".getBytes(), "FIFTH".getBytes()
	};
	private final byte[][] MORE_EMPTY_SIGS = new byte[][] {
			EMPTY_SIG, EMPTY_SIG, EMPTY_SIG, EMPTY_SIG
	};
	private final List<JKey> pubKeys = new ArrayList<>();
	private final List<KeyTree> kts = List.of(
			KeyTree.withRoot(ed25519()),
			KeyTree.withRoot(list(ed25519(), ed25519())),
			KeyTree.withRoot(threshold(1, list(ed25519()), ed25519()))
	);
	private PubKeyToSigBytes sigBytes;
	private TxnScopedPlatformSigFactory sigFactory;

	@BeforeEach
	public void setup() throws Throwable {
		pubKeys.clear();
		sigBytes = mock(PubKeyToSigBytes.class);
		sigFactory = mock(TxnScopedPlatformSigFactory.class);
		for (KeyTree kt : kts) {
			pubKeys.add(kt.asJKey());
		}
	}

	@Test
	public void createsOnlyNonDegenerateSigs() throws Throwable {
		given(sigBytes.sigBytesFor(any())).willReturn(MOCK_SIG, MORE_EMPTY_SIGS);

		// when:
		PlatformSigsCreationResult result = createEd25519PlatformSigsFrom(pubKeys, sigBytes, sigFactory);

		// then:
		AtomicInteger nextSigIndex = new AtomicInteger(0);
		for (KeyTree kt : kts) {
			kt.traverseLeaves(leaf -> {
				ByteString pk = leaf.asKey().getEd25519();
				if (nextSigIndex.get() == 0) {
					verify(sigFactory).create(pk, ByteString.copyFrom(MOCK_SIG));
				} else {
					verify(sigFactory, never()).create(pk, ByteString.copyFrom(EMPTY_SIG));
				}
				nextSigIndex.addAndGet(1);
			});
		}
		// and:
		assertEquals(1, result.getPlatformSigs().size());
	}

	@Test
	public void createsSigsInTraversalOrder() throws Throwable {
		given(sigBytes.sigBytesFor(any())).willReturn(MOCK_SIG, MORE_MOCK_SIGS);

		// when:
		PlatformSigsCreationResult result = createEd25519PlatformSigsFrom(pubKeys, sigBytes, sigFactory);

		// then:
		AtomicInteger nextSigIndex = new AtomicInteger(0);
		for (KeyTree kt : kts) {
			kt.traverseLeaves(leaf -> {
				ByteString pk = leaf.asKey().getEd25519();
				byte[] sigBytes = (nextSigIndex.get() == 0) ? MOCK_SIG : MORE_MOCK_SIGS[nextSigIndex.get() - 1];
				verify(sigFactory).create(pk, ByteString.copyFrom(sigBytes));
				nextSigIndex.addAndGet(1);
			});
		}
		// and:
		assertEquals(5, result.getPlatformSigs().size());
	}

	@Test
	public void ignoresAmbiguousScheduledSig() throws Throwable {
		// setup:
		JKey scheduledKey = new JEd25519Key("01234578901234578901234578901".getBytes());
		// and:
		scheduledKey.setForScheduledTxn(true);

		given(sigBytes.sigBytesFor(any())).willThrow(KeyPrefixMismatchException.class);

		// when:
		PlatformSigsCreationResult result = createEd25519PlatformSigsFrom(List.of(scheduledKey), sigBytes, sigFactory);

		// then:
		assertFalse(result.hasFailed());
		assertTrue(result.getPlatformSigs().isEmpty());
	}

	@Test
	public void doesntIgnoreUnrecognizedProblemForScheduledSig() throws Throwable {
		// setup:
		JKey scheduledKey = new JEd25519Key("01234578901234578901234578901".getBytes());
		// and:
		scheduledKey.setForScheduledTxn(true);

		given(sigBytes.sigBytesFor(any())).willThrow(IllegalStateException.class);

		// when:
		PlatformSigsCreationResult result = createEd25519PlatformSigsFrom(List.of(scheduledKey), sigBytes, sigFactory);

		// then:
		assertTrue(result.hasFailed());
	}

	@Test
	public void failsOnInsufficientSigs() throws Throwable {
		given(sigBytes.sigBytesFor(any())).willReturn(MOCK_SIG).willThrow(Exception.class);

		// when:
		PlatformSigsCreationResult result = createEd25519PlatformSigsFrom(pubKeys, sigBytes, sigFactory);

		// then:
		assertEquals(1, result.getPlatformSigs().size());
		assertTrue(result.hasFailed());
	}

	@Test
	public void returnsSuccessSigStatusByDefault() {
		// given:
		TransactionID txnId = TransactionID.getDefaultInstance();
		PlatformSigsCreationResult subject = new PlatformSigsCreationResult();
		SignatureStatus expectedStatus = new SignatureStatus(
				SignatureStatusCode.SUCCESS, ResponseCodeEnum.OK,
				true, txnId, null, null, null, null);

		// when:
		SignatureStatus status = subject.asSignatureStatus(true, txnId);

		// then:
		assertEquals(expectedStatus.toLogMessage(), status.toLogMessage());
	}

	@Test
	public void reportsInsufficientSigs() {
		// given:
		TransactionID txnId = TransactionID.getDefaultInstance();
		PlatformSigsCreationResult subject = new PlatformSigsCreationResult();
		subject.setTerminatingEx(new KeySignatureCountMismatchException("No!"));
		// and:
		SignatureStatus expectedStatus = new SignatureStatus(
				SignatureStatusCode.KEY_COUNT_MISMATCH, ResponseCodeEnum.INVALID_SIGNATURE_COUNT_MISMATCHING_KEY,
				true, txnId, null, null, null, null);

		// when:
		SignatureStatus status = subject.asSignatureStatus(true, txnId);

		// then:
		assertEquals(expectedStatus.toLogMessage(), status.toLogMessage());
	}

	@Test
	public void reportsInvalidSigMap() {
		// given:
		TransactionID txnId = TransactionID.getDefaultInstance();
		PlatformSigsCreationResult subject = new PlatformSigsCreationResult();
		subject.setTerminatingEx(new KeyPrefixMismatchException("No!"));
		// and:
		SignatureStatus expectedStatus = new SignatureStatus(
				SignatureStatusCode.KEY_PREFIX_MISMATCH, ResponseCodeEnum.KEY_PREFIX_MISMATCH,
				true, txnId, null, null, null, null);

		// when:
		SignatureStatus status = subject.asSignatureStatus(true, txnId);

		// then:
		assertEquals(expectedStatus.toLogMessage(), status.toLogMessage());
	}

	@Test
	public void reportsNonspecificInvalidSig() {
		// given:
		TransactionID txnId = TransactionID.getDefaultInstance();
		PlatformSigsCreationResult subject = new PlatformSigsCreationResult();
		subject.setTerminatingEx(new Exception());
		// and:
		SignatureStatus expectedStatus = new SignatureStatus(
				SignatureStatusCode.GENERAL_ERROR, ResponseCodeEnum.INVALID_SIGNATURE,
				true, txnId, null, null, null, null);

		// when:
		SignatureStatus status = subject.asSignatureStatus(true, txnId);

		// then:
		assertEquals(expectedStatus.toLogMessage(), status.toLogMessage());
	}
}
