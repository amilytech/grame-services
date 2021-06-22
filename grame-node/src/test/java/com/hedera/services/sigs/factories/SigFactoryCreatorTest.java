package com.grame.services.sigs.factories;

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

import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.state.merkle.MerkleSchedule;
import com.grame.services.utils.SignedTxnAccessor;
import com.gramegrame.api.proto.java.AccountAmount;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.CryptoTransferTransactionBody;
import com.gramegrame.api.proto.java.SignedTransaction;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransferList;
import com.swirlds.fcmap.FCMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.BDDMockito.mock;

public class SigFactoryCreatorTest {
	FCMap<MerkleEntityId, MerkleSchedule> scheduledTxns;

	SignedTxnAccessor accessor;

	SigFactoryCreator subject;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		scheduledTxns = (FCMap<MerkleEntityId, MerkleSchedule>) mock(FCMap.class);

		subject = new SigFactoryCreator();
	}

	@Test
	public void usesBodyBytesForNonScheduleTxn() {
		givenScopedTxn(withoutLinkedSchedule());

		// when:
		var factory = subject.createScopedFactory(accessor);

		// then:
		assertThat(factory, instanceOf(BodySigningSigFactory.class));
	}

	private void givenScopedTxn(TransactionBody txn) {
		accessor = SignedTxnAccessor.uncheckedFrom(Transaction.newBuilder()
				.setSignedTransactionBytes(SignedTransaction.newBuilder()
						.setBodyBytes(txn.toByteString()).build().toByteString())
				.build());
	}

	private TransactionBody withoutLinkedSchedule() {
		return TransactionBody.newBuilder()
				.setMemo("You won't want to hear this.")
				.setCryptoTransfer(CryptoTransferTransactionBody.newBuilder()
						.setTransfers(TransferList.newBuilder()
								.addAccountAmounts(AccountAmount.newBuilder()
										.setAmount(123L)
										.setAccountID(AccountID.newBuilder().setAccountNum(75231)))))
				.build();
	}
}
