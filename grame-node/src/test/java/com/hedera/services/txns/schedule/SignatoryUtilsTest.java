package com.grame.services.txns.schedule;

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

import com.grame.services.keys.InHandleActivationHelper;
import com.grame.services.legacy.core.jproto.JEd25519Key;
import com.grame.services.legacy.core.jproto.JKey;
import com.grame.services.state.merkle.MerkleSchedule;
import com.grame.services.store.schedule.ScheduleStore;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.ScheduleID;
import com.gramegrame.api.proto.java.TransactionBody;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.NO_NEW_VALID_SIGNATURES;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.SOME_SIGNATURES_WERE_INVALID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SignatoryUtilsTest {
	private JKey goodKey = new JEd25519Key("angelic".getBytes());
	private TransactionBody scheduledTxn = TransactionBody.getDefaultInstance();
	private ScheduleID id = IdUtils.asSchedule("0.0.75231");
	private Optional<List<JKey>> noValidNoInvalid = Optional.of(Collections.emptyList());
	private Optional<List<JKey>> goodValidNoInvalid = Optional.of(List.of(goodKey));

	@Mock
	ScheduleStore store;
	@Mock
	MerkleSchedule schedule;
	@Mock
	InHandleActivationHelper activationHelper;

	@Test
	void respondsToNoAttemptsCorrectly() {
		given(store.get(id)).willReturn(schedule);
		given(schedule.ordinaryViewOfScheduledTxn()).willReturn(scheduledTxn);
		given(activationHelper.areScheduledPartiesActive(any(), any())).willReturn(false);

		// when:
		var outcome = SignatoryUtils.witnessScoped(id, store, noValidNoInvalid, activationHelper);

		// then:
		assertEquals(Pair.of(NO_NEW_VALID_SIGNATURES, false), outcome);
	}

	@Test
	void respondsToNoAttemptsButNowActiveCorrectly() {
		given(store.get(id)).willReturn(schedule);
		given(schedule.ordinaryViewOfScheduledTxn()).willReturn(scheduledTxn);
		given(activationHelper.areScheduledPartiesActive(any(), any())).willReturn(true);

		// when:
		var outcome = SignatoryUtils.witnessScoped(id, store, noValidNoInvalid, activationHelper);

		// then:
		assertEquals(Pair.of(OK, true), outcome);
		// and:
		verify(activationHelper, never()).visitScheduledCryptoSigs(any());
	}

	@Test
	void respondsToPresumedInvalidCorrectly() {
		// when:
		var outcome = SignatoryUtils.witnessScoped(id, store, Optional.empty(), activationHelper);

		// then:
		assertEquals(Pair.of(SOME_SIGNATURES_WERE_INVALID, false), outcome);
	}

	@Test
	void respondsToRepeatedCorrectlyIfNotActive() {
		given(store.get(id)).willReturn(schedule);
		given(schedule.ordinaryViewOfScheduledTxn()).willReturn(scheduledTxn);
		given(activationHelper.areScheduledPartiesActive(any(), any())).willReturn(false);
		// and:
		given(schedule.witnessValidEd25519Signature(eq(goodKey.getEd25519()))).willReturn(false);
		willAnswer(inv -> {
			Consumer<MerkleSchedule> action = inv.getArgument(1);
			action.accept(schedule);
			return null;
		}).given(store).apply(eq(id), any());

		// when:
		var outcome = SignatoryUtils.witnessScoped(id, store, goodValidNoInvalid, activationHelper);

		// then:
		assertEquals(Pair.of(NO_NEW_VALID_SIGNATURES, false), outcome);
	}

	@Test
	void respondsToRepeatedCorrectlyIfActive() {
		given(store.get(id)).willReturn(schedule);
		given(schedule.ordinaryViewOfScheduledTxn()).willReturn(scheduledTxn);
		given(activationHelper.areScheduledPartiesActive(any(), any())).willReturn(true);
		// and:
		given(schedule.witnessValidEd25519Signature(eq(goodKey.getEd25519()))).willReturn(false);
		willAnswer(inv -> {
			Consumer<MerkleSchedule> action = inv.getArgument(1);
			action.accept(schedule);
			return null;
		}).given(store).apply(eq(id), any());

		// when:
		var outcome = SignatoryUtils.witnessScoped(id, store, goodValidNoInvalid, activationHelper);

		// then:
		assertEquals(Pair.of(OK, true), outcome);
	}

	@Test
	void respondsToActivatingCorrectly() {
		given(store.get(id)).willReturn(schedule);
		given(schedule.ordinaryViewOfScheduledTxn()).willReturn(scheduledTxn);
		given(activationHelper.areScheduledPartiesActive(any(), any())).willReturn(true);
		// and:
		given(schedule.witnessValidEd25519Signature(eq(goodKey.getEd25519()))).willReturn(true);
		willAnswer(inv -> {
			Consumer<MerkleSchedule> action = inv.getArgument(1);
			action.accept(schedule);
			return null;
		}).given(store).apply(eq(id), any());

		// when:
		var outcome = SignatoryUtils.witnessScoped(id, store, goodValidNoInvalid, activationHelper);

		// then:
		assertEquals(Pair.of(OK, true), outcome);
	}
}
