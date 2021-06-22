package com.grame.services.txns.file;

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
import com.google.protobuf.StringValue;
import com.grame.services.config.EntityNumbers;
import com.grame.services.config.MockEntityNumbers;
import com.grame.services.context.TransactionContext;
import com.grame.services.context.primitives.StateView;
import com.grame.services.files.grameFs;
import com.grame.services.files.TieredgrameFs;
import com.grame.services.txns.validation.OptionValidator;
import com.grame.services.utils.MiscUtils;
import com.grame.services.utils.PlatformTxnAccessor;
import com.grame.test.factories.scenarios.TxnHandlingScenario;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.Duration;
import com.gramegrame.api.proto.java.FileID;
import com.gramegrame.api.proto.java.FileUpdateTransactionBody;
import com.gramegrame.api.proto.java.Key;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionID;
import com.grame.services.files.HFileMeta;
import com.grame.services.legacy.core.jproto.JKey;
import org.apache.commons.codec.DecoderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.AUTHORIZATION_FAILED;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.AUTORENEW_DURATION_NOT_IN_RANGE;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.BAD_ENCODING;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.FAIL_INVALID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.FILE_DELETED;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_EXPIRATION_TIME;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_FILE_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_ZERO_BYTE_IN_STRING;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.MAX_FILE_SIZE_EXCEEDED;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.MEMO_TOO_LONG;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.SUCCESS;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.UNAUTHORIZED;
import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.BDDMockito.*;

class FileUpdateTransitionLogicTest {
	enum UpdateTarget { KEY, EXPIRY, CONTENTS, MEMO }

	long lifetime = 1_234_567L;
	long txnValidDuration = 180;
	long now = Instant.now().getEpochSecond();
	long oldExpiry = now + lifetime;
	long newExpiry = oldExpiry + 2_345_678L;
	Duration expectedDuration = Duration.newBuilder()
			.setSeconds(newExpiry - now)
			.build();
	FileID nonSysFileTarget = IdUtils.asFile("0.1.2222");
	FileID sysFileTarget = IdUtils.asFile("0.1.121");
	Key newWacl = TxnHandlingScenario.MISC_FILE_WACL_KT.asKey();
	JKey oldWacl, actionableNewWacl;
	HFileMeta oldAttr, newAttr, deletedAttr, immutableAttr;
	String oldMemo = "Past";
	String newMemo = "Future";
	byte[] newContents = "STUFF".getBytes();
	AccountID sysAdmin = IdUtils.asAccount("0.0.50");
	AccountID nonSysAdmin = IdUtils.asAccount("0.0.13257");

	TransactionID txnId;
	TransactionBody fileUpdateTxn;
	private PlatformTxnAccessor accessor;

	grameFs hfs;
	OptionValidator validator;
	EntityNumbers number = new MockEntityNumbers();
	TransactionContext txnCtx;

	FileUpdateTransitionLogic subject;

	@BeforeEach
	private void setup() throws Throwable {
		oldWacl = TxnHandlingScenario.SIMPLE_NEW_WACL_KT.asJKey();
		oldAttr = new HFileMeta(false, oldWacl, oldExpiry, oldMemo);
		deletedAttr = new HFileMeta(true, oldWacl, oldExpiry);
		immutableAttr = new HFileMeta(false, StateView.EMPTY_WACL, oldExpiry);

		actionableNewWacl = TxnHandlingScenario.MISC_FILE_WACL_KT.asJKey();
		newAttr = new HFileMeta(false, actionableNewWacl, newExpiry, newMemo);

		accessor = mock(PlatformTxnAccessor.class);
		txnCtx = mock(TransactionContext.class);
		given(txnCtx.activePayer()).willReturn(nonSysAdmin);
		hfs = mock(grameFs.class);
		given(hfs.exists(nonSysFileTarget)).willReturn(true);
		given(hfs.getattr(nonSysFileTarget)).willReturn(oldAttr);

		validator = mock(OptionValidator.class);
		given(validator.isValidAutoRenewPeriod(expectedDuration)).willReturn(false);
		given(validator.hasGoodEncoding(newWacl)).willReturn(true);
		given(validator.memoCheck(newMemo)).willReturn(OK);

		subject = new FileUpdateTransitionLogic(hfs, number, validator, txnCtx);
	}

