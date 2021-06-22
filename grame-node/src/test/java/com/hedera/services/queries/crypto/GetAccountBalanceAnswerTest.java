package com.grame.services.queries.crypto;

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

import com.grame.services.context.primitives.StateView;
import com.grame.services.context.properties.NodeLocalProperties;
import com.grame.services.context.properties.PropertySource;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.state.merkle.MerkleEntityAssociation;
import com.grame.services.state.merkle.MerkleToken;
import com.grame.services.state.merkle.MerkleTokenRelStatus;
import com.grame.services.store.schedule.ScheduleStore;
import com.grame.services.store.tokens.TokenStore;
import com.grame.services.txns.validation.OptionValidator;
import com.grame.test.factories.accounts.MerkleAccountFactory;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.ContractID;
import com.gramegrame.api.proto.java.CryptoGetAccountBalanceQuery;
import com.gramegrame.api.proto.java.CryptoGetAccountBalanceResponse;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.QueryHeader;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.api.proto.java.TokenID;
import com.swirlds.fcmap.FCMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.grame.services.state.merkle.MerkleEntityAssociation.fromAccountTokenRel;
import static com.grame.services.state.merkle.MerkleEntityId.fromAccountId;
import static com.grame.services.state.merkle.MerkleEntityId.fromContractId;
import static com.grame.test.utils.IdUtils.asAccount;
import static com.grame.test.utils.IdUtils.asContract;
import static com.grame.test.utils.IdUtils.tokenBalanceWith;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoGetAccountBalance;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.ACCOUNT_DELETED;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.CONTRACT_DELETED;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_ACCOUNT_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.PLATFORM_NOT_ACTIVE;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.RESULT_SIZE_LIMIT_EXCEEDED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

public class GetAccountBalanceAnswerTest {
	private FCMap accounts;
	private FCMap<MerkleEntityAssociation, MerkleTokenRelStatus> tokenRels;
	private StateView view;
	private OptionValidator optionValidator;
	private String accountIdLit = "0.0.12345";
	private AccountID target = asAccount(accountIdLit);
	private String contractIdLit = "0.0.12346";
	private long balance = 1_234L;
	private long aBalance = 345;
	private long bBalance = 456;
	private long cBalance = 567;
	private long dBalance = 678;
	private TokenID aToken = IdUtils.asToken("0.0.3");
	private TokenID bToken = IdUtils.asToken("0.0.4");
	private TokenID cToken = IdUtils.asToken("0.0.5");
	private TokenID dToken = IdUtils.asToken("0.0.6");
	TokenStore tokenStore;
	ScheduleStore scheduleStore;

	MerkleToken notDeleted, deleted;
	private MerkleAccount accountV = MerkleAccountFactory.newAccount()
			.balance(balance)
			.tokens(aToken, bToken, cToken, dToken)
			.get();
	private MerkleAccount contractV = MerkleAccountFactory.newContract().balance(balance).get();

	private GetAccountBalanceAnswer subject;
	private NodeLocalProperties nodeProps;

	@BeforeEach
	private void setup() {
		deleted = mock(MerkleToken.class);
		given(deleted.isDeleted()).willReturn(true);
		given(deleted.decimals()).willReturn(123);
		notDeleted = mock(MerkleToken.class);
		given(notDeleted.isDeleted()).willReturn(false);
		given(notDeleted.decimals()).willReturn(1).willReturn(2);

		tokenRels = new FCMap<>();
		tokenRels.put(
				fromAccountTokenRel(target, aToken),
				new MerkleTokenRelStatus(aBalance, true, true));
		tokenRels.put(
				fromAccountTokenRel(target, bToken),
				new MerkleTokenRelStatus(bBalance, false, false));
		tokenRels.put(
				fromAccountTokenRel(target, cToken),
				new MerkleTokenRelStatus(cBalance, false, false));
		tokenRels.put(
				fromAccountTokenRel(target, dToken),
				new MerkleTokenRelStatus(dBalance, false, false));

		accounts = mock(FCMap.class);
		nodeProps = mock(NodeLocalProperties.class);
		given(accounts.get(fromAccountId(asAccount(accountIdLit)))).willReturn(accountV);
		given(accounts.get(fromContractId(asContract(contractIdLit)))).willReturn(contractV);

		tokenStore = mock(TokenStore.class);
		given(tokenStore.exists(aToken)).willReturn(true);
		given(tokenStore.exists(bToken)).willReturn(true);
		given(tokenStore.exists(cToken)).willReturn(true);
		given(tokenStore.exists(dToken)).willReturn(false);
		given(tokenStore.get(aToken)).willReturn(notDeleted);
		given(tokenStore.get(bToken)).willReturn(notDeleted);
		given(tokenStore.get(cToken)).willReturn(deleted);

		scheduleStore = mock(ScheduleStore.class);

		view = new StateView(
				tokenStore,
				scheduleStore,
				StateView.EMPTY_TOPICS_SUPPLIER,
				() -> accounts,
				StateView.EMPTY_STORAGE_SUPPLIER,
				() -> tokenRels,
				null,
				nodeProps);

		optionValidator = mock(OptionValidator.class);
		subject = new GetAccountBalanceAnswer(optionValidator);
	}

