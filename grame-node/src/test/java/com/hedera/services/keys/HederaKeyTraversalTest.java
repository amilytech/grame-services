package com.grame.services.keys;

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

import com.google.protobuf.ByteString;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.test.factories.accounts.MerkleAccountFactory;
import com.grame.test.factories.keys.KeyTree;
import com.grame.services.legacy.core.jproto.JKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.MatcherAssert.assertThat;
import static com.grame.test.factories.keys.NodeFactory.*;

public class grameKeyTraversalTest {
	static KeyTree kt;

	@BeforeAll
	private static void setupAll() {
		kt = KeyTree.withRoot(
				list(
						ed25519(),
						threshold(1, list(list(ed25519(), ed25519()), ed25519()), ed25519()),
						ed25519(),
						list(threshold(2, ed25519(), ed25519(), ed25519(), ed25519()))
				)
		);
	}

	@Test
	public void visitsAllSimpleKeys() throws Exception {
		// given:
		JKey jKey = kt.asJKey();
		List<ByteString> expectedEd25519 = ed25519KeysFromKt(kt);

		// when:
		List<ByteString> visitedEd25519 = new ArrayList<>();
		grameKeyTraversal.visitSimpleKeys(jKey, simpleJKey ->
				visitedEd25519.add(ByteString.copyFrom(simpleJKey.getEd25519())));

		// expect:
		assertThat(visitedEd25519, contains(expectedEd25519.toArray(new ByteString[0])));
	}

	@Test
	public void countsSimpleKeys() throws Exception {
		// given:
		JKey jKey = kt.asJKey();

		// expect:
		assertEquals(10, grameKeyTraversal.numSimpleKeys(jKey));
	}

	@Test
	public void countsSimpleKeysForValidAccount() throws Exception {
		// given:
		JKey jKey = kt.asJKey();
		MerkleAccount account = MerkleAccountFactory.newAccount().accountKeys(jKey).get();

		// expect:
		assertEquals(10, grameKeyTraversal.numSimpleKeys(account));
	}

	@Test
	public void countsZeroSimpleKeysForWeirdAccount() throws Exception {
		// given:
		JKey jKey = kt.asJKey();
		MerkleAccount account = MerkleAccountFactory.newAccount().get();

		// expect:
		assertEquals(0, grameKeyTraversal.numSimpleKeys(account));
	}


	private List<ByteString> ed25519KeysFromKt(KeyTree kt) {
		List<ByteString> keys = new ArrayList<>();
		kt.traverseLeaves(leaf -> keys.add(leaf.asKey().getEd25519()));
		return keys;
	}
}
