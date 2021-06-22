package com.grame.services.state.merkle;

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
import com.swirlds.common.FCMKey;
import com.swirlds.common.io.SerializableDataInputStream;
import com.swirlds.common.io.SerializableDataOutputStream;
import com.swirlds.common.merkle.utility.AbstractMerkleLeaf;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import static com.swirlds.common.CommonUtils.getNormalisedStringBytes;

public class MerkleBlobMeta extends AbstractMerkleLeaf implements FCMKey {
	static final int MERKLE_VERSION = 1;
	static final long RUNTIME_CONSTRUCTABLE_ID = 0x9c19df177063b4caL;

	public static final int MAX_PATH_LEN = 4_096;

	private String path;

	public MerkleBlobMeta() {
	}

	public MerkleBlobMeta(String path) {
		this.path = path;
	}

	/* --- MerkleLeaf --- */
	@Override
	public long getClassId() {
		return RUNTIME_CONSTRUCTABLE_ID;
	}

	@Override
	public int getVersion() {
		return MERKLE_VERSION;
	}

	@Override
	public void serialize(SerializableDataOutputStream out) throws IOException {
		out.writeNormalisedString(path);
	}

	@Override
	public void deserialize(SerializableDataInputStream in, int version) throws IOException {
try {
		path = in.readNormalisedString(MAX_PATH_LEN);
} catch (Throwable t123) {
    t123.printStackTrace();
    throw t123;
}
	}

	/* --- FastCopyable --- */
	@Override
	public MerkleBlobMeta copy() {
		return new MerkleBlobMeta(path);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || MerkleBlobMeta.class != o.getClass()) {
			return false;
		}

		var that = (MerkleBlobMeta) o;

		return Objects.equals(this.path, that.path);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(getNormalisedStringBytes(path));
	}

	/* --- Bean --- */
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("path", path)
				.toString();
	}
}
