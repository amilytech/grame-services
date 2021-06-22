package com.grame.services.state.expiry;

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
import com.grame.services.records.RecordCache;
import com.grame.services.state.EntityCreator;
import com.grame.services.state.submerkle.ExpirableTxnRecord;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.TransactionRecord;

public enum NoopExpiringCreations implements EntityCreator {
	NOOP_EXPIRING_CREATIONS;

	@Override
	public void setLedger(grameLedger ledger) { }

	@Override
	public void setRecordCache(RecordCache recordCache) { }

	@Override
	public ExpirableTxnRecord createExpiringRecord(
			AccountID id,
			TransactionRecord record,
			long now,
			long submittingMember
	) {
		throw new UnsupportedOperationException();
	}
}
