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

import com.grame.services.state.merkle.MerkleTokenRelStatus;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.TokenID;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HashMapBackingTokenRels implements BackingStore<Pair<AccountID, TokenID>, MerkleTokenRelStatus> {
	private Map<Pair<AccountID, TokenID>, MerkleTokenRelStatus> rels = new HashMap<>();

	@Override
	public void flushMutableRefs() { }

	@Override
	public MerkleTokenRelStatus getRef(Pair<AccountID, TokenID> id) {
		return rels.get(id);
	}

	@Override
	public void put(Pair<AccountID, TokenID> id, MerkleTokenRelStatus rel) {
		rels.put(id, rel);
	}

	@Override
	public boolean contains(Pair<AccountID, TokenID> id) {
		return rels.containsKey(id);
	}

	@Override
	public void remove(Pair<AccountID, TokenID> id) {
		rels.remove(id);
	}

	@Override
	public Set<Pair<AccountID, TokenID>> idSet() {
		return rels.keySet();
	}

	@Override
	public MerkleTokenRelStatus getUnsafeRef(Pair<AccountID, TokenID> id) {
		return rels.get(id);
	}
}
