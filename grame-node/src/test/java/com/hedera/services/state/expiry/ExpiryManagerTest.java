package com.grame.services.state.expiry;

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

import com.grame.services.ledger.grameLedger;
import com.grame.services.legacy.core.jproto.TxnReceipt;
import com.grame.services.records.RecordCache;
import com.grame.services.records.TxnIdRecentHistory;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.state.merkle.MerkleSchedule;
import com.grame.services.state.submerkle.EntityId;
import com.grame.services.state.submerkle.ExpirableTxnRecord;
import com.grame.services.state.submerkle.RichInstant;
import com.grame.services.state.submerkle.TxnId;
import com.grame.services.store.schedule.ScheduleStore;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.ScheduleID;
import com.gramegrame.api.proto.java.TransactionID;
import com.gramegrame.api.proto.java.TransactionReceipt;
import com.swirlds.fcmap.FCMap;
import javafx.util.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.inOrder;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.times;

class ExpiryManagerTest {
	long a = 13257, b = 75231;
	long[] aPayer = { 55 };
	long[] bPayer = { 33 };

	long expiry = 1_234_567L;
	AccountID payer = IdUtils.asAccount("0.0.13257");
	ScheduleID schedule = IdUtils.asSchedule("0.0.12345");
	EntityId entityId = EntityId.ofNullableScheduleId(schedule);
	Consumer<EntityId> entityIdConsumer;
	Pair<Long, Consumer<EntityId>> expiringEntity;

	RecordCache recordCache;
	grameLedger ledger;
	FCMap<MerkleEntityId, MerkleAccount> accounts;
	FCMap<MerkleEntityId, MerkleSchedule> schedules;
	Map<TransactionID, TxnIdRecentHistory> txnHistories;

	ScheduleStore scheduleStore;

	ExpiryManager subject;

	@BeforeEach
	public void setup() {
		accounts = new FCMap<>();
		schedules = new FCMap<>();
		txnHistories = new HashMap<>();
		recordCache = mock(RecordCache.class);

		scheduleStore = mock(ScheduleStore.class);

		ledger = mock(grameLedger.class);

		entityIdConsumer = mock(Consumer.class);
		expiringEntity = mock(Pair.class);
		given(expiringEntity.getKey()).willReturn(schedule.getScheduleNum());
		given(expiringEntity.getValue()).willReturn(entityIdConsumer);

		subject = new ExpiryManager(recordCache, txnHistories, scheduleStore, schedules);
	}

	@Test
	public void purgesRecordsAsExpected() {
		// setup:
		InOrder inOrder = inOrder(ledger);

		givenAccount(a, aPayer);
		givenAccount(b, bPayer);
		// given:
		subject.restartTrackingFrom(accounts);

		// when:
		subject.purgeExpiredRecordsAt(33, ledger);

		// then:
		inOrder.verify(ledger).purgeExpiredRecords(
				argThat(asAccount(b)::equals),
				longThat(l -> l == 33),
				any());
		// and:
		verify(recordCache).forgetAnyOtherExpiredHistory(33);
	}

	@Test
	public void purgesEntitiesAsExpected() {
		// given:
		givenSchedule(schedule.getScheduleNum());
		// and:
		subject.restartEntitiesTracking();

		// when:
		subject.purgeExpiredEntitiesAt(expiry);

		// then:
		verify(scheduleStore).expire(entityId);
		// and:
		assertTrue(subject.entityExpiries.allExpiries.isEmpty());
	}

	private AccountID asAccount(long num) {
		return IdUtils.asAccount(String.format("0.0.%d", num));
	}

	@Test
	void restartClearsExistingState() {
		// setup:
		long oldExpiry = 2_345_678L;
		AccountID payer = IdUtils.asAccount("0.0.2");
		txnHistories = mock(Map.class);

		// given:
		subject = new ExpiryManager(recordCache, txnHistories, scheduleStore, schedules);
		// and:
		subject.trackRecord(payer, oldExpiry);
		// and:
		givenAccount(2, new long[] { expiry });
		// and:
		given(txnHistories.computeIfAbsent(any(), any())).willReturn(new TxnIdRecentHistory());

		// when:
		subject.restartTrackingFrom(accounts);

		// then:
		verify(recordCache).reset();
		verify(txnHistories).clear();
		assertEquals(expiry, subject.payerExpiries.now);
		assertEquals(1, subject.payerExpiries.allExpiries.size());
		var expiryEvent = subject.payerExpiries.allExpiries.getFirst();
		assertEquals(2, expiryEvent.getId());
		assertEquals(expiry, expiryEvent.getExpiry());
	}

	@Test
	public void restartsTrackingAsExpected() {
		givenAccount(a, aPayer);
		givenAccount(b, bPayer);

		// when:
		subject.restartTrackingFrom(accounts);

		// then:
		var e1 = subject.payerExpiries.allExpiries.poll();
		assertEquals(b, e1.getId());
		assertEquals(33, e1.getExpiry());
		var e2 = subject.payerExpiries.allExpiries.poll();
		assertEquals(a, e2.getId());
		assertEquals(55, e2.getExpiry());
		// and:
		assertTrue(subject.payerExpiries.allExpiries.isEmpty());
		// and:
		long[] allPayerTs = Stream.of(aPayer, bPayer)
				.flatMap(a -> Arrays.stream(a).boxed())
				.mapToLong(Long::valueOf)
				.toArray();
		assertTrue(Arrays.stream(allPayerTs).mapToObj(t -> txnIdOf(t).toGrpc()).allMatch(txnHistories::containsKey));
		// and:
		assertTrue(txnHistories.values().stream().noneMatch(TxnIdRecentHistory::isStagePending));
	}

