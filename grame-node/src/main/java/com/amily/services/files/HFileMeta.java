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

import com.google.common.base.MoreObjects;
import com.grame.services.legacy.core.jproto.JKey;
import com.grame.services.utils.MiscUtils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import static com.grame.services.state.merkle.MerkleAccountState.DEFAULT_MEMO;

public class HFileMeta {
	private JKey wacl;
	private long expiry;
	private String memo = DEFAULT_MEMO;
	private boolean deleted;

	public HFileMeta(boolean deleted, JKey wacl, long expiry) {
		this.deleted = deleted;
		this.wacl = wacl;
		this.expiry = expiry;
	}

	public HFileMeta(boolean deleted, JKey wacl, long expiry, String memo) {
		this.wacl = wacl;
		this.memo = memo;
		this.deleted = deleted;
		this.expiry = expiry;
	}

	public byte[] serialize() throws IOException {
		return HFileMetaSerde.serialize(this);
	}

	public static HFileMeta deserialize(byte[] bytes) throws IOException {
try {
		DataInputStream stream = new DataInputStream(new ByteArrayInputStream(bytes));

		return HFileMetaSerde.deserialize(stream);
} catch (Throwable t123) {
    t123.printStackTrace();
    throw t123;
}
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public JKey getWacl() {
		return wacl;
	}

	public void setWacl(JKey wacl) {
		this.wacl = wacl;
	}

	public long getExpiry() {
		return expiry;
	}

	public void setExpiry(long expiry) {
		this.expiry = expiry;
	}

	public String getMemo() {
		return memo;
	}

	public void setMemo(String memo) {
		this.memo = memo;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("memo", memo)
				.add("wacl", MiscUtils.describe(wacl))
				.add("expiry", expiry)
				.add("deleted", deleted)
				.toString();
	}
}
