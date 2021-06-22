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
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.Duration;
import com.gramegrame.api.proto.java.FileID;
import com.gramegrame.api.proto.java.FreezeTransactionBody;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.builder.RequestBuilder;
import com.grame.services.legacy.TestHelper;

public class FreezeTestHelper {

	private static AccountID payerAccountId;
	private static AccountID nodeAccountId =
			RequestBuilder.getAccountIdBuild(3l, 0l, 0l);

	public static Transaction createFreezeTransaction(boolean paidBy58, boolean valid, FileID fileID) {
		return createFreezeTransaction(paidBy58, valid, fileID, null);
	}

	public static Transaction createFreezeTransaction(boolean paidBy58, boolean valid, FileID fileID, byte[] fileHash) {
		FreezeTransactionBody freezeBody;
		if (valid) {
			int[] startHourMin = CommonUtilsTest.getUTCHourMinFromMillis(System.currentTimeMillis() + 60000);
			int[] endHourMin = CommonUtilsTest.getUTCHourMinFromMillis(System.currentTimeMillis() + 120000);
			var builder = getFreezeTranBuilder(startHourMin[0], startHourMin[1], endHourMin[0], endHourMin[1]);
			if (fileID != null) {
				builder.setUpdateFile(fileID);
				builder.setFileHash(ByteString.copyFrom(fileHash));
			}
			freezeBody =  builder.build();
		} else {
			freezeBody = getFreezeTranBuilder(25, 1, 3, 4).build();
		}


		Timestamp timestamp = TestHelper.getDefaultCurrentTimestampUTC();
		Duration transactionDuration = RequestBuilder.getDuration(30);

		long transactionFee = 100000000000l;
		String memo = "Freeze Test";

		if (paidBy58) {
			payerAccountId = RequestBuilder.getAccountIdBuild(58l, 0l, 0l);
		} else {
			payerAccountId = RequestBuilder.getAccountIdBuild(100l, 0l, 0l);
		}

		TransactionBody.Builder body = RequestBuilder.getTxBodyBuilder(transactionFee,
				timestamp, transactionDuration, true, memo,
				payerAccountId, nodeAccountId);
		body.setFreeze(freezeBody);
		byte[] bodyBytesArr = body.build().toByteArray();
		ByteString bodyBytes = ByteString.copyFrom(bodyBytesArr);
		return Transaction.newBuilder().setBodyBytes(bodyBytes).build();
	}

	private static FreezeTransactionBody.Builder getFreezeTranBuilder(int startHour, int startMin, int endHour, int endMin){
		return FreezeTransactionBody.newBuilder()
				.setStartHour(startHour).setStartMin(startMin)
				.setEndHour(endHour).setEndMin(endMin);
	}
}
