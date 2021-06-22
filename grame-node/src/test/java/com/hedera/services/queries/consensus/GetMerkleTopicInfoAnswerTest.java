package com.grame.services.queries.consensus;

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

import com.google.protobuf.ByteString;
import com.grame.services.context.properties.NodeLocalProperties;
import com.grame.services.context.properties.PropertySource;
import com.grame.services.state.merkle.MerkleTopic;
import com.grame.services.context.primitives.StateView;
import com.grame.services.txns.validation.OptionValidator;
import com.grame.test.factories.topics.TopicFactory;
import com.gramegrame.api.proto.java.ConsensusGetTopicInfoQuery;
import com.gramegrame.api.proto.java.ConsensusGetTopicInfoResponse;
import com.gramegrame.api.proto.java.ConsensusTopicInfo;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Key;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.QueryHeader;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.ResponseHeader;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.api.proto.java.TopicID;
import com.gramegrame.api.proto.java.Transaction;
import com.grame.services.state.merkle.MerkleEntityId;
import com.swirlds.fcmap.FCMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.grame.test.factories.scenarios.TxnHandlingScenario.COMPLEX_KEY_ACCOUNT_KT;
import static com.grame.test.utils.TxnUtils.payerSponsoredTransfer;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TOPIC_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.PLATFORM_NOT_ACTIVE;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.TOPIC_EXPIRED;
import static com.gramegrame.api.proto.java.ResponseType.ANSWER_ONLY;
import static com.gramegrame.api.proto.java.ResponseType.COST_ANSWER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static com.grame.test.utils.IdUtils.*;
import static com.grame.test.factories.scenarios.TxnHandlingScenario.MISC_ACCOUNT_KT;

class GetMerkleTopicInfoAnswerTest {
	long seqNo = 1_234L;
	FCMap topics;
	byte[] hash = "NOT A HASH".getBytes();
	StateView view;
	OptionValidator optionValidator;
	Key adminKey, submitKey;
	long fee = 1_234L;
	String id = "1.2.3";
	String node = "0.0.3";
	String payer = "0.0.12345";
	String target = "3.2.1";
	String memo = "This was Mr. Bleaney's room...";
	String idLit = "0.0.12345";
	long expiry = 1_234_567L;
	long duration = 55L;
	MerkleTopic merkleTopic;
	private Transaction paymentTxn;

	GetTopicInfoAnswer subject;
	NodeLocalProperties nodeProps;

	@BeforeEach
	private void setup() throws Exception {
		adminKey = COMPLEX_KEY_ACCOUNT_KT.asKey();
		submitKey = MISC_ACCOUNT_KT.asKey();
		topics = mock(FCMap.class);
		merkleTopic = TopicFactory.newTopic()
				.adminKey(adminKey)
				.submitKey(submitKey)
				.memo(memo)
				.expiry(expiry)
				.deleted(false)
				.autoRenewDuration(duration)
				.autoRenewId(asAccount(id))
				.get();
		merkleTopic.setRunningHash(hash);
		merkleTopic.setSequenceNumber(seqNo);
		MerkleEntityId key = MerkleEntityId.fromTopicId(asTopic(target));
		given(topics.get(key)).willReturn(merkleTopic);

		nodeProps = mock(NodeLocalProperties.class);
		view = new StateView(() -> topics, StateView.EMPTY_ACCOUNTS_SUPPLIER, nodeProps, null);
		optionValidator = mock(OptionValidator.class);

		subject = new GetTopicInfoAnswer(optionValidator);
	}

	@Test
	public void syntaxCheckRequiresId() {
		// given:
		ConsensusGetTopicInfoQuery op = ConsensusGetTopicInfoQuery.newBuilder().build();
		Query query = Query.newBuilder().setConsensusGetTopicInfo(op).build();

		// when:
		ResponseCodeEnum status = subject.checkValidity(query, view);

		// expect:
		assertEquals(INVALID_TOPIC_ID, status);
	}

	@Test
	public void requiresOkMetaValidity() {
		// setup:
		TopicID id = asTopic(idLit);

		// given:
		ConsensusGetTopicInfoQuery op = ConsensusGetTopicInfoQuery.newBuilder()
				.setTopicID(id)
				.build();
		Query query = Query.newBuilder().setConsensusGetTopicInfo(op).build();

		// when:
		Response response = subject.responseGiven(query, view, PLATFORM_NOT_ACTIVE);
		ResponseCodeEnum status = response.getConsensusGetTopicInfo()
				.getHeader()
				.getNodeTransactionPrecheckCode();

		// expect:
		assertEquals(PLATFORM_NOT_ACTIVE, status);
		assertEquals(id, response.getConsensusGetTopicInfo().getTopicID());
	}

