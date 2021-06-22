package com.grame.services.sigs.metadata;

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

import com.grame.services.legacy.core.jproto.JEd25519Key;
import com.grame.services.legacy.core.jproto.JKey;
import com.grame.services.sigs.metadata.lookups.SafeLookupResult;
import com.grame.services.sigs.order.KeyOrderingFailure;
import com.grame.services.state.merkle.MerkleToken;
import com.grame.services.state.submerkle.EntityId;
import com.grame.services.store.tokens.TokenStore;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.TokenID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

class DelegatingSigMetadataLookupTest {
	JKey adminKey;
	JKey freezeKey;
	String symbol = "NotAnHbar";
	String tokenName = "TokenName";
	int decimals = 2;
	long totalSupply = 1_000_000;
	boolean freezeDefault = true;
	boolean accountsKycGrantedByDefault = true;
	EntityId treasury = new EntityId(1,2, 3);
	TokenID id = IdUtils.asToken("1.2.666");

	MerkleToken token;
	TokenStore tokenStore;

	Function<TokenID, SafeLookupResult<TokenSigningMetadata>> subject;

	@BeforeEach
	public void setup() {
		adminKey = new JEd25519Key("not-a-real-admin-key".getBytes());
		freezeKey = new JEd25519Key("not-a-real-freeze-key".getBytes());

		token = new MerkleToken(Long.MAX_VALUE, totalSupply, decimals, symbol, tokenName,  freezeDefault, accountsKycGrantedByDefault, treasury);

		tokenStore = mock(TokenStore.class);

		subject = SigMetadataLookup.REF_LOOKUP_FACTORY.apply(tokenStore);
	}

	@Test
	public void returnsExpectedFailIfExplicitlyMissing() {
		given(tokenStore.resolve(id)).willReturn(TokenID.newBuilder()
				.setShardNum(0L)
				.setRealmNum(0L)
				.setTokenNum(0L)
				.build());

		// when:
		var result = subject.apply(id);

		// then:
		assertEquals(KeyOrderingFailure.MISSING_TOKEN, result.failureIfAny());
	}

	@Test
	public void returnsExpectedFailIfMissing() {
		given(tokenStore.resolve(id)).willReturn(TokenStore.MISSING_TOKEN);

		// when:
		var result = subject.apply(id);

		// then:
		assertEquals(KeyOrderingFailure.MISSING_TOKEN, result.failureIfAny());
	}

	@Test
	public void returnsExpectedMetaIfPresent() {
		// setup:
		token.setFreezeKey(freezeKey);
		var expected = TokenSigningMetadata.from(token);

		given(tokenStore.resolve(id)).willReturn(id);
		given(tokenStore.get(id)).willReturn(token);

		// when:
		var result = subject.apply(id);

		// then:
		assertEquals(KeyOrderingFailure.NONE, result.failureIfAny());
		// and:
		assertEquals(expected.adminKey(), result.metadata().adminKey());
		assertEquals(expected.optionalFreezeKey(), result.metadata().optionalFreezeKey());
	}
}
