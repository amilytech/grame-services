package com.grame.services.txns.crypto;

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
import com.grame.services.exceptions.InsufficientFundsException;
import com.grame.services.ledger.accounts.grameAccountCustomizer;
import com.grame.services.ledger.grameLedger;
import com.grame.services.ledger.properties.AccountProperty;
import com.grame.services.txns.validation.OptionValidator;
import com.grame.services.utils.PlatformTxnAccessor;
import com.grame.test.factories.txns.SignedTxnFactory;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.CryptoCreateTransactionBody;
import com.gramegrame.api.proto.java.Duration;
import com.gramegrame.api.proto.java.Key;
import com.gramegrame.api.proto.java.KeyList;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionID;
import com.grame.services.state.submerkle.EntityId;
import com.grame.services.legacy.core.jproto.JKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.EnumMap;

import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.BDDMockito.*;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.*;
import static com.grame.services.ledger.properties.AccountProperty.*;

public class CryptoCreateTransitionLogicTest {
	final private Key key = SignedTxnFactory.DEFAULT_PAYER_KT.asKey();
	final private long customAutoRenewPeriod = 100_001L;
	final private long customSendThreshold = 49_000L;
	final private long customReceiveThreshold = 51_001L;
	final private Long balance = 1_234L;
	final private String memo = "The particular is pounded til it is man";
	final private AccountID proxy = AccountID.newBuilder().setAccountNum(4_321L).build();
	final private AccountID payer = AccountID.newBuilder().setAccountNum(1_234L).build();
	final private AccountID created = AccountID.newBuilder().setAccountNum(9_999L).build();

	private long expiry;
	private Instant consensusTime;
	private grameLedger ledger;
	private OptionValidator validator;
	private TransactionBody cryptoCreateTxn;
	private TransactionContext txnCtx;
	private PlatformTxnAccessor accessor;
	private CryptoCreateTransitionLogic subject;

	@BeforeEach
	private void setup() {
		consensusTime = Instant.now();

		txnCtx = mock(TransactionContext.class);
		given(txnCtx.consensusTime()).willReturn(consensusTime);
		ledger = mock(grameLedger.class);
		accessor = mock(PlatformTxnAccessor.class);
		validator = mock(OptionValidator.class);
		withRubberstampingValidator();

		subject = new CryptoCreateTransitionLogic(ledger, validator, txnCtx);
	}

	@Test
	public void hasCorrectApplicability() {
		givenValidTxnCtx();

		// expect:
		assertTrue(subject.applicability().test(cryptoCreateTxn));
		assertFalse(subject.applicability().test(TransactionBody.getDefaultInstance()));
	}

	@Test
	public void returnsMemoTooLongWhenValidatorSays() {
		givenValidTxnCtx();
		given(validator.memoCheck(memo)).willReturn(MEMO_TOO_LONG);

		// expect:
		assertEquals(MEMO_TOO_LONG, subject.syntaxCheck().apply(cryptoCreateTxn));
	}

	@Test
	public void returnsKeyRequiredOnEmptyKey() {
		givenValidTxnCtx(Key.newBuilder().setKeyList(KeyList.getDefaultInstance()).build());

		// expect:
		assertEquals(KEY_REQUIRED, subject.syntaxCheck().apply(cryptoCreateTxn));
	}

	@Test
	public void requiresKey() {
		givenMissingKey();

		// expect:
		assertEquals(KEY_REQUIRED, subject.syntaxCheck().apply(cryptoCreateTxn));
	}

	@Test
	public void rejectsMissingAutoRenewPeriod() {
		givenMissingAutoRenewPeriod();

		// expect:
		assertEquals(INVALID_RENEWAL_PERIOD, subject.syntaxCheck().apply(cryptoCreateTxn));
	}

	@Test
	public void rejectsNegativeBalance() {
		givenAbsurdInitialBalance();

		// expect:
		assertEquals(INVALID_INITIAL_BALANCE, subject.syntaxCheck().apply(cryptoCreateTxn));
	}

	@Test
	public void rejectsNegativeSendThreshold() {
		givenAbsurdSendThreshold();

		// expect:
		assertEquals(INVALID_SEND_RECORD_THRESHOLD, subject.syntaxCheck().apply(cryptoCreateTxn));
	}

	@Test
	public void rejectsNegativeReceiveThreshold() {
		givenAbsurdReceiveThreshold();

		// expect:
		assertEquals(INVALID_RECEIVE_RECORD_THRESHOLD, subject.syntaxCheck().apply(cryptoCreateTxn));
	}

	@Test
	public void rejectsKeyWithBadEncoding() {
		givenValidTxnCtx();
		given(validator.hasGoodEncoding(any())).willReturn(false);

		// expect:
		assertEquals(BAD_ENCODING, subject.syntaxCheck().apply(cryptoCreateTxn));
	}

	@Test
	public void rejectsInvalidAutoRenewPeriod() {
		givenValidTxnCtx();
		given(validator.isValidAutoRenewPeriod(any())).willReturn(false);

		// expect:
		assertEquals(AUTORENEW_DURATION_NOT_IN_RANGE, subject.syntaxCheck().apply(cryptoCreateTxn));
	}

	@Test
	public void acceptsValidTxn() {
		givenValidTxnCtx();

		// expect:
		assertEquals(OK, subject.syntaxCheck().apply(cryptoCreateTxn));
	}

