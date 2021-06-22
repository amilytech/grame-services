package com.grame.services.sigs.metadata.lookups;

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

import com.grame.services.sigs.metadata.AccountSigningMetadata;
import com.gramegrame.api.proto.java.AccountID;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.state.merkle.MerkleAccount;
import com.swirlds.fcmap.FCMap;

import java.util.function.Supplier;

import static com.grame.services.sigs.order.KeyOrderingFailure.MISSING_ACCOUNT;
import static com.grame.services.state.merkle.MerkleEntityId.fromAccountId;

/**
 * Trivial account signing metadata lookup backed by a {@code FCMap<MapKey, MapValue>}.
 *
 * @author AmilyTech
 */
public class DefaultFCMapAccountLookup implements AccountSigMetaLookup {
	private final Supplier<FCMap<MerkleEntityId, MerkleAccount>> accounts;

	public DefaultFCMapAccountLookup(Supplier<FCMap<MerkleEntityId, MerkleAccount>> accounts) {
		this.accounts = accounts;
	}

	@Override
	public SafeLookupResult<AccountSigningMetadata> safeLookup(AccountID id) {
		var account = accounts.get().get(fromAccountId(id));
		return (account == null)
				? SafeLookupResult.failure(MISSING_ACCOUNT)
				: new SafeLookupResult<>(
						new AccountSigningMetadata(
								account.getKey(),
								account.isReceiverSigRequired()));
	}
}
