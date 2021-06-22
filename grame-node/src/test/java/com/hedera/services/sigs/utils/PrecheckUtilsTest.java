package com.grame.services.sigs.utils;

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

import static com.grame.test.factories.txns.PlatformTxnFactory.from;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.grame.services.utils.PlatformTxnAccessor;
import com.grame.test.factories.txns.SignedTxnFactory;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.TransactionBody;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static com.grame.test.factories.txns.CryptoTransferFactory.*;
import static com.grame.test.factories.txns.CryptoUpdateFactory.*;
import static com.grame.services.sigs.utils.PrecheckUtils.*;
import static com.grame.test.factories.txns.TinyBarsFromTo.*;

public class PrecheckUtilsTest {
	final String nodeId = SignedTxnFactory.DEFAULT_NODE_ID;
	final AccountID node = SignedTxnFactory.DEFAULT_NODE;
	final Predicate<TransactionBody> subject = queryPaymentTestFor(node);

	@Test
	public void queryPaymentsMustBeCryptoTransfers() throws Throwable {
		// given:
		TransactionBody txn = new PlatformTxnAccessor(from(
			newSignedCryptoUpdate("0.0.2").get()
		)).getTxn();

		// expect:
		assertFalse(subject.test(txn));
	}

	@Test
	public void transferWithoutTargetNodeIsNotQueryPayment() throws Throwable {
		// given:
		TransactionBody txn = new PlatformTxnAccessor(from(
				newSignedCryptoTransfer().transfers(
					tinyBarsFromTo("0.0.1024", "0.0.2048", 1_000L)
				).get()
		)).getTxn();

		// expect:
		assertFalse(subject.test(txn));
	}

	@Test
	public void queryPaymentTransfersToTargetNode() throws Throwable {
		// given:
		TransactionBody txn = new PlatformTxnAccessor(from(
				newSignedCryptoTransfer().transfers(
						tinyBarsFromTo(nodeId, "0.0.2048", 1_000L)
				).get()
		)).getTxn();

		// expect:
		assertFalse(subject.test(txn));
	}
}
