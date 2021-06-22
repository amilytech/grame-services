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

import com.grame.services.state.serdes.DomainSerdes;
import com.swirlds.common.io.SerializableDataInputStream;
import com.swirlds.common.io.SerializableDataOutputStream;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.inOrder;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.willAnswer;

class SolidityLogTest {
	byte[] data = "hgfedcba".getBytes();
	byte[] otherData = "abcdefgh".getBytes();
	byte[] bloom = "ijklmnopqrstuvwxyz".getBytes();
	EntityId contractId = new EntityId(1L, 2L, 3L);
	List<byte[]> topics = List.of("first".getBytes(), "second".getBytes(), "third".getBytes());

	DomainSerdes serdes;
	DataInputStream din;
	SerializableDataInputStream in;

	SolidityLog subject;

	@BeforeEach
	public void setup() {
		din = mock(DataInputStream.class);
		in = mock(SerializableDataInputStream.class);
		serdes = mock(DomainSerdes.class);

		subject = new SolidityLog(contractId, bloom, topics, data);

		SolidityLog.serdes = serdes;
	}

	@AfterEach
	public void cleanup() {
		SolidityLog.serdes = new DomainSerdes();
	}

	@Test
	public void toStringWorks() {
		// expect:
		assertEquals(
				"SolidityLog{data="	+ Hex.encodeHexString(data) + ", " +
					"bloom=" + Hex.encodeHexString(bloom) + ", " +
					"contractId=" + contractId + ", " +
					"topics=" + topics.stream().map(Hex::encodeHexString).collect(toList()) + "}",
				subject.toString()
		);
	}

	@Test
	public void objectContractWorks() {
		// given:
		var one = subject;
		var two = new SolidityLog(contractId, bloom, topics, otherData);
		var three = new SolidityLog(contractId, bloom, topics, data);

		// then:
		assertNotEquals(one, null);
		assertNotEquals(one, new Object());
		assertNotEquals(two, one);
		assertEquals(three, one);
		assertEquals(one, one);
		// and:
		assertEquals(one.hashCode(), three.hashCode());
		assertNotEquals(one.hashCode(), two.hashCode());
	}

	@Test
	public void beanWorks() {
		// expect:
		assertEquals(
				new SolidityLog(
						subject.getContractId(),
						subject.getBloom(),
						subject.getTopics(),
						subject.getData())
				, subject);
	}

	@Test
	public void serializableDetWorks() {
		// expect;
		assertEquals(SolidityLog.MERKLE_VERSION, subject.getVersion());
		assertEquals(SolidityLog.RUNTIME_CONSTRUCTABLE_ID, subject.getClassId());
	}

	@Test
	public void serializeWorks() throws IOException {
		// setup:
		var out = mock(SerializableDataOutputStream.class);
		// and:
		InOrder inOrder = inOrder(serdes, out);

		// when:
		subject.serialize(out);

		// then:
		inOrder.verify(out).writeByteArray(argThat((byte[] bytes) -> Arrays.equals(bytes, data)));
		inOrder.verify(out).writeByteArray(argThat((byte[] bytes) -> Arrays.equals(bytes, bloom)));
		inOrder.verify(serdes).writeNullableSerializable(contractId, out);
		inOrder.verify(out).writeInt(topics.size());
		inOrder.verify(out).writeByteArray(argThat((byte[] bytes) -> Arrays.equals(bytes, topics.get(0))));
		inOrder.verify(out).writeByteArray(argThat((byte[] bytes) -> Arrays.equals(bytes, topics.get(1))));
		inOrder.verify(out).writeByteArray(argThat((byte[] bytes) -> Arrays.equals(bytes, topics.get(2))));
	}

	@Test
	public void deserializeWorks() throws IOException {
		// setup:
		var in = mock(SerializableDataInputStream.class);
		// and:
		var readSubject = new SolidityLog();

		given(in.readByteArray(SolidityLog.MAX_BLOOM_BYTES)).willReturn(bloom);
		given(in.readByteArray(SolidityLog.MAX_DATA_BYTES)).willReturn(data);
		given(serdes.readNullableSerializable(in)).willReturn(contractId);
		given(in.readInt()).willReturn(topics.size());
		given(in.readByteArray(SolidityLog.MAX_TOPIC_BYTES))
				.willReturn(topics.get(0))
				.willReturn(topics.get(1))
				.willReturn(topics.get(2));

		// when:
		readSubject.deserialize(in, SolidityLog.MERKLE_VERSION);

		// then:
		assertEquals(subject, readSubject);
	}
}