	@Test
	public void followsHappyPathWithOverrides() throws Throwable {
		// setup:
		ArgumentCaptor<grameAccountCustomizer> captor = ArgumentCaptor.forClass(grameAccountCustomizer.class);
		expiry = consensusTime.getEpochSecond() + customAutoRenewPeriod;

		givenValidTxnCtx();
		// and:
		given(ledger.create(any(), anyLong(), any())).willReturn(created);

		// when:
		subject.doStateTransition();

		// then:
		verify(ledger).create(argThat(payer::equals), longThat(balance::equals), captor.capture());
		verify(txnCtx).setCreated(created);
		verify(txnCtx).setStatus(SUCCESS);
		// and:
		EnumMap<AccountProperty, Object> changes = captor.getValue().getChanges();
		assertEquals(6, changes.size());
		assertEquals(customAutoRenewPeriod, (long)changes.get(AUTO_RENEW_PERIOD));
		assertEquals(expiry, (long)changes.get(EXPIRY));
		assertEquals(key, JKey.mapJKey((JKey)changes.get(KEY)));
		assertEquals(true, changes.get(IS_RECEIVER_SIG_REQUIRED));
		assertEquals(EntityId.ofNullableAccountId(proxy), changes.get(PROXY));
		assertEquals(memo, changes.get(MEMO));
	}

	@Test
	public void translatesInsufficientPayerBalance() {
		givenValidTxnCtx();
		given(ledger.create(any(), anyLong(), any())).willThrow(InsufficientFundsException.class);

		// when:
		subject.doStateTransition();

		// then:
		verify(txnCtx).setStatus(INSUFFICIENT_PAYER_BALANCE);
	}

	@Test
	public void translatesUnknownException() {
		givenValidTxnCtx();
		cryptoCreateTxn = cryptoCreateTxn.toBuilder()
				.setCryptoCreateAccount(cryptoCreateTxn.getCryptoCreateAccount().toBuilder().setKey(unmappableKey()))
				.build();
		given(accessor.getTxn()).willReturn(cryptoCreateTxn);
		given(txnCtx.accessor()).willReturn(accessor);

		// when:
		subject.doStateTransition();

		// then:
		verify(txnCtx).setStatus(FAIL_INVALID);
	}

	private Key unmappableKey() {
		return Key.getDefaultInstance();
	}

	private void givenMissingKey() {
		cryptoCreateTxn = TransactionBody.newBuilder()
				.setTransactionID(ourTxnId())
				.setCryptoCreateAccount(
						CryptoCreateTransactionBody.newBuilder()
								.setInitialBalance(balance)
								.build()
				).build();
	}

	private void givenMissingAutoRenewPeriod() {
		cryptoCreateTxn = TransactionBody.newBuilder()
				.setTransactionID(ourTxnId())
				.setCryptoCreateAccount(
						CryptoCreateTransactionBody.newBuilder()
								.setKey(key)
								.setInitialBalance(balance)
								.build()
				).build();
	}

	private void givenAbsurdSendThreshold() {
		cryptoCreateTxn = TransactionBody.newBuilder()
				.setTransactionID(ourTxnId())
				.setCryptoCreateAccount(
						CryptoCreateTransactionBody.newBuilder()
								.setAutoRenewPeriod(Duration.newBuilder().setSeconds(1L))
								.setKey(key)
								.setSendRecordThreshold(-1L)
								.build()
				).build();
	}

	private void givenAbsurdReceiveThreshold() {
		cryptoCreateTxn = TransactionBody.newBuilder()
				.setTransactionID(ourTxnId())
				.setCryptoCreateAccount(
						CryptoCreateTransactionBody.newBuilder()
								.setAutoRenewPeriod(Duration.newBuilder().setSeconds(1L))
								.setKey(key)
								.setReceiveRecordThreshold(-1L)
								.build()
				).build();
	}

	private void givenAbsurdInitialBalance() {
		cryptoCreateTxn = TransactionBody.newBuilder()
				.setTransactionID(ourTxnId())
				.setCryptoCreateAccount(
						CryptoCreateTransactionBody.newBuilder()
								.setAutoRenewPeriod(Duration.newBuilder().setSeconds(1L))
								.setKey(key)
								.setInitialBalance(-1L)
								.build()
				).build();
	}

	private void givenValidTxnCtx() {
		givenValidTxnCtx(key);
	}

	private void givenValidTxnCtx(Key toUse) {
		cryptoCreateTxn = TransactionBody.newBuilder()
				.setTransactionID(ourTxnId())
				.setCryptoCreateAccount(
						CryptoCreateTransactionBody.newBuilder()
								.setMemo(memo)
								.setInitialBalance(balance)
								.setProxyAccountID(proxy)
								.setReceiverSigRequired(true)
								.setAutoRenewPeriod(Duration.newBuilder().setSeconds(customAutoRenewPeriod))
								.setReceiveRecordThreshold(customReceiveThreshold)
								.setSendRecordThreshold(customSendThreshold)
								.setKey(toUse)
								.build()
				).build();
		given(accessor.getTxn()).willReturn(cryptoCreateTxn);
		given(txnCtx.accessor()).willReturn(accessor);
	}

	private TransactionID ourTxnId() {
		return TransactionID.newBuilder()
				.setAccountID(payer)
				.setTransactionValidStart(
						Timestamp.newBuilder().setSeconds(consensusTime.getEpochSecond()))
				.build();
	}

	private void withRubberstampingValidator() {
		given(validator.isValidAutoRenewPeriod(any())).willReturn(true);
		given(validator.hasGoodEncoding(any())).willReturn(true);
		given(validator.memoCheck(any())).willReturn(OK);
	}
}
