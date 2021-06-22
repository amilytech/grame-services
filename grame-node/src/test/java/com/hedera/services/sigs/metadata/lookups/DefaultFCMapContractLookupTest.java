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
import com.grame.services.legacy.core.jproto.JKeyList;
import com.grame.services.sigs.order.KeyOrderingFailure;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.ContractID;
import com.swirlds.fcmap.FCMap;
import org.junit.jupiter.api.Test;

import static com.grame.test.factories.accounts.MerkleAccountFactory.newAccount;
import static com.grame.test.factories.accounts.MerkleAccountFactory.newContract;
import static com.grame.test.factories.accounts.MockFCMapFactory.newAccounts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultFCMapContractLookupTest {
	private final String id = "0.0.1337";
	private final ContractID contract = IdUtils.asContract(id);
	private FCMap<MerkleEntityId, MerkleAccount> accounts;
	private DefaultFCMapContractLookup subject;

	@Test
	public void failsSafelyOnMissingAccount() {
		// given:
		accounts = newAccounts().get();
		subject = new DefaultFCMapContractLookup(() -> accounts);

		// when:
		var result = subject.safeLookup(contract);

		// then:
		assertFalse(result.succeeded());
		assertEquals(KeyOrderingFailure.INVALID_CONTRACT, result.failureIfAny());
	}

	@Test
	public void failsOnDeletedAccount() {
		// given:
		accounts = newAccounts().withAccount(id, newContract().deleted(true).get()).get();
		subject = new DefaultFCMapContractLookup(() -> accounts);

		// when:
		var result = subject.safeLookup(contract);

		// then:
		assertFalse(result.succeeded());
		assertEquals(KeyOrderingFailure.INVALID_CONTRACT, result.failureIfAny());
	}

	@Test
	public void failsNormalAccountInsteadOfSmartContract() {
		// given:
		accounts = newAccounts().withAccount(id, newAccount().get()).get();
		subject = new DefaultFCMapContractLookup(() -> accounts);

		// when:
		var result = subject.safeLookup(contract);

		// then:
		assertFalse(result.succeeded());
		assertEquals(KeyOrderingFailure.INVALID_CONTRACT, result.failureIfAny());
	}

	@Test
	public void failsOnNullAccountKeys() {
		// given:
		accounts = newAccounts().withAccount(id, newContract().get()).get();
		subject = new DefaultFCMapContractLookup(() -> accounts);

		// when:
		var result = subject.safeLookup(contract);

		// then:
		assertFalse(result.succeeded());
		assertEquals(KeyOrderingFailure.IMMUTABLE_CONTRACT, result.failureIfAny());
	}

	@Test
	public void failsOnContractIdKey() {
		// given:
		accounts = newAccounts().withAccount(id, newContract().accountKeys(new JContractIDKey(contract)).get()).get();
		subject = new DefaultFCMapContractLookup(() -> accounts);

		// when:
		var result = subject.safeLookup(contract);

		// then:
		assertFalse(result.succeeded());
		assertEquals(KeyOrderingFailure.IMMUTABLE_CONTRACT, result.failureIfAny());
	}

	@Test
	public void returnsLegalKey() throws Exception {
		// given:
		JKey desiredKey = new JKeyList();
		accounts = newAccounts().withAccount(id, newContract().accountKeys(desiredKey).get()).get();
		subject = new DefaultFCMapContractLookup(() -> accounts);

		// when:
		var result = subject.safeLookup(contract);

		// then:
		assertTrue(result.succeeded());
	}
}
