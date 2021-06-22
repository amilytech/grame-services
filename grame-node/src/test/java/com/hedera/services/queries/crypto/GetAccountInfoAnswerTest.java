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
import com.grame.services.legacy.core.jproto.JEd25519Key;
import com.grame.services.state.merkle.MerkleAccountTokens;
import com.grame.services.state.merkle.MerkleEntityAssociation;
import com.grame.services.state.merkle.MerkleToken;
import com.grame.services.state.merkle.MerkleTokenRelStatus;
import com.grame.services.state.submerkle.RawTokenRelationship;
import com.grame.services.store.schedule.ScheduleStore;
import com.grame.services.store.tokens.TokenStore;
import com.grame.services.txns.validation.OptionValidator;
import com.grame.test.factories.accounts.MerkleAccountFactory;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.CryptoGetInfoQuery;
import com.gramegrame.api.proto.java.CryptoGetInfoResponse;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.QueryHeader;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.api.proto.java.TokenID;
import com.gramegrame.api.proto.java.Transaction;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.state.submerkle.EntityId;
import com.grame.services.legacy.core.jproto.JKey;
import com.swirlds.fcmap.FCMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.grame.services.context.primitives.StateView.GONE_TOKEN;
import static com.grame.services.state.merkle.MerkleEntityAssociation.fromAccountTokenRel;
import static com.grame.services.utils.EntityIdUtils.asSolidityAddress;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoGetInfo;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.*;
import static com.gramegrame.api.proto.java.ResponseType.ANSWER_ONLY;
import static com.gramegrame.api.proto.java.ResponseType.COST_ANSWER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.*;
import static com.grame.test.utils.IdUtils.*;
import static com.grame.test.utils.TxnUtils.*;
import static com.grame.test.factories.scenarios.TxnHandlingScenario.COMPLEX_KEY_ACCOUNT_KT;

class GetAccountInfoAnswerTest {
	private StateView view;
	private TokenStore tokenStore;
	private ScheduleStore scheduleStore;
	private FCMap<MerkleEntityId, MerkleAccount> accounts;
	private FCMap<MerkleEntityAssociation, MerkleTokenRelStatus> tokenRels;
	private OptionValidator optionValidator;

	private String node = "0.0.3";
	private String memo = "When had I my own will?";
	private String payer = "0.0.12345";
	private AccountID payerId = IdUtils.asAccount(payer);
	private MerkleAccount payerAccount;
	private String target = payer;
	private MerkleToken token;
	private MerkleToken deletedToken;
	TokenID firstToken = tokenWith(555),
			secondToken = tokenWith(666),
			thirdToken = tokenWith(777),
			fourthToken = tokenWith(888),
			missingToken = tokenWith(999);
	long firstBalance = 123, secondBalance = 234, thirdBalance = 345, fourthBalance = 456, missingBalance = 567;

	private long fee = 1_234L;
	private Transaction paymentTxn;

	private GetAccountInfoAnswer subject;

	private NodeLocalProperties nodeProps;

