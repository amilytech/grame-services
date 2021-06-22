package com.grame.services.legacy.unit;

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
import com.grame.services.legacy.handler.TransactionHandler;
import com.gramegrame.api.proto.java.Transaction;
import com.swirlds.common.Platform;
import com.swirlds.common.internal.SettingsCommon;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;

public class CheckTransactionSizeTest {
	@BeforeClass
	public static void setupAll() {
		SettingsCommon.transactionMaxBytes = 1_234;
	}

	@Test
	public void validateTxSize_negative_Test() {
		byte[] bytes = new byte[Platform.getTransactionMaxBytes()];
		Transaction transaction = Transaction.newBuilder().setBodyBytes(ByteString.copyFrom(bytes)).build();
		assert Platform.getTransactionMaxBytes() < transaction.toByteArray().length;
		Assert.assertFalse(TransactionHandler.validateTxSize(transaction));
	}

	@Test
//	@Ignore
	@Disabled
	public void validateTxSize_positive_Test() {
		byte[] bytes = new byte[Platform.getTransactionMaxBytes() - 30];
		Transaction transaction = Transaction.newBuilder().setBodyBytes(ByteString.copyFrom(bytes)).build();
		assert Platform.getTransactionMaxBytes() > transaction.toByteArray().length;
		Assert.assertTrue(TransactionHandler.validateTxSize(transaction));
	}
}
