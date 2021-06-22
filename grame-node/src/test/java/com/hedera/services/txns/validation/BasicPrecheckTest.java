package com.grame.services.txns.validation;

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

import com.grame.services.context.properties.GlobalDynamicProperties;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.Duration;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.grame.services.txns.validation.PureValidationTest.from;
import static com.grame.test.utils.IdUtils.asAccount;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INSUFFICIENT_TX_FEE;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_NODE_ACCOUNT;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TRANSACTION_DURATION;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TRANSACTION_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TRANSACTION_START;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_ZERO_BYTE_IN_STRING;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.PAYER_ACCOUNT_NOT_FOUND;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.TRANSACTION_ID_FIELD_NOT_ALLOWED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyLong;
import static org.mockito.BDDMockito.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.longThat;
import static org.mockito.BDDMockito.mock;

class BasicPrecheckTest {
	int validityBufferOverride = 7;
	AccountID node = asAccount("0.0.3");
	AccountID payer = asAccount("0.0.13257");
	long duration = 1_234;
	Instant startTime = Instant.now();
	TransactionID txnId = TransactionID.newBuilder()
			.setAccountID(payer)
			.setTransactionValidStart(from(startTime.getEpochSecond(), startTime.getNano()))
			.build();
	String memo = "Our souls, which to advance their state / Were gone out, hung twixt her and me.";
	TransactionBody txn;

	OptionValidator validator;
	GlobalDynamicProperties dynamicProperties;

	BasicPrecheck subject;

	@BeforeEach
	private void setup() {
		validator = mock(OptionValidator.class);
		dynamicProperties = mock(GlobalDynamicProperties.class);

		given(validator.isValidTxnDuration(anyLong())).willReturn(true);
		given(validator.isPlausibleTxnFee(anyLong())).willReturn(true);
		given(validator.isPlausibleAccount(node)).willReturn(true);
		given(validator.isPlausibleAccount(payer)).willReturn(true);
		given(validator.memoCheck(memo)).willReturn(OK);
		given(validator.chronologyStatusForTxn(any(), anyLong(), any())).willReturn(OK);
		given(dynamicProperties.minValidityBuffer()).willReturn(validityBufferOverride);

		subject = new BasicPrecheck(validator, dynamicProperties);

		txn = TransactionBody.newBuilder()
				.setTransactionID(txnId)
				.setTransactionValidDuration(Duration.newBuilder().setSeconds(duration))
				.setNodeAccountID(node)
				.setMemo(memo)
				.build();
	}

	@Test
	void rejectsUseOfScheduledField() {
		// setup:
		txn = txn.toBuilder()
				.setTransactionID(TransactionID.newBuilder()
						.setScheduled(true).build())
				.build();

		// when:
		var status = subject.validate(txn);

		// then:
		assertEquals(TRANSACTION_ID_FIELD_NOT_ALLOWED, status);
	}

	@Test
	public void assertsValidDuration() {
		given(validator.isValidTxnDuration(anyLong())).willReturn(false);

		// when:
		var status = subject.validate(txn);

		// then:
		assertEquals(INVALID_TRANSACTION_DURATION, status);
	}

	@Test
	public void assertsValidChronology() {
		given(validator.chronologyStatusForTxn(
				argThat(startTime::equals),
				longThat(l -> l == (duration - validityBufferOverride)),
				any())).willReturn(INVALID_TRANSACTION_START);

		// when:
		var status = subject.validate(txn);

		// then:
		assertEquals(INVALID_TRANSACTION_START, status);
	}

	@Test
	public void assertsExtantTransactionId() {
		// when:
		var status = subject.validate(TransactionBody.getDefaultInstance());

		// then:
		assertEquals(INVALID_TRANSACTION_ID, status);
	}

	@Test
	public void assertsPlausibleTxnFee() {
		given(validator.isPlausibleTxnFee(anyLong())).willReturn(false);

		// when:
		var status = subject.validate(txn);

		// then:
		assertEquals(INSUFFICIENT_TX_FEE, status);
	}

	@Test
	public void assertsPlausiblePayer() {
		given(validator.isPlausibleAccount(payer)).willReturn(false);

		// when:
		var status = subject.validate(txn);

		// then:
		assertEquals(PAYER_ACCOUNT_NOT_FOUND, status);
	}

	@Test
	public void assertsPlausibleNode() {
		given(validator.isPlausibleAccount(node)).willReturn(false);

		// when:
		var status = subject.validate(txn);

		// then:
		assertEquals(INVALID_NODE_ACCOUNT, status);
	}

	@Test
	public void assertsValidMemo() {
		given(validator.memoCheck(memo)).willReturn(INVALID_ZERO_BYTE_IN_STRING);

		// when:
		var status = subject.validate(txn);

		// then:
		assertEquals(INVALID_ZERO_BYTE_IN_STRING, status);
	}
}