	@BeforeEach
	private void setup() throws Throwable {
		tokenRels = new FCMap<>();
		tokenRels.put(
				fromAccountTokenRel(payerId, firstToken),
				new MerkleTokenRelStatus(firstBalance, true, true));
		tokenRels.put(
				fromAccountTokenRel(payerId, secondToken),
				new MerkleTokenRelStatus(secondBalance, false, false));
		tokenRels.put(
				fromAccountTokenRel(payerId, thirdToken),
				new MerkleTokenRelStatus(thirdBalance, true, true));
		tokenRels.put(
				fromAccountTokenRel(payerId, fourthToken),
				new MerkleTokenRelStatus(fourthBalance, false, false));
		tokenRels.put(
				fromAccountTokenRel(payerId, missingToken),
				new MerkleTokenRelStatus(missingBalance, false, false));

		token = mock(MerkleToken.class);
		given(token.kycKey()).willReturn(Optional.of(new JEd25519Key("kyc".getBytes())));
		given(token.freezeKey()).willReturn(Optional.of(new JEd25519Key("freeze".getBytes())));
		given(token.hasKycKey()).willReturn(true);
		given(token.hasFreezeKey()).willReturn(true);
		given(token.decimals())
				.willReturn(1).willReturn(2).willReturn(3)
				.willReturn(1).willReturn(2).willReturn(3);
		deletedToken = mock(MerkleToken.class);
		given(deletedToken.isDeleted()).willReturn(true);
		given(deletedToken.decimals()).willReturn(4);

		tokenStore = mock(TokenStore.class);
		given(tokenStore.exists(firstToken)).willReturn(true);
		given(tokenStore.exists(secondToken)).willReturn(true);
		given(tokenStore.exists(thirdToken)).willReturn(true);
		given(tokenStore.exists(fourthToken)).willReturn(true);
		given(tokenStore.exists(missingToken)).willReturn(false);
		given(tokenStore.get(firstToken)).willReturn(token);
		given(tokenStore.get(secondToken)).willReturn(token);
		given(tokenStore.get(thirdToken)).willReturn(token);
		given(tokenStore.get(fourthToken)).willReturn(deletedToken);
		given(token.symbol()).willReturn("HEYMA");
		given(deletedToken.symbol()).willReturn("THEWAY");

		scheduleStore = mock(ScheduleStore.class);

		var tokens = new MerkleAccountTokens();
		tokens.associateAll(Set.of(firstToken, secondToken, thirdToken, fourthToken, missingToken));
		payerAccount = MerkleAccountFactory.newAccount()
				.accountKeys(COMPLEX_KEY_ACCOUNT_KT)
				.memo(memo)
				.proxy(asAccount("1.2.3"))
				.senderThreshold(1_234L)
				.receiverThreshold(4_321L)
				.receiverSigRequired(true)
				.balance(555L)
				.autoRenewPeriod(1_000_000L)
				.expirationTime(9_999_999L)
				.get();
		payerAccount.setTokens(tokens);

		accounts = mock(FCMap.class);
		given(accounts.get(MerkleEntityId.fromAccountId(asAccount(target)))).willReturn(payerAccount);

		nodeProps = mock(NodeLocalProperties.class);
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

		subject = new GetAccountInfoAnswer(optionValidator);
	}

	@Test
	public void getsCostAnswerResponse() throws Throwable {
		// setup:
		Query query = validQuery(COST_ANSWER, fee, target);

		// when:
		Response response = subject.responseGiven(query, view, OK, fee);

		// then:
		assertTrue(response.hasCryptoGetInfo());
		assertEquals(OK, response.getCryptoGetInfo().getHeader().getNodeTransactionPrecheckCode());
		assertEquals(COST_ANSWER, response.getCryptoGetInfo().getHeader().getResponseType());
		assertEquals(fee, response.getCryptoGetInfo().getHeader().getCost());
	}

	@Test
	public void getsInvalidResponse() throws Throwable {
		// setup:
		Query query = validQuery(COST_ANSWER, fee, target);

		// when:
		Response response = subject.responseGiven(query, view, ACCOUNT_DELETED, fee);

		// then:
		assertTrue(response.hasCryptoGetInfo());
		assertEquals(ACCOUNT_DELETED, response.getCryptoGetInfo().getHeader().getNodeTransactionPrecheckCode());
		assertEquals(COST_ANSWER, response.getCryptoGetInfo().getHeader().getResponseType());
		assertEquals(fee, response.getCryptoGetInfo().getHeader().getCost());
	}

	@Test
	public void identifiesFailInvalid() throws Throwable {
		// setup:
		Query query = validQuery(ANSWER_ONLY, fee, target);
		// and:
		StateView view = mock(StateView.class);

		given(view.infoForAccount(any())).willReturn(Optional.empty());

		// when:
		Response response = subject.responseGiven(query, view, OK, fee);

		// then:
		assertTrue(response.hasCryptoGetInfo());
		assertEquals(FAIL_INVALID, response.getCryptoGetInfo().getHeader().getNodeTransactionPrecheckCode());
		assertEquals(ANSWER_ONLY, response.getCryptoGetInfo().getHeader().getResponseType());
	}

