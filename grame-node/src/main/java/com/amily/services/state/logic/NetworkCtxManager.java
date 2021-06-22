package com.grame.services.state.logic;

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

import com.grame.services.context.domain.trackers.IssEventInfo;
import com.grame.services.context.properties.PropertySource;
import com.grame.services.fees.FeeMultiplierSource;
import com.grame.services.fees.HbarCentExchange;
import com.grame.services.state.initialization.SystemFilesManager;
import com.grame.services.state.merkle.MerkleNetworkContext;
import com.grame.services.stats.HapiOpCounters;
import com.grame.services.throttling.FunctionalityThrottling;
import com.gramegrame.api.proto.java.grameFunctionality;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import static com.grame.services.context.domain.trackers.IssEventStatus.ONGOING_ISS;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.SECONDS;

public class NetworkCtxManager {
	public static Logger log = LogManager.getLogger(NetworkCtxManager.class);

	private final int issResetPeriod;

	private final IssEventInfo issInfo;
	private final HapiOpCounters opCounters;
	private final HbarCentExchange exchange;
	private final SystemFilesManager systemFilesManager;
	private final FeeMultiplierSource feeMultiplierSource;
	private final FunctionalityThrottling handleThrottling;
	private final Supplier<MerkleNetworkContext> networkCtx;

	public NetworkCtxManager(
			IssEventInfo issInfo,
			PropertySource properties,
			HapiOpCounters opCounters,
			HbarCentExchange exchange,
			SystemFilesManager systemFilesManager,
			FeeMultiplierSource feeMultiplierSource,
			FunctionalityThrottling handleThrottling,
			Supplier<MerkleNetworkContext> networkCtx
	) {
		issResetPeriod = properties.getIntProperty("iss.resetPeriod");

		this.issInfo = issInfo;
		this.opCounters = opCounters;
		this.exchange = exchange;
		this.networkCtx = networkCtx;
		this.systemFilesManager = systemFilesManager;
		this.feeMultiplierSource = feeMultiplierSource;
		this.handleThrottling = handleThrottling;
	}

	public void setObservableFilesNotLoaded() {
		systemFilesManager.setObservableFilesNotLoaded();
	}

	public void loadObservableSysFilesIfNeeded() {
		if (!systemFilesManager.areObservableFilesLoaded()) {
			var networkCtxNow = networkCtx.get();
			log.info("Observable files not yet loaded, doing now with network context {}", networkCtxNow);
			systemFilesManager.loadObservableSystemFiles();
			networkCtxNow.resetWithSavedSnapshots(handleThrottling);
			feeMultiplierSource.resetExpectations();
			networkCtxNow.updateWithSavedCongestionStarts(feeMultiplierSource);
		}
	}

	public void advanceConsensusClockTo(Instant consensusTime) {
		var networkCtxNow = networkCtx.get();
		var lastConsensusTime = networkCtxNow.consensusTimeOfLastHandledTxn();
		if (lastConsensusTime != null && !inSameUtcDay(lastConsensusTime, consensusTime)) {
			networkCtxNow.midnightRates().replaceWith(exchange.activeRates());
		}
		networkCtxNow.setConsensusTimeOfLastHandledTxn(consensusTime);

		if (issInfo.status() == ONGOING_ISS) {
			var resetTime = issInfo.consensusTimeOfRecentAlert().get().plus(issResetPeriod, SECONDS);
			if (consensusTime.isAfter(resetTime)) {
				issInfo.relax();
			}
		}
	}

	public void prepareForIncorporating(grameFunctionality op)	 {
		/* This is only to monitor the current network usage for automated
		congestion pricing; we don't actually throttle consensus transactions. */
		handleThrottling.shouldThrottle(op);

		feeMultiplierSource.updateMultiplier(networkCtx.get().consensusTimeOfLastHandledTxn());
	}

	public void finishIncorporating(grameFunctionality op) {
		opCounters.countHandled(op);

		var networkCtxNow = networkCtx.get();
		networkCtxNow.updateSnapshotsFrom(handleThrottling);
		networkCtxNow.updateCongestionStartsFrom(feeMultiplierSource);
	}

	public static boolean inSameUtcDay(Instant now, Instant then) {
		return LocalDateTime.ofInstant(now, UTC).getDayOfYear() == LocalDateTime.ofInstant(then, UTC).getDayOfYear();
	}

	int getIssResetPeriod() {
		return issResetPeriod;
	}
}
