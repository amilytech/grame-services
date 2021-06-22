package com.grame.services.fees.calculation;

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

import com.grame.services.context.primitives.StateView;
import com.grame.services.fees.FeeMultiplierSource;
import com.grame.services.fees.HbarCentExchange;
import com.grame.services.legacy.core.jproto.JKey;
import com.grame.services.utils.SignedTxnAccessor;
import com.grame.test.factories.keys.KeyTree;
import com.grame.test.factories.scenarios.TxnHandlingScenario;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.ExchangeRate;
import com.gramegrame.api.proto.java.FeeComponents;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.exception.InvalidTxBodyException;
import com.gramegrame.fee.FeeBuilder;
import com.gramegrame.fee.FeeObject;
import com.gramegrame.fee.SigValueObj;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static com.grame.services.fees.calculation.AwareFcfsUsagePrices.DEFAULT_USAGE_PRICES;
import static com.grame.test.factories.txns.CryptoCreateFactory.newSignedCryptoCreate;
import static com.grame.test.factories.txns.CryptoTransferFactory.newSignedCryptoTransfer;
import static com.grame.test.factories.txns.ContractCreateFactory.newSignedContractCreate;
import static com.grame.test.factories.txns.ContractCallFactory.newSignedContractCall;
import static com.grame.test.factories.txns.FileCreateFactory.newSignedFileCreate;
import static com.grame.test.factories.txns.TinyBarsFromTo.tinyBarsFromTo;
import static com.grame.test.utils.IdUtils.asAccountString;
import static com.gramegrame.api.proto.java.grameFunctionality.ContractCall;
import static com.gramegrame.api.proto.java.grameFunctionality.ContractCreate;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoCreate;
import static com.gramegrame.api.proto.java.ResponseType.ANSWER_ONLY;
import static com.gramegrame.fee.FeeBuilder.FEE_DIVISOR_FACTOR;
import static com.gramegrame.fee.FeeBuilder.getTinybarsFromTinyCents;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.willThrow;

class UsageBasedFeeCalculatorTest {
	FeeComponents mockFees = FeeComponents.newBuilder()
			.setMax(1_234_567L)
			.setGas(5_000_000L)
			.setBpr(1_000_000L)
			.setBpt(2_000_000L)
			.setRbh(3_000_000L)
			.setSbh(4_000_000L).build();
	FeeData mockFeeData = FeeData.newBuilder()
			.setNetworkdata(mockFees).setNodedata(mockFees).setServicedata(mockFees).build();
	FeeData currentPrices = mockFeeData;
	FeeData resourceUsage = mockFeeData;
	ExchangeRate currentRate = ExchangeRate.newBuilder().setCentEquiv(22).setHbarEquiv(1).build();
	Query query;
	StateView view;
	Timestamp at = Timestamp.newBuilder().setSeconds(1_234_567L).build();
	HbarCentExchange exchange;
	UsagePricesProvider usagePrices;
	TxnResourceUsageEstimator correctOpEstimator;
	TxnResourceUsageEstimator incorrectOpEstimator;
	QueryResourceUsageEstimator correctQueryEstimator;
	QueryResourceUsageEstimator incorrectQueryEstimator;
	Function<grameFunctionality, List<TxnResourceUsageEstimator>> txnUsageEstimators;

	long balance = 1_234_567L;
	AccountID payer = IdUtils.asAccount("0.0.75231");
	AccountID receiver = IdUtils.asAccount("0.0.86342");
	UsageBasedFeeCalculator subject;
	/* Has nine simple keys. */
	KeyTree complexKey = TxnHandlingScenario.COMPLEX_KEY_ACCOUNT_KT;
	JKey payerKey;
	Transaction signedTxn;
	SignedTxnAccessor accessor;

	AtomicLong suggestedMultiplier = new AtomicLong(1L);

	@BeforeEach
	private void setup() throws Throwable {
		view = mock(StateView.class);
		query = mock(Query.class);
		payerKey = complexKey.asJKey();
		exchange = mock(HbarCentExchange.class);
		signedTxn = newSignedCryptoCreate()
				.balance(balance)
				.payerKt(complexKey)
				.txnValidStart(at)
				.get();
		accessor = new SignedTxnAccessor(signedTxn);
		usagePrices = mock(UsagePricesProvider.class);
		given(usagePrices.activePrices()).willReturn(currentPrices);
		correctOpEstimator = mock(TxnResourceUsageEstimator.class);
		incorrectOpEstimator = mock(TxnResourceUsageEstimator.class);
		correctQueryEstimator = mock(QueryResourceUsageEstimator.class);
		incorrectQueryEstimator = mock(QueryResourceUsageEstimator.class);

		txnUsageEstimators = (Function<grameFunctionality, List<TxnResourceUsageEstimator>>)mock(Function.class);

		subject = new UsageBasedFeeCalculator(
				exchange,
				usagePrices,
				new NestedMultiplierSource(),
				List.of(incorrectQueryEstimator, correctQueryEstimator),
				txnUsageEstimators);
	}