	@Test
	public void requiresNothing() {
		// setup:
		CryptoGetAccountBalanceQuery costAnswerOp = CryptoGetAccountBalanceQuery.newBuilder()
				.setHeader(QueryHeader.newBuilder().setResponseType(ResponseType.COST_ANSWER))
				.build();
		Query costAnswerQuery = Query.newBuilder().setCryptogetAccountBalance(costAnswerOp).build();
		CryptoGetAccountBalanceQuery answerOnlyOp = CryptoGetAccountBalanceQuery.newBuilder()
				.setHeader(QueryHeader.newBuilder().setResponseType(ResponseType.ANSWER_ONLY))
				.build();
		Query answerOnlyQuery = Query.newBuilder().setCryptogetAccountBalance(answerOnlyOp).build();

		// expect:
		assertFalse(subject.requiresNodePayment(costAnswerQuery));
		assertFalse(subject.requiresNodePayment(answerOnlyQuery));
		assertFalse(subject.needsAnswerOnlyCost(answerOnlyQuery));
		assertFalse(subject.needsAnswerOnlyCost(costAnswerQuery));
	}

	@Test
	public void hasNoPayment() {
		// expect:
		assertFalse(subject.extractPaymentFrom(mock(Query.class)).isPresent());
	}

	@Test
	public void syntaxCheckRequiresId() {
		// given:
		CryptoGetAccountBalanceQuery op = CryptoGetAccountBalanceQuery.newBuilder().build();
		Query query = Query.newBuilder().setCryptogetAccountBalance(op).build();

		// when:
		ResponseCodeEnum status = subject.checkValidity(query, view);

		// expect:
		assertEquals(INVALID_ACCOUNT_ID, status);
	}

	@Test
	public void syntaxCheckValidatesCidIfPresent() {
		// setup:
		ContractID cid = asContract(contractIdLit);

		// given:
		CryptoGetAccountBalanceQuery op = CryptoGetAccountBalanceQuery.newBuilder()
				.setContractID(cid)
				.build();
		Query query = Query.newBuilder().setCryptogetAccountBalance(op).build();
		// and:
		given(optionValidator.queryableContractStatus(cid, accounts)).willReturn(CONTRACT_DELETED);

		// when:
		ResponseCodeEnum status = subject.checkValidity(query, view);

		// expect:
		assertEquals(CONTRACT_DELETED, status);
	}

	@Test
	public void getsValidity() {
		// given:
		Response response = Response.newBuilder().setCryptogetAccountBalance(
				CryptoGetAccountBalanceResponse.newBuilder()
						.setHeader(subject.answerOnlyHeader(RESULT_SIZE_LIMIT_EXCEEDED))).build();

		// expect:
		assertEquals(RESULT_SIZE_LIMIT_EXCEEDED, subject.extractValidityFrom(response));
	}

	@Test
	public void requiresOkMetaValidity() {
		// setup:
		AccountID id = asAccount(accountIdLit);

		// given:
		CryptoGetAccountBalanceQuery op = CryptoGetAccountBalanceQuery.newBuilder()
				.setAccountID(id)
				.build();
		Query query = Query.newBuilder().setCryptogetAccountBalance(op).build();

		// when:
		Response response = subject.responseGiven(query, view, PLATFORM_NOT_ACTIVE);
		ResponseCodeEnum status = response.getCryptogetAccountBalance()
				.getHeader()
				.getNodeTransactionPrecheckCode();

		// expect:
		assertEquals(PLATFORM_NOT_ACTIVE, status);
		assertEquals(id, response.getCryptogetAccountBalance().getAccountID());
	}

	@Test
	public void syntaxCheckValidatesIdIfPresent() {
		// setup:
		AccountID id = asAccount(accountIdLit);

		// given:
		CryptoGetAccountBalanceQuery op = CryptoGetAccountBalanceQuery.newBuilder()
				.setAccountID(id)
				.build();
		Query query = Query.newBuilder().setCryptogetAccountBalance(op).build();
		// and:
		given(optionValidator.queryableAccountStatus(id, accounts))
				.willReturn(ACCOUNT_DELETED);

		// when:
		ResponseCodeEnum status = subject.checkValidity(query, view);

		// expect:
		assertEquals(ACCOUNT_DELETED, status);
	}

	@Test
	public void answersWithAccountBalance() {
		// setup:
		AccountID id = asAccount(accountIdLit);

		// given:
		CryptoGetAccountBalanceQuery op = CryptoGetAccountBalanceQuery.newBuilder()
				.setAccountID(id)
				.build();
		Query query = Query.newBuilder().setCryptogetAccountBalance(op).build();

		// when:
		Response response = subject.responseGiven(query, view, OK);
		ResponseCodeEnum status = response.getCryptogetAccountBalance()
				.getHeader()
				.getNodeTransactionPrecheckCode();
		long answer = response.getCryptogetAccountBalance().getBalance();

		// expect:
		assertTrue(response.getCryptogetAccountBalance().hasHeader(), "Missing response header!");
		assertEquals(
				List.of(tokenBalanceWith(aToken, aBalance, 1),
						tokenBalanceWith(bToken, bBalance, 2),
						tokenBalanceWith(cToken, cBalance, 123),
						tokenBalanceWith(dToken, dBalance, 0)
				),
				response.getCryptogetAccountBalance().getTokenBalancesList());
		assertEquals(OK, status);
		assertEquals(balance, answer);
		assertEquals(id, response.getCryptogetAccountBalance().getAccountID());
	}

	@Test
	public void recognizesFunction() {
		// expect:
		assertEquals(CryptoGetAccountBalance, subject.canonicalFunction());
	}
}
