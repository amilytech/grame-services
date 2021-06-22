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

import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.fee.FileFeeBuilder;
import com.gramegrame.fee.SigValueObj;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.*;

class FileDeleteResourceUsageTest {
	private SigValueObj sigUsage;
	private FileFeeBuilder usageEstimator;
	private FileDeleteResourceUsage subject;

	private TransactionBody nonFileDeleteTxn;
	private TransactionBody fileDeleteTxn;

	@BeforeEach
	private void setup() throws Throwable {
		fileDeleteTxn = mock(TransactionBody.class);
		given(fileDeleteTxn.hasFileDelete()).willReturn(true);

		nonFileDeleteTxn = mock(TransactionBody.class);
		given(nonFileDeleteTxn.hasFileDelete()).willReturn(false);

		sigUsage = mock(SigValueObj.class);
		usageEstimator = mock(FileFeeBuilder.class);

		subject = new FileDeleteResourceUsage(usageEstimator);
	}

	@Test
	public void recognizesApplicability() {
		// expect:
		assertTrue(subject.applicableTo(fileDeleteTxn));
		assertFalse(subject.applicableTo(nonFileDeleteTxn));
	}

	@Test
	public void delegatesToCorrectEstimate() throws Exception {
		// when:
		subject.usageGiven(fileDeleteTxn, sigUsage, null);

		// then:
		verify(usageEstimator).getFileDeleteTxFeeMatrices(fileDeleteTxn, sigUsage);
	}
}
