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

import com.grame.services.legacy.core.jproto.JContractIDKey;
import com.grame.services.legacy.core.jproto.JKey;
import com.grame.services.sigs.metadata.ContractSigningMetadata;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.state.merkle.MerkleEntityId;
import com.gramegrame.api.proto.java.ContractID;
import com.swirlds.fcmap.FCMap;

import java.util.function.Supplier;

import static com.grame.services.sigs.order.KeyOrderingFailure.IMMUTABLE_CONTRACT;
import static com.grame.services.sigs.order.KeyOrderingFailure.INVALID_CONTRACT;
import static com.grame.services.state.merkle.MerkleEntityId.fromContractId;

public class DefaultFCMapContractLookup implements ContractSigMetaLookup {
	private final Supplier<FCMap<MerkleEntityId, MerkleAccount>> accounts;

	public DefaultFCMapContractLookup(Supplier<FCMap<MerkleEntityId, MerkleAccount>> accounts) {
		this.accounts = accounts;
	}

	@Override
	public SafeLookupResult<ContractSigningMetadata> safeLookup(ContractID id) {
		var contract = accounts.get().get(fromContractId(id));
		if (contract == null || contract.isDeleted() || !contract.isSmartContract()) {
			return SafeLookupResult.failure(INVALID_CONTRACT);
		} else {
			JKey key;
			if ((key = contract.getKey()) == null || key instanceof JContractIDKey) {
				return SafeLookupResult.failure(IMMUTABLE_CONTRACT);
			} else {
				return new SafeLookupResult<>(new ContractSigningMetadata(key, contract.isReceiverSigRequired()));
			}
		}
	}
}
