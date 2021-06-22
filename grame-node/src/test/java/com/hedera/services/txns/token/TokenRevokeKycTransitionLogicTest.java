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
import com.grame.services.ledger.grameLedger;
import com.grame.services.store.tokens.TokenStore;
import com.grame.services.utils.PlatformTxnAccessor;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.TokenID;
import com.gramegrame.api.proto.java.TokenRevokeKycTransactionBody;
import com.gramegrame.api.proto.java.TransactionBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.FAIL_INVALID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_ACCOUNT_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TOKEN_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.SUCCESS;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.TOKEN_HAS_NO_KYC_KEY;
import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;

class TokenRevokeKycTransitionLogicTest {
	private TokenID tokenId = IdUtils.asToken("0.0.12345");
	private AccountID account = IdUtils.asAccount("0.0.54321");

	private TokenStore tokenStore;
	private grameLedger ledger;
	private TransactionContext txnCtx;
	private PlatformTxnAccessor accessor;

	private TransactionBody tokenRevokeKycTxn;
	private TokenRevokeKycTransitionLogic subject;

	@BeforeEach
	private void setup() {
		ledger = mock(grameLedger.class);
		tokenStore = mock(TokenStore.class);
		accessor = mock(PlatformTxnAccessor.class);

		txnCtx = mock(TransactionContext.class);

		subject = new TokenRevokeKycTransitionLogic(tokenStore, ledger, txnCtx);
	}

	@Test
	public void capturesInvalidRevokeKyc() {
		givenValidTxnCtx();
		// and:
		given(ledger.revokeKyc(account, tokenId)).willReturn(TOKEN_HAS_NO_KYC_KEY);

		// when:
		subject.doStateTransition();

		// then:
		verify(txnCtx).setStatus(TOKEN_HAS_NO_KYC_KEY);
	}

	@Test
	public void followsHappyPath() {
		givenValidTxnCtx();
		// and:
		given(ledger.revokeKyc(account, tokenId)).willReturn(OK);

		// when:
		subject.doStateTransition();

		// then:
		verify(ledger).revokeKyc(account, tokenId);
		verify(txnCtx).setStatus(SUCCESS);
	}

	@Test
	public void hasCorrectApplicability() {
		givenValidTxnCtx();

		// expect:
		assertTrue(subject.applicability().test(tokenRevokeKycTxn));
		assertFalse(subject.applicability().test(TransactionBody.getDefaultInstance()));
	}

	@Test
	public void setsFailInvalidIfUnhandledException() {
		givenValidTxnCtx();
		// and:
		given(ledger.revokeKyc(any(), any())).willThrow(IllegalArgumentException.class);

		// when:
		subject.doStateTransition();

		// then:
		verify(txnCtx).setStatus(FAIL_INVALID);
	}

	@Test
	public void acceptsValidTxn() {
		givenValidTxnCtx();

		// expect:
		assertEquals(OK, subject.syntaxCheck().apply(tokenRevokeKycTxn));
	}

	@Test
	public void rejectsMissingToken() {
		givenMissingToken();

		// expect:
		assertEquals(INVALID_TOKEN_ID, subject.syntaxCheck().apply(tokenRevokeKycTxn));
	}

	@Test
	public void rejectsMissingAccount() {
		givenMissingAccount();

		// expect:
		assertEquals(INVALID_ACCOUNT_ID, subject.syntaxCheck().apply(tokenRevokeKycTxn));
	}

	private void givenValidTxnCtx() {
		tokenRevokeKycTxn = TransactionBody.newBuilder()
				.setTokenRevokeKyc(TokenRevokeKycTransactionBody.newBuilder()
						.setAccount(account)
						.setToken(tokenId))
				.build();
		given(accessor.getTxn()).willReturn(tokenRevokeKycTxn);
		given(txnCtx.accessor()).willReturn(accessor);
		given(tokenStore.resolve(tokenId)).willReturn(tokenId);
	}

	private void givenMissingToken() {
		tokenRevokeKycTxn = TransactionBody.newBuilder()
				.setTokenRevokeKyc(TokenRevokeKycTransactionBody.newBuilder())
				.build();
	}

	private void givenMissingAccount() {
		tokenRevokeKycTxn = TransactionBody.newBuilder()
				.setTokenRevokeKyc(TokenRevokeKycTransactionBody.newBuilder()
						.setToken(tokenId))
				.build();
	}
}
