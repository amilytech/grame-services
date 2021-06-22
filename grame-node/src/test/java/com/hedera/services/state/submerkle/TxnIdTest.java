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

import com.google.protobuf.ByteString;
import com.grame.services.state.serdes.DomainSerdes;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.TransactionID;
import com.swirlds.common.io.SerializableDataInputStream;
import com.swirlds.common.io.SerializableDataOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.booleanThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(JUnitPlatform.class)
class TxnIdTest {
	private AccountID payer = IdUtils.asAccount("0.0.75231");
	private EntityId fcPayer = EntityId.ofNullableAccountId(payer);
	private Timestamp validStart = Timestamp.newBuilder()
			.setSeconds(1_234_567L)
			.setNanos(89)
			.build();
	private ByteString nonce = ByteString.copyFrom("THIS_IS_NEW".getBytes());
	private RichInstant fcValidStart = RichInstant.fromGrpc(validStart);

	DomainSerdes serdes;
	SerializableDataInputStream din;
	SerializableDataOutputStream dout;

	TxnId subject;

	@Test
	void serializeWorksForScheduled() throws IOException {
		// setup:
		subject = scheduledSubject();
		// and:
		dout = mock(SerializableDataOutputStream.class);
		serdes = mock(DomainSerdes.class);
		TxnId.serdes = serdes;

		// given:
		InOrder inOrder = Mockito.inOrder(serdes, dout);

		// when:
		subject.serialize(dout);

		// then:
		inOrder.verify(dout).writeSerializable(fcPayer, Boolean.TRUE);
		inOrder.verify(serdes).serializeTimestamp(fcValidStart, dout);
		inOrder.verify(dout, times(1)).writeBoolean(anyBoolean());

		// cleanup:
		TxnId.serdes = new DomainSerdes();
	}

	@Test
	public void preV0120DeserializeWorks() throws IOException {
		// setup:
		subject = unscheduledSubject();
		// and:
		din = mock(SerializableDataInputStream.class);
		serdes = mock(DomainSerdes.class);
		TxnId.serdes = serdes;

		given(din.readSerializable(booleanThat(Boolean.TRUE::equals), any(Supplier.class))).willReturn(fcPayer);
		given(serdes.deserializeTimestamp(din)).willReturn(fcValidStart);
		// and:
		var deserializedId = new TxnId();

		// when:
		deserializedId.deserialize(din, TxnId.PRE_RELEASE_0120_VERSION);

		// then:
		assertEquals(subject, deserializedId);
		verify(din, never()).readBoolean();
		verify(din, never()).readByteArray(Integer.MAX_VALUE);

		// cleanup:
		TxnId.serdes = new DomainSerdes();
	}

	@Test
	public void v0120DeserializeIgnoresNonce() throws IOException {
		// setup:
		subject = scheduledSubject();
		// and:
		din = mock(SerializableDataInputStream.class);
		serdes = mock(DomainSerdes.class);
		TxnId.serdes = serdes;

		given(din.readSerializable(booleanThat(Boolean.TRUE::equals), any(Supplier.class))).willReturn(fcPayer);
		given(serdes.deserializeTimestamp(din)).willReturn(fcValidStart);
		given(din.readBoolean()).willReturn(true).willReturn(true);
		given(din.readByteArray(anyInt())).willReturn(nonce.toByteArray());
		// and:
		var deserializedId = new TxnId();

		// when:
		deserializedId.deserialize(din, TxnId.RELEASE_0120_VERSION);

		// then:
		assertEquals(subject, deserializedId);
		// and:
		verify(din, times(2)).readBoolean();
		verify(din).readByteArray(Integer.MAX_VALUE);

		// cleanup:
		TxnId.serdes = new DomainSerdes();
	}

	@Test
	public void v0130DeserializeForgetsNonce() throws IOException {
		// setup:
		subject = scheduledSubject();
		// and:
		din = mock(SerializableDataInputStream.class);
		serdes = mock(DomainSerdes.class);
		TxnId.serdes = serdes;

		given(din.readSerializable(booleanThat(Boolean.TRUE::equals), any(Supplier.class))).willReturn(fcPayer);
		given(serdes.deserializeTimestamp(din)).willReturn(fcValidStart);
		given(din.readBoolean()).willReturn(true);
		// and:
		var deserializedId = new TxnId();

		// when:
		deserializedId.deserialize(din, TxnId.RELEASE_0130_VERSION);

		// then:
		assertEquals(subject, deserializedId);
		// and:
		verify(din, times(1)).readBoolean();

		// cleanup:
		TxnId.serdes = new DomainSerdes();
	}

	@Test
	public void equalsWorks() {
		// given:
		subject = scheduledSubject();

		// expect:
		assertNotEquals(subject, unscheduledSubject());
	}

	@Test
	public void hashCodeWorks() {
		// given:
		subject = scheduledSubject();

		// expect:
		assertNotEquals(subject.hashCode(), unscheduledSubject().hashCode());
	}

	@Test
	public void toStringWorks() {
		// given:
		subject = scheduledSubject();
		// and:
		String expRepr = "TxnId{payer=EntityId{shard=0, realm=0, num=75231}, " +
				"validStart=RichInstant{seconds=1234567, nanos=89}, " +
				"scheduled=true}";

		// expect:
		assertEquals(expRepr, subject.toString());
	}

	@Test
	public void toGrpcWorks() {
		// given:
		var subject = scheduledSubject();
		// and:
		var expected = base().setScheduled(true).build();

		// expect:
		assertEquals(expected, subject.toGrpc());
	}

	@Test
	public void merkleWorks() {
		// given:
		var subject = new TxnId();

		// expect:
		assertEquals(TxnId.RUNTIME_CONSTRUCTABLE_ID, subject.getClassId());
		assertEquals(TxnId.RELEASE_0130_VERSION, subject.getVersion());
	}

	private TxnId unscheduledSubject() {
		return TxnId.fromGrpc(base().build());
	}

	private TxnId scheduledSubject() {
		return TxnId.fromGrpc(base()
				.setScheduled(true)
				.build());
	}

	private TransactionID.Builder base() {
		return TransactionID.newBuilder()
				.setAccountID(payer)
				.setTransactionValidStart(validStart);
	}
}
