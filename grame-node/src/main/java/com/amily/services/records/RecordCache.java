package com.grame.services.records;

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

import com.google.common.cache.Cache;
import com.grame.services.context.ServicesContext;
import com.grame.services.legacy.core.jproto.TxnReceipt;
import com.grame.services.state.expiry.MonotonicFullQueueExpiries;
import com.grame.services.state.submerkle.ExpirableTxnRecord;
import com.grame.services.utils.TxnAccessor;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.TransactionID;
import com.gramegrame.api.proto.java.TransactionReceipt;
import com.gramegrame.api.proto.java.TransactionRecord;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.grame.services.utils.MiscUtils.asTimestamp;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.FAIL_INVALID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.UNKNOWN;
import static java.util.stream.Collectors.toList;

public class RecordCache {
	static final TransactionReceipt UNKNOWN_RECEIPT = TransactionReceipt.newBuilder()
			.setStatus(UNKNOWN)
			.build();

	public static final Boolean MARKER = Boolean.TRUE;

	private ServicesContext ctx;
	private Cache<TransactionID, Boolean> timedReceiptCache;
	private Map<TransactionID, TxnIdRecentHistory> histories;

	MonotonicFullQueueExpiries<TransactionID> recordExpiries = new MonotonicFullQueueExpiries<>();

	public RecordCache(
			ServicesContext ctx,
			Cache<TransactionID, Boolean> timedReceiptCache,
			Map<TransactionID, TxnIdRecentHistory> histories
	) {
		this.ctx = ctx;
		this.histories = histories;
		this.timedReceiptCache = timedReceiptCache;
	}

	public void addPreConsensus(TransactionID txnId) {
		timedReceiptCache.put(txnId, Boolean.TRUE);
	}

	public void setPostConsensus(
			TransactionID txnId,
			ResponseCodeEnum status,
			ExpirableTxnRecord record
	) {
		var recentHistory = histories.computeIfAbsent(txnId, ignore -> new TxnIdRecentHistory());
		recentHistory.observe(record, status);
	}

	public void setFailInvalid(
			AccountID effectivePayer,
			TxnAccessor accessor,
			Instant consensusTimestamp,
			long submittingMember
	) {
		var txnId = accessor.getTxnId();
		var transactionRecord = TransactionRecord.newBuilder()
				.setTransactionID(txnId)
				.setReceipt(TransactionReceipt.newBuilder().setStatus(FAIL_INVALID))
				.setMemo(accessor.getTxn().getMemo())
				.setTransactionHash(accessor.getHash())
				.setConsensusTimestamp(asTimestamp(consensusTimestamp));
		if (accessor.isTriggeredTxn()) {
			transactionRecord.setScheduleRef(accessor.getScheduleRef());
		}
		var grpc = transactionRecord.build();
		var record = ctx.creator().createExpiringRecord(
				effectivePayer,
				grpc,
				consensusTimestamp.getEpochSecond(),
				submittingMember);
		var recentHistory = histories.computeIfAbsent(txnId, ignore -> new TxnIdRecentHistory());
		recentHistory.observe(record, FAIL_INVALID);
	}

	public boolean isReceiptPresent(TransactionID txnId) {
		return histories.containsKey(txnId) || timedReceiptCache.getIfPresent(txnId) == MARKER;
	}

	public TransactionReceipt getPriorityReceipt(TransactionID txnId) {
		var recentHistory = histories.get(txnId);
		return recentHistory != null
				? receiptFrom(recentHistory)
				: (timedReceiptCache.getIfPresent(txnId) == MARKER ? UNKNOWN_RECEIPT : null);
	}

	public List<TransactionRecord> getDuplicateRecords(TransactionID txnId) {
		return duplicatesOf(txnId);
	}

	public List<TransactionReceipt> getDuplicateReceipts(TransactionID txnId) {
		return duplicatesOf(txnId).stream().map(TransactionRecord::getReceipt).collect(toList());
	}

	private List<TransactionRecord> duplicatesOf(TransactionID txnId) {
		var recentHistory = histories.get(txnId);
		if (recentHistory == null) {
			return Collections.emptyList();
		} else {
			return recentHistory.duplicateRecords()
					.stream()
					.map(ExpirableTxnRecord::asGrpc)
					.collect(toList());
		}
	}

	private TransactionReceipt receiptFrom(TxnIdRecentHistory recentHistory) {
		return Optional.ofNullable(recentHistory.priorityRecord())
				.map(ExpirableTxnRecord::getReceipt)
				.map(TxnReceipt::toGrpc)
				.orElse(UNKNOWN_RECEIPT);
	}

	public TransactionRecord getPriorityRecord(TransactionID txnId) {
		var history = histories.get(txnId);
		if (history != null) {
			return Optional.ofNullable(history.priorityRecord())
					.map(ExpirableTxnRecord::asGrpc)
					.orElse(null);
		}
		return null;
	}

	public void forgetAnyOtherExpiredHistory(long now) {
		while (recordExpiries.hasExpiringAt(now)) {
			var txnId = recordExpiries.expireNextAt(now);
			var history = histories.get(txnId);
			if (history != null) {
				history.forgetExpiredAt(now);
				if (history.isForgotten()) {
					histories.remove(txnId);
				}
			}
		}
	}

	public void trackForExpiry(ExpirableTxnRecord record) {
		recordExpiries.track(record.getTxnId().toGrpc(), record.getExpiry());
	}

	public void reset() {
		recordExpiries.reset();
	}
}
