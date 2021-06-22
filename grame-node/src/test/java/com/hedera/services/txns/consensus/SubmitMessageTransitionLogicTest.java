package com.grame.services.txns.consensus;

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
import com.grame.services.context.TransactionContext;
import com.grame.services.context.properties.GlobalDynamicProperties;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.state.merkle.MerkleTopic;
import com.grame.services.txns.validation.OptionValidator;
import com.grame.services.utils.MiscUtils;
import com.grame.services.utils.PlatformTxnAccessor;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.ConsensusMessageChunkInfo;
import com.gramegrame.api.proto.java.ConsensusSubmitMessageTransactionBody;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionID;
import com.swirlds.fcmap.FCMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.grame.test.utils.IdUtils.asTopic;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_CHUNK_NUMBER;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_CHUNK_TRANSACTION_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TOPIC_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TOPIC_MESSAGE;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.MESSAGE_SIZE_TOO_LARGE;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.SUCCESS;
import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;

class SubmitMessageTransitionLogicTest {
	private static final String TOPIC_ID = "8.6.75";
	private static final long EPOCH_SECOND = 1546304461;

	private Instant consensusTime;
	private TransactionBody transactionBody;
	private TransactionContext transactionContext;
	private PlatformTxnAccessor accessor;
	private OptionValidator validator;
	private SubmitMessageTransitionLogic subject;
	private FCMap<MerkleEntityId, MerkleTopic> topics = new FCMap<>();
	private GlobalDynamicProperties globalDynamicProperties;
	final private AccountID payer = AccountID.newBuilder().setAccountNum(1_234L).build();

	@BeforeEach
	private void setup() {
		consensusTime = Instant.ofEpochSecond(EPOCH_SECOND);

		transactionContext = mock(TransactionContext.class);
		given(transactionContext.consensusTime()).willReturn(consensusTime);
		accessor = mock(PlatformTxnAccessor.class);
		validator = mock(OptionValidator.class);
		topics.clear();
		globalDynamicProperties = mock(GlobalDynamicProperties.class);
		given(globalDynamicProperties.messageMaxBytesAllowed()).willReturn(1024);
		subject = new SubmitMessageTransitionLogic(() -> topics, validator, transactionContext, globalDynamicProperties);
	}

	@Test
	public void rubberstampsSyntax() {
		// expect:
		assertEquals(OK, subject.syntaxCheck().apply(null));
	}

	@Test
	public void hasCorrectApplicability() {
		// given:
		givenValidTransactionContext();

		// expect:
		assertTrue(subject.applicability().test(transactionBody));
		assertFalse(subject.applicability().test(TransactionBody.getDefaultInstance()));
	}

	@Test
	public void followsHappyPath() {
		// given:
		givenValidTransactionContext();

		// when:
		subject.doStateTransition();

		// then:
		var topic = topics.get(MerkleEntityId.fromTopicId(asTopic(TOPIC_ID)));
		assertNotNull(topic);
		assertEquals(1L, topic.getSequenceNumber()); // Starts at 0.

		// Hash depends on prior state of topic (default topic object has 0s for runningHash and 0L for seqNum),
		// consensus timestamp, message.
		assertEquals("c44860f057eca2ea865821f5211420afe231dc2a485c277405d14f8421bb97f4a34ddd53db84bcf064045d10e7fca822",
				MiscUtils.commonsBytesToHex(topic.getRunningHash()));

		verify(transactionContext).setStatus(SUCCESS);
	}

	@Test
	public void failsWithEmptyMessage() {
		// given:
		givenTransactionContextNoMessage();

		// when:
		subject.doStateTransition();

		// then:
		assertUnchangedTopics();
		verify(transactionContext).setStatus(INVALID_TOPIC_MESSAGE);
	}

	@Test
	public void failsForLargeMessage() {
		// given:
		givenValidTransactionContext();
		given(globalDynamicProperties.messageMaxBytesAllowed()).willReturn(5);

		// when:
		subject.doStateTransition();

		// then:
		assertUnchangedTopics();
		verify(transactionContext).setStatus(MESSAGE_SIZE_TOO_LARGE);
	}



	@Test
	public void failsForInvalidTopic() {
		// given:
		givenTransactionContextInvalidTopic();

		// when:
		subject.doStateTransition();

		// then:
		assertTrue(topics.isEmpty());
		verify(transactionContext).setStatus(INVALID_TOPIC_ID);
	}

	@Test
	public void failsForInvalidChunkNumber() {
		// given:
		givenChunkMessage(2, 3, defaultTxnId());

		// when:
		subject.doStateTransition();

		// then:
		verify(transactionContext).setStatus(INVALID_CHUNK_NUMBER);
	}

