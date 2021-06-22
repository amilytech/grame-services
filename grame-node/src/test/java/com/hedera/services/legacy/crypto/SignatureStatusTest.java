package com.grame.services.legacy.crypto;

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

import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.ScheduleID;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionID;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(JUnitPlatform.class)
class SignatureStatusTest {
	@Test
	public void formatsUnresolvableSigners() {
		// given:
		String expMsg = "Cannot resolve required signers for scheduled txn " +
				"[ source = 'handleTransaction', scheduled = '', error = 'INVALID_SCHEDULE_ID' ]";
		// and:
		var errorReport = new SignatureStatus(
				SignatureStatusCode.INVALID_SCHEDULE_ID,
				ResponseCodeEnum.INVALID_SCHEDULE_ID,
				true,
				TransactionID.getDefaultInstance(),
				ScheduleID.getDefaultInstance());

		// when:
		var subject = new SignatureStatus(
				SignatureStatusCode.UNRESOLVABLE_REQUIRED_SIGNERS,
				ResponseCodeEnum.UNRESOLVABLE_REQUIRED_SIGNERS,
				true,
				TransactionID.getDefaultInstance(),
				TransactionBody.getDefaultInstance(),
				errorReport);

		// expect:
		assertEquals(expMsg, subject.toLogMessage());
	}

	@Test
	public void formatsNestedScheduleCreate() {
		// setup:
		var txnId = TransactionID.newBuilder().setAccountID(IdUtils.asAccount("0.0.75231")).build();
		// given:
		String expMsg = String.format("Specified txn cannot be scheduled " +
				"[ source = 'handleTransaction', transactionId = '%s' ]", SignatureStatus.format(txnId));
		// and:
		var subject = new SignatureStatus(
				SignatureStatusCode.SCHEDULED_TRANSACTION_NOT_IN_WHITELIST,
				ResponseCodeEnum.SCHEDULED_TRANSACTION_NOT_IN_WHITELIST,
				true,
				txnId);

		// expect:
		assertEquals(expMsg, subject.toLogMessage());
	}
}