	@Test
	public void restartsEntitiesTrackingAsExpected() {
		givenSchedule(schedule.getScheduleNum());

		// when:
		subject.restartEntitiesTracking();

		// then:
		var e = subject.entityExpiries.allExpiries.poll();
		assertEquals(schedule.getScheduleNum(), e.getId().getKey());
		assertEquals(expiry, e.getExpiry());
		// and:
		assertTrue(subject.entityExpiries.allExpiries.isEmpty());
	}

	private void givenAccount(long num, long[] payerExpiries) {
		var account = new MerkleAccount();
		for (long t : payerExpiries) {
			account.records().offer(withExpiry(t));
		}
		var id = new MerkleEntityId(0, 0, num);
		accounts.put(id, account);
	}

	private void givenSchedule(long num) {
		var schedule = new MerkleSchedule();
		schedule.setExpiry(expiry);
		var id = new MerkleEntityId(0, 0, num);
		schedules.put(id, schedule);
	}

	@Test
	public void expungesAncientHistory() {
		// given:
		var c = 13258L;
		var rec = withExpiry(c);
		var txnId = txnIdOf(c).toGrpc();
		// and:
		subject.sharedNow = c;
		// and:
		given(txnHistories.get(txnId).isForgotten()).willReturn(true);
		// and:
		var forgottenHistory = txnHistories.get(txnId);

		// when:
		subject.updateHistory(rec);

		// then:
		verify(forgottenHistory).forgetExpiredAt(c);
		// and:
		assertFalse(txnHistories.containsKey(txnId));
	}

	@Test
	public void updatesHistoryAsExpected() {
		// given:
		var c = 13258L;
		var rec = withExpiry(c);
		var txnId = txnIdOf(c).toGrpc();
		// and:
		subject.sharedNow = c;
		// and:
		given(txnHistories.get(txnId).isForgotten()).willReturn(false);

		// when:
		subject.updateHistory(rec);

		// then:
		verify(txnHistories.get(txnId)).forgetExpiredAt(c);
		// and:
		assertTrue(txnHistories.containsKey(txnId));
	}

	@Test
	public void safeWhenNoHistoryAvailable() {
		// given:
		var c = 13258L;
		var rec = withExpiry(c);
		var txnId = txnIdOf(c).toGrpc();
		// and:
		subject.sharedNow = c;
		txnHistories.remove(txnId);

		// expect:
		assertDoesNotThrow(() -> subject.updateHistory(rec));
	}

	@Test
	public void usesBestGuessSubmittingMember() {
		// setup:
		long givenPayerNum = 75231;
		var rec = withPayer(givenPayerNum);
		// and:
		var history = mock(TxnIdRecentHistory.class);

		txnHistories.put(txnIdOf(givenPayerNum).toGrpc(), history);

		// when:
		subject.stage(rec);
		subject.stage(rec);
		subject.stage(rec);
		subject.stage(rec);
		subject.stage(rec);

		// then:
		verify(history, times(5)).stage(rec);
	}

	private ExpirableTxnRecord withPayer(long num) {
		return new ExpirableTxnRecord(
				TxnReceipt.fromGrpc(TransactionReceipt.newBuilder().setStatus(SUCCESS).build()),
				"NOPE".getBytes(),
				txnIdOf(num),
				RichInstant.fromJava(Instant.now()),
				null,
				0,
				null,
				null,
				null
		);
	}

	private ExpirableTxnRecord withExpiry(long t) {
		var grpcTxn = txnIdOf(t).toGrpc();
		txnHistories.put(grpcTxn, mock(TxnIdRecentHistory.class));
		var r = new ExpirableTxnRecord(
				TxnReceipt.fromGrpc(TransactionReceipt.newBuilder().setStatus(SUCCESS).build()),
				"NOPE".getBytes(),
				txnIdOf(t),
				RichInstant.fromJava(Instant.now()),
				null,
				0,
				null,
				null,
				null
		);
		r.setExpiry(t);
		return r;
	}

	private TxnId txnIdOf(long t) {
		return TxnId.fromGrpc(TransactionID.newBuilder().
						setAccountID(IdUtils.asAccount(String.format("0.0.%d", t))).build());
	}

	@Test
	public void addsExpectedExpiryForPayer() {
		// setup:
		subject.payerExpiries = (MonotonicFullQueueExpiries<Long>) mock(MonotonicFullQueueExpiries.class);

		// when:
		subject.trackRecord(payer, expiry);

		// then:
		verify(subject.payerExpiries).track(Long.valueOf(13257), expiry);
	}

	@Test
	public void addsExpectedExpiringEntity() {
		// setup:
		subject.entityExpiries = (MonotonicFullQueueExpiries<Pair<Long, Consumer<EntityId>>>) mock(MonotonicFullQueueExpiries.class);

		// when:
		subject.trackEntity(expiringEntity, expiry);

		// then:
		verify(subject.entityExpiries).track(expiringEntity, expiry);
	}
}