	@Test
	public void syntaxCheckValidatesTidIfPresent() {
		// setup:
		TopicID tid = asTopic(idLit);

		// given:
		ConsensusGetTopicInfoQuery op = ConsensusGetTopicInfoQuery.newBuilder()
				.setTopicID(tid)
				.build();
		Query query = Query.newBuilder().setConsensusGetTopicInfo(op).build();
		// and:
		given(optionValidator.queryableTopicStatus(tid, topics)).willReturn(TOPIC_EXPIRED);

		// when:
		ResponseCodeEnum status = subject.checkValidity(query, view);

		// expect:
		assertEquals(TOPIC_EXPIRED, status);
	}

	@Test
	public void getsCostAnswerResponse() throws Throwable {
		// setup:
		Query query = validQuery(COST_ANSWER, fee, target);

		// when:
		Response response = subject.responseGiven(query, view, OK, fee);

		// then:
		assertTrue(response.hasConsensusGetTopicInfo());
		assertEquals(OK, response.getConsensusGetTopicInfo().getHeader().getNodeTransactionPrecheckCode());
		assertEquals(COST_ANSWER, response.getConsensusGetTopicInfo().getHeader().getResponseType());
		assertEquals(fee, response.getConsensusGetTopicInfo().getHeader().getCost());
	}

	@Test
	public void getsValidity() {
		// given:
		Response response = Response.newBuilder()
				.setConsensusGetTopicInfo(
						ConsensusGetTopicInfoResponse.newBuilder()
								.setHeader(ResponseHeader.newBuilder()
										.setNodeTransactionPrecheckCode(TOPIC_EXPIRED))).build();

		// expect:
		assertEquals(TOPIC_EXPIRED, subject.extractValidityFrom(response));
	}


	@Test
	public void recognizesFunction() {
		// expect:
		assertEquals(grameFunctionality.ConsensusGetTopicInfo, subject.canonicalFunction());
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
	public void getsExpectedPayment() throws Throwable {
		// given:
		Query query = validQuery(COST_ANSWER, fee, target);

		// expect:
		assertEquals(paymentTxn, subject.extractPaymentFrom(query).get().getBackwardCompatibleSignedTxn());
	}

	@Test
	public void getsTheTopicInfo() throws Throwable {
		// setup:
		Query query = validQuery(ANSWER_ONLY, fee, target);

		// when:
		Response response = subject.responseGiven(query, view, OK, fee);

		// then:
		assertTrue(response.hasConsensusGetTopicInfo());
		assertEquals(OK, response.getConsensusGetTopicInfo().getHeader().getNodeTransactionPrecheckCode());
		assertEquals(ANSWER_ONLY, response.getConsensusGetTopicInfo().getHeader().getResponseType());
		assertEquals(0, response.getConsensusGetTopicInfo().getHeader().getCost());
		assertEquals(asTopic(target), response.getConsensusGetTopicInfo().getTopicID());
		// and:
		ConsensusTopicInfo info = response.getConsensusGetTopicInfo().getTopicInfo();
		assertEquals(adminKey, info.getAdminKey());
		assertEquals(submitKey, info.getSubmitKey());
		assertEquals(merkleTopic.getExpirationTimestamp().getSeconds(), info.getExpirationTime().getSeconds());
		assertEquals(merkleTopic.getAutoRenewDurationSeconds(), info.getAutoRenewPeriod().getSeconds());
		assertEquals(ByteString.copyFrom(merkleTopic.getRunningHash()), info.getRunningHash());
		assertEquals(merkleTopic.getAutoRenewAccountId().num(), info.getAutoRenewAccount().getAccountNum());
		assertEquals(merkleTopic.getSequenceNumber(), info.getSequenceNumber());
		assertEquals(merkleTopic.getMemo(), info.getMemo());
	}

	@Test
	public void getsTopicInfoWithEmptyRunningHash() throws Throwable {
		// setup:
		Query query = validQuery(ANSWER_ONLY, fee, target);
		merkleTopic.setRunningHash(null);
		given(topics.get(asTopic(target))).willReturn(merkleTopic);

		// when:
		Response response = subject.responseGiven(query, view, OK, fee);

		// then:
		assertTrue(response.hasConsensusGetTopicInfo());
		// and:
		ConsensusTopicInfo info = response.getConsensusGetTopicInfo().getTopicInfo();
		assertArrayEquals(new byte[48], info.getRunningHash().toByteArray());
	}

	private Query validQuery(ResponseType type, long payment, String idLit) throws Throwable {
		this.paymentTxn = payerSponsoredTransfer(payer, COMPLEX_KEY_ACCOUNT_KT, node, payment);
		QueryHeader.Builder header = QueryHeader.newBuilder()
				.setPayment(this.paymentTxn)
				.setResponseType(type);
		ConsensusGetTopicInfoQuery.Builder op = ConsensusGetTopicInfoQuery.newBuilder()
				.setHeader(header)
				.setTopicID(asTopic(idLit));
		return Query.newBuilder().setConsensusGetTopicInfo(op).build();
	}
}
