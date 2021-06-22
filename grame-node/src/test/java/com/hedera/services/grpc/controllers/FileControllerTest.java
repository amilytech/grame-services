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
import com.grame.services.queries.file.FileAnswers;
import com.grame.services.txns.submission.TxnResponseHelper;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.gramegrame.api.proto.java.grameFunctionality.FileAppend;
import static com.gramegrame.api.proto.java.grameFunctionality.FileCreate;
import static com.gramegrame.api.proto.java.grameFunctionality.FileDelete;
import static com.gramegrame.api.proto.java.grameFunctionality.FileGetContents;
import static com.gramegrame.api.proto.java.grameFunctionality.FileGetInfo;
import static com.gramegrame.api.proto.java.grameFunctionality.FileUpdate;
import static com.gramegrame.api.proto.java.grameFunctionality.SystemDelete;
import static com.gramegrame.api.proto.java.grameFunctionality.SystemUndelete;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;

public class FileControllerTest {
	Query query = Query.getDefaultInstance();
	Transaction txn = Transaction.getDefaultInstance();
	FileAnswers answers;
	TxnResponseHelper txnResponseHelper;
	QueryResponseHelper queryResponseHelper;
	StreamObserver<Response> queryObserver;
	StreamObserver<TransactionResponse> txnObserver;

	FileController subject;

	@BeforeEach
	private void setup() {
		answers = mock(FileAnswers.class);
		txnObserver = mock(StreamObserver.class);
		queryObserver = mock(StreamObserver.class);

		txnResponseHelper = mock(TxnResponseHelper.class);
		queryResponseHelper = mock(QueryResponseHelper.class);

		subject = new FileController(answers, txnResponseHelper, queryResponseHelper);
	}

	@Test
	public void forwardsUpdateAsExpected() {
		// when:
		subject.updateFile(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, FileUpdate);
	}

	@Test
	public void forwardsCreateAsExpected() {
		// when:
		subject.createFile(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, FileCreate);
	}

	@Test
	public void forwardsDeleteAsExpected() {
		// when:
		subject.deleteFile(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, FileDelete);
	}

	@Test
	public void forwardsAppendAsExpected() {
		// when:
		subject.appendContent(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, FileAppend);
	}

	@Test
	public void forwardsSysDelAsExpected() {
		// when:
		subject.systemDelete(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, SystemDelete);
	}

	@Test
	public void forwardsSysUndelAsExpected() {
		// when:
		subject.systemUndelete(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, SystemUndelete);
	}

	@Test
	public void forwardsFileInfoAsExpected() {
		// when:
		subject.getFileInfo(query, queryObserver);

		// expect:
		verify(answers).fileInfo();
		verify(queryResponseHelper).answer(query, queryObserver, null, FileGetInfo);
	}

	@Test
	public void forwardsFileContentsAsExpected() {
		// when:
		subject.getFileContent(query, queryObserver);

		// expect:
		verify(answers).fileContents();
		verify(queryResponseHelper).answer(query, queryObserver, null, FileGetContents);
	}
}
