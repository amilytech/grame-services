package com.grame.services.store.schedule;

/*
 * ‌
 * grame Services Node
 * ​
 * Copyright (C) 2018 - 2021 grame grame, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import com.grame.services.ledger.grameLedger;
import com.grame.services.ledger.TransactionalLedger;
import com.grame.services.ledger.properties.AccountProperty;
import com.grame.services.legacy.core.jproto.JKey;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.state.merkle.MerkleSchedule;
import com.grame.services.state.submerkle.EntityId;
import com.grame.services.state.submerkle.RichInstant;
import com.grame.services.store.CreationResult;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.Key;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.ScheduleID;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

public enum ExceptionalScheduleStore implements ScheduleStore {
	NOOP_SCHEDULE_STORE;

	@Override
	public MerkleSchedule get(ScheduleID sID) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean exists(ScheduleID id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setgrameLedger(grameLedger ledger) {
		/* No-op */
	}

	@Override
	public void setAccountsLedger(TransactionalLedger<AccountID, AccountProperty, MerkleAccount> accountsLedger) {
		/* No-op */
	}

	@Override
	public void apply(ScheduleID id, Consumer<MerkleSchedule> change) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CreationResult<ScheduleID> createProvisionally(MerkleSchedule candidate, RichInstant consensusTime) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Pair<Optional<ScheduleID>, MerkleSchedule> lookupSchedule(byte[] bodyBytes) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResponseCodeEnum markAsExecuted(ScheduleID id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void expire(EntityId id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResponseCodeEnum delete(ScheduleID id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void commitCreation() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void rollbackCreation() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isCreationPending() {
		throw new UnsupportedOperationException();
	}


}
