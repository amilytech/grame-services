package com.grame.services.store.schedule;

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

import com.grame.services.context.TransactionContext;
import com.grame.services.context.properties.GlobalDynamicProperties;
import com.grame.services.ledger.grameLedger;
import com.grame.services.ledger.TransactionalLedger;
import com.grame.services.ledger.ids.EntityIdSource;
import com.grame.services.ledger.properties.AccountProperty;
import com.grame.services.legacy.core.jproto.JKey;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.state.merkle.MerkleSchedule;
import com.grame.services.state.merkle.MerkleScheduleTest;
import com.grame.services.state.submerkle.EntityId;
import com.grame.services.state.submerkle.RichInstant;
import com.grame.test.utils.IdUtils;
import com.grame.test.utils.TxnUtils;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.Key;
import com.gramegrame.api.proto.java.ScheduleID;
import com.swirlds.fcmap.FCMap;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.grame.services.ledger.properties.AccountProperty.IS_DELETED;
import static com.grame.services.state.merkle.MerkleEntityId.fromScheduleId;
import static com.grame.services.state.submerkle.EntityId.ofNullableAccountId;
import static com.grame.services.utils.MiscUtils.asKeyUnchecked;
import static com.grame.test.factories.scenarios.TxnHandlingScenario.SCHEDULE_ADMIN_KT;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_SCHEDULE_ACCOUNT_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_SCHEDULE_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_SCHEDULE_PAYER_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.SCHEDULE_IS_IMMUTABLE;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.SCHEDULE_ALREADY_DELETED;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.SCHEDULE_ALREADY_EXECUTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class grameScheduleStoreTest {
	static final int SIGNATURE_BYTES = 64;
	EntityIdSource ids;
	FCMap<MerkleEntityId, MerkleSchedule> schedules;
	TransactionalLedger<AccountID, AccountProperty, MerkleAccount> accountsLedger;
	grameLedger grameLedger;
	GlobalDynamicProperties globalDynamicProperties;

	MerkleSchedule schedule;
	MerkleSchedule anotherSchedule;
	MerkleAccount account;
	TransactionContext txnCtx;

	byte[] transactionBody;
	String entityMemo;
	int transactionBodyHashCode;
	RichInstant schedulingTXValidStart;
	RichInstant consensusTime;
	Key adminKey;
	JKey adminJKey;

	ScheduleID created = IdUtils.asSchedule("1.2.333333");
	AccountID schedulingAccount = IdUtils.asAccount("1.2.333");
	AccountID payerId = IdUtils.asAccount("1.2.456");
	AccountID anotherPayerId = IdUtils.asAccount("1.2.457");

	EntityId entityPayer = ofNullableAccountId(payerId);
	EntityId entitySchedulingAccount = ofNullableAccountId(schedulingAccount);

	long expectedExpiry = 1_234_567L;

	grameScheduleStore subject;

	@BeforeEach
	public void setup() {
		transactionBody = TxnUtils.randomUtf8Bytes(SIGNATURE_BYTES);
		entityMemo = "Some memo here";
		transactionBodyHashCode = Arrays.hashCode(transactionBody);
		schedulingTXValidStart = new RichInstant(123, 456);
		consensusTime = new RichInstant(expectedExpiry, 0);
		adminKey = SCHEDULE_ADMIN_KT.asKey();
		adminJKey = SCHEDULE_ADMIN_KT.asJKeyUnchecked();

		schedule = mock(MerkleSchedule.class);
		anotherSchedule = mock(MerkleSchedule.class);

		given(schedule.hasAdminKey()).willReturn(true);
		given(schedule.adminKey()).willReturn(Optional.of(SCHEDULE_ADMIN_KT.asJKeyUnchecked()));
		given(schedule.payer()).willReturn(ofNullableAccountId(payerId));
		given(schedule.memo()).willReturn(Optional.of(entityMemo));

		given(anotherSchedule.payer()).willReturn(ofNullableAccountId(anotherPayerId));

		ids = mock(EntityIdSource.class);
		given(ids.newScheduleId(schedulingAccount)).willReturn(created);

		account = mock(MerkleAccount.class);

		grameLedger = mock(grameLedger.class);
		txnCtx = mock(TransactionContext.class);
		globalDynamicProperties = mock(GlobalDynamicProperties.class);

		accountsLedger = (TransactionalLedger<AccountID, AccountProperty, MerkleAccount>) mock(
				TransactionalLedger.class);
		given(accountsLedger.exists(payerId)).willReturn(true);
		given(accountsLedger.exists(schedulingAccount)).willReturn(true);
		given(accountsLedger.get(payerId, IS_DELETED)).willReturn(false);
		given(accountsLedger.get(schedulingAccount, IS_DELETED)).willReturn(false);

		schedules = (FCMap<MerkleEntityId, MerkleSchedule>) mock(FCMap.class);
		given(schedules.get(fromScheduleId(created))).willReturn(schedule);
		given(schedules.containsKey(fromScheduleId(created))).willReturn(true);

		subject = new grameScheduleStore(globalDynamicProperties, ids, txnCtx, () -> schedules);
		subject.setAccountsLedger(accountsLedger);
		subject.setgrameLedger(grameLedger);
	}

	@Test
	void rebuildsAsExpected() {
		// setup:
		var parentTxn = MerkleScheduleTest.scheduleCreateTxnWith(
				asKeyUnchecked(adminJKey),
				entityMemo,
				entityPayer.toGrpcAccountId(),
				entitySchedulingAccount.toGrpcAccountId(),
				schedulingTXValidStart.toGrpc());
		var expected = MerkleSchedule.from(parentTxn.toByteArray(), 0L);
		ArgumentCaptor<BiConsumer<MerkleEntityId, MerkleSchedule>> captor = forClass(BiConsumer.class);
		// and:
		var expectedKey = expected.toContentAddressableView();

		// when:
		subject.rebuildViews();

		// then:
		verify(schedules, times(2)).forEach(captor.capture());
		// and:
		BiConsumer<MerkleEntityId, MerkleSchedule> visitor = captor.getAllValues().get(1);

		// and when:
		visitor.accept(fromScheduleId(created), expected);

		// then:
		var extant = subject.getExtantSchedules();
		assertEquals(1, extant.size());
		// and:
		assertTrue(extant.containsKey(expectedKey));
		assertEquals(created, extant.get(expectedKey).toScheduleId());
	}

	@Test
	public void commitAndRollbackThrowIseIfNoPendingCreation() {
		// expect:
		assertThrows(IllegalStateException.class, subject::commitCreation);
		assertThrows(IllegalStateException.class, subject::rollbackCreation);
	}

	@Test
	public void commitPutsToMapAndClears() {
		// setup:
		subject.pendingId = created;
		subject.pendingCreation = schedule;

		given(schedule.toContentAddressableView()).willReturn(schedule);

		// when:
		subject.commitCreation();

		// then:
		verify(schedule).toContentAddressableView();
		verify(schedules).put(fromScheduleId(created), schedule);
		assertTrue(subject.getExtantSchedules().containsKey(schedule));
		// and:
		assertSame(subject.pendingId, grameScheduleStore.NO_PENDING_ID);
		assertNull(subject.pendingCreation);
	}

	@Test
	public void rollbackReclaimsIdAndClears() {
		// setup:
		subject.pendingId = created;
		subject.pendingCreation = schedule;

		// when:
		subject.rollbackCreation();

		// then:
		verify(schedules, never()).put(fromScheduleId(created), schedule);
		verify(ids).reclaimLastId();
		// and:
		assertSame(subject.pendingId, grameScheduleStore.NO_PENDING_ID);
		assertNull(subject.pendingCreation);
	}

	@Test
	public void understandsPendingCreation() {
		// expect:
		assertFalse(subject.isCreationPending());

		// and when:
		subject.pendingId = created;

		// expect:
		assertTrue(subject.isCreationPending());
	}

	@Test
	public void getThrowsIseOnMissing() {
		given(schedules.containsKey(fromScheduleId(created))).willReturn(false);

		// expect:
		assertThrows(IllegalArgumentException.class, () -> subject.get(created));
	}

	@Test
	public void applicationRejectsMissing() {
		// setup:
		var change = mock(Consumer.class);

		given(schedules.containsKey(fromScheduleId(created))).willReturn(false);

		// expect:
		assertThrows(IllegalArgumentException.class, () -> subject.apply(created, change));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void provisionalApplicationWorks() {
		// setup:
		Consumer<MerkleSchedule> change = mock(Consumer.class);
		subject.pendingId = created;
		subject.pendingCreation = schedule;

		// when:
		subject.apply(created, change);

		// then:
		verify(change).accept(schedule);
		verify(schedules, never()).getForModify(fromScheduleId(created));
	}

	@Test
	public void applicationWorks() {
		// setup:
		var change = mock(Consumer.class);
		given(schedules.getForModify(fromScheduleId(created))).willReturn(schedule);
		// and:
		InOrder inOrder = Mockito.inOrder(change, schedules);

		// when:
		subject.apply(created, change);

		// then:
		inOrder.verify(schedules).getForModify(fromScheduleId(created));
		inOrder.verify(change).accept(schedule);
		inOrder.verify(schedules).replace(fromScheduleId(created), schedule);
	}

	@Test
	public void applicationAlwaysReplacesModifiableSchedule() {
		// setup:
		var change = mock(Consumer.class);
		var key = fromScheduleId(created);

		given(schedules.getForModify(key)).willReturn(schedule);

		willThrow(IllegalStateException.class).given(change).accept(any());

		// when:
		assertThrows(IllegalArgumentException.class, () -> subject.apply(created, change));

		// then:
		verify(schedules).replace(key, schedule);
	}

	@Test
	public void createProvisionallyWorks() {
		var parentTxn = MerkleScheduleTest.scheduleCreateTxnWith(
				asKeyUnchecked(adminJKey),
				entityMemo,
				entityPayer.toGrpcAccountId(),
				entitySchedulingAccount.toGrpcAccountId(),
				schedulingTXValidStart.toGrpc());
        var expected = MerkleSchedule.from(parentTxn.toByteArray(), 0L);

		// when:
		var outcome = subject.createProvisionally(expected, consensusTime);

		// then:
		assertEquals(OK, outcome.getStatus());
		assertEquals(created, outcome.getCreated().get());
		// and:
		assertEquals(created, subject.pendingId);
		assertSame(expected, subject.pendingCreation);
		assertEquals(expectedExpiry, expected.expiry());
	}

	@Test
	public void createProvisionallyRejectsInvalidPayer() {
		var parentTxn = MerkleScheduleTest.scheduleCreateTxnWith(
				asKeyUnchecked(adminJKey),
				entityMemo,
				IdUtils.asAccount("22.33.44"),
				entitySchedulingAccount.toGrpcAccountId(),
				schedulingTXValidStart.toGrpc());

		// when:
		var outcome = subject.createProvisionally(MerkleSchedule.from(parentTxn.toByteArray(), 0L), consensusTime);

		// then:
		assertEquals(INVALID_SCHEDULE_PAYER_ID, outcome.getStatus());
		assertTrue(outcome.getCreated().isEmpty());
	}

	@Test
	public void getCanReturnPending() {
		// setup:
		subject.pendingId = created;
		subject.pendingCreation = schedule;

		// expect:
		assertSame(schedule, subject.get(created));
	}

	@Test
	public void existenceCheckUnderstandsPendingIdOnlyAppliesIfCreationPending() {
		// expect:
		assertFalse(subject.exists(grameScheduleStore.NO_PENDING_ID));
	}

	@Test
	public void createProvisionallyRejectsInvalidScheduler() {
		var parentTxn = MerkleScheduleTest.scheduleCreateTxnWith(
				asKeyUnchecked(adminJKey),
				entityMemo,
				entityPayer.toGrpcAccountId(),
				IdUtils.asAccount("22.33.44"),
				schedulingTXValidStart.toGrpc());

		// when:
		var outcome = subject.createProvisionally(MerkleSchedule.from(parentTxn.toByteArray(), 0L), consensusTime);

		// then:
		assertEquals(INVALID_SCHEDULE_ACCOUNT_ID, outcome.getStatus());
		assertEquals(Optional.empty(), outcome.getCreated());
		// and:
		assertNull(subject.pendingCreation);
		assertEquals(ScheduleID.getDefaultInstance(), subject.pendingId);
	}

	@Test
	public void rejectsCreateProvisionallyDeletedPayer() {
		// given:
		given(accountsLedger.get(payerId, IS_DELETED)).willReturn(true);

		var parentTxn = MerkleScheduleTest.scheduleCreateTxnWith(
				asKeyUnchecked(adminJKey),
				entityMemo,
				entityPayer.toGrpcAccountId(),
				entitySchedulingAccount.toGrpcAccountId(),
				schedulingTXValidStart.toGrpc());

		// when:
		var outcome = subject.createProvisionally(MerkleSchedule.from(parentTxn.toByteArray(), 0L), consensusTime);

		// then:
		assertEquals(INVALID_SCHEDULE_PAYER_ID, outcome.getStatus());
		assertEquals(Optional.empty(), outcome.getCreated());
		// and:
		assertNull(subject.pendingCreation);
		assertEquals(ScheduleID.getDefaultInstance(), subject.pendingId);
	}

	@Test
	public void rejectsCreateProvisionallyDeletedScheduler() {
		given(accountsLedger.get(schedulingAccount, IS_DELETED)).willReturn(true);

		var parentTxn = MerkleScheduleTest.scheduleCreateTxnWith(
				asKeyUnchecked(adminJKey),
				entityMemo,
				entityPayer.toGrpcAccountId(),
				entitySchedulingAccount.toGrpcAccountId(),
				schedulingTXValidStart.toGrpc());

		// when:
		var outcome = subject.createProvisionally(MerkleSchedule.from(parentTxn.toByteArray(), 0L), consensusTime);

		// then:
		assertEquals(INVALID_SCHEDULE_ACCOUNT_ID, outcome.getStatus());
		assertEquals(Optional.empty(), outcome.getCreated());
		// and:
		assertNull(subject.pendingCreation);
		assertEquals(ScheduleID.getDefaultInstance(), subject.pendingId);
	}

	@Test
	public void rejectsCreateProvisionallyWithMissingSchedulingAccount() {
		given(accountsLedger.exists(schedulingAccount)).willReturn(false);

		var parentTxn = MerkleScheduleTest.scheduleCreateTxnWith(
				asKeyUnchecked(adminJKey),
				entityMemo,
				entityPayer.toGrpcAccountId(),
				entitySchedulingAccount.toGrpcAccountId(),
				schedulingTXValidStart.toGrpc());

		// when:
		var outcome = subject.createProvisionally(MerkleSchedule.from(parentTxn.toByteArray(), 0L), consensusTime);

		// then:
		assertEquals(INVALID_SCHEDULE_ACCOUNT_ID, outcome.getStatus());
		assertEquals(Optional.empty(), outcome.getCreated());
		// and:
		assertNull(subject.pendingCreation);
		assertEquals(ScheduleID.getDefaultInstance(), subject.pendingId);
	}

	@Test
	public void recognizesCollidingSchedule() {
		// setup:
		var parentTxn = MerkleScheduleTest.scheduleCreateTxnWith(
				asKeyUnchecked(adminJKey),
				entityMemo,
				entityPayer.toGrpcAccountId(),
				entitySchedulingAccount.toGrpcAccountId(),
				schedulingTXValidStart.toGrpc());
		var candSchedule = MerkleSchedule.from(parentTxn.toByteArray(), expectedExpiry);
		var cav = candSchedule.toContentAddressableView();

		// given:
		subject.getExtantSchedules().put(cav, fromScheduleId(created));

		// when:
		var scheduleIdPair = subject.lookupSchedule(parentTxn.toByteArray());

		assertEquals(Pair.of(Optional.of(created), schedule), scheduleIdPair);
	}

	@Test
	public void recognizesCollisionWithPending() {
		// setup:
		var parentTxn = MerkleScheduleTest.scheduleCreateTxnWith(
				asKeyUnchecked(adminJKey),
				entityMemo,
				entityPayer.toGrpcAccountId(),
				entitySchedulingAccount.toGrpcAccountId(),
				schedulingTXValidStart.toGrpc());
		var candSchedule = MerkleSchedule.from(parentTxn.toByteArray(), expectedExpiry);

		// given:
		subject.pendingCreation = candSchedule;
		subject.pendingId = created;

		// when:
		var scheduleIdPair = subject.lookupSchedule(parentTxn.toByteArray());

		assertEquals(Pair.of(Optional.of(created), candSchedule), scheduleIdPair);
	}

	@Test
	public void understandsMissing() {
		// setup:
		var parentTxn = MerkleScheduleTest.scheduleCreateTxnWith(
				asKeyUnchecked(adminJKey),
				entityMemo,
				entityPayer.toGrpcAccountId(),
				entitySchedulingAccount.toGrpcAccountId(),
				schedulingTXValidStart.toGrpc());
		var expected = MerkleSchedule.from(parentTxn.toByteArray(), 0L);

		// when:
		var scheduleIdPair = subject.lookupSchedule(parentTxn.toByteArray());

		assertTrue(scheduleIdPair.getLeft().isEmpty());
		assertEquals(expected, scheduleIdPair.getRight());
	}

	@Test
	public void deletesAsExpected() {
		// setup:
		var now = schedulingTXValidStart.toJava();

		// given:
		given(schedules.getForModify(fromScheduleId(created))).willReturn(schedule);
		given(txnCtx.consensusTime()).willReturn(now);

		// when:
		var outcome = subject.delete(created);

		// then:
		verify(schedule).markDeleted(now);
		verify(schedules).replace(fromScheduleId(created), schedule);
		// and:
		assertEquals(OK, outcome);
	}

	@Test
	public void rejectsDeletionMissingAdminKey() {
		// given:
		given(schedule.adminKey()).willReturn(Optional.empty());

		// when:
		var outcome = subject.delete(created);

		// then:
		verify(schedules, never()).remove(fromScheduleId(created));
		// and:
		assertEquals(SCHEDULE_IS_IMMUTABLE, outcome);
	}

	@Test
	public void rejectsDeletionAlreadyDeleted() {
		// given:
		given(schedule.isDeleted()).willReturn(true);

		// when:
		var outcome = subject.delete(created);

		// then:
		assertEquals(SCHEDULE_ALREADY_DELETED, outcome);
	}

	@Test
	public void rejectsExecutionWhenDeleted() {
		// given:
		given(schedule.isDeleted()).willReturn(true);

		// when:
		var outcome = subject.markAsExecuted(created);

		// then:
		assertEquals(SCHEDULE_ALREADY_DELETED, outcome);
	}

	@Test
	public void rejectsExecutionWhenExecuted() {
		// given:
		given(schedule.isExecuted()).willReturn(true);

		// when:
		var outcome = subject.markAsExecuted(created);

		// then:
		assertEquals(SCHEDULE_ALREADY_EXECUTED, outcome);
	}

	@Test
	public void rejectsDeletionMissingSchedule() {
		// given:
		given(schedules.containsKey(fromScheduleId(created))).willReturn(false);

		// when:
		var outcome = subject.delete(created);

		// then:
		verify(schedules, never()).remove(fromScheduleId(created));
		// and:
		assertEquals(INVALID_SCHEDULE_ID, outcome);
	}

	@Test
	public void rejectsExecutionMissingSchedule() {
		// given:
		given(schedules.containsKey(fromScheduleId(created))).willReturn(false);

		// when:
		var outcome = subject.markAsExecuted(created);

		// then:
		assertEquals(INVALID_SCHEDULE_ID, outcome);
	}

	@Test
	public void marksExecutedAsExpected() {
		// setup:
		var now = schedulingTXValidStart.toJava();

		// given:
		given(txnCtx.consensusTime()).willReturn(now);
		given(schedules.getForModify(fromScheduleId(created))).willReturn(schedule);

		// when:
		subject.markAsExecuted(created);

		// then:
		verify(schedule).markExecuted(now.plusNanos(1L));
		verify(schedules, never()).remove(fromScheduleId(created));
	}

	@Test
	void expiresAsExpected() {
		// setup:
		subject.getExtantSchedules().put(schedule, fromScheduleId(created));

		// when:
		subject.expire(EntityId.ofNullableScheduleId(created));

		// then:
		verify(schedules).remove(fromScheduleId(created));
		// and:
		assertFalse(subject.getExtantSchedules().containsKey(schedule));
	}

	@Test
	public void throwsOnExpiringMissingSchedule() {
		// given:
		given(schedules.containsKey(fromScheduleId(created))).willReturn(false);

		// when:
		assertThrows(IllegalArgumentException.class, () -> subject.expire(EntityId.ofNullableScheduleId(created)));
	}

	@Test
	public void throwsOnExpiringPending() {
		var parentTxn = MerkleScheduleTest.scheduleCreateTxnWith(
				asKeyUnchecked(adminJKey),
				entityMemo,
				entityPayer.toGrpcAccountId(),
				entitySchedulingAccount.toGrpcAccountId(),
				schedulingTXValidStart.toGrpc());
		var expected = MerkleSchedule.from(parentTxn.toByteArray(), 0L);

		// when:
		subject.createProvisionally(expected, consensusTime);

		// when:
		assertThrows(IllegalArgumentException.class,
				() -> subject.expire(EntityId.ofNullableScheduleId(subject.pendingId)));
	}
}
