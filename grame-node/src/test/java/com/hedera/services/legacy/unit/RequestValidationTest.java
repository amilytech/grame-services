package com.grame.services.legacy.unit;

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
import com.grame.services.config.MockAccountNumbers;
import com.grame.services.config.MockEntityNumbers;
import com.grame.services.config.MockGlobalDynamicProps;
import com.grame.services.context.primitives.StateView;
import com.grame.services.fees.StandardExemptions;
import com.grame.services.legacy.handler.TransactionHandler;
import com.grame.services.context.ContextPlatformStatus;
import com.grame.services.security.ops.SystemOpPolicies;
import com.grame.services.txns.validation.BasicPrecheck;
import com.grame.services.utils.MiscUtils;
import com.grame.test.mocks.TestContextValidator;
import com.grame.test.mocks.TestExchangeRates;
import com.grame.test.mocks.TestFeesFactory;
import com.grame.test.mocks.TestProperties;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.Duration;
import com.gramegrame.api.proto.java.Key;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.builder.RequestBuilder;
import com.grame.services.context.domain.security.PermissionedAccountsRange;
import com.grame.services.legacy.proto.utils.CommonUtils;

import java.security.KeyPair;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.swirlds.common.PlatformStatus;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.KeyPairGenerator;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static com.grame.test.mocks.TestUsagePricesProvider.TEST_USAGE_PRICES;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.NOT_SUPPORTED;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;

/**
 * @author Akshay
 * @Date : 8/13/2018
 */
public class RequestValidationTest {

  /**
   * testing nodeAccount Validation function for positive and negative scenario
   */
  @Test
  public void testNodeAccountValidation() throws Exception {
    long nodeAccShard = 0;
    long nodeAccRealm = 2;
    long nodeAccnNum = 1007;
    AccountID nodeAcc = AccountID.newBuilder().setShardNum(nodeAccShard).setRealmNum(nodeAccRealm)
        .setAccountNum(nodeAccnNum).build();

    var policies = new SystemOpPolicies(new MockEntityNumbers());
    var platformStatus = new ContextPlatformStatus();
    platformStatus.set(PlatformStatus.ACTIVE);
    TransactionHandler trHandler =
        new TransactionHandler(
                null,
                null,
                null,
                nodeAcc,
                null,
                TestFeesFactory.FEES_FACTORY.get(),
                () -> StateView.EMPTY_VIEW,
                new BasicPrecheck(TestContextValidator.TEST_VALIDATOR, new MockGlobalDynamicProps()),
                null,
                new MockAccountNumbers(),
                policies,
                new StandardExemptions(new MockAccountNumbers(), policies),
                platformStatus,
                null);
    Timestamp timestamp =
        RequestBuilder.getTimestamp(Instant.now(Clock.systemUTC()).minusSeconds(10));

    Duration transactionDuration = RequestBuilder.getDuration(30);

    KeyPair pair = new KeyPairGenerator().generateKeyPair();

    byte[] pubKey = ((EdDSAPublicKey) pair.getPublic()).getAbyte();

    String pubKeyStr = MiscUtils.commonsBytesToHex(pubKey);

    Key key = Key.newBuilder().setEd25519(ByteString.copyFromUtf8(pubKeyStr)).build();
    List<Key> keyList = new ArrayList<Key>();
    keyList.add(key);

    long transactionFee = 100l;
    boolean generateRecord = true;
    String memo = "NodeAccount test";
    long initialBalance = 10000l;
    long sendRecordThreshold = 100l;
    long receiveRecordThreshold = 100l;
    boolean receiverSigRequired = true;
    Duration autoRenewPeriod = RequestBuilder.getDuration(500);

    Transaction matchingNodeAccTransaction =
        RequestBuilder.getCreateAccountBuilder(nodeAccnNum, nodeAccRealm, nodeAccShard, nodeAccnNum,
            nodeAccRealm, nodeAccShard, transactionFee, timestamp, transactionDuration,
            generateRecord, memo, keyList.size(), keyList, initialBalance, sendRecordThreshold,
            receiveRecordThreshold, receiverSigRequired, autoRenewPeriod);
    Transaction nonMatchingTransaction =
        RequestBuilder.getCreateAccountBuilder(nodeAccnNum, nodeAccRealm, nodeAccShard, nodeAccnNum,
            nodeAccRealm + 1, nodeAccShard + 1, transactionFee, timestamp, transactionDuration,
            generateRecord, memo, keyList.size(), keyList, initialBalance, sendRecordThreshold,
            receiveRecordThreshold, receiverSigRequired, autoRenewPeriod);
    TransactionBody matchingBody = CommonUtils.extractTransactionBody(matchingNodeAccTransaction);
    TransactionBody nonMatchingBody = CommonUtils.extractTransactionBody(nonMatchingTransaction);
    ResponseCodeEnum matchingPreCheckReturn = trHandler.validateNodeAccount(matchingBody);
    Assertions.assertEquals(matchingPreCheckReturn, OK);
    ResponseCodeEnum nonMatchingPreCheckReturn = trHandler.validateNodeAccount(nonMatchingBody);
    Assertions.assertNotEquals(nonMatchingPreCheckReturn, OK);
  }

  @Test
  public void apiPermissionValidation_Number_Test() {
    AccountID payerAccountId = AccountID.newBuilder().setAccountNum(55).build();
    String accountIdRange = "55";
    PermissionedAccountsRange accountRange = PermissionedAccountsRange.from(accountIdRange);
    Assertions.assertEquals(
            OK,
            accountRange.contains(payerAccountId.getAccountNum()) ? OK : NOT_SUPPORTED);
  }

  @Test
  public void apiPermissionValidation_Range_Positive_Test() {
    AccountID payerAccountId = AccountID.newBuilder().setAccountNum(55).build();
    String accountIdRange = "55-*";
    PermissionedAccountsRange accountRange = PermissionedAccountsRange.from(accountIdRange);
    Assertions.assertEquals(
            OK,
            accountRange.contains(payerAccountId.getAccountNum()) ? OK : NOT_SUPPORTED);
  }

  @Test
  public void apiPermissionValidation_Range_Negative_Test() {
    AccountID payerAccountId = AccountID.newBuilder().setAccountNum(54).build();
    String accountIdRange = "55-*";
    PermissionedAccountsRange accountRange = PermissionedAccountsRange.from(accountIdRange);
    Assertions.assertEquals(
            NOT_SUPPORTED,
            accountRange.contains(payerAccountId.getAccountNum()) ? OK : NOT_SUPPORTED);
  }
}
