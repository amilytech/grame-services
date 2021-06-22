package com.grame.services.fees.calculation.consensus.txns;

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

import com.grame.services.state.merkle.MerkleTopic;
import com.grame.services.context.primitives.StateView;
import com.grame.services.fees.calculation.TxnResourceUsageEstimator;
import com.grame.services.state.merkle.MerkleEntityId;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.exception.InvalidTxBodyException;
import com.gramegrame.fee.SigValueObj;
import com.grame.services.legacy.core.jproto.JKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.grame.services.fees.calculation.FeeCalcUtils.ZERO_EXPIRY;
import static com.gramegrame.fee.ConsensusServiceFeeBuilder.getConsensusUpdateTopicFee;
import static com.gramegrame.fee.ConsensusServiceFeeBuilder.getUpdateTopicRbsIncrease;

public class UpdateTopicResourceUsage implements TxnResourceUsageEstimator {
    private static final Logger log = LogManager.getLogger(UpdateTopicResourceUsage.class);

    @Override
    public boolean applicableTo(TransactionBody txn) {
        return txn.hasConsensusUpdateTopic();
    }

    @Override
    public FeeData usageGiven(TransactionBody txn, SigValueObj sigUsage, StateView view) throws InvalidTxBodyException {
        try {
            MerkleTopic merkleTopic = view.topics().get(MerkleEntityId.fromTopicId(txn.getConsensusUpdateTopic().getTopicID()));
            long rbsIncrease = getUpdateTopicRbsIncrease(
                    txn.getTransactionID().getTransactionValidStart(),
                    JKey.mapJKey(merkleTopic.getAdminKey()),
                    JKey.mapJKey(merkleTopic.getSubmitKey()),
                    merkleTopic.getMemo(),
                    merkleTopic.hasAutoRenewAccountId(),
                    lookupExpiry(merkleTopic),
                    txn.getConsensusUpdateTopic());
            return getConsensusUpdateTopicFee(txn, rbsIncrease, sigUsage);
        } catch (Exception illegal) {
            log.warn("Usage estimation unexpectedly failed for {}!", txn, illegal);
            throw new InvalidTxBodyException(illegal);
        }
    }

    private Timestamp lookupExpiry(MerkleTopic merkleTopic) {
        try {
            return Timestamp.newBuilder()
                    .setSeconds(merkleTopic.getExpirationTimestamp().getSeconds())
                    .build();
        } catch (NullPointerException e) {
			/* Effect is to charge nothing for RBH */
            log.warn("Missing topic expiry data, ignoring expiration time in fee calculation!", e);
            return ZERO_EXPIRY;
        }
    }
}
