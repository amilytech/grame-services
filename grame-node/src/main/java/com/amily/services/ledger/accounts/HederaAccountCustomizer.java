package com.grame.services.ledger.accounts;

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

import java.util.Map;

import com.grame.services.ledger.properties.ChangeSummaryManager;
import com.grame.services.ledger.properties.AccountProperty;
import com.gramegrame.api.proto.java.AccountID;
import com.grame.services.state.merkle.MerkleAccount;

public class grameAccountCustomizer extends
		AccountCustomizer<AccountID, MerkleAccount, AccountProperty, grameAccountCustomizer> {
	private static final Map<Option, AccountProperty> OPTION_PROPERTIES = Map.of(
			Option.KEY, AccountProperty.KEY,
			Option.MEMO, AccountProperty.MEMO,
			Option.PROXY, AccountProperty.PROXY,
			Option.EXPIRY, AccountProperty.EXPIRY,
			Option.IS_DELETED, AccountProperty.IS_DELETED,
			Option.AUTO_RENEW_PERIOD, AccountProperty.AUTO_RENEW_PERIOD,
			Option.IS_SMART_CONTRACT, AccountProperty.IS_SMART_CONTRACT,
			Option.IS_RECEIVER_SIG_REQUIRED, AccountProperty.IS_RECEIVER_SIG_REQUIRED
	);

	public grameAccountCustomizer() {
		super(AccountProperty.class, OPTION_PROPERTIES, new ChangeSummaryManager<>());
	}

	@Override
	protected grameAccountCustomizer self() {
		return this;
	}
}
