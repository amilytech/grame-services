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
import com.grame.services.queries.consensus.HcsAnswers;
import com.grame.services.txns.submission.TxnResponseHelper;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionResponse;
import com.gramegrame.service.proto.java.ConsensusServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusCreateTopic;
import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusDeleteTopic;
import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusGetTopicInfo;
import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusSubmitMessage;
import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusUpdateTopic;

public class ConsensusController extends ConsensusServiceGrpc.ConsensusServiceImplBase {
	private static final Logger log = LogManager.getLogger(ConsensusController.class);

	private final HcsAnswers hcsAnswers;
	private final TxnResponseHelper txnHelper;
	private final QueryResponseHelper queryHelper;

	public static final String GET_TOPIC_INFO_METRIC = "getTopicInfo";
	public static final String CREATE_TOPIC_METRIC = "createTopic";
	public static final String UPDATE_TOPIC_METRIC = "updateTopic";
	public static final String DELETE_TOPIC_METRIC = "deleteTopic";
	public static final String SUBMIT_MESSAGE_METRIC = "submitMessage";

	public ConsensusController(
			HcsAnswers hcsAnswers,
			TxnResponseHelper txnHelper,
			QueryResponseHelper queryHelper
	) {
		this.hcsAnswers = hcsAnswers;
		this.txnHelper = txnHelper;
		this.queryHelper = queryHelper;
	}

	@Override
	public void getTopicInfo(Query query, StreamObserver<Response> observer) {
		queryHelper.answer(query, observer, hcsAnswers.topicInfo(), ConsensusGetTopicInfo);
	}

	@Override
	public void createTopic(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, ConsensusCreateTopic);
	}

	@Override
	public void updateTopic(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, ConsensusUpdateTopic);
	}

	@Override
	public void deleteTopic(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, ConsensusDeleteTopic);
	}

	@Override
	public void submitMessage(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, ConsensusSubmitMessage);
	}
}
