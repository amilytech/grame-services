package com.grame.services.files.interceptors;

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

import com.grame.services.config.FileNumbers;
import com.grame.services.context.properties.PropertySource;
import com.gramegrame.api.proto.java.FileID;

public class MockFileNumbers extends FileNumbers {
	private long shard = 0L, realm = 0L;

	public void setShard(long shard) {
		this.shard = shard;
	}

	public void setRealm(long realm) {
		this.realm = realm;
	}

	public MockFileNumbers() {
		super(null, null);
	}

	@Override
	public long addressBook() {
		return 101;
	}

	@Override
	public long nodeDetails() {
		return 102;
	}

	@Override
	public long feeSchedules() {
		return 111;
	}

	@Override
	public long exchangeRates() {
		return 112;
	}

	@Override
	public long applicationProperties() {
		return 121;
	}

	@Override
	public long apiPermissions() {
		return 122;
	}

	@Override
	public long softwareUpdateZip() {
		return 150;
	}

	@Override
	public long throttleDefinitions() {
		return 123;
	}

	@Override
	public FileID toFid(long num) {
		return FileID.newBuilder()
				.setShardNum(shard)
				.setRealmNum(realm)
				.setFileNum(num).build();
	}
}
