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
import com.grame.services.queries.token.TokenAnswers;
import com.grame.services.txns.submission.TxnResponseHelper;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionResponse;
import com.gramegrame.service.proto.java.TokenServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.gramegrame.api.proto.java.grameFunctionality.TokenAccountWipe;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenAssociateToAccount;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenBurn;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenCreate;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenDelete;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenDissociateFromAccount;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenFreezeAccount;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenGetInfo;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenGrantKycToAccount;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenMint;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenRevokeKycFromAccount;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenUnfreezeAccount;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenUpdate;

public class TokenController extends TokenServiceGrpc.TokenServiceImplBase {
	private static final Logger log = LogManager.getLogger(TokenController.class);

	private final TokenAnswers tokenAnswers;
	private final TxnResponseHelper txnHelper;
	private final QueryResponseHelper queryHelper;

	public TokenController(
			TokenAnswers tokenAnswers,
			TxnResponseHelper txnHelper,
			QueryResponseHelper queryHelper
	) {
		this.txnHelper = txnHelper;
		this.queryHelper = queryHelper;
		this.tokenAnswers = tokenAnswers;
	}

	@Override
	public void createToken(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, TokenCreate);
	}

	@Override
	public void deleteToken(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, TokenDelete);
	}

	@Override
	public void mintToken(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, TokenMint);
	}

	@Override
	public void burnToken(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, TokenBurn);
	}

	@Override
	public void updateToken(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, TokenUpdate);
	}

	@Override
	public void wipeTokenAccount(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, TokenAccountWipe);
	}

	@Override
	public void freezeTokenAccount(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, TokenFreezeAccount);
	}

	@Override
	public void unfreezeTokenAccount(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, TokenUnfreezeAccount);
	}

	@Override
	public void grantKycToTokenAccount(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, TokenGrantKycToAccount);
	}

	@Override
	public void revokeKycFromTokenAccount(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, TokenRevokeKycFromAccount);
	}

	@Override
	public void associateTokens(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, TokenAssociateToAccount);
	}

	@Override
	public void dissociateTokens(Transaction signedTxn, StreamObserver<TransactionResponse> observer) {
		txnHelper.submit(signedTxn, observer, TokenDissociateFromAccount);
	}

	@Override
	public void getTokenInfo(Query query, StreamObserver<Response> observer) {
		queryHelper.answer(query, observer, tokenAnswers.getTokenInfo(), TokenGetInfo);
	}
}