	@Test
	public void doesntUpdateWaclIfNoNew() {
		// setup:
		grameFs.UpdateResult res = mock(grameFs.UpdateResult.class);
		ArgumentCaptor<HFileMeta> captor = ArgumentCaptor.forClass(HFileMeta.class);

		givenTxnCtxUpdating(EnumSet.of(UpdateTarget.EXPIRY));
		// and:
		given(hfs.getattr(nonSysFileTarget)).willReturn(
				new HFileMeta(false, oldWacl, oldExpiry));
		given(hfs.setattr(any(), any())).willReturn(res);

		// when:
		subject.doStateTransition();

		// then:
		verify(hfs).setattr(argThat(nonSysFileTarget::equals), captor.capture());
		// and:
		assertEquals(oldAttr.getWacl().toString(), captor.getValue().getWacl().toString());
	}

	@Test
	public void doesntOverwriteIfNoContents() {
		grameFs.UpdateResult res = mock(grameFs.UpdateResult.class);
		givenTxnCtxUpdating(EnumSet.of(UpdateTarget.EXPIRY, UpdateTarget.KEY));
		// and:
		given(hfs.getattr(nonSysFileTarget)).willReturn(oldAttr);
		given(hfs.setattr(any(), any())).willReturn(res);

		// when:
		subject.doStateTransition();

		// then:
		verify(hfs, never()).overwrite(any(), any());
	}

	@Test
	public void allowsSysAdminToUpdateImmutableSysFile() {
		givenTxnCtxUpdating(EnumSet.of(UpdateTarget.CONTENTS), sysFileTarget);
		given(txnCtx.activePayer()).willReturn(sysAdmin);
		// and:
		given(hfs.exists(sysFileTarget)).willReturn(true);
		given(hfs.getattr(sysFileTarget)).willReturn(immutableAttr);
		// and:
		given(hfs.overwrite(any(), any()))
				.willReturn(new TieredgrameFs.SimpleUpdateResult(false, true, SUCCESS));
		given(hfs.setattr(any(), any()))
				.willReturn(new TieredgrameFs.SimpleUpdateResult(true, false, SUCCESS));

		// when:
		subject.doStateTransition();

		// then:
		verify(hfs).overwrite(
				argThat(sysFileTarget::equals),
				argThat(bytes -> Arrays.equals(newContents, bytes)));
		verify(txnCtx).setStatus(SUCCESS);
	}

	@Test
	public void doesntAllowSysAdminToUpdateImmutableNonSysFile() {
		givenTxnCtxUpdating(EnumSet.of(UpdateTarget.CONTENTS), nonSysFileTarget);
		given(txnCtx.activePayer()).willReturn(sysAdmin);
		// and:
		given(hfs.exists(nonSysFileTarget)).willReturn(true);
		given(hfs.getattr(nonSysFileTarget)).willReturn(immutableAttr);
		// and:
		given(hfs.overwrite(any(), any()))
				.willReturn(new TieredgrameFs.SimpleUpdateResult(false, true, SUCCESS));
		given(hfs.setattr(any(), any()))
				.willReturn(new TieredgrameFs.SimpleUpdateResult(true, false, SUCCESS));

		// when:
		subject.doStateTransition();

		// then:
		verify(hfs, never()).overwrite(
				argThat(nonSysFileTarget::equals),
				argThat(bytes -> Arrays.equals(newContents, bytes)));
		verify(txnCtx).setStatus(UNAUTHORIZED);
	}

	@Test
	public void rejectsUpdatingImmutableContents() {
		givenTxnCtxUpdating(EnumSet.of(UpdateTarget.CONTENTS));
		// and:
		given(hfs.getattr(nonSysFileTarget)).willReturn(immutableAttr);

		// when:
		subject.doStateTransition();

		// then:
		verify(hfs, never()).overwrite(any(), any());
		// and:
		verify(txnCtx).setStatus(UNAUTHORIZED);
	}

	@Test
	public void rejectsUpdatingImmutableKey() {
		givenTxnCtxUpdating(EnumSet.of(UpdateTarget.KEY));
		// and:
		given(hfs.getattr(nonSysFileTarget)).willReturn(immutableAttr);

		// when:
		subject.doStateTransition();

		// then:
		verify(hfs, never()).overwrite(any(), any());
		// and:
		verify(txnCtx).setStatus(UNAUTHORIZED);
	}

	@Test
	public void allowsUpdatingExpirationOnly() {
		givenTxnCtxUpdating(EnumSet.of(UpdateTarget.EXPIRY));
		// and:
		given(hfs.setattr(any(), any()))
				.willReturn(new TieredgrameFs.SimpleUpdateResult(true, false, SUCCESS));
		given(hfs.getattr(nonSysFileTarget)).willReturn(immutableAttr);

		// when:
		subject.doStateTransition();

		// then:
		verify(hfs, never()).overwrite(any(), any());
		// and:
		verify(txnCtx).setStatus(SUCCESS);
	}

