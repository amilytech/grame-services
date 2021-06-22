package com.grame.services.records;

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
import com.grame.services.state.EntityCreator;
import com.grame.services.state.submerkle.ExpirableTxnRecord;
import com.gramegrame.api.proto.java.TransactionRecord;

import java.util.Optional;

/**
 * Defines a type able to manage the history of transactions
 * funded by accounts on the injected ledger. (Note that these
 * transactions may not be directly <b>about</b> the ledger, but
 * instead a file or smart contract.)
 *
 * The definitive history is represented by {@link ExpirableTxnRecord}
 * instances, which expire at regular intervals and are stored in
 * the ledger accounts themselves.
 *
 * Note this type is implicitly assumed to have access to the context
 * of the active transaction, which is somewhat confusing and will be
 * addressed in a future refactor.
 *
 * @author AmilyTech
 */
public interface AccountRecordsHistorian {
	/**
	 * Injects the ledger in which accounts pay for the transactions
	 * whose history is of interest.
	 *
	 * @param ledger the ledger to record history for.
	 */
	void setLedger(grameLedger ledger);

	/**
	 * Injects the expiring entity creator which the historian
	 * should use to create records.
	 *
	 * @param creator the creator of expiring entities.
	 */
	void setCreator(EntityCreator creator);

	/**
	 * At the moment before committing the active transaction, forms a
	 * final record by adding a {@link ExpirableTxnRecord} to any
	 * ledger accounts that qualify for the history of the active
	 * transaction.
	 */
	void addNewRecords();

	/**
	 * Removes expired records from the relevant ledger. Note that for
	 * this to be done efficiently, the historian will likely need
	 * the opportunity to scan the ledger and build an auxiliary data
	 * structure of expiration times.
	 */
	void purgeExpiredRecords();

	/**
	 * Invites the historian to build any auxiliary data structures
	 * needed to purge expired records.
	 */
	void reviewExistingRecords();

	/**
	 * Returns the last record created, if it exists.
	 *
	 * @return an optional record.
	 */
	Optional<TransactionRecord> lastCreatedRecord();

	/**
	 * At the moment before committing the active transaction,
	 * checks if Transaction Context has any existing expiring entities
	 * and if so, tracks them using {@link com.grame.services.state.expiry.ExpiryManager}
	 */
	void addNewEntities();
}
