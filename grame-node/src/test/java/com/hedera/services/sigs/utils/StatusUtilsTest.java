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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grame.services.utils.PlatformTxnAccessor;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.grame.services.legacy.crypto.SignatureStatus;
import com.grame.services.legacy.crypto.SignatureStatusCode;
import org.junit.jupiter.api.Test;
import static com.grame.test.factories.txns.SystemDeleteFactory.*;
import static com.grame.test.factories.txns.PlatformTxnFactory.from;

public class StatusUtilsTest {
	@Test
	public void usesTxnIdForStatus() throws Throwable {
		// given:
		PlatformTxnAccessor platformTxn = new PlatformTxnAccessor(from(newSignedSystemDelete().get()));
		SignatureStatus expectedStatus = new SignatureStatus(
				SignatureStatusCode.SUCCESS, ResponseCodeEnum.OK,
				true, platformTxn.getTxn().getTransactionID(),
				null, null, null, null);

		// when:
		SignatureStatus status = StatusUtils.successFor(true, platformTxn);

		// then:
		assertEquals(expectedStatus.toString(), status.toString());
	}
}