	@Test
	public void doesntUpdateExpiryIfRetraction() {
		givenTxnCtxUpdating(EnumSet.of(UpdateTarget.EXPIRY, UpdateTarget.KEY));
		fileUpdateTxn = fileUpdateTxn.toBuilder()
				.setFileUpdate(fileUpdateTxn.getFileUpdate().toBuilder()
						.setExpirationTime(Timestamp.newBuilder().setSeconds(now)))
				.build();
		given(accessor.getTxn()).willReturn(fileUpdateTxn);
		// and:
		given(hfs.getattr(nonSysFileTarget)).willReturn(oldAttr);

		// when:
		subject.doStateTransition();

		// then:
		assertEquals(oldExpiry, oldAttr.getExpiry());
	}

	@Test
	public void shortCircuitsOnFailedOverwrite() {
		givenTxnCtxUpdating(EnumSet.allOf(UpdateTarget.class));
		// and:
		given(hfs.getattr(nonSysFileTarget)).willReturn(oldAttr);
		given(hfs.overwrite(any(), any()))
				.willReturn(new TieredgrameFs.SimpleUpdateResult(
						false,
						false,
						AUTHORIZATION_FAILED));

		// when:
		subject.doStateTransition();

		// then:
		verify(hfs, never()).setattr(any(), any());
		verify(txnCtx).setStatus(AUTHORIZATION_FAILED);
	}

	@Test
	public void happyPathFlows() {
		// setup:
		InOrder inOrder = inOrder(hfs, txnCtx);

		givenTxnCtxUpdating(EnumSet.allOf(UpdateTarget.class));
		// and:
		given(hfs.overwrite(any(), any()))
				.willReturn(new TieredgrameFs.SimpleUpdateResult(false, true, SUCCESS));
		given(hfs.setattr(any(), any()))
				.willReturn(new TieredgrameFs.SimpleUpdateResult(true, false, SUCCESS));
		given(hfs.getattr(nonSysFileTarget)).willReturn(oldAttr);

		// when:
		subject.doStateTransition();

		// then:
		inOrder.verify(hfs).overwrite(
				argThat(nonSysFileTarget::equals),
				argThat(bytes -> Arrays.equals(newContents, bytes)));
		inOrder.verify(hfs).setattr(
				argThat(nonSysFileTarget::equals),
				argThat(attr -> newAttr.toString().equals(attr.toString())));
		inOrder.verify(txnCtx).setStatus(SUCCESS);
	}

	@Test
	public void transitionValidatesKeyIfPresent() {
		givenTxnCtxUpdating(EnumSet.of(UpdateTarget.KEY));
		// and:
		given(validator.hasGoodEncoding(newWacl)).willReturn(false);

		// when:
		subject.doStateTransition();

		// expect:
		verify(validator).hasGoodEncoding(newWacl);
		verify(txnCtx).setStatus(BAD_ENCODING);
	}

	@Test
	public void recoversFromUnrecognizedDetailMessage() {
		givenTxnCtxUpdating(EnumSet.of(UpdateTarget.EXPIRY));
		// and:
		given(hfs.exists(nonSysFileTarget)).willReturn(true);
		given(hfs.getattr(nonSysFileTarget)).willThrow(new IllegalArgumentException("OOPS"));

		// when:
		subject.doStateTransition();

		// expect:
		verify(txnCtx).setStatus(FAIL_INVALID);
	}

	@Test
	public void usesFailInvalidOnUnknownFailure() {
		givenTxnCtxUpdating(EnumSet.of(UpdateTarget.EXPIRY));
		// and:
		given(hfs.exists(nonSysFileTarget)).willReturn(true);
		given(hfs.getattr(nonSysFileTarget)).willThrow(NullPointerException.class);

		// when:
		subject.doStateTransition();

		// expect:
		verify(txnCtx).setStatus(FAIL_INVALID);
	}

	@Test
	public void transitionRejectsDeletedFile() {
		givenTxnCtxUpdating(EnumSet.of(UpdateTarget.EXPIRY));
		// and:
		given(hfs.exists(nonSysFileTarget)).willReturn(true);
		given(hfs.getattr(nonSysFileTarget)).willReturn(deletedAttr);

		// when:
		subject.doStateTransition();

		// expect:
		verify(txnCtx).setStatus(FILE_DELETED);
	}

	@Test
	public void transitionRejectsMissingFid() {
		givenTxnCtxUpdating(EnumSet.of(UpdateTarget.EXPIRY));
		// and:
		given(hfs.exists(nonSysFileTarget)).willReturn(false);

		// when:
		subject.doStateTransition();

		// expect:
		verify(txnCtx).setStatus(INVALID_FILE_ID);
		verify(hfs).exists(nonSysFileTarget);
	}

