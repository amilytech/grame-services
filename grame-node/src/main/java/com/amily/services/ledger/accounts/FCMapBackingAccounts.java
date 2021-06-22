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

import com.grame.services.ledger.grameLedger;
import com.gramegrame.api.proto.java.AccountID;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.state.merkle.MerkleAccount;
import com.swirlds.fcmap.FCMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static com.grame.services.state.merkle.MerkleEntityId.fromAccountId;
import static com.grame.services.utils.EntityIdUtils.readableId;

public class FCMapBackingAccounts implements BackingStore<AccountID, MerkleAccount> {
	Set<AccountID> existingAccounts = new HashSet<>();
	Map<AccountID, MerkleAccount> cache = new HashMap<>();

	private final Supplier<FCMap<MerkleEntityId, MerkleAccount>> delegate;

	public FCMapBackingAccounts(Supplier<FCMap<MerkleEntityId, MerkleAccount>> delegate) {
		this.delegate = delegate;
		rebuildFromSources();
	}

	@Override
	public void rebuildFromSources() {
		existingAccounts.clear();
		delegate.get().keySet().stream()
				.map(MerkleEntityId::toAccountId)
				.forEach(existingAccounts::add);
	}

	@Override
	public void flushMutableRefs() {
		cache.keySet()
				.stream()
				.sorted(grameLedger.ACCOUNT_ID_COMPARATOR)
				.forEach(id -> delegate.get().replace(fromAccountId(id), cache.get(id)));
		cache.clear();
	}

	@Override
	public MerkleAccount getRef(AccountID id) {
		return cache.computeIfAbsent(id, ignore -> delegate.get().getForModify(fromAccountId(id)));
	}

	@Override
	public void put(AccountID id, MerkleAccount account) {
		MerkleEntityId delegateId = fromAccountId(id);
		if (!existingAccounts.contains(id)) {
			delegate.get().put(delegateId, account);
			existingAccounts.add(id);
		} else if (!cache.containsKey(id) || (cache.get(id) != account)) {
			throw new IllegalArgumentException(String.format(
					"Argument 'id=%s' does not map to a mutable ref!",
					readableId(id)));
		}
	}

	@Override
	public boolean contains(AccountID id) {
		return existingAccounts.contains(id);
	}

	@Override
	public void remove(AccountID id) {
		existingAccounts.remove(id);
		delegate.get().remove(fromAccountId(id));
	}

	@Override
	public Set<AccountID> idSet() {
		return existingAccounts;
	}

	@Override
	public MerkleAccount getUnsafeRef(AccountID id) {
		return delegate.get().get(fromAccountId(id));
	}
}
