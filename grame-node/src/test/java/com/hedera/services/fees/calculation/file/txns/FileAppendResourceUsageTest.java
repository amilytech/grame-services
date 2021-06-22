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

import static org.junit.jupiter.api.Assertions.*;
import com.google.protobuf.ByteString;
import com.grame.services.context.primitives.StateView;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.FileAppendTransactionBody;
import com.gramegrame.api.proto.java.FileID;
import com.gramegrame.api.proto.java.Key;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.fee.FileFeeBuilder;
import com.gramegrame.fee.SigValueObj;
import com.grame.services.files.HFileMeta;
import com.grame.services.legacy.core.jproto.JKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.BDDMockito.*;

class FileAppendResourceUsageTest {
	private SigValueObj sigUsage;
	private FileFeeBuilder usageEstimator;
	private FileAppendResourceUsage subject;

	StateView view;
	FileID fid = IdUtils.asFile("1.2.3");

	private TransactionBody nonFileAppendTxn;
	private TransactionBody fileAppendTxn;

	@BeforeEach
	private void setup() throws Throwable {
		FileAppendTransactionBody append = mock(FileAppendTransactionBody.class);
		given(append.getFileID()).willReturn(fid);
		fileAppendTxn = mock(TransactionBody.class);
		given(fileAppendTxn.hasFileAppend()).willReturn(true);
		given(fileAppendTxn.getFileAppend()).willReturn(append);

		nonFileAppendTxn = mock(TransactionBody.class);
		given(nonFileAppendTxn.hasFileAppend()).willReturn(false);

		sigUsage = mock(SigValueObj.class);
		usageEstimator = mock(FileFeeBuilder.class);

		view = mock(StateView.class);

		subject = new FileAppendResourceUsage(usageEstimator);
	}

	@Test
	public void recognizesApplicability() {
		// expect:
		assertTrue(subject.applicableTo(fileAppendTxn));
		assertFalse(subject.applicableTo(nonFileAppendTxn));
	}

	@Test
	public void delegatesToCorrectEstimate() throws Exception {
		// setup:
		JKey wacl = JKey.mapKey(Key.newBuilder().setEd25519(ByteString.copyFrom("YUUP".getBytes())).build());
		HFileMeta jInfo = new HFileMeta(false, wacl, Long.MAX_VALUE);
		// and:
		Timestamp expiry = Timestamp.newBuilder().setSeconds(Long.MAX_VALUE).build();

		given(view.attrOf(fid)).willReturn(Optional.of(jInfo));

		// when:
		subject.usageGiven(fileAppendTxn, sigUsage, view);

		// then:
		verify(usageEstimator).getFileAppendTxFeeMatrices(fileAppendTxn, expiry, sigUsage);
	}
}
