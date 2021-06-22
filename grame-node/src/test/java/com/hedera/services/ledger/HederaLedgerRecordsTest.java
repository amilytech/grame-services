package com.grame.services.ledger;

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

import com.grame.services.state.submerkle.ExpirableTxnRecord;
import com.swirlds.fcqueue.FCQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.stream.Collectors;

import static com.grame.services.ledger.properties.AccountProperty.RECORDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.verify;

public class grameLedgerRecordsTest extends BasegrameLedgerTest {
	@BeforeEach
	void setup() {
		commonSetup();
		setupWithMockLedger();
	}

	@Test
	public void addsNewPayerRecordLast() {
		// setup:
		FCQueue<ExpirableTxnRecord> records = asExpirableRecords(100L, 50L, 200L, 311L);
		addPayerRecords(misc, records);
		// and:
		ExpirableTxnRecord newRecord = asExpirableRecords(1_000L).peek();

		// when:
		subject.addRecord(misc, newRecord);

		// then:
		ArgumentCaptor<FCQueue> captor = ArgumentCaptor.forClass(FCQueue.class);
		verify(accountsLedger).set(
				argThat(misc::equals),
				argThat(RECORDS::equals),
				captor.capture());
		// and:
		assertTrue(captor.getValue() == records);
		assertThat(
				((FCQueue<ExpirableTxnRecord>) captor.getValue())
						.stream()
						.map(ExpirableTxnRecord::getExpiry)
						.collect(Collectors.toList()),
				contains(100L, 50L, 200L, 311L, 1_000L));
	}
}