	@Test
	public void estimatesContractCallPayerBalanceChanges() throws Throwable {
		// setup:
		long gas = 1_234L, sent = 5_432L;
		signedTxn = newSignedContractCall()
				.payer(asAccountString(payer))
				.gas(gas)
				.sending(sent)
				.txnValidStart(at)
				.get();
		accessor = new SignedTxnAccessor(signedTxn);

		given(exchange.rate(at)).willReturn(currentRate);
		given(usagePrices.pricesGiven(ContractCall, at)).willReturn(currentPrices);
		// and:
		long expectedGasPrice =
				getTinybarsFromTinyCents(currentRate, mockFees.getGas() / FEE_DIVISOR_FACTOR);

		// expect:
		assertEquals(-(gas * expectedGasPrice + sent), subject.estimatedNonFeePayerAdjustments(accessor, at));
	}

	@Test
	public void estimatesCryptoCreatePayerBalanceChanges() throws Throwable {
		// expect:
		assertEquals(-balance, subject.estimatedNonFeePayerAdjustments(accessor, at));
	}

	@Test
	public void estimatesContractCreatePayerBalanceChanges() throws Throwable {
		// setup:
		long gas = 1_234L, initialBalance = 5_432L;
		signedTxn = newSignedContractCreate()
				.payer(asAccountString(payer))
				.gas(gas)
				.initialBalance(initialBalance)
				.txnValidStart(at)
				.get();
		accessor = new SignedTxnAccessor(signedTxn);

		given(exchange.rate(at)).willReturn(currentRate);
		given(usagePrices.pricesGiven(ContractCreate, at)).willReturn(currentPrices);
		// and:
		long expectedGasPrice =
				getTinybarsFromTinyCents(currentRate, mockFees.getGas() / FEE_DIVISOR_FACTOR);

		// expect:
		assertEquals(-(gas * expectedGasPrice + initialBalance), subject.estimatedNonFeePayerAdjustments(accessor, at));
	}

	@Test
	public void estimatesMiscNoNetChange() throws Throwable {
		// setup:
		signedTxn = newSignedFileCreate()
				.payer(asAccountString(payer))
				.txnValidStart(at)
				.get();
		accessor = new SignedTxnAccessor(signedTxn);

		// expect:
		assertEquals(0L, subject.estimatedNonFeePayerAdjustments(accessor, at));
	}

	@Test
	public void estimatesCryptoTransferPayerBalanceChanges() throws Throwable {
		// setup:
		long sent = 1_234L;
		signedTxn = newSignedCryptoTransfer()
				.payer(asAccountString(payer))
				.transfers(tinyBarsFromTo(asAccountString(payer), asAccountString(receiver), sent))
				.txnValidStart(at)
				.get();
		accessor = new SignedTxnAccessor(signedTxn);

		// expect:
		assertEquals(-sent, subject.estimatedNonFeePayerAdjustments(accessor, at));
	}

	@Test
	public void estimatesFutureGasPriceInTinybars() {
		given(exchange.rate(at)).willReturn(currentRate);
		given(usagePrices.pricesGiven(CryptoCreate, at)).willReturn(currentPrices);
		// and:
		long expected = getTinybarsFromTinyCents(currentRate, mockFees.getGas() / FEE_DIVISOR_FACTOR);

		// when:
		long actual = subject.estimatedGasPriceInTinybars(CryptoCreate, at);

		// then:
		assertEquals(expected, actual);
	}

	@Test
	public void computesActiveGasPriceInTinybars() {
		given(exchange.activeRate()).willReturn(currentRate);
		// and:
		long expected = getTinybarsFromTinyCents(currentRate, mockFees.getGas() / FEE_DIVISOR_FACTOR);

		// when:
		long actual = subject.activeGasPriceInTinybars();

		// then:
		assertEquals(expected, actual);
	}

	@Test
	public void loadPriceSchedulesOnInit() throws Exception {
		// when:
		subject.init();

		// expect:
		verify(usagePrices).loadPriceSchedules();
	}

	@Test
	public void throwsIseOnBadScheduleInFcfs() {
		willThrow(IllegalStateException.class).given(usagePrices).loadPriceSchedules();

		// expect:
		assertThrows(IllegalStateException.class, () -> subject.init());
	}

