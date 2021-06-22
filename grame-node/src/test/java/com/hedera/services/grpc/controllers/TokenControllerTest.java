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
import com.grame.services.queries.token.TokenAnswers;
import com.grame.services.txns.submission.TxnResponseHelper;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.grame.services.grpc.controllers.NetworkController.GET_VERSION_INFO_METRIC;
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
import static org.mockito.BDDMockito.*;

class TokenControllerTest {
	Query query = Query.getDefaultInstance();
	Transaction txn = Transaction.getDefaultInstance();

	TokenAnswers answers;
	TxnResponseHelper txnResponseHelper;
	QueryResponseHelper queryResponseHelper;
	StreamObserver<Response> queryObserver;
	StreamObserver<TransactionResponse> txnObserver;

	TokenController subject;

	@BeforeEach
	private void setup() {
		answers = mock(TokenAnswers.class);
		txnObserver = mock(StreamObserver.class);
		queryObserver = mock(StreamObserver.class);

		txnResponseHelper = mock(TxnResponseHelper.class);
		queryResponseHelper = mock(QueryResponseHelper.class);

		subject = new TokenController(answers, txnResponseHelper, queryResponseHelper);
	}

	@Test
	public void forwardTokenCreateAsExpected() {
		// when:
		subject.createToken(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, TokenCreate);
	}

	@Test
	public void forwardTokenFreezeAsExpected() {
		// when:
		subject.freezeTokenAccount(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, TokenFreezeAccount);
	}

	@Test
	public void forwardTokenUnfreezeAsExpected() {
		// when:
		subject.unfreezeTokenAccount(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, TokenUnfreezeAccount);
	}

	@Test
	public void forwardGrantKyc() {
		// when:
		subject.grantKycToTokenAccount(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, TokenGrantKycToAccount);
	}

	@Test
	public void forwardRevokeKyc() {
		// when:
		subject.revokeKycFromTokenAccount(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, TokenRevokeKycFromAccount);
	}

	@Test
	public void forwardDelete() {
		// when:
		subject.deleteToken(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, TokenDelete);
	}

	@Test
	public void forwardUpdate() {
		// when:
		subject.updateToken(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, TokenUpdate);
	}

	@Test
	public void forwardMint() {
		// when:
		subject.mintToken(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, TokenMint);
	}

	@Test
	public void forwardBurn() {
		// when:
		subject.burnToken(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, TokenBurn);
	}

	@Test
	public void forwardWipe() {
		// when:
		subject.wipeTokenAccount(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, TokenAccountWipe);
	}

	@Test
	public void forwardDissociate() {
		// when:
		subject.dissociateTokens(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, TokenDissociateFromAccount);
	}

	@Test
	public void forwardAssociate() {
		// when:
		subject.associateTokens(txn, txnObserver);

		// expect:
		verify(txnResponseHelper).submit(txn, txnObserver, TokenAssociateToAccount);
	}

	@Test
	public void forwardsTokenInfoAsExpected() {
		// when:
		subject.getTokenInfo(query, queryObserver);

		// expect:
		verify(queryResponseHelper).answer(query, queryObserver,null , TokenGetInfo);
	}
}
