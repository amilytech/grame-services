package com.grame.services.fees.calculation.crypto.txns;

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

import com.grame.services.usage.SigUsage;
import com.grame.services.usage.crypto.CryptoOpsUsage;
import com.grame.services.usage.file.FileOpsUsage;
import com.grame.services.utils.SignedTxnAccessor;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.fee.CryptoFeeBuilder;
import com.gramegrame.fee.SigValueObj;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.*;
import static com.grame.test.factories.txns.CryptoCreateFactory.newSignedCryptoCreate;
import static com.grame.test.factories.txns.CryptoTransferFactory.newSignedCryptoTransfer;

class CryptoCreateResourceUsageTest {
	int numSigs = 10, sigsSize = 100, numPayerKeys = 3;
	SigValueObj svo = new SigValueObj(numSigs, numPayerKeys, sigsSize);
	private CryptoOpsUsage cryptoOpsUsage;
	private CryptoCreateResourceUsage subject;

	private TransactionBody nonCryptoCreateTxn;
	private TransactionBody cryptoCreateTxn;

	@BeforeEach
	private void setup() throws Throwable {
		cryptoCreateTxn = new SignedTxnAccessor(newSignedCryptoCreate().get()).getTxn();
		nonCryptoCreateTxn = new SignedTxnAccessor(newSignedCryptoTransfer().get()).getTxn();

		cryptoOpsUsage = mock(CryptoOpsUsage.class);

		subject = new CryptoCreateResourceUsage(cryptoOpsUsage);
	}

	@Test
	public void recognizesApplicability() {
		// expect:
		assertTrue(subject.applicableTo(cryptoCreateTxn));
		assertFalse(subject.applicableTo(nonCryptoCreateTxn));
	}

	@Test
	public void delegatesToCorrectEstimate() throws Exception {
		var sigUsage = new SigUsage(svo.getTotalSigCount(), svo.getSignatureSize(), svo.getPayerAcctSigCount());

		// when:
		subject.usageGiven(cryptoCreateTxn, svo, null);

		// then:
		verify(cryptoOpsUsage).cryptoCreateUsage(cryptoCreateTxn, sigUsage);
	}
}
