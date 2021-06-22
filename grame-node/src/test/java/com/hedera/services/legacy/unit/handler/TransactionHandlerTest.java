package com.grame.services.legacy.unit.handler;

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

import com.grame.services.config.MockAccountNumbers;
import com.grame.services.config.MockEntityNumbers;
import com.grame.services.context.primitives.StateView;
import com.grame.services.fees.FeeCalculator;
import com.grame.services.fees.HbarCentExchange;
import com.grame.services.fees.StandardExemptions;
import com.grame.services.fees.calculation.UsagePricesProvider;
import com.grame.services.legacy.handler.TransactionHandler;
import com.grame.services.context.ContextPlatformStatus;
import com.grame.services.queries.validation.QueryFeeCheck;
import com.grame.services.records.RecordCache;
import com.grame.services.security.ops.SystemOpPolicies;
import com.grame.services.sigs.verification.PrecheckVerifier;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.throttling.FunctionalityThrottling;
import com.grame.services.throttling.TransactionThrottling;
import com.grame.services.txns.validation.BasicPrecheck;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionID;
import com.swirlds.common.Platform;
import com.swirlds.common.PlatformStatus;
import com.swirlds.fcmap.FCMap;
import org.junit.jupiter.api.BeforeEach;

import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TransactionHandlerTest {
	private RecordCache recordCache;
	private PrecheckVerifier precheckVerifier;
	private FCMap<MerkleEntityId, MerkleAccount> accounts;
	private AccountID nodeAccount;
	private TransactionThrottling txnThrottling;
	private UsagePricesProvider usagePrices;
	private HbarCentExchange exchange;
	private FeeCalculator fees;
	private Supplier<StateView> stateView;
	private BasicPrecheck basicPrecheck;
	private QueryFeeCheck queryFeeCheck;
	private FunctionalityThrottling throttling;

	private TransactionHandler subject;

	@BeforeEach
	public void setUp() {
		recordCache = mock(RecordCache.class);
		precheckVerifier = mock(PrecheckVerifier.class);
		accounts = mock(FCMap.class);
		nodeAccount = mock(AccountID.class);
		txnThrottling = mock(TransactionThrottling.class);
		usagePrices = mock(UsagePricesProvider.class);
		exchange = mock(HbarCentExchange.class);
		fees = mock(FeeCalculator.class);
		stateView = mock(Supplier.class);
		basicPrecheck = mock(BasicPrecheck.class);
		queryFeeCheck = mock(QueryFeeCheck.class);
		throttling = mock(FunctionalityThrottling.class);

		var policies = new SystemOpPolicies(new MockEntityNumbers());
		var platformStatus = new ContextPlatformStatus();
		platformStatus.set(PlatformStatus.ACTIVE);
		subject = new TransactionHandler(
				recordCache,
				precheckVerifier,
				() -> accounts,
				nodeAccount,
				txnThrottling,
				fees,
				stateView,
				basicPrecheck,
				queryFeeCheck,
				new MockAccountNumbers(),
				policies,
				new StandardExemptions(new MockAccountNumbers(), policies),
				platformStatus,
				null);
	}
}
