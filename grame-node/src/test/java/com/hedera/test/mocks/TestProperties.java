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

import com.grame.services.context.properties.PropertySource;

import java.util.Set;

public enum TestProperties implements PropertySource {
	TEST_PROPERTIES;

	@Override
	public boolean containsProperty(String name) {
		return false;
	}

	@Override
	public Object getProperty(String name) {
		if (name.equals("contracts.maxStorageKb")) {
			return 1024;
		} else if (name.equals("grame.transaction.minValidityBufferSecs")) {
			return 10;
		} else {
			return null;
		}
	}

	@Override
	public Set<String> allPropertyNames() {
		return Set.of(
			"contracts.maxStorageKb"
		);
	}
}
