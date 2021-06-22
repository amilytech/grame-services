package com.grame.services.queries.answering;

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

import com.grame.services.context.properties.NodeLocalProperties;
import com.grame.services.context.properties.PropertySource;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.context.primitives.StateView;
import com.grame.services.records.RecordCache;
import com.grame.test.factories.accounts.MerkleAccountFactory;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.TransactionGetRecordQuery;
import com.gramegrame.api.proto.java.TransactionID;
import com.gramegrame.api.proto.java.TransactionReceipt;
import com.gramegrame.api.proto.java.TransactionRecord;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.state.submerkle.ExpirableTxnRecord;
import com.swirlds.fcmap.FCMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.grame.test.utils.TxnUtils.withAdjustments;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.ACCOUNT_REPEATED_IN_ACCOUNT_AMOUNTS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.*;
import static com.grame.test.utils.IdUtils.*;
import static com.grame.services.state.serdes.DomainSerdesTest.recordOne;

class AnswerFunctionsTest {
	private String payer = "0.0.12345";
	private TransactionID targetTxnId = TransactionID.newBuilder()
			.setAccountID(asAccount(payer))
			.setTransactionValidStart(Timestamp.newBuilder().setSeconds(1_234L))
			.build();
	private TransactionID missingTxnId = TransactionID.newBuilder()
			.setAccountID(asAccount(payer))
			.setTransactionValidStart(Timestamp.newBuilder().setSeconds(4_321L))
			.build();
	private TransactionID absentTxnId = TransactionID.newBuilder()
			.setAccountID(asAccount("3.2.1"))
			.setTransactionValidStart(Timestamp.newBuilder().setSeconds(4_321L))
			.build();

	private ExpirableTxnRecord targetRecord = constructTargetRecord();
	private TransactionRecord cachedTargetRecord = targetRecord.asGrpc();
	private MerkleAccount payerAccount;
	private String target = payer;
	private long fee = 1_234L;
	private StateView view;
	private RecordCache recordCache;
	private FCMap<MerkleEntityId, MerkleAccount> accounts;

	private NodeLocalProperties nodeProps;

	private AnswerFunctions subject;

	@BeforeEach
	private void setup() {
		payerAccount = MerkleAccountFactory.newAccount().get();
		payerAccount.records().offer(recordOne());
		payerAccount.records().offer(targetRecord);

		accounts = mock(FCMap.class);
		given(accounts.get(MerkleEntityId.fromAccountId(asAccount(target)))).willReturn(payerAccount);
		nodeProps = mock(NodeLocalProperties.class);
		view = new StateView(StateView.EMPTY_TOPICS_SUPPLIER, () -> accounts, nodeProps, null);

		recordCache = mock(RecordCache.class);

		subject = new AnswerFunctions();
	}

	@Test
	public void returnsEmptyOptionalWhenProblematic() {
		// setup:
		Query validQuery = getRecordQuery(absentTxnId);

		given(recordCache.getPriorityRecord(absentTxnId)).willReturn(null);

		// when:
		Optional<TransactionRecord> record = subject.txnRecord(recordCache, view, validQuery);

		// then:
		assertFalse(record.isPresent());
	}

	@Test
	public void findsInPayerAccountIfPresentThere() {
		// setup:
		Query validQuery = getRecordQuery(targetTxnId);

		given(recordCache.getPriorityRecord(targetTxnId)).willReturn(null);

		// when:
		Optional<TransactionRecord> record = subject.txnRecord(recordCache, view, validQuery);

		// then:
		assertEquals(cachedTargetRecord, record.get());
	}

	@Test
	public void usesCacheIfPresentThere() {
		// setup:
		Query validQuery = getRecordQuery(targetTxnId);

		given(recordCache.getPriorityRecord(targetTxnId)).willReturn(cachedTargetRecord);

		// when:
		Optional<TransactionRecord> record = subject.txnRecord(recordCache, view, validQuery);

		// then:
		assertEquals(cachedTargetRecord, record.get());
		verify(accounts, never()).get(any());
		verify(recordCache, never()).isReceiptPresent(any());
	}

	ExpirableTxnRecord constructTargetRecord() {
		TransactionRecord record = TransactionRecord.newBuilder()
				.setReceipt(TransactionReceipt.newBuilder().setStatus(ACCOUNT_REPEATED_IN_ACCOUNT_AMOUNTS))
				.setTransactionID(targetTxnId)
				.setMemo("Dim galleries, dusk winding stairs got past...")
				.setConsensusTimestamp(Timestamp.newBuilder().setSeconds(9_999_999_999L))
				.setTransactionFee(555L)
				.setTransferList(withAdjustments(
						asAccount("0.0.2"), -2L,
						asAccount("0.0.2"), -2L,
						asAccount("0.0.1001"), 2L,
						asAccount("0.0.1002"), 2L))
				.build();
		return ExpirableTxnRecord.fromGprc(record);
	}

	Query getRecordQuery(TransactionID txnId) {
		TransactionGetRecordQuery.Builder op = TransactionGetRecordQuery.newBuilder().setTransactionID(txnId);
		return Query.newBuilder().setTransactionGetRecord(op).build();
	}
}
