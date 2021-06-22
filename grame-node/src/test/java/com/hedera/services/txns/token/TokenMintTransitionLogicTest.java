package com.grame.services.txns.token;

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
import com.grame.services.state.merkle.MerkleToken;
import com.grame.services.store.tokens.TokenStore;
import com.grame.services.utils.PlatformTxnAccessor;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.TokenID;
import com.gramegrame.api.proto.java.TokenMintTransactionBody;
import com.gramegrame.api.proto.java.TransactionBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.FAIL_INVALID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TOKEN_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TOKEN_MINT_AMOUNT;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.SUCCESS;
import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.never;

class TokenMintTransitionLogicTest {
	long amount = 123L;
	private TokenID id = IdUtils.asToken("1.2.3");

	private TokenStore tokenStore;
	private TransactionContext txnCtx;
	private PlatformTxnAccessor accessor;
	private MerkleToken token;

	private TransactionBody tokenMintTxn;
	private TokenMintTransitionLogic subject;

	@BeforeEach
	private void setup() {
		tokenStore = mock(TokenStore.class);
		accessor = mock(PlatformTxnAccessor.class);
		token = mock(MerkleToken.class);

		txnCtx = mock(TransactionContext.class);

		subject = new TokenMintTransitionLogic(tokenStore, txnCtx);
	}

	@Test
	public void capturesInvalidMint() {
		givenValidTxnCtx();
		// and:
		given(tokenStore.mint(id, amount)).willReturn(INVALID_TOKEN_MINT_AMOUNT);

		// when:
		subject.doStateTransition();

		// then:
		verify(txnCtx).setStatus(INVALID_TOKEN_MINT_AMOUNT);
	}

	@Test
	public void rejectsBadRefForSafety() {
		givenValidTxnCtx();
		// and:
		given(tokenStore.resolve(id)).willReturn(TokenStore.MISSING_TOKEN);

		// when:
		subject.doStateTransition();

		// then:
		verify(tokenStore, never()).mint(id, amount);
		verify(txnCtx).setStatus(INVALID_TOKEN_ID);
	}

	@Test
	public void followsHappyPath() {
		givenValidTxnCtx();
		// and:
		given(tokenStore.mint(id, amount)).willReturn(OK);

		// when:
		subject.doStateTransition();

		// then:
		verify(tokenStore).mint(id, amount);
		verify(txnCtx).setStatus(SUCCESS);
		verify(txnCtx).setNewTotalSupply(amount);
	}

	@Test
	public void hasCorrectApplicability() {
		givenValidTxnCtx();

		// expect:
		assertTrue(subject.applicability().test(tokenMintTxn));
		assertFalse(subject.applicability().test(TransactionBody.getDefaultInstance()));
	}

	@Test
	public void setsFailInvalidIfUnhandledException() {
		givenValidTxnCtx();
		// and:
		given(tokenStore.mint(any(), anyLong()))
				.willThrow(IllegalArgumentException.class);

		// when:
		subject.doStateTransition();

		// then:
		verify(txnCtx).setStatus(FAIL_INVALID);
	}

	@Test
	public void acceptsValidTxn() {
		givenValidTxnCtx();

		// expect:
		assertEquals(OK, subject.syntaxCheck().apply(tokenMintTxn));
	}

	@Test
	public void rejectsMissingToken() {
		givenMissingToken();

		// expect:
		assertEquals(INVALID_TOKEN_ID, subject.syntaxCheck().apply(tokenMintTxn));
	}

	@Test
	public void rejectsInvalidNegativeAmount() {
		givenInvalidNegativeAmount();

		// expect:
		assertEquals(INVALID_TOKEN_MINT_AMOUNT, subject.syntaxCheck().apply(tokenMintTxn));
	}

	@Test
	public void rejectsInvalidZeroAmount() {
		givenInvalidZeroAmount();

		// expect:
		assertEquals(INVALID_TOKEN_MINT_AMOUNT, subject.syntaxCheck().apply(tokenMintTxn));
	}

	private void givenValidTxnCtx() {
		tokenMintTxn = TransactionBody.newBuilder()
				.setTokenMint(TokenMintTransactionBody.newBuilder()
						.setToken(id)
						.setAmount(amount))
				.build();
		given(accessor.getTxn()).willReturn(tokenMintTxn);
		given(txnCtx.accessor()).willReturn(accessor);
		given(tokenStore.resolve(id)).willReturn(id);
		given(tokenStore.get(id)).willReturn(token);
		given(token.totalSupply()).willReturn(amount);
	}

	private void givenMissingToken() {
		tokenMintTxn = TransactionBody.newBuilder()
				.setTokenMint(
						TokenMintTransactionBody.newBuilder()
								.build()
				).build();
	}

	private void givenInvalidNegativeAmount() {
		tokenMintTxn = TransactionBody.newBuilder()
				.setTokenMint(
						TokenMintTransactionBody.newBuilder()
								.setToken(id)
								.setAmount(-1)
								.build()
				).build();
	}

	private void givenInvalidZeroAmount() {
		tokenMintTxn = TransactionBody.newBuilder()
				.setTokenMint(
						TokenMintTransactionBody.newBuilder()
								.setToken(id)
								.setAmount(0)
								.build()
				).build();
	}
}
