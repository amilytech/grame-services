package com.grame.services.context.properties;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.grame.services.context.properties.PropUtils.loadOverride;
import static java.util.Collections.unmodifiableSet;
import static java.util.Map.entry;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class BootstrapProperties implements PropertySource {
	private static Map<String, Object> MISSING_PROPS = null;

	private static Function<String, InputStream> nullableResourceStreamProvider =
			BootstrapProperties.class.getClassLoader()::getResourceAsStream;

	static Logger log = LogManager.getLogger(BootstrapProperties.class);

	static ThrowingStreamProvider resourceStreamProvider = resource -> {
		var in = nullableResourceStreamProvider.apply(resource);
		if (in == null) {
			throw new IOException(String.format("Resource '%s' cannot be loaded.", resource));
		}
		return in;
	};
	private static ThrowingStreamProvider fileStreamProvider = loc -> Files.newInputStream(Paths.get(loc));

	String BOOTSTRAP_PROPS_RESOURCE = "bootstrap.properties";
	String BOOTSTRAP_OVERRIDE_PROPS_LOC = "data/config/bootstrap.properties";

	Map<String, Object> bootstrapProps = MISSING_PROPS;

	private void initPropsFromResource() {
		var resourceProps = new Properties();
		load(BOOTSTRAP_PROPS_RESOURCE, resourceProps);
		loadOverride(BOOTSTRAP_OVERRIDE_PROPS_LOC, resourceProps, fileStreamProvider, log);

		Set<String> unrecognizedProps = new HashSet<>(resourceProps.stringPropertyNames());
		unrecognizedProps.removeAll(BOOTSTRAP_PROP_NAMES);
		if (!unrecognizedProps.isEmpty()) {
			var msg = String.format(
					"'%s' contains unrecognized properties: %s!",
					BOOTSTRAP_PROPS_RESOURCE,
					unrecognizedProps);
			throw new IllegalStateException(msg);
		}
		var missingProps = BOOTSTRAP_PROP_NAMES.stream()
				.filter(name -> !resourceProps.containsKey(name))
				.sorted()
				.collect(toList());
		if (!missingProps.isEmpty()) {
			var msg = String.format(
					"'%s' is missing properties: %s!",
					BOOTSTRAP_PROPS_RESOURCE,
					missingProps);
			throw new IllegalStateException(msg);
		}

		bootstrapProps = new HashMap<>();
		BOOTSTRAP_PROP_NAMES
				.stream()
				.forEach(prop -> bootstrapProps.put(
						prop,
						transformFor(prop).apply(resourceProps.getProperty(prop))));

		var msg = "Resolved bootstrap properties:\n  " + BOOTSTRAP_PROP_NAMES.stream()
				.sorted()
				.map(name -> String.format("%s=%s", name, bootstrapProps.get(name)))
				.collect(Collectors.joining("\n  "));
		log.info(msg);
	}

	private void load(String resource, Properties intoProps) {
		try (InputStream fin = resourceStreamProvider.newInputStream(resource)) {
			intoProps.load(fin);
		} catch (IOException e) {
			throw new IllegalStateException(
					String.format("'%s' could not be loaded!", resource),
					e);
		}
	}

	void ensureProps() {
		if (bootstrapProps == MISSING_PROPS) {
			initPropsFromResource();
		}
	}

	@Override
	public boolean containsProperty(String name) {
		return BOOTSTRAP_PROP_NAMES.contains(name);
	}

	@Override
	public Object getProperty(String name) {
		ensureProps();
		if (bootstrapProps.containsKey(name)) {
			return bootstrapProps.get(name);
		} else {
			throw new IllegalArgumentException(String.format("Argument 'name=%s' is invalid!", name));
		}
	}

	@Override
	public Set<String> allPropertyNames() {
		return BOOTSTRAP_PROP_NAMES;
	}

	static final Set<String> BOOTSTRAP_PROPS = Set.of(
			"bootstrap.feeSchedulesJson.resource",
			"bootstrap.genesisB64Keystore.keyName",
			"bootstrap.genesisB64Keystore.path",
			"bootstrap.genesisPemPassphrase.path",
			"bootstrap.genesisPem.path",
			"bootstrap.hapiPermissions.path",
			"bootstrap.networkProperties.path",
			"bootstrap.rates.currentHbarEquiv",
			"bootstrap.rates.currentCentEquiv",
			"bootstrap.rates.currentExpiry",
			"bootstrap.rates.nextHbarEquiv",
			"bootstrap.rates.nextCentEquiv",
			"bootstrap.rates.nextExpiry",
			"bootstrap.system.entityExpiry",
			"bootstrap.throttleDefsJson.resource"
	);

	static final Set<String> GLOBAL_STATIC_PROPS = Set.of(
			"accounts.addressBookAdmin",
			"accounts.exchangeRatesAdmin",
			"accounts.feeSchedulesAdmin",
			"accounts.freezeAdmin",
			"accounts.systemAdmin",
			"accounts.systemDeleteAdmin",
			"accounts.systemUndeleteAdmin",
			"accounts.treasury",
			"files.addressBook",
			"files.diskFsBaseDir.path",
			"files.networkProperties",
			"files.exchangeRates",
			"files.feeSchedules",
			"files.hapiPermissions",
			"files.nodeDetails",
			"files.softwareUpdateZip",
			"files.throttleDefinitions",
			"grame.numReservedSystemEntities",
			"grame.realm",
			"grame.shard",
			"ledger.numSystemAccounts",
			"ledger.totalTinyBarFloat"
	);

	static final Set<String> GLOBAL_DYNAMIC_PROPS = Set.of(
			"balances.exportDir.path",
			"balances.exportEnabled",
			"balances.exportPeriodSecs",
			"balances.exportTokenBalances",
			"balances.nodeBalanceWarningThreshold",
			"cache.records.ttl",
			"contracts.defaultLifetime",
			"contracts.localCall.estRetBytes",
			"contracts.maxGas",
			"contracts.maxStorageKb",
			"files.maxSizeKb",
			"fees.minCongestionPeriod",
			"fees.percentCongestionMultipliers",
			"fees.tokenTransferUsageMultiplier",
			"grame.transaction.maxMemoUtf8Bytes",
			"grame.transaction.maxValidDuration",
			"grame.transaction.minValidDuration",
			"grame.transaction.minValidityBufferSecs",
			"ledger.autoRenewPeriod.maxDuration",
			"ledger.autoRenewPeriod.minDuration",
			"ledger.keepRecordsInState",
			"ledger.fundingAccount",
			"ledger.maxAccountNum",
			"ledger.transfers.maxLen",
			"ledger.tokenTransfers.maxLen",
			"ledger.schedule.txExpiryTimeSecs",
			"rates.intradayChangeLimitPercent",
			"scheduling.whitelist",
			"tokens.maxPerAccount",
			"tokens.maxSymbolUtf8Bytes",
			"tokens.maxTokenNameUtf8Bytes",
			"consensus.message.maxBytesAllowed"
	);

	static final Set<String> NODE_PROPS = Set.of(
			"dev.onlyDefaultNodeListens",
			"dev.defaultListeningNodeAccount",
			"grpc.port",
			"grpc.tlsPort",
			"grame.accountsExportPath",
			"grame.exportAccountsOnStartup",
			"grame.profiles.active",
			"grame.recordStream.isEnabled",
			"grame.recordStream.logDir",
			"grame.recordStream.logPeriod",
			"grame.recordStream.queueCapacity",
			"netty.mode",
			"netty.prod.flowControlWindow",
			"netty.prod.maxConcurrentCalls",
			"netty.prod.maxConnectionAge",
			"netty.prod.maxConnectionAgeGrace",
			"netty.prod.maxConnectionIdle",
			"netty.prod.keepAliveTime",
			"netty.prod.keepAliveTimeout",
			"netty.tlsCrt.path",
			"netty.tlsKey.path",
			"queries.blob.lookupRetries",
			"precheck.account.maxLookupRetries",
			"precheck.account.lookupRetryBackoffIncrementMs",
			"stats.hapiOps.speedometerUpdateIntervalMs",
			"stats.runningAvgHalfLifeSecs",
			"stats.speedometerHalfLifeSecs"
	);

	public static final Set<String> BOOTSTRAP_PROP_NAMES = unmodifiableSet(
			Stream.of(BOOTSTRAP_PROPS, GLOBAL_STATIC_PROPS, GLOBAL_DYNAMIC_PROPS, NODE_PROPS)
					.flatMap(Set::stream)
					.collect(toSet()));

	public static Function<String, Object> transformFor(String prop) {
		return PROP_TRANSFORMS.getOrDefault(prop, AS_STRING);
	}

	static final Map<String, Function<String, Object>> PROP_TRANSFORMS = Map.ofEntries(
			entry("accounts.addressBookAdmin", AS_LONG),
			entry("accounts.exchangeRatesAdmin", AS_LONG),
			entry("accounts.feeSchedulesAdmin", AS_LONG),
			entry("accounts.freezeAdmin", AS_LONG),
			entry("accounts.systemAdmin", AS_LONG),
			entry("accounts.systemDeleteAdmin", AS_LONG),
			entry("accounts.systemUndeleteAdmin", AS_LONG),
			entry("accounts.treasury", AS_LONG),
			entry("balances.exportEnabled", AS_BOOLEAN),
			entry("balances.exportPeriodSecs", AS_INT),
			entry("balances.nodeBalanceWarningThreshold", AS_LONG),
			entry("cache.records.ttl", AS_INT),
			entry("dev.onlyDefaultNodeListens", AS_BOOLEAN),
			entry("balances.exportTokenBalances", AS_BOOLEAN),
			entry("files.addressBook", AS_LONG),
			entry("files.networkProperties", AS_LONG),
			entry("files.exchangeRates", AS_LONG),
			entry("files.feeSchedules", AS_LONG),
			entry("files.hapiPermissions", AS_LONG),
			entry("files.softwareUpdateZip", AS_LONG),
			entry("files.nodeDetails", AS_LONG),
			entry("files.throttleDefinitions", AS_LONG),
			entry("grpc.port", AS_INT),
			entry("grpc.tlsPort", AS_INT),
			entry("grame.exportAccountsOnStartup", AS_BOOLEAN),
			entry("grame.numReservedSystemEntities", AS_LONG),
			entry("grame.profiles.active", AS_PROFILE),
			entry("grame.realm", AS_LONG),
			entry("grame.recordStream.logPeriod", AS_LONG),
			entry("grame.recordStream.isEnabled", AS_BOOLEAN),
			entry("grame.recordStream.queueCapacity", AS_INT),
			entry("grame.shard", AS_LONG),
			entry("grame.transaction.maxMemoUtf8Bytes", AS_INT),
			entry("grame.transaction.maxValidDuration", AS_LONG),
			entry("grame.transaction.minValidDuration", AS_LONG),
			entry("grame.transaction.minValidityBufferSecs", AS_INT),
			entry("ledger.autoRenewPeriod.maxDuration", AS_LONG),
			entry("ledger.autoRenewPeriod.minDuration", AS_LONG),
			entry("netty.mode", AS_PROFILE),
			entry("precheck.account.maxLookupRetries", AS_INT),
			entry("precheck.account.lookupRetryBackoffIncrementMs", AS_INT),
			entry("queries.blob.lookupRetries", AS_INT),
			entry("bootstrap.rates.currentHbarEquiv", AS_INT),
			entry("bootstrap.rates.currentCentEquiv", AS_INT),
			entry("bootstrap.rates.currentExpiry", AS_LONG),
			entry("bootstrap.rates.nextHbarEquiv", AS_INT),
			entry("bootstrap.rates.nextCentEquiv", AS_INT),
			entry("bootstrap.rates.nextExpiry", AS_LONG),
			entry("bootstrap.system.entityExpiry", AS_LONG),
			entry("fees.minCongestionPeriod", AS_INT),
			entry("fees.tokenTransferUsageMultiplier", AS_INT),
			entry("fees.percentCongestionMultipliers", AS_CONGESTION_MULTIPLIERS),
			entry("files.maxSizeKb", AS_INT),
			entry("ledger.fundingAccount", AS_LONG),
			entry("ledger.keepRecordsInState", AS_BOOLEAN),
			entry("ledger.maxAccountNum", AS_LONG),
			entry("ledger.numSystemAccounts", AS_INT),
			entry("ledger.transfers.maxLen", AS_INT),
			entry("ledger.tokenTransfers.maxLen", AS_INT),
			entry("ledger.totalTinyBarFloat", AS_LONG),
			entry("ledger.schedule.txExpiryTimeSecs", AS_INT),
			entry("netty.prod.flowControlWindow", AS_INT),
			entry("netty.prod.maxConcurrentCalls", AS_INT),
			entry("netty.prod.maxConnectionAge", AS_LONG),
			entry("netty.prod.maxConnectionAgeGrace", AS_LONG),
			entry("netty.prod.maxConnectionIdle", AS_LONG),
			entry("netty.prod.keepAliveTime", AS_LONG),
			entry("netty.prod.keepAliveTimeout", AS_LONG),
			entry("tokens.maxPerAccount", AS_INT),
			entry("tokens.maxSymbolUtf8Bytes", AS_INT),
			entry("tokens.maxTokenNameUtf8Bytes", AS_INT),
			entry("contracts.localCall.estRetBytes", AS_INT),
			entry("contracts.maxStorageKb", AS_INT),
			entry("contracts.defaultLifetime", AS_LONG),
			entry("contracts.maxGas", AS_INT),
			entry("rates.intradayChangeLimitPercent", AS_INT),
			entry("scheduling.whitelist", AS_FUNCTIONS),
			entry("stats.hapiOps.speedometerUpdateIntervalMs", AS_LONG),
			entry("stats.runningAvgHalfLifeSecs", AS_DOUBLE),
			entry("stats.speedometerHalfLifeSecs", AS_DOUBLE),
			entry("consensus.message.maxBytesAllowed", AS_INT)
	);
}
