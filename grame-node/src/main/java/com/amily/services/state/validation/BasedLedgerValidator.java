package com.grame.services.state.validation;

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

import com.grame.services.config.grameNumbers;
import com.grame.services.context.properties.GlobalDynamicProperties;
import com.grame.services.context.properties.PropertySource;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.state.merkle.MerkleEntityId;
import com.swirlds.fcmap.FCMap;

public class BasedLedgerValidator implements LedgerValidator {
	private final long expectedFloat;

	private final grameNumbers grameNums;
	private final GlobalDynamicProperties dynamicProperties;

	public BasedLedgerValidator(
			grameNumbers grameNums,
			PropertySource properties,
			GlobalDynamicProperties dynamicProperties
	) {
		this.expectedFloat = properties.getLongProperty("ledger.totalTinyBarFloat");

		this.grameNums = grameNums;
		this.dynamicProperties = dynamicProperties;
	}

	@Override
	public void assertIdsAreValid(FCMap<MerkleEntityId, MerkleAccount> accounts) {
		for (MerkleEntityId id : accounts.keySet()) {
			if (id.getRealm() != grameNums.realm()) {
				throw new IllegalStateException(String.format("Invalid realm in account %s", id.toAbbrevString()));
			}
			if (id.getShard() != grameNums.shard()) {
				throw new IllegalStateException(String.format("Invalid shard in account %s", id.toAbbrevString()));
			}
			if (id.getNum() < 1 || id.getNum() > dynamicProperties.maxAccountNum()) {
				throw new IllegalStateException(String.format("Invalid num in account %s", id.toAbbrevString()));
			}
		}
	}

	@Override
	public boolean hasExpectedTotalBalance(FCMap<MerkleEntityId, MerkleAccount> accounts) {
		return expectedFloat == accounts.values().stream().mapToLong(MerkleAccount::getBalance).sum();
	}
}
