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
import com.grame.services.fees.FeeCalculator;
import com.grame.services.fees.FeeMultiplierSource;
import com.grame.services.fees.HbarCentExchange;
import com.grame.services.keys.grameKeyTraversal;
import com.grame.services.legacy.core.jproto.JKey;
import com.grame.services.utils.TxnAccessor;
import com.gramegrame.api.proto.java.AccountAmount;
import com.gramegrame.api.proto.java.ExchangeRate;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.ResponseType;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.exception.InvalidTxBodyException;
import com.gramegrame.fee.FeeBuilder;
import com.gramegrame.fee.FeeObject;
import com.gramegrame.fee.SigValueObj;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

import static com.grame.services.fees.calculation.AwareFcfsUsagePrices.DEFAULT_USAGE_PRICES;
import static com.gramegrame.api.proto.java.grameFunctionality.ContractCall;
import static com.gramegrame.api.proto.java.grameFunctionality.ContractCreate;
import static com.gramegrame.fee.FeeBuilder.FEE_DIVISOR_FACTOR;
import static com.gramegrame.fee.FeeBuilder.getTinybarsFromTinyCents;

/**
 * Implements a {@link FeeCalculator} in terms of injected usage prices,
 * exchange rates, and collections of estimators which can infer the
 * resource usage of various transactions and queries.
 *
 * @author AmilyTech
 */
public class UsageBasedFeeCalculator implements FeeCalculator {
	private static final Logger log = LogManager.getLogger(UsageBasedFeeCalculator.class);

	private final HbarCentExchange exchange;
	private final FeeMultiplierSource feeMultiplierSource;
	private final UsagePricesProvider usagePrices;
	private final List<QueryResourceUsageEstimator> queryUsageEstimators;
	private final Function<grameFunctionality, List<TxnResourceUsageEstimator>> txnUsageEstimators;

	public UsageBasedFeeCalculator(
			HbarCentExchange exchange,
			UsagePricesProvider usagePrices,
			FeeMultiplierSource feeMultiplierSource,
			List<QueryResourceUsageEstimator> queryUsageEstimators,
			Function<grameFunctionality, List<TxnResourceUsageEstimator>> txnUsageEstimators
	) {
		this.exchange = exchange;
		this.usagePrices = usagePrices;
		this.feeMultiplierSource = feeMultiplierSource;
		this.queryUsageEstimators = queryUsageEstimators;
		this.txnUsageEstimators = txnUsageEstimators;
	}

	@Override
	public void init() {
		usagePrices.loadPriceSchedules();
	}

	@Override
	public FeeObject computePayment(
			Query query,
			FeeData usagePrices,
			StateView view,
			Timestamp at,
			Map<String, Object> queryCtx
	) {
		return compute(query, usagePrices, at, estimator -> estimator.usageGiven(query, view, queryCtx));
	}

	@Override
	public FeeObject estimatePayment(
			Query query,
			FeeData usagePrices,
			StateView view,
			Timestamp at,
			ResponseType type
	) {
		return compute(query, usagePrices, at, estimator -> estimator.usageGivenType(query, view, type));
	}

	private FeeObject compute(
			Query query,
			FeeData usagePrices,
			Timestamp at,
			Function<QueryResourceUsageEstimator, FeeData> usageFn
	) {
		var usageEstimator = getQueryUsageEstimator(query);
		var queryUsage = usageFn.apply(usageEstimator);
		return FeeBuilder.getFeeObject(usagePrices, queryUsage, exchange.rate(at));
	}

	@Override
	public FeeObject computeFee(TxnAccessor accessor, JKey payerKey, StateView view) {
		return feeGiven(accessor, payerKey, view, usagePrices.activePrices(), exchange.activeRate());
	}

	@Override
	public FeeObject estimateFee(TxnAccessor accessor, JKey payerKey, StateView view, Timestamp at) {
		FeeData prices = uncheckedPricesGiven(accessor, at);

		return feeGiven(accessor, payerKey, view, prices, exchange.rate(at));
	}

	@Override
	public long activeGasPriceInTinybars() {
		return gasPriceInTinybars(usagePrices.activePrices(), exchange.activeRate());
	}

