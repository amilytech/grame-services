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
import com.grame.services.queries.contract.ContractAnswers;
import com.grame.services.txns.submission.TxnResponseHelper;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionResponse;
import com.gramegrame.service.proto.java.SmartContractServiceGrpc;
import io.grpc.stub.StreamObserver;

import static com.gramegrame.api.proto.java.grameFunctionality.ContractCall;
import static com.gramegrame.api.proto.java.grameFunctionality.ContractCallLocal;
import static com.gramegrame.api.proto.java.grameFunctionality.ContractCreate;
import static com.gramegrame.api.proto.java.grameFunctionality.ContractDelete;
import static com.gramegrame.api.proto.java.grameFunctionality.ContractGetBytecode;
import static com.gramegrame.api.proto.java.grameFunctionality.ContractGetInfo;
import static com.gramegrame.api.proto.java.grameFunctionality.ContractGetRecords;
import static com.gramegrame.api.proto.java.grameFunctionality.ContractUpdate;
import static com.gramegrame.api.proto.java.grameFunctionality.GetBySolidityID;
import static com.gramegrame.api.proto.java.grameFunctionality.SystemDelete;
import static com.gramegrame.api.proto.java.grameFunctionality.SystemUndelete;

public class ContractController extends SmartContractServiceGrpc.SmartContractServiceImplBase {
	/* Transactions */
	public static final String CALL_CONTRACT_METRIC = "contractCallMethod";
	public static final String CREATE_CONTRACT_METRIC = "createContract";
	public static final String UPDATE_CONTRACT_METRIC = "updateContract";
	public static final String DELETE_CONTRACT_METRIC = "deleteContract";
	/* Queries */
	public static final String GET_CONTRACT_INFO_METRIC = "getContractInfo";
	public static final String LOCALCALL_CONTRACT_METRIC = "contractCallLocalMethod";
	public static final String GET_CONTRACT_RECORDS_METRIC = "getTxRecordByContractID";
	public static final String GET_CONTRACT_BYTECODE_METRIC = "ContractGetBytecode";
	public static final String GET_SOLIDITY_ADDRESS_INFO_METRIC = "getBySolidityID";

	private final ContractAnswers contractAnswers;
	private final TxnResponseHelper txnHelper;
	private final QueryResponseHelper queryHelper;

	public ContractController(
			ContractAnswers contractAnswers,
			TxnResponseHelper txnHelper,
			QueryResponseHelper queryHelper
	) {
		this.txnHelper = txnHelper;
		this.queryHelper = queryHelper;
		this.contractAnswers = contractAnswers;
	}

	@Override
	public void createContract(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, ContractCreate);
	}

	@Override
	public void updateContract(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, ContractUpdate);
	}

	@Override
	public void contractCallMethod(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, ContractCall);
	}

	@Override
	public void getContractInfo(Query query, StreamObserver<Response> observer) {
		queryHelper.answer(query, observer, contractAnswers.getContractInfo(), ContractGetInfo);
	}

	@Override
	public void contractCallLocalMethod(Query query, StreamObserver<Response> observer) {
		queryHelper.answer(query, observer, contractAnswers.contractCallLocal(), ContractCallLocal);
	}

	@Override
	public void contractGetBytecode(Query query, StreamObserver<Response> observer) {
		queryHelper.answer(query, observer, contractAnswers.getBytecode(), ContractGetBytecode);
	}

	@Override
	public void getBySolidityID(Query query, StreamObserver<Response> observer) {
		queryHelper.answer(query, observer, contractAnswers.getBySolidityId(), GetBySolidityID);
	}

	@Override
	public void getTxRecordByContractID(Query query, StreamObserver<Response> observer) {
		queryHelper.answer(query, observer, contractAnswers.getContractRecords(), ContractGetRecords);
	}

	@Override
	public void deleteContract(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, ContractDelete);
	}

	@Override
	public void systemDelete(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, SystemDelete);
	}

	@Override
	public void systemUndelete(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, SystemUndelete);
	}
}