	@Test
	public void transitionCatchesInvalidExpiryIfPresent() {
		givenTxnCtxUpdating(EnumSet.of(UpdateTarget.EXPIRY));
		// and:
		willThrow(new IllegalArgumentException(TieredgrameFs.IllegalArgumentType.FILE_WOULD_BE_EXPIRED.toString()))
				.given(hfs).setattr(argThat(nonSysFileTarget::equals), any());

		// when:
		subject.doStateTransition();

		// expect:
		verify(txnCtx).setStatus(INVALID_EXPIRATION_TIME);
	}

	@Test
	public void transitionCatchesBadlyEncodedKey() {
		givenTxnCtxUpdating(EnumSet.of(UpdateTarget.KEY));
		// and:
		willThrow(new IllegalArgumentException(new DecoderException()))
				.given(hfs).setattr(argThat(nonSysFileTarget::equals), any());

		// when:
		subject.doStateTransition();

		// expect:
		verify(txnCtx).setStatus(BAD_ENCODING);
	}

	@Test
	public void transitionCatchesOversizeIfThrown() {
		givenTxnCtxUpdating(EnumSet.of(UpdateTarget.CONTENTS));
		// and:
		willThrow(new IllegalArgumentException(TieredgrameFs.IllegalArgumentType.OVERSIZE_CONTENTS.toString()))
				.given(hfs).overwrite(argThat(nonSysFileTarget::equals), any());

		// when:
		subject.doStateTransition();

		// expect:
		verify(txnCtx).setStatus(MAX_FILE_SIZE_EXCEEDED);
	}

	@Test
	public void syntaxCheckTestsMemo() {
		givenTxnCtxUpdating(EnumSet.of(UpdateTarget.MEMO));
		given(validator.memoCheck(newMemo)).willReturn(INVALID_ZERO_BYTE_IN_STRING);

		// when:
		var syntaxCheck = subject.syntaxCheck();
		var status = syntaxCheck.apply(fileUpdateTxn);

		// expect:
		assertEquals(INVALID_ZERO_BYTE_IN_STRING, status);
	}

	@Test
	public void syntaxCheckTestsExpiryAsAutoRenewPeriod() {
		givenTxnCtxUpdating(EnumSet.of(UpdateTarget.EXPIRY));
		given(validator.isValidAutoRenewPeriod(expectedDuration)).willReturn(false);

		// when:
		var syntaxCheck = subject.syntaxCheck();
		var status = syntaxCheck.apply(fileUpdateTxn);

		// expect:
		assertEquals(AUTORENEW_DURATION_NOT_IN_RANGE, status);
		verify(validator).isValidAutoRenewPeriod(expectedDuration);
	}

	@Test
	public void hasCorrectApplicability() {
		givenTxnCtxUpdating(EnumSet.noneOf(UpdateTarget.class));

		// expect:
		assertTrue(subject.applicability().test(fileUpdateTxn));
		assertFalse(subject.applicability().test(TransactionBody.getDefaultInstance()));
	}

	private void givenTxnCtxUpdating(EnumSet<UpdateTarget> targets) {
		givenTxnCtxUpdating(targets, nonSysFileTarget);
	}

	private void givenTxnCtxUpdating(EnumSet<UpdateTarget> targets, FileID target) {
		FileUpdateTransactionBody.Builder op = FileUpdateTransactionBody.newBuilder();

		op.setFileID(target);
		if (targets.contains(UpdateTarget.KEY)) {
			op.setKeys(newWacl.getKeyList());
		}
		if (targets.contains(UpdateTarget.CONTENTS)) {
			op.setContents(ByteString.copyFrom(newContents));
		}
		if (targets.contains(UpdateTarget.EXPIRY)) {
			op.setExpirationTime(MiscUtils.asTimestamp(Instant.ofEpochSecond(newExpiry)));
		}
		if (targets.contains(UpdateTarget.MEMO)) {
			op.setMemo(StringValue.newBuilder().setValue(newMemo).build());
		}

		txnId = TransactionID.newBuilder()
				.setTransactionValidStart(MiscUtils.asTimestamp(Instant.ofEpochSecond(now)))
				.build();
		fileUpdateTxn = TransactionBody.newBuilder()
				.setTransactionID(txnId)
				.setTransactionValidDuration(Duration.newBuilder().setSeconds(txnValidDuration))
				.setFileUpdate(op)
				.build();
		given(accessor.getTxn()).willReturn(fileUpdateTxn);
		given(txnCtx.accessor()).willReturn(accessor);

	}
}
