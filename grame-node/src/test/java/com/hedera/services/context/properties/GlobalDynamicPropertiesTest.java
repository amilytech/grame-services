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
import com.gramegrame.api.proto.java.CryptoCreate;
import com.gramegrame.api.proto.java.grameFunctionality;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class GlobalDynamicPropertiesTest {
	static final String[] balanceExportPaths = new String[] {
			"/opt/hgcapp/accountBalances",
			"data/saved/accountBalances"
	};

	PropertySource properties;

	grameNumbers numbers;
	CongestionMultipliers oddCongestion = CongestionMultipliers.from("90,11x,95,27x,99,103x");
	CongestionMultipliers evenCongestion = CongestionMultipliers.from("90,10x,95,25x,99,100x");
	GlobalDynamicProperties subject;

	@BeforeEach
	public void setup() {
		numbers = mock(grameNumbers.class);
		given(numbers.shard()).willReturn(1L);
		given(numbers.realm()).willReturn(2L);
		properties = mock(PropertySource.class);
	}

	@Test
	public void constructsAsExpected() {
		givenPropsWithSeed(1);

		// when:
		subject = new GlobalDynamicProperties(numbers, properties);

		// expect:
		assertFalse(subject.shouldKeepRecordsInState());
		assertEquals(1, subject.maxTokensPerAccount());
		assertEquals(2, subject.maxTokenSymbolUtf8Bytes());
		assertEquals(3L, subject.maxAccountNum());
		assertEquals(6, subject.maxFileSizeKb());
		assertEquals(accountWith(1L, 2L, 7L), subject.fundingAccount());
		assertEquals(8, subject.cacheRecordsTtl());
		assertEquals(9, subject.maxContractStorageKb());
		assertEquals(10, subject.ratesIntradayChangeLimitPercent());
		assertEquals(11, subject.balancesExportPeriodSecs());
		assertTrue(subject.shouldExportBalances());
		assertEquals(13L, subject.nodeBalanceWarningThreshold());
		assertEquals(balanceExportPaths[1], subject.pathToBalancesExportDir());
		assertTrue(subject.shouldExportTokenBalances());
		assertEquals(15, subject.maxTransferListSize());
		assertEquals(16, subject.maxTokenTransferListSize());
		assertEquals(17, subject.maxMemoUtf8Bytes());
		assertEquals(18L, subject.maxTxnDuration());
		assertEquals(19L, subject.minTxnDuration());
		assertEquals(20, subject.minValidityBuffer());
		assertEquals(21, subject.maxGas());
		assertEquals(22L, subject.defaultContractLifetime());
		assertEquals(23, subject.feesTokenTransferUsageMultiplier());
		assertEquals(24, subject.maxAutoRenewDuration());
		assertEquals(25, subject.minAutoRenewDuration());
		assertEquals(26, subject.localCallEstRetBytes());
		assertEquals(27, subject.scheduledTxExpiryTimeSecs());
		assertEquals(28, subject.messageMaxBytesAllowed());
		assertEquals(Set.of(grameFunctionality.CryptoTransfer), subject.schedulingWhitelist());
		assertEquals(oddCongestion, subject.congestionMultipliers());
		assertEquals(29, subject.feesMinCongestionPeriod());
	}

	@Test
	public void reloadWorksAsExpected() {
		givenPropsWithSeed(2);

		// when:
		subject = new GlobalDynamicProperties(numbers, properties);

		// expect:
		assertTrue(subject.shouldKeepRecordsInState());
		assertEquals(2, subject.maxTokensPerAccount());
		assertEquals(3, subject.maxTokenSymbolUtf8Bytes());
		assertEquals(4L, subject.maxAccountNum());
		assertEquals(7, subject.maxFileSizeKb());
		assertEquals(accountWith(1L, 2L, 8L), subject.fundingAccount());
		assertEquals(9, subject.cacheRecordsTtl());
		assertEquals(10, subject.maxContractStorageKb());
		assertEquals(11, subject.ratesIntradayChangeLimitPercent());
		assertEquals(12, subject.balancesExportPeriodSecs());
		assertFalse(subject.shouldExportBalances());
		assertEquals(14L, subject.nodeBalanceWarningThreshold());
		assertEquals(balanceExportPaths[0], subject.pathToBalancesExportDir());
		assertFalse(subject.shouldExportTokenBalances());
		assertEquals(16, subject.maxTransferListSize());
		assertEquals(17, subject.maxTokenTransferListSize());
		assertEquals(18, subject.maxMemoUtf8Bytes());
		assertEquals(19L, subject.maxTxnDuration());
		assertEquals(20L, subject.minTxnDuration());
		assertEquals(21, subject.minValidityBuffer());
		assertEquals(22, subject.maxGas());
		assertEquals(23L, subject.defaultContractLifetime());
		assertEquals(24, subject.feesTokenTransferUsageMultiplier());
		assertEquals(25, subject.maxAutoRenewDuration());
		assertEquals(26, subject.minAutoRenewDuration());
		assertEquals(27, subject.localCallEstRetBytes());
		assertEquals(28, subject.scheduledTxExpiryTimeSecs());
		assertEquals(29, subject.messageMaxBytesAllowed());
		assertEquals(Set.of(grameFunctionality.CryptoCreate), subject.schedulingWhitelist());
		assertEquals(evenCongestion, subject.congestionMultipliers());
		assertEquals(30, subject.feesMinCongestionPeriod());
	}

	private void givenPropsWithSeed(int i) {
		given(properties.getIntProperty("tokens.maxPerAccount")).willReturn(i);
		given(properties.getIntProperty("tokens.maxSymbolUtf8Bytes")).willReturn(i + 1);
		given(properties.getBooleanProperty("ledger.keepRecordsInState")).willReturn((i % 2) == 0);
		given(properties.getLongProperty("ledger.maxAccountNum")).willReturn((long)i + 2);
		given(properties.getIntProperty("files.maxSizeKb")).willReturn(i + 5);
		given(properties.getLongProperty("ledger.fundingAccount")).willReturn((long)i + 6);
		given(properties.getIntProperty("cache.records.ttl")).willReturn(i + 7);
		given(properties.getIntProperty("contracts.maxStorageKb")).willReturn(i + 8);
		given(properties.getIntProperty("rates.intradayChangeLimitPercent")).willReturn(i + 9);
		given(properties.getIntProperty("balances.exportPeriodSecs")).willReturn(i + 10);
		given(properties.getBooleanProperty("balances.exportEnabled")).willReturn((i + 11) % 2 == 0);
		given(properties.getLongProperty("balances.nodeBalanceWarningThreshold")).willReturn(i + 12L);
		given(properties.getStringProperty("balances.exportDir.path")).willReturn(balanceExportPaths[i % 2]);
		given(properties.getBooleanProperty("balances.exportTokenBalances")).willReturn((i + 13) % 2 == 0);
		given(properties.getIntProperty("ledger.transfers.maxLen")).willReturn(i + 14);
		given(properties.getIntProperty("ledger.tokenTransfers.maxLen")).willReturn(i + 15);
		given(properties.getIntProperty("grame.transaction.maxMemoUtf8Bytes")).willReturn(i + 16);
		given(properties.getLongProperty("grame.transaction.maxValidDuration")).willReturn(i + 17L);
		given(properties.getLongProperty("grame.transaction.minValidDuration")).willReturn(i + 18L);
		given(properties.getIntProperty("grame.transaction.minValidityBufferSecs")).willReturn(i + 19);
		given(properties.getIntProperty("contracts.maxGas")).willReturn(i + 20);
		given(properties.getLongProperty("contracts.defaultLifetime")).willReturn(i + 21L);
		given(properties.getIntProperty("fees.tokenTransferUsageMultiplier")).willReturn(i + 22);
		given(properties.getLongProperty("ledger.autoRenewPeriod.maxDuration")).willReturn(i + 23L);
		given(properties.getLongProperty("ledger.autoRenewPeriod.minDuration")).willReturn(i + 24L);
		given(properties.getIntProperty("contracts.localCall.estRetBytes")).willReturn(i + 25);
		given(properties.getIntProperty("ledger.schedule.txExpiryTimeSecs")).willReturn(i + 26);
		given(properties.getIntProperty("consensus.message.maxBytesAllowed")).willReturn(i + 27);
		given(properties.getFunctionsProperty("scheduling.whitelist")).willReturn(i % 2 == 0
				? Set.of(grameFunctionality.CryptoCreate)
				: Set.of(grameFunctionality.CryptoTransfer));
		given(properties.getCongestionMultiplierProperty("fees.percentCongestionMultipliers"))
				.willReturn(i % 2 == 0 ? evenCongestion : oddCongestion);
		given(properties.getIntProperty("fees.minCongestionPeriod")).willReturn(i + 28);
	}

	private AccountID accountWith(long shard, long realm, long num) {
		return AccountID.newBuilder()
				.setShardNum(shard)
				.setRealmNum(realm)
				.setAccountNum(num)
				.build();
	}
}