	@Test
	public void failsWithIseGivenApplicableButUnusableCalculator() throws InvalidTxBodyException {
		// setup:
		SigValueObj expectedSigUsage = new SigValueObj(
				FeeBuilder.getSignatureCount(signedTxn),
				9,
				FeeBuilder.getSignatureSize(signedTxn));

		given(correctOpEstimator.applicableTo(accessor.getTxn())).willReturn(true);
		given(txnUsageEstimators.apply(CryptoCreate)).willReturn(List.of(correctOpEstimator));
		given(correctOpEstimator.usageGiven(
				argThat(accessor.getTxn()::equals),
				argThat(factory.apply(expectedSigUsage)),
				argThat(view::equals))).willThrow(InvalidTxBodyException.class);

		// when:
		assertThrows(IllegalArgumentException.class, () -> subject.computeFee(accessor, payerKey, view));
	}

	@Test
	public void failsWithNseeSansApplicableUsageCalculator() {
		// expect:
		assertThrows(NoSuchElementException.class, () -> subject.computeFee(accessor, payerKey, view));
		assertThrows(NoSuchElementException.class,
				() -> subject.computePayment(query, currentPrices, view, at, Collections.emptyMap()));
	}

	@Test
	public void invokesQueryDelegateAsExpected() {
		// setup:
		FeeObject expectedFees = FeeBuilder.getFeeObject(currentPrices, resourceUsage, currentRate);

		given(correctQueryEstimator.applicableTo(query)).willReturn(true);
		given(incorrectQueryEstimator.applicableTo(query)).willReturn(false);
		given(correctQueryEstimator.usageGiven(
				argThat(query::equals),
				argThat(view::equals),
				any())).willReturn(resourceUsage);
		given(incorrectQueryEstimator.usageGiven(any(), any())).willThrow(RuntimeException.class);
		given(exchange.rate(at)).willReturn(currentRate);

		// when:
		FeeObject fees = subject.computePayment(query, currentPrices, view, at, Collections.emptyMap());

		// then:
		assertEquals(fees.getNodeFee(), expectedFees.getNodeFee());
		assertEquals(fees.getNetworkFee(), expectedFees.getNetworkFee());
		assertEquals(fees.getServiceFee(), expectedFees.getServiceFee());
	}

	@Test
	public void invokesQueryDelegateByTypeAsExpected() {
		// setup:
		FeeObject expectedFees = FeeBuilder.getFeeObject(currentPrices, resourceUsage, currentRate);

		given(correctQueryEstimator.applicableTo(query)).willReturn(true);
		given(incorrectQueryEstimator.applicableTo(query)).willReturn(false);
		given(correctQueryEstimator.usageGivenType(query, view, ANSWER_ONLY)).willReturn(resourceUsage);
		given(incorrectQueryEstimator.usageGivenType(any(), any(), any())).willThrow(RuntimeException.class);
		given(exchange.rate(at)).willReturn(currentRate);

		// when:
		FeeObject fees = subject.estimatePayment(query, currentPrices, view, at, ANSWER_ONLY);

		// then:
		assertEquals(fees.getNodeFee(), expectedFees.getNodeFee());
		assertEquals(fees.getNetworkFee(), expectedFees.getNetworkFee());
		assertEquals(fees.getServiceFee(), expectedFees.getServiceFee());
	}

	@Test
	public void usesMultiplierAsExpected() throws Exception {
		// setup:
		long multiplier = 5L;
		SigValueObj expectedSigUsage = new SigValueObj(
				FeeBuilder.getSignatureCount(signedTxn),
				9,
				FeeBuilder.getSignatureSize(signedTxn));
		FeeObject expectedFees = FeeBuilder.getFeeObject(currentPrices, resourceUsage, currentRate, multiplier);
		suggestedMultiplier.set(multiplier);

		given(correctOpEstimator.applicableTo(accessor.getTxn())).willReturn(true);
		given(txnUsageEstimators.apply(CryptoCreate)).willReturn(List.of(correctOpEstimator));
		given(correctOpEstimator.usageGiven(
				argThat(accessor.getTxn()::equals),
				argThat(factory.apply(expectedSigUsage)),
				argThat(view::equals))).willReturn(resourceUsage);
		given(exchange.activeRate()).willReturn(currentRate);

		// when:
		FeeObject fees = subject.computeFee(accessor, payerKey, view);

		// then:
		assertEquals(fees.getNodeFee(), expectedFees.getNodeFee());
		assertEquals(fees.getNetworkFee(), expectedFees.getNetworkFee());
		assertEquals(fees.getServiceFee(), expectedFees.getServiceFee());
	}

