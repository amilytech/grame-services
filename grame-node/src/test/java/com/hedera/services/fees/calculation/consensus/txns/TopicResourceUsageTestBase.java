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

import com.grame.services.context.properties.NodeLocalProperties;
import com.grame.services.context.properties.PropertySource;
import com.grame.services.state.merkle.MerkleTopic;
import com.grame.services.context.primitives.StateView;
import com.gramegrame.api.proto.java.*;
import com.gramegrame.fee.SigValueObj;
import com.grame.services.state.merkle.MerkleEntityId;
import com.swirlds.fcmap.FCMap;

import static com.grame.test.utils.IdUtils.asTopic;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

class TopicResourceUsageTestBase {
    protected static final int totalSigCount = 1;
    protected static final int payerAcctSigCount = 2;
    protected static final int signatureSize = 64;

    // Base services RBH when even when no extra transaction specific rbs is charged.
    protected static final int baseServicesRbh = 6;
    // Base network RBH when even when no extra transaction specific rbs is charged.
    protected static final int baseNetworkRbh = 1;
    protected static final int nodeBpr = 4; // always equal to INT_SIZE
    protected static final int baseBpt = 140; // size of transaction fields and sigs

    protected StateView view;
    protected FCMap<MerkleEntityId, MerkleTopic> topics;
    protected TopicID topicId = asTopic("0.0.1234");
    protected SigValueObj sigValueObj = new SigValueObj(totalSigCount, payerAcctSigCount, signatureSize);
    protected NodeLocalProperties nodeProps;

    void setup() throws Throwable {
        topics = mock(FCMap.class);
        nodeProps = mock(NodeLocalProperties.class);
        view = new StateView(() -> topics, StateView.EMPTY_ACCOUNTS_SUPPLIER, nodeProps, null);
    }

    protected void checkServicesFee(FeeData feeData, int extraRbh) {
        // Only rbh component is non-zero in services FeeComponents.
        checkFeeComponents(feeData.getServicedata(), 0, 0, baseServicesRbh + extraRbh, 0);
    }

    protected void checkNetworkFee(FeeData feeData, int extraBpt, int extraRbh) {
        checkFeeComponents(feeData.getNetworkdata(), baseBpt + extraBpt, totalSigCount, baseNetworkRbh + extraRbh, 0);
    }

    protected void checkNodeFee(FeeData feeData, int extraBpt) {
        checkFeeComponents(feeData.getNodedata(), baseBpt + extraBpt, payerAcctSigCount, 0, nodeBpr);
    }

    protected void checkFeeComponents(
            FeeComponents actual, int bpt, int vpt, int rbh, int bpr) {
        FeeComponents expected = FeeComponents.newBuilder()
                .setConstant(1)
                .setBpt(bpt)
                .setVpt(vpt)
                .setRbh(rbh)
                .setBpr(bpr)
                .build();  // other components are always 0 for topic transactions
        assertEquals(expected, actual);
    }
}
