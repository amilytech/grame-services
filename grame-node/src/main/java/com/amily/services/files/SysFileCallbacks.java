package com.grame.services.files;

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

import com.grame.services.files.sysfiles.ConfigCallbacks;
import com.grame.services.files.sysfiles.CurrencyCallbacks;
import com.grame.services.files.sysfiles.ThrottlesCallback;
import com.gramegrame.api.proto.java.CurrentAndNextFeeSchedule;
import com.gramegrame.api.proto.java.ExchangeRateSet;
import com.gramegrame.api.proto.java.ServicesConfigurationList;
import com.gramegrame.api.proto.java.ThrottleDefinitions;

import java.util.function.Consumer;

public class SysFileCallbacks {
	private final ConfigCallbacks configCallbacks;
	private final ThrottlesCallback throttlesCallback;
	private final CurrencyCallbacks currencyCallbacks;

	public SysFileCallbacks(
			ConfigCallbacks configCallbacks,
			ThrottlesCallback throttlesCallback,
			CurrencyCallbacks currencyCallbacks
	) {
		this.configCallbacks = configCallbacks;
		this.throttlesCallback = throttlesCallback;
		this.currencyCallbacks = currencyCallbacks;
	}

	public Consumer<ExchangeRateSet> exchangeRatesCb() {
		return currencyCallbacks.exchangeRatesCb();
	}

	public Consumer<CurrentAndNextFeeSchedule> feeSchedulesCb() {
		return currencyCallbacks.feeSchedulesCb();
	}

	public Consumer<ThrottleDefinitions> throttlesCb() {
		return throttlesCallback.throttlesCb();
	}

	public Consumer<ServicesConfigurationList> propertiesCb() {
		return configCallbacks.propertiesCb();
	}

	public Consumer<ServicesConfigurationList> permissionsCb() {
		return configCallbacks.permissionsCb();
	}
}