	@Test
	public void getsTheAccountInfo() throws Throwable {
		// setup:
		Query query = validQuery(ANSWER_ONLY, fee, target);

		// when:
		Response response = subject.responseGiven(query, view, OK, fee);

		// then:
		assertTrue(response.hasCryptoGetInfo());
		assertTrue(response.getCryptoGetInfo().hasHeader(), "Missing response header!");
		assertEquals(OK, response.getCryptoGetInfo().getHeader().getNodeTransactionPrecheckCode());
		assertEquals(ANSWER_ONLY, response.getCryptoGetInfo().getHeader().getResponseType());
		assertEquals(0, response.getCryptoGetInfo().getHeader().getCost());
		// and:
		CryptoGetInfoResponse.AccountInfo info = response.getCryptoGetInfo().getAccountInfo();
		assertEquals(asAccount(payer), info.getAccountID());
		String address = Hex.toHexString(asSolidityAddress(0, 0L, 12_345L));
		assertEquals(address, info.getContractAccountID());
		assertEquals(payerAccount.getBalance(), info.getBalance());
		assertEquals(payerAccount.getAutoRenewSecs(), info.getAutoRenewPeriod().getSeconds());
		assertEquals(payerAccount.getProxy(), EntityId.ofNullableAccountId(info.getProxyAccountID()));
		assertEquals(JKey.mapJKey(payerAccount.getKey()), info.getKey());
		assertEquals(payerAccount.isReceiverSigRequired(), info.getReceiverSigRequired());
		assertEquals(payerAccount.getExpiry(), info.getExpirationTime().getSeconds());
		assertEquals(memo, info.getMemo());
		// and:
		assertEquals(
				List.of(
						new RawTokenRelationship(
								firstBalance, 0, 0,
								firstToken.getTokenNum(), true, true).asGrpcFor(token),
						new RawTokenRelationship(
								secondBalance, 0, 0,
								secondToken.getTokenNum(), false, false).asGrpcFor(token),
						new RawTokenRelationship(
								thirdBalance, 0, 0,
								thirdToken.getTokenNum(), true, true).asGrpcFor(token),
						new RawTokenRelationship(
								fourthBalance, 0, 0,
								fourthToken.getTokenNum(), false, false).asGrpcFor(deletedToken),
						new RawTokenRelationship(
								missingBalance, 0, 0,
								missingToken.getTokenNum(), false, false).asGrpcFor(GONE_TOKEN)),

				info.getTokenRelationshipsList());
	}

	@Test
	public void usesValidator() throws Throwable {
		// setup:
		Query query = validQuery(COST_ANSWER, fee, target);

		given(optionValidator.queryableAccountStatus(asAccount(target), accounts)).willReturn(ACCOUNT_DELETED);

		// when:
		ResponseCodeEnum validity = subject.checkValidity(query, view);

		// then:
		assertEquals(ACCOUNT_DELETED, validity);
		// and:
		verify(optionValidator).queryableAccountStatus(any(), any());
	}

	@Test
	public void getsExpectedPayment() throws Throwable {
		// given:
		Query query = validQuery(COST_ANSWER, fee, target);

		// expect:
		assertEquals(paymentTxn, subject.extractPaymentFrom(query).get().getBackwardCompatibleSignedTxn());
	}

	@Test
	public void requiresAnswerOnlyCostAsExpected() throws Throwable {
		// expect:
		assertTrue(subject.needsAnswerOnlyCost(validQuery(COST_ANSWER, 0, target)));
		assertFalse(subject.needsAnswerOnlyCost(validQuery(ANSWER_ONLY, 0, target)));
	}

	@Test
	public void requiresAnswerOnlyPayment() throws Throwable {
		// expect:
		assertFalse(subject.requiresNodePayment(validQuery(COST_ANSWER, 0, target)));
		assertTrue(subject.requiresNodePayment(validQuery(ANSWER_ONLY, 0, target)));
	}

	@Test
	public void getsValidity() {
		// given:
		Response response = Response.newBuilder().setCryptoGetInfo(
				CryptoGetInfoResponse.newBuilder()
						.setHeader(subject.answerOnlyHeader(RESULT_SIZE_LIMIT_EXCEEDED))).build();

		// expect:
		assertEquals(RESULT_SIZE_LIMIT_EXCEEDED, subject.extractValidityFrom(response));
	}

	@Test
	public void recognizesFunction() {
		// expect:
		assertEquals(CryptoGetInfo, subject.canonicalFunction());
	}

	private Query validQuery(ResponseType type, long payment, String idLit) throws Throwable {
		this.paymentTxn = payerSponsoredTransfer(payer, COMPLEX_KEY_ACCOUNT_KT, node, payment);
		QueryHeader.Builder header = QueryHeader.newBuilder()
				.setPayment(this.paymentTxn)
				.setResponseType(type);
		CryptoGetInfoQuery.Builder op = CryptoGetInfoQuery.newBuilder()
				.setHeader(header)
				.setAccountID(asAccount(idLit));
		return Query.newBuilder().setCryptoGetInfo(op).build();
	}
}
