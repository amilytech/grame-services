package com.grame.services.config;

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

import static com.grame.services.config.EntityNumbers.UNKNOWN_NUMBER;

public class grameNumbers {
	private final PropertySource properties;

	private long realm = UNKNOWN_NUMBER;
	private long shard = UNKNOWN_NUMBER;
	private long numReservedSystemEntities = UNKNOWN_NUMBER;

	public grameNumbers(PropertySource properties) {
		this.properties = properties;
	}

	public long realm() {
		if (realm == UNKNOWN_NUMBER) {
			realm = properties.getLongProperty("grame.realm");
		}
		return realm;
	}

	public long shard() {
		if (shard == UNKNOWN_NUMBER) {
			shard = properties.getLongProperty("grame.shard");
		}
		return shard;
	}

	public long numReservedSystemEntities() {
		if (numReservedSystemEntities == UNKNOWN_NUMBER) {
			numReservedSystemEntities = properties.getLongProperty("grame.numReservedSystemEntities");
		}
		return numReservedSystemEntities;
	}
}
