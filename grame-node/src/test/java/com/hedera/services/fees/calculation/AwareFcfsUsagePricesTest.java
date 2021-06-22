package com.grame.services.fees.calculation;

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

import static com.grame.services.fees.calculation.AwareFcfsUsagePrices.DEFAULT_USAGE_PRICES;
import static com.gramegrame.api.proto.java.grameFunctionality.UNRECOGNIZED;
import static org.junit.jupiter.api.Assertions.*;

import com.google.common.io.Files;
import com.grame.services.context.TransactionContext;
import com.grame.services.fees.bootstrap.JsonToProtoSerdeTest;
import com.grame.services.files.grameFs;
import com.grame.services.files.interceptors.MockFileNumbers;
import com.grame.services.utils.PlatformTxnAccessor;
import com.grame.test.mocks.MockAppender;
import com.grame.test.utils.IdUtils;
import com.grame.test.utils.TxnUtils;
import com.gramegrame.api.proto.java.CryptoTransferTransactionBody;
import com.gramegrame.api.proto.java.CurrentAndNextFeeSchedule;
import com.gramegrame.api.proto.java.FeeComponents;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.FeeSchedule;
import com.gramegrame.api.proto.java.FileID;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.TimestampSeconds;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionFeeSchedule;
import com.gramegrame.api.proto.java.TransactionID;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.mockito.BDDMockito.*;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoTransfer;

class AwareFcfsUsagePricesTest {
	FileID schedules = IdUtils.asFile("0.0.111");
	long currentExpiry = 1_234_567;
	long nextExpiry = currentExpiry + 1_000;
	FeeComponents currResourceUsagePrices = FeeComponents.newBuilder()
			.setMin(currentExpiry)
			.setMax(currentExpiry)
			.setBpr(1_000_000L)
			.setBpt(2_000_000L)
			.setRbh(3_000_000L)
			.setSbh(4_000_000L)
			.build();
	FeeComponents nextResourceUsagePrices = FeeComponents.newBuilder()
			.setMin(nextExpiry)
			.setMax(nextExpiry)
			.setBpr(1_000_000L)
			.setBpt(2_000_000L)
			.setRbh(3_000_000L)
			.setSbh(4_000_000L)
			.build();
	FeeData currUsagePrices = FeeData.newBuilder()
			.setNetworkdata(currResourceUsagePrices)
			.setNodedata(currResourceUsagePrices)
			.setServicedata(currResourceUsagePrices)
			.build();
	FeeData nextUsagePrices = FeeData.newBuilder()
			.setNetworkdata(nextResourceUsagePrices)
			.setNodedata(nextResourceUsagePrices)
			.setServicedata(nextResourceUsagePrices)
			.build();

	FeeData nextCryptoTransferUsagePrices = nextUsagePrices;
	FeeData currentCryptoTransferUsagePrices = currUsagePrices;
	FeeSchedule nextFeeSchedule, currentFeeSchedule;
	CurrentAndNextFeeSchedule feeSchedules;

	AwareFcfsUsagePrices subject;

	TransactionBody cryptoTransferTxn = TransactionBody.newBuilder()
			.setTransactionID(TransactionID.newBuilder()
					.setTransactionValidStart(Timestamp.newBuilder().setSeconds(nextExpiry - 1)))
			.setCryptoTransfer(CryptoTransferTransactionBody.newBuilder()
					.setTransfers(TxnUtils.withAdjustments(
							IdUtils.asAccount("1.2.3"), 1,
							IdUtils.asAccount("2.2.3"), 1,
							IdUtils.asAccount("3.2.3"), -2))
			).build();

	grameFs hfs;
	TransactionContext txnCtx;
	PlatformTxnAccessor accessor;

	@BeforeEach
	private void setup() {
		nextFeeSchedule = FeeSchedule.newBuilder()
				.setExpiryTime(TimestampSeconds.newBuilder().setSeconds(nextExpiry))
				.addTransactionFeeSchedule(TransactionFeeSchedule.newBuilder()
						.setgrameFunctionality(CryptoTransfer)
						.setFeeData(nextCryptoTransferUsagePrices))
				.build();
		currentFeeSchedule = FeeSchedule.newBuilder()
				.setExpiryTime(TimestampSeconds.newBuilder().setSeconds(currentExpiry))
				.addTransactionFeeSchedule(TransactionFeeSchedule.newBuilder()
						.setgrameFunctionality(CryptoTransfer)
						.setFeeData(currentCryptoTransferUsagePrices))
				.build();
		feeSchedules = CurrentAndNextFeeSchedule.newBuilder()
				.setCurrentFeeSchedule(currentFeeSchedule)
				.setNextFeeSchedule(nextFeeSchedule)
				.build();

		hfs = mock(grameFs.class);
		given(hfs.exists(schedules)).willReturn(true);
		given(hfs.cat(schedules)).willReturn(feeSchedules.toByteArray());

		accessor = mock(PlatformTxnAccessor.class);
		given(accessor.getTxn()).willReturn(cryptoTransferTxn);
		given(accessor.getTxnId()).willReturn(cryptoTransferTxn.getTransactionID());
		given(accessor.getFunction()).willReturn(CryptoTransfer);
		txnCtx = mock(TransactionContext.class);
		given(txnCtx.accessor()).willReturn(accessor);

		subject = new AwareFcfsUsagePrices(hfs, new MockFileNumbers(), txnCtx);
	}

