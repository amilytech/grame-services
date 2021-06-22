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

import com.grame.test.factories.keys.KeyTree;
import com.gramegrame.api.proto.java.ConsensusUpdateTopicTransactionBody;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;

import java.time.Instant;
import java.util.Optional;

import static com.grame.test.utils.IdUtils.asAccount;
import static com.grame.test.utils.IdUtils.asTopic;

public class ConsensusUpdateTopicFactory extends SignedTxnFactory<ConsensusUpdateTopicFactory> {
	private String topicId;
	private String autoRenewAccountId;
	private Optional<KeyTree> adminKey = Optional.empty();
	private Optional<Instant> expirationTime = Optional.empty();

	private ConsensusUpdateTopicFactory(String topicId) { this.topicId = topicId; }
	public static ConsensusUpdateTopicFactory newSignedConsensusUpdateTopic(String topicId) {
		return new ConsensusUpdateTopicFactory(topicId);
	}

	@Override
	protected ConsensusUpdateTopicFactory self() {
		return this;
	}

	@Override
	protected long feeFor(Transaction signedTxn, int numPayerKeys) {
		return 0;
	}

	@Override
	protected void customizeTxn(TransactionBody.Builder transactionBody) {
		ConsensusUpdateTopicTransactionBody.Builder op = ConsensusUpdateTopicTransactionBody.newBuilder();
		if (null != topicId) {
			op.setTopicID(asTopic(topicId));
		}
		if (null != autoRenewAccountId) {
			op.setAutoRenewAccount(asAccount(autoRenewAccountId));
		}
		adminKey.ifPresent(k -> op.setAdminKey(k.asKey(keyFactory)));
		expirationTime.ifPresent(et -> op.setExpirationTime(
				Timestamp.newBuilder().setSeconds(et.getEpochSecond()).setNanos(et.getNano()).build()));
		transactionBody.setConsensusUpdateTopic(op);
	}

	public ConsensusUpdateTopicFactory topicId(String topicId) {
		this.topicId = topicId;
		return this;
	}

	public ConsensusUpdateTopicFactory autoRenewAccountId(String autoRenewAccountId) {
		this.autoRenewAccountId = autoRenewAccountId;
		return this;
	}

	public ConsensusUpdateTopicFactory adminKey(KeyTree adminKey) {
		this.adminKey = Optional.of(adminKey);
		return this;
	}

	public ConsensusUpdateTopicFactory expirationTime(Instant expirationTime) {
		this.expirationTime = Optional.of(expirationTime);
		return this;
	}
}