	@Override
	public long estimatedGasPriceInTinybars(grameFunctionality function, Timestamp at) {
		var rates = exchange.rate(at);
		var prices = usagePrices.pricesGiven(function, at);
		return gasPriceInTinybars(prices, rates);
	}

	@Override
	public long estimatedNonFeePayerAdjustments(TxnAccessor accessor, Timestamp at) {
		switch (accessor.getFunction()) {
			case CryptoCreate:
				var cryptoCreateOp = accessor.getTxn().getCryptoCreateAccount();
				return -cryptoCreateOp.getInitialBalance();
			case CryptoTransfer:
				var payer = accessor.getPayer();
				var cryptoTransferOp = accessor.getTxn().getCryptoTransfer();
				var adjustments = cryptoTransferOp.getTransfers().getAccountAmountsList();
				long cryptoTransferNet = 0L;
				for (AccountAmount adjustment : adjustments) {
					if (payer.equals(adjustment.getAccountID())) {
						cryptoTransferNet += adjustment.getAmount();
					}
				}
				return cryptoTransferNet;
			case ContractCreate:
				var contractCreateOp = accessor.getTxn().getContractCreateInstance();
				return -contractCreateOp.getInitialBalance()
						- contractCreateOp.getGas() * estimatedGasPriceInTinybars(ContractCreate, at);
			case ContractCall:
				var contractCallOp = accessor.getTxn().getContractCall();
				return -contractCallOp.getAmount()
						- contractCallOp.getGas() * estimatedGasPriceInTinybars(ContractCall, at);
			default:
				return 0L;
		}
	}

	private long gasPriceInTinybars(FeeData prices, ExchangeRate rates) {
		long priceInTinyCents = prices.getServicedata().getGas() / FEE_DIVISOR_FACTOR;
		long priceInTinyBars = getTinybarsFromTinyCents(rates, priceInTinyCents);
		return Math.max(priceInTinyBars, 1L);
	}

	private FeeData uncheckedPricesGiven(TxnAccessor accessor, Timestamp at) {
		try {
			return usagePrices.pricesGiven(accessor.getFunction(), at);
		} catch (Exception e) {
			log.warn("Using default usage prices to calculate fees for {}!", accessor.getSignedTxn4Log(), e);
		}
		return DEFAULT_USAGE_PRICES;
	}

	private FeeObject feeGiven(
			TxnAccessor accessor,
			JKey payerKey,
			StateView view,
			FeeData prices,
			ExchangeRate rate
	) {
		var sigUsage = getSigUsage(accessor, payerKey);
		var usageEstimator = getTxnUsageEstimator(accessor);
		try {
			FeeData metrics = usageEstimator.usageGiven(accessor.getTxn(), sigUsage, view);
			return FeeBuilder.getFeeObject(prices, metrics, rate, feeMultiplierSource.currentMultiplier());
		} catch (InvalidTxBodyException e) {
			log.warn(
					"Argument accessor={} malformed for implied estimator {}!",
					accessor.getSignedTxn4Log(),
					usageEstimator);
			throw new IllegalArgumentException(e);
		}
	}

	private QueryResourceUsageEstimator getQueryUsageEstimator(Query query) {
		Optional<QueryResourceUsageEstimator> usageEstimator = queryUsageEstimators
				.stream()
				.filter(estimator -> estimator.applicableTo(query))
				.findAny();
		if (usageEstimator.isPresent()) {
			return usageEstimator.get();
		}
		throw new NoSuchElementException("No estimator exists for the given query");
	}

	private TxnResourceUsageEstimator getTxnUsageEstimator(TxnAccessor accessor) {
		var txn = accessor.getTxn();
		var estimators = Optional
				.ofNullable(txnUsageEstimators.apply(accessor.getFunction()))
				.orElse(Collections.emptyList());
		for (TxnResourceUsageEstimator estimator : estimators) {
			if (estimator.applicableTo(txn)) {
				return estimator;
			}
		}
		throw new NoSuchElementException("No estimator exists for the given transaction");
	}

	private SigValueObj getSigUsage(TxnAccessor accessor, JKey payerKey) {
		return new SigValueObj(
				FeeBuilder.getSignatureCount(accessor.getBackwardCompatibleSignedTxn()),
				grameKeyTraversal.numSimpleKeys(payerKey),
				FeeBuilder.getSignatureSize(accessor.getBackwardCompatibleSignedTxn()));
	}
}
