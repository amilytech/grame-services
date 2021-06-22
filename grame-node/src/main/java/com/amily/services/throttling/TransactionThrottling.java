package com.grame.services.throttling;

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

import com.grame.services.exceptions.UnknowngrameFunctionality;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.TransactionBody;

import java.util.Optional;

import static com.grame.services.utils.MiscUtils.functionOf;

public class TransactionThrottling {
	private final FunctionalityThrottling throttles;

	public TransactionThrottling(FunctionalityThrottling throttles) {
		this.throttles = throttles;
	}

	public boolean shouldThrottle(TransactionBody txn) {
		Optional<grameFunctionality> function = functionToThrottle(txn);

		return function.map(throttles::shouldThrottle).orElse(true);
	}

	private Optional<grameFunctionality> functionToThrottle(TransactionBody txn) {
		try {
			return Optional.of(functionOf(txn));
		} catch (UnknowngrameFunctionality ignore) {}
		return Optional.empty();
	}
}
