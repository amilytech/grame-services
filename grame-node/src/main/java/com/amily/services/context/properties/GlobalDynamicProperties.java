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

import com.grame.services.config.grameNumbers;
import com.grame.services.fees.calculation.CongestionMultipliers;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.grameFunctionality;

import java.util.Set;

public class GlobalDynamicProperties {
	private final grameNumbers grameNums;
	private final PropertySource properties;

	private int maxTokensPerAccount;
	private int maxTokenSymbolUtf8Bytes;
	private int maxTokenNameUtf8Bytes;
	private int maxFileSizeKb;
	private int cacheRecordsTtl;
	private int maxContractStorageKb;
	private int balancesExportPeriodSecs;
	private int ratesIntradayChangeLimitPercent;
	private long maxAccountNum;
	private long nodeBalanceWarningThreshold;
	private String pathToBalancesExportDir;
	private boolean shouldKeepRecordsInState;
	private boolean shouldExportBalances;
	private boolean shouldExportTokenBalances;
	private AccountID fundingAccount;
	private int maxTransfersLen;
	private int maxTokenTransfersLen;
	private int maxMemoUtf8Bytes;
	private long maxTxnDuration;
	private long minTxnDuration;
	private int minValidityBuffer;
	private int maxGas;
	private long defaultContractLifetime;
	private int feesTokenTransferUsageMultiplier;
	private long maxAutoRenewDuration;
	private long minAutoRenewDuration;
	private int localCallEstRetBytes;
	private int scheduledTxExpiryTimeSecs;
	private int messageMaxBytesAllowed;
	private Set<grameFunctionality> schedulingWhitelist;
	private CongestionMultipliers congestionMultipliers;
	private int feesMinCongestionPeriod;

	public GlobalDynamicProperties(
			grameNumbers grameNums,
			PropertySource properties
	) {
		this.grameNums = grameNums;
		this.properties = properties;

		reload();
	}

	public void reload() {
		shouldKeepRecordsInState = properties.getBooleanProperty("ledger.keepRecordsInState");
		maxTokensPerAccount = properties.getIntProperty("tokens.maxPerAccount");
		maxTokenSymbolUtf8Bytes = properties.getIntProperty("tokens.maxSymbolUtf8Bytes");
		maxTokenNameUtf8Bytes = properties.getIntProperty("tokens.maxTokenNameUtf8Bytes");
		maxAccountNum = properties.getLongProperty("ledger.maxAccountNum");
		maxFileSizeKb = properties.getIntProperty("files.maxSizeKb");
		fundingAccount = AccountID.newBuilder()
				.setShardNum(grameNums.shard())
				.setRealmNum(grameNums.realm())
				.setAccountNum(properties.getLongProperty("ledger.fundingAccount"))
				.build();
		cacheRecordsTtl = properties.getIntProperty("cache.records.ttl");
		maxContractStorageKb = properties.getIntProperty("contracts.maxStorageKb");
		ratesIntradayChangeLimitPercent = properties.getIntProperty("rates.intradayChangeLimitPercent");
		balancesExportPeriodSecs = properties.getIntProperty("balances.exportPeriodSecs");
		shouldExportBalances = properties.getBooleanProperty("balances.exportEnabled");
		nodeBalanceWarningThreshold = properties.getLongProperty("balances.nodeBalanceWarningThreshold");
		pathToBalancesExportDir = properties.getStringProperty("balances.exportDir.path");
		shouldExportTokenBalances = properties.getBooleanProperty("balances.exportTokenBalances");
		maxTransfersLen = properties.getIntProperty("ledger.transfers.maxLen");
		maxTokenTransfersLen = properties.getIntProperty("ledger.tokenTransfers.maxLen");
		maxMemoUtf8Bytes = properties.getIntProperty("grame.transaction.maxMemoUtf8Bytes");
		maxTxnDuration = properties.getLongProperty("grame.transaction.maxValidDuration");
		minTxnDuration = properties.getLongProperty("grame.transaction.minValidDuration");
		minValidityBuffer = properties.getIntProperty("grame.transaction.minValidityBufferSecs");
		maxGas = properties.getIntProperty("contracts.maxGas");
		defaultContractLifetime = properties.getLongProperty("contracts.defaultLifetime");
		feesTokenTransferUsageMultiplier = properties.getIntProperty("fees.tokenTransferUsageMultiplier");
		maxAutoRenewDuration = properties.getLongProperty("ledger.autoRenewPeriod.maxDuration");
		minAutoRenewDuration = properties.getLongProperty("ledger.autoRenewPeriod.minDuration");
		localCallEstRetBytes = properties.getIntProperty("contracts.localCall.estRetBytes");
		scheduledTxExpiryTimeSecs = properties.getIntProperty("ledger.schedule.txExpiryTimeSecs");
		schedulingWhitelist = properties.getFunctionsProperty("scheduling.whitelist");
		messageMaxBytesAllowed = properties.getIntProperty( "consensus.message.maxBytesAllowed");
		congestionMultipliers = properties.getCongestionMultiplierProperty("fees.percentCongestionMultipliers");
		feesMinCongestionPeriod = properties.getIntProperty("fees.minCongestionPeriod");
	}

