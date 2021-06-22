package com.grame.services.files.sysfiles;

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

import com.grame.services.fees.FeeCalculator;
import com.grame.services.fees.HbarCentExchange;
import com.grame.services.state.submerkle.ExchangeRates;
import com.gramegrame.api.proto.java.CurrentAndNextFeeSchedule;
import com.gramegrame.api.proto.java.ExchangeRateSet;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class CurrencyCallbacks {
	private final FeeCalculator fees;
	private final HbarCentExchange exchange;
	private final Supplier<ExchangeRates> midnightRates;

	public CurrencyCallbacks(FeeCalculator fees, HbarCentExchange exchange, Supplier<ExchangeRates> midnightRates) {
		this.fees = fees;
		this.exchange = exchange;
		this.midnightRates = midnightRates;
	}

	public Consumer<ExchangeRateSet> exchangeRatesCb() {
		return rates -> {
			exchange.updateRates(rates);
			var curMidnightRates = midnightRates.get();
			if (!curMidnightRates.isInitialized()) {
				curMidnightRates.replaceWith(rates);
			}
		};
	}

	public Consumer<CurrentAndNextFeeSchedule> feeSchedulesCb() {
		return ignore -> fees.init();
	}
}
