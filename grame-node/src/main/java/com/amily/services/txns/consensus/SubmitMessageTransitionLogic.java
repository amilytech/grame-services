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

import com.grame.services.context.TransactionContext;
import com.grame.services.context.properties.GlobalDynamicProperties;
import com.grame.services.state.merkle.MerkleTopic;
import com.grame.services.txns.TransitionLogic;
import com.grame.services.txns.validation.OptionValidator;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.TransactionBody;
import com.grame.services.state.merkle.MerkleEntityId;
import com.swirlds.fcmap.FCMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TOPIC_MESSAGE;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_CHUNK_NUMBER;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_CHUNK_TRANSACTION_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TRANSACTION;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.MESSAGE_SIZE_TOO_LARGE;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.SUCCESS;


public class SubmitMessageTransitionLogic implements TransitionLogic {
	protected static final Logger log = LogManager.getLogger(SubmitMessageTransitionLogic.class);

	private static final Function<TransactionBody, ResponseCodeEnum> SYNTAX_RUBBER_STAMP = ignore -> OK;

	private final OptionValidator validator;
	private final TransactionContext transactionContext;
	private final Supplier<FCMap<MerkleEntityId, MerkleTopic>> topics;
	private final GlobalDynamicProperties globalDynamicProperties;

	public SubmitMessageTransitionLogic(
			Supplier<FCMap<MerkleEntityId, MerkleTopic>> topics,
			OptionValidator validator,
			TransactionContext transactionContext,
			GlobalDynamicProperties globalDynamicProperties
	) {
		this.topics = topics;
		this.validator = validator;
		this.transactionContext = transactionContext;
		this.globalDynamicProperties = globalDynamicProperties;
	}

	@Override
	public void doStateTransition() {
		var transactionBody = transactionContext.accessor().getTxn();
		var op = transactionBody.getConsensusSubmitMessage();

		if (op.getMessage().isEmpty()) {
			transactionContext.setStatus(INVALID_TOPIC_MESSAGE);
			return;
		}

		if(op.getMessage().size() > globalDynamicProperties.messageMaxBytesAllowed() ) {
			transactionContext.setStatus(MESSAGE_SIZE_TOO_LARGE);
			return;
		}

		var topicStatus = validator.queryableTopicStatus(op.getTopicID(), topics.get());
		if (OK != topicStatus) {
			transactionContext.setStatus(topicStatus);
			return;
		}

		if (op.hasChunkInfo()) {
			var chunkInfo = op.getChunkInfo();
			if (!(1 <= chunkInfo.getNumber() && chunkInfo.getNumber() <= chunkInfo.getTotal())) {
				transactionContext.setStatus(INVALID_CHUNK_NUMBER);
				return;
			}
			if (!chunkInfo.getInitialTransactionID().getAccountID().equals(
					transactionBody.getTransactionID().getAccountID())) {
				transactionContext.setStatus(INVALID_CHUNK_TRANSACTION_ID);
				return;
			}
			if (1 == chunkInfo.getNumber() &&
					!chunkInfo.getInitialTransactionID().equals(transactionBody.getTransactionID())) {
				transactionContext.setStatus(INVALID_CHUNK_TRANSACTION_ID);
				return;
			}
		}

		var topicId = MerkleEntityId.fromTopicId(op.getTopicID());
		var mutableTopic = topics.get().getForModify(topicId);
		try {
			mutableTopic.updateRunningHashAndSequenceNumber(
					transactionBody.getTransactionID().getAccountID(),
					op.getMessage().toByteArray(),
					op.getTopicID(),
					transactionContext.consensusTime());
			topics.get().put(topicId, mutableTopic);
			transactionContext.setTopicRunningHash(mutableTopic.getRunningHash(), mutableTopic.getSequenceNumber());
			transactionContext.setStatus(SUCCESS);
		} catch (IOException e) {
			log.error("Updating topic running hash failed.", e);
			transactionContext.setStatus(INVALID_TRANSACTION);
		}
	}

	@Override
	public Predicate<TransactionBody> applicability() {
		return TransactionBody::hasConsensusSubmitMessage;
	}

	@Override
	public Function<TransactionBody, ResponseCodeEnum> syntaxCheck() {
		return SYNTAX_RUBBER_STAMP;
	}
}
