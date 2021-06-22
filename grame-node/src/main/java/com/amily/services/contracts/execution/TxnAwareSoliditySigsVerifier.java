package com.grame.services.contracts.execution;

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

import com.grame.services.context.TransactionContext;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.keys.grameKeyActivation;
import com.grame.services.keys.SyncActivationCheck;
import com.grame.services.sigs.PlatformSigOps;
import com.grame.services.sigs.factories.BodySigningSigFactory;
import com.grame.services.sigs.verification.SyncVerifier;
import com.gramegrame.api.proto.java.AccountID;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.legacy.core.jproto.JKey;
import com.swirlds.fcmap.FCMap;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.grame.services.keys.grameKeyActivation.ONLY_IF_SIG_IS_VALID;
import static com.grame.services.keys.grameKeyActivation.isActive;
import static com.grame.services.sigs.sourcing.DefaultSigBytesProvider.DEFAULT_SIG_BYTES;
import static com.grame.services.state.merkle.MerkleEntityId.fromAccountId;
import static java.util.stream.Collectors.toList;

public class TxnAwareSoliditySigsVerifier implements SoliditySigsVerifier {
	private final SyncVerifier syncVerifier;
	private final TransactionContext txnCtx;
	private final SyncActivationCheck check;
	private final Supplier<FCMap<MerkleEntityId, MerkleAccount>> accounts;

	public TxnAwareSoliditySigsVerifier(
			SyncVerifier syncVerifier,
			TransactionContext txnCtx,
			SyncActivationCheck check,
			Supplier<FCMap<MerkleEntityId, MerkleAccount>> accounts
	) {
		this.txnCtx = txnCtx;
		this.accounts = accounts;
		this.syncVerifier = syncVerifier;
		this.check = check;
	}

	@Override
	public boolean allRequiredKeysAreActive(Set<AccountID> touched) {
		var payer = txnCtx.activePayer();
		var requiredKeys = touched.stream()
				.filter(id -> !payer.equals(id))
				.flatMap(this::keyRequirement)
				.collect(toList());
		if (requiredKeys.isEmpty()) {
			return true;
		} else {
			return check.allKeysAreActive(
					requiredKeys,
					syncVerifier,
					txnCtx.accessor(),
					PlatformSigOps::createEd25519PlatformSigsFrom,
					DEFAULT_SIG_BYTES::allPartiesSigBytesFor,
					BodySigningSigFactory::new,
					(key, sigsFn) -> isActive(key, sigsFn, ONLY_IF_SIG_IS_VALID),
					grameKeyActivation::pkToSigMapFrom);
		}
	}

	private Stream<JKey> keyRequirement(AccountID id) {
		return Optional.ofNullable(accounts.get().get(fromAccountId(id)))
				.filter(account -> !account.isSmartContract())
				.filter(MerkleAccount::isReceiverSigRequired)
				.map(MerkleAccount::getKey)
				.stream();
	}
}
