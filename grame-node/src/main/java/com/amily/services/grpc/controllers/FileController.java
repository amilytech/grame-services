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
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionResponse;
import com.gramegrame.service.proto.java.FileServiceGrpc;
import io.grpc.stub.StreamObserver;

import static com.gramegrame.api.proto.java.grameFunctionality.FileAppend;
import static com.gramegrame.api.proto.java.grameFunctionality.FileCreate;
import static com.gramegrame.api.proto.java.grameFunctionality.FileDelete;
import static com.gramegrame.api.proto.java.grameFunctionality.FileGetContents;
import static com.gramegrame.api.proto.java.grameFunctionality.FileGetInfo;
import static com.gramegrame.api.proto.java.grameFunctionality.FileUpdate;
import static com.gramegrame.api.proto.java.grameFunctionality.NONE;
import static com.gramegrame.api.proto.java.grameFunctionality.SystemDelete;
import static com.gramegrame.api.proto.java.grameFunctionality.SystemUndelete;

public class FileController extends FileServiceGrpc.FileServiceImplBase {
	private final FileAnswers fileAnswers;
	private final TxnResponseHelper txnHelper;
	private final QueryResponseHelper queryHelper;

	public static final String GET_FILE_INFO_METRIC = "getFileInfo";
	public static final String GET_FILE_CONTENT_METRIC = "getFileContent";
	public static final String UPDATE_FILE_METRIC = "updateFile";
	public static final String CREATE_FILE_METRIC = "createFile";
	public static final String DELETE_FILE_METRIC = "deleteFile";
	public static final String FILE_APPEND_METRIC = "appendContent";

	public FileController(
			FileAnswers fileAnswers,
			TxnResponseHelper txnHelper,
			QueryResponseHelper queryHelper
	) {
		this.txnHelper = txnHelper;
		this.queryHelper = queryHelper;
		this.fileAnswers = fileAnswers;
	}

	@Override
	public void updateFile(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, FileUpdate);
	}

	@Override
	public void createFile(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, FileCreate);
	}

	@Override
	public void deleteFile(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, FileDelete);
	}

	@Override
	public void appendContent(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, FileAppend);
	}

	@Override
	public void systemDelete(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, SystemDelete);
	}

	@Override
	public void systemUndelete(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, SystemUndelete);
	}

	@Override
	public void getFileInfo(Query query, StreamObserver<Response> observer) {
		queryHelper.answer(query, observer, fileAnswers.fileInfo(), FileGetInfo);
	}

	@Override
	public void getFileContent(Query query, StreamObserver<Response> observer) {
		queryHelper.answer(query, observer, fileAnswers.fileContents(), FileGetContents);
	}
}