	@Test
	public void getsActivePrices() throws Exception {
		// given:
		subject.loadPriceSchedules();

		// when:
		FeeData actual = subject.activePrices();

		// then:
		assertEquals(nextUsagePrices, actual);
	}

	@Test
	public void getsDefaultPricesIfActiveTxnInvalid() throws Exception {
		// given:
		subject.loadPriceSchedules();
		// and:
		given(accessor.getTxn()).willReturn(TransactionBody.getDefaultInstance());
		given(accessor.getFunction()).willReturn(UNRECOGNIZED);

		// when:
		FeeData actual = subject.activePrices();

		// then:
		assertEquals(DEFAULT_USAGE_PRICES, actual);
	}


	@Test
	public void getsTransferUsagePricesAtCurrent() throws Exception {
		// given:
		subject.loadPriceSchedules();
		Timestamp at = Timestamp.newBuilder()
				.setSeconds(currentExpiry - 1)
				.build();

		// when:
		FeeData actual = subject.pricesGiven(CryptoTransfer, at);

		// then:
		assertEquals(currentCryptoTransferUsagePrices, actual);
	}

	@Test
	public void returnsDefaultUsagePricesForUnsupported() throws Exception {
		// setup:
		MockAppender mockAppender = new MockAppender();
		var log = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(AwareFcfsUsagePrices.class);
		log.addAppender(mockAppender);
		Level levelForReset = log.getLevel();
		log.setLevel(Level.DEBUG);

		// given:
		subject.loadPriceSchedules();
		Timestamp at = Timestamp.newBuilder()
				.setSeconds(currentExpiry - 1)
				.build();

		// when:
		FeeData actual = subject.pricesGiven(UNRECOGNIZED, at);

		// then:
		assertEquals(DEFAULT_USAGE_PRICES, actual);
		assertEquals(1, mockAppender.size());
		assertEquals("DEBUG - Default usage price will be used, no specific usage prices available for function UNRECOGNIZED @ 1970-01-15T06:56:06Z!",
				mockAppender.get(0));

		// tearDown:
		log.setLevel(levelForReset);
		log.removeAppender(mockAppender);
		mockAppender.clear();
	}

	@Test
	public void getsTransferUsagePricesPastCurrentBeforeNextExpiry() throws Exception {
		// given:
		subject.loadPriceSchedules();
		Timestamp at = Timestamp.newBuilder()
				.setSeconds(nextExpiry - 1)
				.build();

		// when:
		FeeData actual = subject.pricesGiven(CryptoTransfer, at);

		// then:
		assertEquals(nextCryptoTransferUsagePrices, actual);
	}

	@Test
	public void loadsGoodScheduleUneventfully() throws Exception {
		// setup:
		byte[] bytes = Files.toByteArray(new File(JsonToProtoSerdeTest.R4_FEE_SCHEDULE_REPR_PATH));
		CurrentAndNextFeeSchedule expectedFeeSchedules = CurrentAndNextFeeSchedule.parseFrom(bytes);

		given(hfs.exists(schedules)).willReturn(true);
		given(hfs.cat(schedules)).willReturn(bytes);

		// when:
		subject.loadPriceSchedules();

		// then:
		assertEquals(expectedFeeSchedules, subject.feeSchedules);
	}

	@Test
	public void throwsNfseOnMissingScheduleInFcfs() {
		given(hfs.exists(schedules)).willReturn(false);

		// expect:
		assertThrows(IllegalStateException.class, () -> subject.loadPriceSchedules());
	}

	@Test
	public void throwsNfseOnBadScheduleInFcfs() {
		given(hfs.exists(schedules)).willReturn(true);
		given(hfs.cat(any())).willReturn("NONSENSE".getBytes());

		// expect:
		assertThrows(IllegalStateException.class, () -> subject.loadPriceSchedules());
	}

	@Test
	public void usesDefaultPricesForUnexpectedFailure() {
		given(accessor.getFunction()).willThrow(IllegalStateException.class);

		// when:
		var prices = subject.activePrices();

		// then:
		assertEquals(DEFAULT_USAGE_PRICES, prices);
	}
}
