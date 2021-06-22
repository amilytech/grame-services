package com.grame.services.stats;

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
import com.gramegrame.api.proto.java.grameFunctionality;
import com.swirlds.common.Platform;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.grame.services.stats.ServicesStatsConfig.COUNTER_ANSWERED_DESC_TPL;
import static com.grame.services.stats.ServicesStatsConfig.COUNTER_ANSWERED_NAME_TPL;
import static com.grame.services.stats.ServicesStatsConfig.COUNTER_HANDLED_DESC_TPL;
import static com.grame.services.stats.ServicesStatsConfig.COUNTER_HANDLED_NAME_TPL;
import static com.grame.services.stats.ServicesStatsConfig.IGNORED_FUNCTIONS;
import static com.grame.services.stats.ServicesStatsConfig.COUNTER_RECEIVED_DESC_TPL;
import static com.grame.services.stats.ServicesStatsConfig.COUNTER_RECEIVED_NAME_TPL;
import static com.grame.services.stats.ServicesStatsConfig.COUNTER_SUBMITTED_DESC_TPL;
import static com.grame.services.stats.ServicesStatsConfig.COUNTER_SUBMITTED_NAME_TPL;
import static com.grame.services.utils.MiscUtils.QUERY_FUNCTIONS;
import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusSubmitMessage;

public class HapiOpCounters {
	static Supplier<grameFunctionality[]> allFunctions = grameFunctionality.class::getEnumConstants;

	private final CounterFactory counter;
	private final MiscRunningAvgs runningAvgs;
	private final TransactionContext txnCtx;
	private final Function<grameFunctionality, String> statNameFn;

	EnumMap<grameFunctionality, AtomicLong> receivedOps = new EnumMap<>(grameFunctionality.class);
	EnumMap<grameFunctionality, AtomicLong> handledTxns = new EnumMap<>(grameFunctionality.class);
	EnumMap<grameFunctionality, AtomicLong> submittedTxns = new EnumMap<>(grameFunctionality.class);
	EnumMap<grameFunctionality, AtomicLong> answeredQueries = new EnumMap<>(grameFunctionality.class);

	public HapiOpCounters(
			CounterFactory counter,
			MiscRunningAvgs runningAvgs,
			TransactionContext txnCtx,
			Function<grameFunctionality, String> statNameFn
	) {
		this.txnCtx = txnCtx;
		this.counter = counter;
		this.statNameFn = statNameFn;
		this.runningAvgs = runningAvgs;

		Arrays.stream(allFunctions.get())
				.filter(function -> !IGNORED_FUNCTIONS.contains(function))
				.forEach(function -> {
			receivedOps.put(function, new AtomicLong());
			if (QUERY_FUNCTIONS.contains(function)) {
				answeredQueries.put(function, new AtomicLong());
			} else {
				submittedTxns.put(function, new AtomicLong());
				handledTxns.put(function, new AtomicLong());
			}
		});
	}

	public void registerWith(Platform platform) {
		registerCounters(platform, receivedOps, COUNTER_RECEIVED_NAME_TPL, COUNTER_RECEIVED_DESC_TPL);
		registerCounters(platform, submittedTxns, COUNTER_SUBMITTED_NAME_TPL, COUNTER_SUBMITTED_DESC_TPL);
		registerCounters(platform, handledTxns, COUNTER_HANDLED_NAME_TPL, COUNTER_HANDLED_DESC_TPL);
		registerCounters(platform, answeredQueries, COUNTER_ANSWERED_NAME_TPL, COUNTER_ANSWERED_DESC_TPL);
	}

	private void registerCounters(
			Platform platform,
			Map<grameFunctionality, AtomicLong> counters,
			String nameTpl,
			String descTpl
	) {
		for (Map.Entry<grameFunctionality, AtomicLong> entry : counters.entrySet())	{
			var baseName = statNameFn.apply(entry.getKey());
			var fullName = String.format(nameTpl, baseName);
			var description = String.format(descTpl, baseName);
			platform.addAppStatEntry(counter.from(fullName, description, entry.getValue()::get));
		}
	}

	public void countReceived(grameFunctionality op) {
		safeIncrement(receivedOps, op);
	}

	public long receivedSoFar(grameFunctionality op) {
		return IGNORED_FUNCTIONS.contains(op) ? 0 : receivedOps.get(op).get();
	}

	public void countSubmitted(grameFunctionality txn) {
		safeIncrement(submittedTxns, txn);
	}

	public long submittedSoFar(grameFunctionality txn) {
		return IGNORED_FUNCTIONS.contains(txn) ? 0 : submittedTxns.get(txn).get();
	}

	public void countHandled(grameFunctionality txn) {
		safeIncrement(handledTxns, txn);
		if (txn == ConsensusSubmitMessage) {
			int txnBytes = txnCtx.accessor().getTxn().getSerializedSize();
			runningAvgs.recordHandledSubmitMessageSize(txnBytes);
		}
	}

	public long handledSoFar(grameFunctionality txn) {
		return IGNORED_FUNCTIONS.contains(txn) ? 0 : handledTxns.get(txn).get();
	}

	public void countAnswered(grameFunctionality query) {
		safeIncrement(answeredQueries, query);
	}

	public long answeredSoFar(grameFunctionality query) {
		return IGNORED_FUNCTIONS.contains(query) ? 0 : answeredQueries.get(query).get();
	}

	private void safeIncrement(
			Map<grameFunctionality, AtomicLong> counters,
			grameFunctionality function
	) {
		if (!IGNORED_FUNCTIONS.contains(function)) {
			counters.get(function).getAndIncrement();
		}
	}
}
