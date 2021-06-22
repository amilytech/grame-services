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

import com.grame.services.context.TransactionContext;
import com.grame.services.files.grameFs;
import com.grame.services.files.TieredgrameFs;
import com.grame.services.state.submerkle.EntityId;
import com.grame.services.utils.MiscUtils;
import com.grame.services.utils.PlatformTxnAccessor;
import com.grame.test.factories.scenarios.TxnHandlingScenario;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.Duration;
import com.gramegrame.api.proto.java.FileID;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.SystemUndeleteTransactionBody;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionID;
import com.grame.services.files.HFileMeta;
import com.grame.services.legacy.core.jproto.JKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.Map;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_FILE_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.SUCCESS;
import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.BDDMockito.*;

class FileSysUndelTransitionLogicTest {
	enum TargetType { VALID, MISSING, DELETED }
	enum OldExpiryType { NONE, FUTURE, PAST }

	long now = Instant.now().getEpochSecond();
	long lifetime = 1_000_000L;
	long currExpiry = now + lifetime / 2;
	long oldPastExpiry = now - 1;
	long oldFutureExpiry = now + lifetime;

	FileID undeleted = IdUtils.asFile("0.0.13257");
	FileID missing = IdUtils.asFile("0.0.75231");
	FileID deleted = IdUtils.asFile("0.0.666");

	grameFs.UpdateResult success = new TieredgrameFs.SimpleUpdateResult(
			true,
			false,
			SUCCESS);

	JKey wacl;
	HFileMeta attr, deletedAttr;

	TransactionID txnId;
	TransactionBody fileSysUndelTxn;
	PlatformTxnAccessor accessor;

	grameFs hfs;
	Map<EntityId, Long> oldExpiries;
	TransactionContext txnCtx;

	FileSysUndelTransitionLogic subject;

	@BeforeEach
	private void setup() throws Throwable {
		wacl = TxnHandlingScenario.SIMPLE_NEW_WACL_KT.asJKey();
		attr = new HFileMeta(false, wacl, currExpiry);
		deletedAttr = new HFileMeta(true, wacl, currExpiry);

		accessor = mock(PlatformTxnAccessor.class);
		txnCtx = mock(TransactionContext.class);
		oldExpiries = mock(Map.class);

		hfs = mock(grameFs.class);
		given(hfs.exists(undeleted)).willReturn(true);
		given(hfs.exists(deleted)).willReturn(true);
		given(hfs.exists(missing)).willReturn(false);
		given(hfs.getattr(undeleted)).willReturn(attr);
		given(hfs.getattr(deleted)).willReturn(deletedAttr);

		subject = new FileSysUndelTransitionLogic(hfs, oldExpiries, txnCtx);
	}

	@Test
	public void happyPathFlows() {
		// setup:
		InOrder inOrder = inOrder(hfs, txnCtx, oldExpiries);

		givenTxnCtxSysUndeleting(TargetType.DELETED, OldExpiryType.FUTURE);
		// and:
		given(hfs.sudoSetattr(any(), any())).willReturn(success);

		// when:
		subject.doStateTransition();

		// then:
		assertFalse(deletedAttr.isDeleted());
		assertEquals(oldFutureExpiry, deletedAttr.getExpiry());
		inOrder.verify(hfs).sudoSetattr(deleted, deletedAttr);
		inOrder.verify(oldExpiries).remove(EntityId.ofNullableFileId(deleted));
		inOrder.verify(txnCtx).setStatus(SUCCESS);
	}

	@Test
	public void destroysIfOldExpiryIsPast() {
		givenTxnCtxSysUndeleting(TargetType.DELETED, OldExpiryType.PAST);

		// when:
		subject.doStateTransition();

		// then:
		verify(hfs).rm(deleted);
		verify(oldExpiries).remove(EntityId.ofNullableFileId(deleted));
		verify(hfs, never()).sudoSetattr(any(), any());
	}

	@Test
	public void detectsUndeleted() {
		givenTxnCtxSysUndeleting(TargetType.VALID, OldExpiryType.FUTURE);

		// when:
		subject.doStateTransition();

		// then:
		verify(txnCtx).setStatus(INVALID_FILE_ID);
	}

	@Test
	public void detectsMissing() {
		givenTxnCtxSysUndeleting(TargetType.MISSING, OldExpiryType.FUTURE);

		// when:
		subject.doStateTransition();

		// then:
		verify(txnCtx).setStatus(INVALID_FILE_ID);
	}

	@Test
	public void detectsUserDeleted() {
		givenTxnCtxSysUndeleting(TargetType.DELETED, OldExpiryType.NONE);

		// when:
		subject.doStateTransition();

		// then:
		verify(txnCtx).setStatus(INVALID_FILE_ID);
	}

	@Test
	public void hasCorrectApplicability() {
		// setup:
		SystemUndeleteTransactionBody.Builder op = SystemUndeleteTransactionBody.newBuilder()
				.setContractID(IdUtils.asContract("0.0.1001"));
		var contractSysUndelTxn = TransactionBody.newBuilder()
				.setSystemUndelete(op)
				.build();

		givenTxnCtxSysUndeleting(TargetType.VALID, OldExpiryType.FUTURE);

		// expect:
		assertTrue(subject.applicability().test(fileSysUndelTxn));
		assertFalse(subject.applicability().test(TransactionBody.getDefaultInstance()));
		assertFalse(subject.applicability().test(contractSysUndelTxn));
	}

	@Test
	public void syntaxCheckRubberstamps() {
		// given:
		var syntaxCheck = subject.syntaxCheck();

		// expect:
		assertEquals(ResponseCodeEnum.OK, syntaxCheck.apply(TransactionBody.getDefaultInstance()));
	}

	private void givenTxnCtxSysUndeleting(TargetType type, OldExpiryType expiryType) {
		SystemUndeleteTransactionBody.Builder op = SystemUndeleteTransactionBody.newBuilder();

		FileID id = null;
		switch (type) {
			case VALID:
				op.setFileID(undeleted);
				id = undeleted;
				break;
			case MISSING:
				op.setFileID(missing);
				id = missing;
				break;
			case DELETED:
				op.setFileID(deleted);
				id = deleted;
				break;
		}
		EntityId entity = EntityId.ofNullableFileId(id);

		switch (expiryType) {
			case NONE:
				given(oldExpiries.containsKey(entity)).willReturn(false);
				given(oldExpiries.get(entity)).willReturn(null);
				break;
			case FUTURE:
				given(oldExpiries.containsKey(entity)).willReturn(true);
				given(oldExpiries.get(entity)).willReturn(oldFutureExpiry);
				break;
			case PAST:
				given(oldExpiries.containsKey(entity)).willReturn(true);
				given(oldExpiries.get(entity)).willReturn(oldPastExpiry);
				break;
		}

		txnId = TransactionID.newBuilder()
				.setTransactionValidStart(MiscUtils.asTimestamp(Instant.ofEpochSecond(Instant.now().getEpochSecond())))
				.build();
		fileSysUndelTxn = TransactionBody.newBuilder()
				.setTransactionID(txnId)
				.setTransactionValidDuration(Duration.newBuilder().setSeconds(180))
				.setSystemUndelete(op)
				.build();
		given(accessor.getTxn()).willReturn(fileSysUndelTxn);
		given(txnCtx.accessor()).willReturn(accessor);
		given(txnCtx.consensusTime()).willReturn(Instant.ofEpochSecond(now));
	}
}
