package com.grame.services.grpc.controllers;

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

import com.grame.services.queries.answering.QueryResponseHelper;
import com.grame.services.queries.crypto.CryptoAnswers;
import com.grame.services.queries.meta.MetaAnswers;
import com.grame.services.txns.submission.TxnResponseHelper;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.gramegrame.api.proto.java.grameFunctionality.CryptoAddLiveHash;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoCreate;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoDelete;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoDeleteLiveHash;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoGetAccountBalance;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoGetAccountRecords;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoGetInfo;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoGetLiveHash;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoGetStakers;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoTransfer;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoUpdate;
import static com.gramegrame.api.proto.java.grameFunctionality.NONE;
import static com.gramegrame.api.proto.java.grameFunctionality.TransactionGetReceipt;
import static com.gramegrame.api.proto.java.grameFunctionality.TransactionGetRecord;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.BDDMockito.*;
import static com.grame.services.grpc.controllers.CryptoController.*;

class CryptoControllerTest {
	Query query = Query.getDefaultInstance();
	Transaction txn = Transaction.getDefaultInstance();
	MetaAnswers metaAnswers;
	CryptoAnswers cryptoAnswers;
	TxnResponseHelper txnResponseHelper;
	QueryResponseHelper queryResponseHelper;
	StreamObserver<Response> queryObserver;
	StreamObserver<TransactionResponse> txnObserver;

	CryptoController subject;

	@BeforeEach
	private void setup() {
		txnObserver = mock(StreamObserver.class);
		queryObserver = mock(StreamObserver.class);

		metaAnswers = mock(MetaAnswers.class);
		cryptoAnswers = mock(CryptoAnswers.class);
		txnResponseHelper = mock(TxnResponseHelper.class);
		queryResponseHelper = mock(QueryResponseHelper.class);

		subject = new CryptoController(metaAnswers, cryptoAnswers, txnResponseHelper, queryResponseHelper);
	}

	@Test
	public void forwardsAccountInfoAsExpected() {
		// when:
		subject.getAccountInfo(query, queryObserver);

		// expect:
		verify(cryptoAnswers).getAccountInfo();
		verify(queryResponseHelper).answer(query, queryObserver, null, CryptoGetInfo);
	}

	@Test
	public void forwardsGetBalanceAsExpected() {
		// when:
		subject.cryptoGetBalance(query, queryObserver);

		// expect:
		verify(cryptoAnswers).getAccountBalance();
		verify(queryResponseHelper).answer(query, queryObserver, null, CryptoGetAccountBalance);
	}

	@Test
	public void forwardsGetRecordsAsExpected() {
		// when:
		subject.getAccountRecords(query, queryObserver);

		// expect:
		verify(cryptoAnswers).getAccountRecords();
		verify(queryResponseHelper).answer(query, queryObserver, null, CryptoGetAccountRecords);
	}

	@Test
	public void forwardsGetStakersAsExpected() {
		// when:
		subject.getStakersByAccountID(query, queryObserver);

		// expect:
		verify(cryptoAnswers).getStakers();
		verify(queryResponseHelper).answer(query, queryObserver, null, CryptoGetStakers);
	}

	@Test
	public void forwardsGetLiveHashAsExpected() {
		// when:
		subject.getLiveHash(query, queryObserver);

		// expect:
		verify(cryptoAnswers).getLiveHash();
		verify(queryResponseHelper).answer(query, queryObserver, null, CryptoGetLiveHash);
	}

	@Test
	public void forwardsGetReceiptAsExpected() {
		// when:
		subject.getTransactionReceipts(query, queryObserver);

		// expect:
		verify(metaAnswers).getTxnReceipt();
		verify(queryResponseHelper).answer(query, queryObserver, null, TransactionGetReceipt);
	}

	@Test
	public void forwardsGetRecordAsExpected() {
		// when:
		subject.getTxRecordByTxID(query, queryObserver);

		// expect:
		verify(metaAnswers).getTxnRecord();
		verify(queryResponseHelper).answer(query, queryObserver, null, TransactionGetRecord);
	}

	@Test
	public void forwardsGetFastRecordAsExpected() {
		// when:
		subject.getFastTransactionRecord(query, queryObserver);

		// expect:
		verify(metaAnswers).getFastTxnRecord();
		verify(queryResponseHelper).answer(query, queryObserver, null, NONE);
	}

	@Test
	public void forwardsTransferAsExpected() {
		// when:
		subject.cryptoTransfer(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, CryptoTransfer);
	}

	@Test
	public void forwardsCreateAsExpected() {
		// when:
		subject.createAccount(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, CryptoCreate);
	}

	@Test
	public void forwardsDeleteAsExpected() {
		// when:
		subject.cryptoDelete(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, CryptoDelete);
	}

	@Test
	public void forwardsUpdateAsExpected() {
		// when:
		subject.updateAccount(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, CryptoUpdate);
	}

	@Test
	public void forwardsAddLiveHashAsExpected() {
		// when:
		subject.addLiveHash(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, CryptoAddLiveHash);
	}

	@Test
	public void forwardsDeleteLiveHashAsExpected() {
		// when:
		subject.deleteLiveHash(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, CryptoDeleteLiveHash);
	}
}
