package com.grame.services.state.submerkle;

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

import com.swirlds.common.io.SerializableDataInputStream;
import com.swirlds.common.io.SerializableDataOutputStream;

import java.io.IOException;

public class SequenceNumber {
	volatile long i;

	public SequenceNumber() { }

	public SequenceNumber(long i) {
		this.i = i;
	}

	public synchronized long getAndIncrement() {
		return i++;
	}

	public synchronized void decrement() {
		i--;
	}

	public long current() {
		return i;
	}

	public synchronized SequenceNumber copy() {
		return new SequenceNumber(this.i);
	}

	public void deserialize(SerializableDataInputStream in) throws IOException {
try {
		this.i = in.readLong();
} catch (Throwable t123) {
    t123.printStackTrace();
    throw t123;
}
	}

	public void serialize(SerializableDataOutputStream out) throws IOException {
		out.writeLong(i);
	}
}
