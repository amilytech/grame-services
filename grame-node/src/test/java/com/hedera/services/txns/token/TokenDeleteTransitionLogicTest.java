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
import com.grame.services.store.tokens.TokenStore;
import com.grame.services.utils.PlatformTxnAccessor;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.TokenDeleteTransactionBody;
import com.gramegrame.api.proto.java.TokenID;
import com.gramegrame.api.proto.java.TransactionBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.FAIL_INVALID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TOKEN_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.SUCCESS;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.TOKEN_WAS_DELETED;
import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;

class TokenDeleteTransitionLogicTest {
	private TokenID tokenId = IdUtils.asToken("0.0.12345");
	private AccountID account = IdUtils.asAccount("0.0.54321");

	private TokenStore tokenStore;
	private TransactionContext txnCtx;
	private PlatformTxnAccessor accessor;

	private TransactionBody tokenDeleteTxn;
	private TokenDeleteTransitionLogic subject;

	@BeforeEach
	private void setup() {
		tokenStore = mock(TokenStore.class);
		accessor = mock(PlatformTxnAccessor.class);

		txnCtx = mock(TransactionContext.class);

		subject = new TokenDeleteTransitionLogic(tokenStore, txnCtx);
	}

	@Test
	public void capturesInvalidDelete() {
		givenValidTxnCtx();
		// and:
		given(tokenStore.delete(tokenId)).willReturn(INVALID_TOKEN_ID);

		// when:
		subject.doStateTransition();

		// then:
		verify(txnCtx).setStatus(INVALID_TOKEN_ID);
	}

	@Test
	public void capturesInvalidDeletionDueToAlreadyDeleted() {
		givenValidTxnCtx();
		// and:
		given(tokenStore.delete(tokenId)).willReturn(TOKEN_WAS_DELETED);

		// when:
		subject.doStateTransition();

		// then:
		verify(txnCtx).setStatus(TOKEN_WAS_DELETED);
	}

	@Test
	public void followsHappyPath() {
		givenValidTxnCtx();
		// and:
		given(tokenStore.delete(tokenId)).willReturn(OK);

		// when:
		subject.doStateTransition();

		// then:
		verify(tokenStore).delete(tokenId);
		verify(txnCtx).setStatus(SUCCESS);
	}

	@Test
	public void hasCorrectApplicability() {
		givenValidTxnCtx();

		// expect:
		assertTrue(subject.applicability().test(tokenDeleteTxn));
		assertFalse(subject.applicability().test(TransactionBody.getDefaultInstance()));
	}

	@Test
	public void setsFailInvalidIfUnhandledException() {
		givenValidTxnCtx();
		// and:
		given(tokenStore.delete(any())).willThrow(IllegalArgumentException.class);

		// when:
		subject.doStateTransition();

		// then:
		verify(txnCtx).setStatus(FAIL_INVALID);
	}

	@Test
	public void acceptsValidTxn() {
		givenValidTxnCtx();

		// expect:
		assertEquals(OK, subject.syntaxCheck().apply(tokenDeleteTxn));
	}

	@Test
	public void rejectsMissingToken() {
		givenMissingToken();

		// expect:
		assertEquals(INVALID_TOKEN_ID, subject.syntaxCheck().apply(tokenDeleteTxn));
	}

	private void givenValidTxnCtx() {
		tokenDeleteTxn = TransactionBody.newBuilder()
				.setTokenDeletion(TokenDeleteTransactionBody.newBuilder()
						.setToken(tokenId))
				.build();
		given(accessor.getTxn()).willReturn(tokenDeleteTxn);
		given(txnCtx.accessor()).willReturn(accessor);
	}

	private void givenMissingToken() {
		tokenDeleteTxn = TransactionBody.newBuilder()
				.setTokenDeletion(TokenDeleteTransactionBody.newBuilder())
				.build();
	}
}