	@Test
	public void failsForDifferentPayers() {
		// given:
		AccountID initialTransactionPayer = AccountID
				.newBuilder()
				.setAccountNum(payer.getAccountNum() + 1)
				.build();
		givenChunkMessage(3, 2, txnId(initialTransactionPayer, EPOCH_SECOND));

		// when:
		subject.doStateTransition();

		// then:
		verify(transactionContext).setStatus(INVALID_CHUNK_TRANSACTION_ID);
	}

	@Test
	public void acceptsChunkNumberDifferentThan1HavingTheSamePayerEvenWhenNotMatchingValidStart() {
		// given:
		givenChunkMessage(5, 5, txnId(payer, EPOCH_SECOND - 30));

		// when:
		subject.doStateTransition();

		// then:
		verify(transactionContext).setStatus(SUCCESS);
	}

	@Test
	public void failsForTransactionIDOfChunkNumber1NotMatchingTheEntireInitialTransactionID() {
		// given:
		givenChunkMessage(4, 1, txnId(payer, EPOCH_SECOND - 30));

		// when:
		subject.doStateTransition();

		// then:
		verify(transactionContext).setStatus(INVALID_CHUNK_TRANSACTION_ID);
	}

	@Test
	public void acceptsChunkNumber1WhenItsTransactionIDMatchesTheEntireInitialTransactionID() {
		// given:
		givenChunkMessage(1, 1, defaultTxnId());

		// when:
		subject.doStateTransition();

		// then:
		verify(transactionContext).setStatus(SUCCESS);
	}

	private void assertUnchangedTopics() {
		var topic = topics.get(MerkleEntityId.fromTopicId(asTopic(TOPIC_ID)));
		assertEquals(0L, topic.getSequenceNumber());
		assertArrayEquals(new byte[48], topic.getRunningHash());
	}

	private ConsensusSubmitMessageTransactionBody.Builder getBasicValidTransactionBodyBuilder() {
		return ConsensusSubmitMessageTransactionBody.newBuilder()
				.setTopicID(asTopic(TOPIC_ID))
				.setMessage(ByteString.copyFrom("valid message".getBytes()));
	}

	private void givenTransaction(ConsensusSubmitMessageTransactionBody.Builder body) {
		transactionBody = TransactionBody.newBuilder()
				.setTransactionID(defaultTxnId())
				.setConsensusSubmitMessage(body.build())
				.build();
		given(accessor.getTxn()).willReturn(transactionBody);
		given(transactionContext.accessor()).willReturn(accessor);
	}

	private void givenValidTransactionContext() {
		givenTransaction(getBasicValidTransactionBodyBuilder());
		given(validator.queryableTopicStatus(asTopic(TOPIC_ID), topics)).willReturn(OK);
		topics.put(MerkleEntityId.fromTopicId(asTopic(TOPIC_ID)), new MerkleTopic());
	}

	private void givenTransactionContextNoMessage() {
		givenTransaction(ConsensusSubmitMessageTransactionBody.newBuilder()
				.setTopicID(asTopic(TOPIC_ID)).setTopicID(asTopic(TOPIC_ID)));
		given(validator.queryableTopicStatus(asTopic(TOPIC_ID), topics)).willReturn(OK);
		topics.put(MerkleEntityId.fromTopicId(asTopic(TOPIC_ID)), new MerkleTopic());
	}

	private void givenTransactionContextInvalidTopic() {
		givenTransaction(getBasicValidTransactionBodyBuilder());
		given(validator.queryableTopicStatus(asTopic(TOPIC_ID), topics)).willReturn(INVALID_TOPIC_ID);
	}

	private void givenChunkMessage(int totalChunks, int chunkNumber, TransactionID initialTransactionID) {
		ConsensusMessageChunkInfo chunkInfo = ConsensusMessageChunkInfo
				.newBuilder()
				.setInitialTransactionID(initialTransactionID)
				.setTotal(totalChunks)
				.setNumber(chunkNumber)
				.build();
		givenTransaction(getBasicValidTransactionBodyBuilder()
				.setChunkInfo(chunkInfo));
		given(validator.queryableTopicStatus(asTopic(TOPIC_ID), topics)).willReturn(OK);
		topics.put(MerkleEntityId.fromTopicId(asTopic(TOPIC_ID)), new MerkleTopic());
	}

	private TransactionID txnId(AccountID payer, long epochSecond) {
		return TransactionID.newBuilder()
				.setAccountID(payer)
				.setTransactionValidStart(
						Timestamp.newBuilder().setSeconds(epochSecond))
				.build();
	}

	private TransactionID defaultTxnId() {
		return txnId(payer, EPOCH_SECOND);
	}
}
