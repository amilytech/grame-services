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
import com.grame.services.queries.meta.MetaAnswers;
import com.grame.services.txns.submission.TxnResponseHelper;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionResponse;
import com.gramegrame.service.proto.java.NetworkServiceGrpc;
import io.grpc.stub.StreamObserver;

import static com.gramegrame.api.proto.java.grameFunctionality.GetVersionInfo;
import static com.gramegrame.api.proto.java.grameFunctionality.UncheckedSubmit;

public class NetworkController extends NetworkServiceGrpc.NetworkServiceImplBase {
	private final MetaAnswers metaAnswers;
	private final TxnResponseHelper txnResponseHelper;
	private final QueryResponseHelper queryHelper;

	public static String GET_VERSION_INFO_METRIC = "getVersionInfo";
	public static String UNCHECKED_SUBMIT_METRIC = "uncheckedSubmit";

	public NetworkController(
			MetaAnswers metaAnswers,
			TxnResponseHelper txnResponseHelper,
			QueryResponseHelper queryHelper
	) {
		this.metaAnswers = metaAnswers;
		this.txnResponseHelper = txnResponseHelper;
		this.queryHelper = queryHelper;
	}

	@Override
	public void getVersionInfo(Query query, StreamObserver<Response> observer) {
		queryHelper.answer(query, observer, metaAnswers.getVersionInfo(), GetVersionInfo);
	}

	@Override
	public void uncheckedSubmit(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnResponseHelper.submit(signedTxn, observer, UncheckedSubmit);
	}
}
