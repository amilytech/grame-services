package com.grame.test.mocks;

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

import com.google.common.io.Files;
import com.grame.services.fees.bootstrap.JsonToProtoSerdeTest;
import com.grame.services.fees.calculation.UsagePricesProvider;
import com.gramegrame.api.proto.java.CurrentAndNextFeeSchedule;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.FeeSchedule;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.TimestampSeconds;
import com.gramegrame.api.proto.java.TransactionFeeSchedule;
import java.io.File;
import java.util.Map;
import java.util.Objects;
import static java.util.stream.Collectors.toMap;
import static com.grame.services.fees.calculation.AwareFcfsUsagePrices.*;

public enum TestUsagePricesProvider implements UsagePricesProvider {
	TEST_USAGE_PRICES;

	CurrentAndNextFeeSchedule feeSchedules;

	Timestamp currFunctionUsagePricesExpiry;
	Timestamp nextFunctionUsagePricesExpiry;

	Map<grameFunctionality, FeeData> currFunctionUsagePrices;
	Map<grameFunctionality, FeeData> nextFunctionUsagePrices;

	TestUsagePricesProvider() {
		loadPriceSchedules();
	}

	@Override
	public void loadPriceSchedules() {
		try {
			byte[] bytes = Files.toByteArray(new File(JsonToProtoSerdeTest.R4_FEE_SCHEDULE_REPR_PATH));
			setFeeSchedules(CurrentAndNextFeeSchedule.parseFrom(bytes));
		} catch (Exception impossible) {
			throw new IllegalStateException(impossible);
		}
	}

	@Override
	public FeeData activePrices() {
		throw new UnsupportedOperationException();
	}

	@Override
	public FeeData pricesGiven(grameFunctionality function, Timestamp at) {
		try {
			Map<grameFunctionality, FeeData> functionUsagePrices = applicableUsagePrices(at);
			FeeData usagePrices = functionUsagePrices.get(function);
			Objects.requireNonNull(usagePrices);
			return usagePrices;
		} catch (Exception e) {
		}
		return DEFAULT_USAGE_PRICES;
	}

	private Map<grameFunctionality, FeeData> applicableUsagePrices(Timestamp at) {
		if (onlyNextScheduleApplies(at)) {
			return nextFunctionUsagePrices;
		} else {
			return currFunctionUsagePrices;
		}
	}

	private boolean onlyNextScheduleApplies(Timestamp at) {
		return at.getSeconds() >= currFunctionUsagePricesExpiry.getSeconds() &&
				at.getSeconds() < nextFunctionUsagePricesExpiry.getSeconds();
	}

	public void setFeeSchedules(CurrentAndNextFeeSchedule feeSchedules) {
		this.feeSchedules = feeSchedules;

		currFunctionUsagePrices = functionUsagePricesFrom(feeSchedules.getCurrentFeeSchedule());
		currFunctionUsagePricesExpiry = asTimestamp(feeSchedules.getCurrentFeeSchedule().getExpiryTime());

		nextFunctionUsagePrices = functionUsagePricesFrom(feeSchedules.getNextFeeSchedule());
		nextFunctionUsagePricesExpiry = asTimestamp(feeSchedules.getNextFeeSchedule().getExpiryTime());
	}

	private Timestamp asTimestamp(TimestampSeconds ts) {
		return Timestamp.newBuilder().setSeconds(ts.getSeconds()).build();
	}

	private Map<grameFunctionality, FeeData> functionUsagePricesFrom(FeeSchedule feeSchedule) {
		return feeSchedule.getTransactionFeeScheduleList()
				.stream()
				.collect(toMap(TransactionFeeSchedule::getgrameFunctionality, TransactionFeeSchedule::getFeeData));
	}
}
