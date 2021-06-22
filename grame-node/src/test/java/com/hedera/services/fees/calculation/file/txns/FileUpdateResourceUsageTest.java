package com.grame.services.fees.calculation.file.txns;

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
import com.grame.services.context.primitives.StateView;
import com.grame.services.usage.file.ExtantFileContext;
import com.grame.services.usage.file.FileOpsUsage;
import com.grame.test.factories.scenarios.TxnHandlingScenario;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.FileGetInfoResponse;
import com.gramegrame.api.proto.java.FileID;
import com.gramegrame.api.proto.java.FileUpdateTransactionBody;
import com.gramegrame.api.proto.java.KeyList;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionID;
import com.gramegrame.fee.SigValueObj;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static com.grame.services.state.merkle.MerkleAccountState.DEFAULT_MEMO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

class FileUpdateResourceUsageTest {
	long now = 1_000_000L;

	KeyList wacl = TxnHandlingScenario.COMPLEX_KEY_ACCOUNT_KT.asKey().getKeyList();
	String memo = "Certainly not!";
	long expiry = 1_234_567L;
	long size = 1L;

	long newExpiry = 2_345_678L;

	int numSigs = 10, sigsSize = 100, numPayerKeys = 3;
	SigValueObj svo = new SigValueObj(numSigs, numPayerKeys, sigsSize);
	private FileOpsUsage fileOpsUsage;

	private FileUpdateResourceUsage subject;

	StateView view;
	FileID fid = IdUtils.asFile("1.2.3");
	FeeData expected;

	private TransactionBody nonFileUpdateTxn;
	private TransactionBody fileUpdateTxn;

	@BeforeEach
	private void setup() throws Throwable {
		fileOpsUsage = mock(FileOpsUsage.class);

		view = mock(StateView.class);

		subject = new FileUpdateResourceUsage(fileOpsUsage);
	}

	@Test
	public void recognizesApplicability() {
		fileUpdateTxn = mock(TransactionBody.class);
		given(fileUpdateTxn.hasFileUpdate()).willReturn(true);

		nonFileUpdateTxn = mock(TransactionBody.class);
		given(nonFileUpdateTxn.hasFileUpdate()).willReturn(false);

		// expect:
		assertTrue(subject.applicableTo(fileUpdateTxn));
		assertFalse(subject.applicableTo(nonFileUpdateTxn));
	}

	@Test
	public void missingCtxScans() {
		// setup:
		long now = 1_234_567L;

		// given:
		var ctx = FileUpdateResourceUsage.missingCtx(now);

		// expect:
		assertEquals(0, ctx.currentSize());
		assertEquals(now, ctx.currentExpiry());
		Assertions.assertSame(KeyList.getDefaultInstance(), ctx.currentWacl());
		Assertions.assertSame(DEFAULT_MEMO, ctx.currentMemo());
	}

	@Test
	public void delegatesToCorrectEstimateWhenUnknown() throws Exception {
		// setup:
		expected = mock(FeeData.class);
		// and:
		ArgumentCaptor<ExtantFileContext> captor = ArgumentCaptor.forClass(ExtantFileContext.class);

		given(fileOpsUsage.fileUpdateUsage(any(), any(), captor.capture())).willReturn(expected);
		given(view.infoForFile(fid)).willReturn(Optional.empty());

		// when:
		fileUpdateTxn = txnAt(now);
		var actual = subject.usageGiven(fileUpdateTxn, svo, view);

		// then:
		assertSame(expected, actual);
		// and:
		var ctxUsed = captor.getValue();
		assertEquals(now, ctxUsed.currentExpiry());
	}

	@Test
	public void delegatesToCorrectEstimateWhenKnown() throws Exception {
		// setup:
		expected = mock(FeeData.class);
		// and:
		var info = FileGetInfoResponse.FileInfo.newBuilder()
				.setExpirationTime(Timestamp.newBuilder().setSeconds(expiry))
				.setMemo(memo)
				.setKeys(wacl)
				.setSize(size)
				.build();
		// and:
		ArgumentCaptor<ExtantFileContext> captor = ArgumentCaptor.forClass(ExtantFileContext.class);

		given(fileOpsUsage.fileUpdateUsage(any(), any(), captor.capture())).willReturn(expected);
		given(view.infoForFile(fid)).willReturn(Optional.of(info));

		// when:
		fileUpdateTxn = txnAt(now);
		var actual = subject.usageGiven(fileUpdateTxn, svo, view);

		// then:
		assertSame(expected, actual);
		// and:
		var ctxUsed = captor.getValue();
		assertEquals(expiry, ctxUsed.currentExpiry());
		assertEquals(memo, ctxUsed.currentMemo());
		assertEquals(wacl, ctxUsed.currentWacl());
		assertEquals(size, ctxUsed.currentSize());
	}

	private TransactionBody txnAt(long now) {
		var op = FileUpdateTransactionBody.newBuilder()
				.setFileID(fid)
				.setContents(ByteString.copyFrom("Though like waves breaking it may be".getBytes()))
				.setKeys(KeyList.newBuilder()
						.addKeys(TxnHandlingScenario.COMPLEX_KEY_ACCOUNT_KT.asKey())
						.addKeys(TxnHandlingScenario.MISC_ACCOUNT_KT.asKey())
						.build())
				.setExpirationTime(Timestamp.newBuilder().setSeconds(newExpiry))
				.build();
		// and:
		return TransactionBody.newBuilder()
				.setTransactionID(TransactionID.newBuilder()
						.setTransactionValidStart(Timestamp.newBuilder().setSeconds(now)))
				.setFileUpdate(op)
				.build();
	}
}