	@Test
	public void invokesOpDelegateAsExpectedWithOneOption() throws Exception {
		// setup:
		SigValueObj expectedSigUsage = new SigValueObj(
				FeeBuilder.getSignatureCount(signedTxn),
				9,
				FeeBuilder.getSignatureSize(signedTxn));
		FeeObject expectedFees = FeeBuilder.getFeeObject(currentPrices, resourceUsage, currentRate);

		given(correctOpEstimator.applicableTo(accessor.getTxn())).willReturn(true);
		given(txnUsageEstimators.apply(CryptoCreate)).willReturn(List.of(correctOpEstimator));
		given(correctOpEstimator.usageGiven(
				argThat(accessor.getTxn()::equals),
				argThat(factory.apply(expectedSigUsage)),
				argThat(view::equals))).willReturn(resourceUsage);
		given(exchange.activeRate()).willReturn(currentRate);

		// when:
		FeeObject fees = subject.computeFee(accessor, payerKey, view);

		// then:
		assertEquals(fees.getNodeFee(), expectedFees.getNodeFee());
		assertEquals(fees.getNetworkFee(), expectedFees.getNetworkFee());
		assertEquals(fees.getServiceFee(), expectedFees.getServiceFee());
	}

	@Test
	public void invokesOpDelegateAsExpectedWithTwoOptions() throws Exception {
		// setup:
		SigValueObj expectedSigUsage = new SigValueObj(
				FeeBuilder.getSignatureCount(signedTxn),
				9,
				FeeBuilder.getSignatureSize(signedTxn));
		FeeObject expectedFees = FeeBuilder.getFeeObject(currentPrices, resourceUsage, currentRate);

		given(correctOpEstimator.applicableTo(accessor.getTxn())).willReturn(true);
		given(incorrectOpEstimator.applicableTo(accessor.getTxn())).willReturn(false);
		given(txnUsageEstimators.apply(CryptoCreate)).willReturn(List.of(incorrectOpEstimator, correctOpEstimator));
		given(correctOpEstimator.usageGiven(
				argThat(accessor.getTxn()::equals),
				argThat(factory.apply(expectedSigUsage)),
				argThat(view::equals))).willReturn(resourceUsage);
		given(incorrectOpEstimator.usageGiven(any(), any(), any())).willThrow(RuntimeException.class);
		given(exchange.rate(at)).willReturn(currentRate);
		given(usagePrices.activePrices()).willThrow(RuntimeException.class);
		given(usagePrices.pricesGiven(CryptoCreate, at)).willReturn(currentPrices);

		// when:
		FeeObject fees = subject.estimateFee(accessor, payerKey, view, at);

		// then:
		assertEquals(fees.getNodeFee(), expectedFees.getNodeFee());
		assertEquals(fees.getNetworkFee(), expectedFees.getNetworkFee());
		assertEquals(fees.getServiceFee(), expectedFees.getServiceFee());
	}


	@Test
	public void invokesOpDelegateAsExpectedForEstimateOfUnrecognizable() throws Exception {
		// setup:
		SigValueObj expectedSigUsage = new SigValueObj(
				FeeBuilder.getSignatureCount(signedTxn),
				9,
				FeeBuilder.getSignatureSize(signedTxn));
		FeeObject expectedFees = FeeBuilder.getFeeObject(DEFAULT_USAGE_PRICES, resourceUsage, currentRate);

		given(txnUsageEstimators.apply(CryptoCreate)).willReturn(List.of(correctOpEstimator));
		given(correctOpEstimator.applicableTo(accessor.getTxn())).willReturn(true);
		given(correctOpEstimator.usageGiven(
				argThat(accessor.getTxn()::equals),
				argThat(factory.apply(expectedSigUsage)),
				argThat(view::equals))).willReturn(resourceUsage);
		given(exchange.rate(at)).willReturn(currentRate);
		given(usagePrices.pricesGiven(CryptoCreate, at)).willThrow(RuntimeException.class);

		// when:
		FeeObject fees = subject.estimateFee(accessor, payerKey, view, at);

		// then:
		assertEquals(fees.getNodeFee(), expectedFees.getNodeFee());
		assertEquals(fees.getNetworkFee(), expectedFees.getNetworkFee());
		assertEquals(fees.getServiceFee(), expectedFees.getServiceFee());
	}

	private Function<SigValueObj, ArgumentMatcher<SigValueObj>> factory = expectedSigUsage -> sigUsage ->
			expectedSigUsage.getSignatureSize() == sigUsage.getSignatureSize()
					&& expectedSigUsage.getPayerAcctSigCount() == sigUsage.getPayerAcctSigCount()
					&& expectedSigUsage.getSignatureSize() == sigUsage.getSignatureSize();

	private class NestedMultiplierSource implements FeeMultiplierSource {
		@Override
		public long currentMultiplier() {
			return suggestedMultiplier.get();
		}

		@Override
		public void resetExpectations() {
			/* No-op */
		}

		@Override
		public void updateMultiplier(Instant consensusNow) {
			/* No-op. */
		}

		@Override
		public void resetCongestionLevelStarts(Instant[] savedStartTimes) {
			/* No-op. */
		}

		@Override
		public Instant[] congestionLevelStarts() {
			return new Instant[0];
		}
	}
}