	public int maxTokensPerAccount() {
		return maxTokensPerAccount;
	}

	public int maxTokenSymbolUtf8Bytes() {
		return maxTokenSymbolUtf8Bytes;
	}

	public long maxAccountNum() {
		return maxAccountNum;
	}

	public int maxTokenNameUtf8Bytes() {
		return maxTokenNameUtf8Bytes;
	}

	public int maxFileSizeKb() {
		return maxFileSizeKb;
	}

	public AccountID fundingAccount() {
		return fundingAccount;
	}

	public int cacheRecordsTtl() {
		return cacheRecordsTtl;
	}

	public int maxContractStorageKb() {
		return maxContractStorageKb;
	}

	public int ratesIntradayChangeLimitPercent() {
		return ratesIntradayChangeLimitPercent;
        }

	public boolean shouldKeepRecordsInState() {
		return shouldKeepRecordsInState;
	}

	public int balancesExportPeriodSecs() {
		return balancesExportPeriodSecs;
	}

	public boolean shouldExportBalances() {
		return shouldExportBalances;
	}

	public long nodeBalanceWarningThreshold() {
		return nodeBalanceWarningThreshold;
	}

	public String pathToBalancesExportDir() {
		return pathToBalancesExportDir;
	}

	public boolean shouldExportTokenBalances() {
		return shouldExportTokenBalances;
	}

	public int maxTransferListSize() {
		return maxTransfersLen;
	}

	public int maxTokenTransferListSize() {
		return maxTokenTransfersLen;
	}

	public int maxMemoUtf8Bytes() {
		return maxMemoUtf8Bytes;
	}

	public long maxTxnDuration() {
		return maxTxnDuration;
	}

	public long minTxnDuration() {
		return minTxnDuration;
	}

	public int minValidityBuffer() {
		return minValidityBuffer;
	}

	public int maxGas() {
		return maxGas;
	}

	public long defaultContractLifetime() {
		return defaultContractLifetime;
	}

	public int feesTokenTransferUsageMultiplier() {
		return feesTokenTransferUsageMultiplier;
	}

	public long maxAutoRenewDuration() {
		return maxAutoRenewDuration;
	}

	public long minAutoRenewDuration() {
		return minAutoRenewDuration;
	}

	public int localCallEstRetBytes() {
		return localCallEstRetBytes;
	}

	public int scheduledTxExpiryTimeSecs() {
		return scheduledTxExpiryTimeSecs;
	}

	public int messageMaxBytesAllowed() {
		return messageMaxBytesAllowed;
	}

	public Set<grameFunctionality> schedulingWhitelist() {
		return schedulingWhitelist;
	}

	public CongestionMultipliers congestionMultipliers() {
		return congestionMultipliers;
	}

	public int feesMinCongestionPeriod() {
		return feesMinCongestionPeriod;
	}
}
