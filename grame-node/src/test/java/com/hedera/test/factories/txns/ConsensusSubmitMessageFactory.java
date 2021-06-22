package com.grame.test.factories.txns;

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
import com.gramegrame.api.proto.java.ConsensusSubmitMessageTransactionBody;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;

import static com.grame.test.utils.IdUtils.asTopic;

public class ConsensusSubmitMessageFactory extends SignedTxnFactory<ConsensusSubmitMessageFactory> {
	private String topicId;
	private String message;

	private ConsensusSubmitMessageFactory(String topicId) { this.topicId = topicId; }
	public static ConsensusSubmitMessageFactory newSignedConsensusSubmitMessage(String topicId) {
		return new ConsensusSubmitMessageFactory(topicId);
	}

	@Override
	protected ConsensusSubmitMessageFactory self() {
		return this;
	}

	@Override
	protected long feeFor(Transaction signedTxn, int numPayerKeys) {
		return 0;
	}

	@Override
	protected void customizeTxn(TransactionBody.Builder transactionBody) {
		ConsensusSubmitMessageTransactionBody.Builder op = ConsensusSubmitMessageTransactionBody.newBuilder();
		if (null != topicId) {
			op.setTopicID(asTopic(topicId));
		}
		if (null != message) {
			op.setMessage(ByteString.copyFromUtf8(message));
		}
		transactionBody.setConsensusSubmitMessage(op);
	}

	public ConsensusSubmitMessageFactory message(String message) {
		this.message = message;
		return this;
	}

	public ConsensusSubmitMessageFactory topicId(String topicId) {
		this.topicId = topicId;
		return this;
	}
}
