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

import com.grame.services.context.properties.NodeLocalProperties;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.swirlds.common.Platform;
import com.swirlds.platform.StatsSpeedometer;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.grame.services.stats.ServicesStatsConfig.IGNORED_FUNCTIONS;
import static com.grame.services.stats.ServicesStatsConfig.SPEEDOMETER_ANSWERED_DESC_TPL;
import static com.grame.services.stats.ServicesStatsConfig.SPEEDOMETER_ANSWERED_NAME_TPL;
import static com.grame.services.stats.ServicesStatsConfig.SPEEDOMETER_HANDLED_DESC_TPL;
import static com.grame.services.stats.ServicesStatsConfig.SPEEDOMETER_HANDLED_NAME_TPL;
import static com.grame.services.stats.ServicesStatsConfig.SPEEDOMETER_RECEIVED_DESC_TPL;
import static com.grame.services.stats.ServicesStatsConfig.SPEEDOMETER_RECEIVED_NAME_TPL;
import static com.grame.services.stats.ServicesStatsConfig.SPEEDOMETER_SUBMITTED_DESC_TPL;
import static com.grame.services.stats.ServicesStatsConfig.SPEEDOMETER_SUBMITTED_NAME_TPL;
import static com.grame.services.utils.MiscUtils.QUERY_FUNCTIONS;

public class HapiOpSpeedometers {
	static Supplier<grameFunctionality[]> allFunctions = grameFunctionality.class::getEnumConstants;

	private final HapiOpCounters counters;
	private final SpeedometerFactory speedometer;
	private final Function<grameFunctionality, String> statNameFn;

	final Map<grameFunctionality, Long> lastReceivedOpsCount = new HashMap<>();
	final Map<grameFunctionality, Long> lastHandledTxnsCount = new HashMap<>();
	final Map<grameFunctionality, Long> lastSubmittedTxnsCount = new HashMap<>();
	final Map<grameFunctionality, Long> lastAnsweredQueriesCount = new HashMap<>();

	final EnumMap<grameFunctionality, StatsSpeedometer> receivedOps = new EnumMap<>(grameFunctionality.class);
	final EnumMap<grameFunctionality, StatsSpeedometer> handledTxns = new EnumMap<>(grameFunctionality.class);
	final EnumMap<grameFunctionality, StatsSpeedometer> submittedTxns = new EnumMap<>(grameFunctionality.class);
	final EnumMap<grameFunctionality, StatsSpeedometer> answeredQueries = new EnumMap<>(grameFunctionality.class);

	public HapiOpSpeedometers(
			HapiOpCounters counters,
			SpeedometerFactory speedometer,
			NodeLocalProperties properties,
			Function<grameFunctionality, String> statNameFn
	) {
		this.counters = counters;
		this.statNameFn = statNameFn;
		this.speedometer = speedometer;

		double halfLife = properties.statsSpeedometerHalfLifeSecs();
		Arrays.stream(allFunctions.get())
				.filter(function -> !IGNORED_FUNCTIONS.contains(function))
				.forEach(function -> {
			receivedOps.put(function, new StatsSpeedometer(halfLife));
			lastReceivedOpsCount.put(function, 0L);
			if (QUERY_FUNCTIONS.contains(function)) {
				answeredQueries.put(function, new StatsSpeedometer(halfLife));
				lastAnsweredQueriesCount.put(function, 0L);
			} else {
				submittedTxns.put(function, new StatsSpeedometer(halfLife));
				lastSubmittedTxnsCount.put(function, 0L);
				handledTxns.put(function, new StatsSpeedometer(halfLife));
				lastHandledTxnsCount.put(function, 0L);
			}
		});
	}

	public void registerWith(Platform platform) {
		registerSpeedometers(platform, receivedOps, SPEEDOMETER_RECEIVED_NAME_TPL, SPEEDOMETER_RECEIVED_DESC_TPL);
		registerSpeedometers(platform, submittedTxns, SPEEDOMETER_SUBMITTED_NAME_TPL, SPEEDOMETER_SUBMITTED_DESC_TPL);
		registerSpeedometers(platform, handledTxns, SPEEDOMETER_HANDLED_NAME_TPL, SPEEDOMETER_HANDLED_DESC_TPL);
		registerSpeedometers(platform, answeredQueries, SPEEDOMETER_ANSWERED_NAME_TPL, SPEEDOMETER_ANSWERED_DESC_TPL);
	}

	private void registerSpeedometers(
			Platform platform,
			Map<grameFunctionality, StatsSpeedometer> speedometers,
			String nameTpl,
			String descTpl
	) {
		for (Map.Entry<grameFunctionality, StatsSpeedometer> entry : speedometers.entrySet())	{
			var baseName = statNameFn.apply(entry.getKey());
			var fullName = String.format(nameTpl, baseName);
			var description = String.format(descTpl, baseName);
			platform.addAppStatEntry(speedometer.from(fullName, description, entry.getValue()));
		}
	}

	public void updateAll() {
		updateSpeedometers(receivedOps, lastReceivedOpsCount, counters::receivedSoFar);
		updateSpeedometers(submittedTxns, lastSubmittedTxnsCount, counters::submittedSoFar);
		updateSpeedometers(handledTxns, lastHandledTxnsCount, counters::handledSoFar);
		updateSpeedometers(answeredQueries, lastAnsweredQueriesCount, counters::answeredSoFar);
	}

	private void updateSpeedometers(
			Map<grameFunctionality, StatsSpeedometer> speedometers,
			Map<grameFunctionality, Long> lastMeasurements,
			Function<grameFunctionality, Long> currMeasurement
	) {
		for (Map.Entry<grameFunctionality, StatsSpeedometer> entry : speedometers.entrySet())	{
			var function = entry.getKey();
			long last = lastMeasurements.get(function);
			long curr = currMeasurement.apply(function);
			entry.getValue().update(curr - last);
			lastMeasurements.put(function, curr);
		}
	}
}
