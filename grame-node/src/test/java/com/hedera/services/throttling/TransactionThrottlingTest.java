package com.grame.services.throttling;

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

import com.gramegrame.api.proto.java.ConsensusCreateTopicTransactionBody;
import com.gramegrame.api.proto.java.ConsensusDeleteTopicTransactionBody;
import com.gramegrame.api.proto.java.ConsensusSubmitMessageTransactionBody;
import com.gramegrame.api.proto.java.ConsensusUpdateTopicTransactionBody;
import com.gramegrame.api.proto.java.TransactionBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.grame.test.utils.IdUtils.asAccount;
import static com.grame.test.utils.IdUtils.asTopic;
import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusCreateTopic;
import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusDeleteTopic;
import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusSubmitMessage;
import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusUpdateTopic;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;

class TransactionThrottlingTest {
	FunctionalityThrottling functionalThrottling;

	TransactionThrottling subject;

	@BeforeEach
	private void setup() {
		functionalThrottling = mock(FunctionalityThrottling.class);

		subject = new TransactionThrottling(functionalThrottling);
	}

	@Test
	public void defaultsToTrueIfUnknownType() {
		// when:
		boolean should = subject.shouldThrottle(TransactionBody.getDefaultInstance());

		// then:
		assertTrue(should);
	}

	@Test
	public void delegatesTopicCreateToFunctionalThrottling() {
		// setup:
		TransactionBody createTxn = TransactionBody.newBuilder()
				.setConsensusCreateTopic(ConsensusCreateTopicTransactionBody.newBuilder().setMemo("Hi!"))
				.build();

		given(functionalThrottling.shouldThrottle(ConsensusCreateTopic)).willReturn(true);

		// when:
		boolean should = subject.shouldThrottle(createTxn);

		// then:
		assertTrue(should);
		verify(functionalThrottling).shouldThrottle(ConsensusCreateTopic);
	}

	@Test
	public void delegatesTopicUpdateToFunctionalThrottling() {
		// setup:
		ConsensusUpdateTopicTransactionBody op = ConsensusUpdateTopicTransactionBody.newBuilder()
				.setAutoRenewAccount(asAccount("0.0.2"))
				.build();
		TransactionBody updateTxn = TransactionBody.newBuilder()
				.setConsensusUpdateTopic(op)
				.build();

		given(functionalThrottling.shouldThrottle(ConsensusUpdateTopic)).willReturn(true);

		// when:
		boolean should = subject.shouldThrottle(updateTxn);

		// then:
		assertTrue(should);
		verify(functionalThrottling).shouldThrottle(ConsensusUpdateTopic);
	}

	@Test
	public void delegatesTopicDeleteToFunctionalThrottling() {
		// setup:
		ConsensusDeleteTopicTransactionBody op = ConsensusDeleteTopicTransactionBody.newBuilder()
				.setTopicID(asTopic("0.0.2"))
				.build();
		TransactionBody deleteTxn = TransactionBody.newBuilder()
				.setConsensusDeleteTopic(op)
				.build();

		given(functionalThrottling.shouldThrottle(ConsensusDeleteTopic)).willReturn(true);

		// when:
		boolean should = subject.shouldThrottle(deleteTxn);

		// then:
		assertTrue(should);
		verify(functionalThrottling).shouldThrottle(ConsensusDeleteTopic);
	}

	@Test
	public void delegatesSubmitMessageToFunctionalThrottling() {
		// setup:
		ConsensusSubmitMessageTransactionBody op = ConsensusSubmitMessageTransactionBody.newBuilder()
				.setTopicID(asTopic("0.0.2"))
				.build();
		TransactionBody submitTxn = TransactionBody.newBuilder()
				.setConsensusSubmitMessage(op)
				.build();

		given(functionalThrottling.shouldThrottle(ConsensusSubmitMessage)).willReturn(true);

		// when:
		boolean should = subject.shouldThrottle(submitTxn);

		// then:
		assertTrue(should);
		verify(functionalThrottling).shouldThrottle(ConsensusSubmitMessage);
	}
}
