package com.grame.test.mocks;

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

import com.google.common.cache.CacheBuilder;
import com.grame.services.config.MockGlobalDynamicProps;
import com.grame.services.context.properties.BootstrapProperties;
import com.grame.services.context.properties.PropertySource;
import com.grame.services.context.properties.StandardizedPropertySources;
import com.grame.services.fees.FeeCalculator;
import com.grame.services.fees.HbarCentExchange;
import com.grame.services.fees.calculation.TxnResourceUsageEstimator;
import com.grame.services.fees.calculation.UsageBasedFeeCalculator;
import com.grame.services.fees.calculation.consensus.queries.GetTopicInfoResourceUsage;
import com.grame.services.fees.calculation.consensus.txns.CreateTopicResourceUsage;
import com.grame.services.fees.calculation.consensus.txns.DeleteTopicResourceUsage;
import com.grame.services.fees.calculation.consensus.txns.SubmitMessageResourceUsage;
import com.grame.services.fees.calculation.consensus.txns.UpdateTopicResourceUsage;
import com.grame.services.fees.calculation.contract.txns.ContractCallResourceUsage;
import com.grame.services.fees.calculation.contract.txns.ContractCreateResourceUsage;
import com.grame.services.fees.calculation.contract.txns.ContractDeleteResourceUsage;
import com.grame.services.fees.calculation.contract.txns.ContractUpdateResourceUsage;
import com.grame.services.fees.calculation.crypto.queries.GetAccountInfoResourceUsage;
import com.grame.services.fees.calculation.crypto.queries.GetAccountRecordsResourceUsage;
import com.grame.services.fees.calculation.crypto.queries.GetTxnRecordResourceUsage;
import com.grame.services.fees.calculation.crypto.txns.CryptoCreateResourceUsage;
import com.grame.services.fees.calculation.crypto.txns.CryptoDeleteResourceUsage;
import com.grame.services.fees.calculation.crypto.txns.CryptoTransferResourceUsage;
import com.grame.services.fees.calculation.crypto.txns.CryptoUpdateResourceUsage;
import com.grame.services.fees.calculation.file.txns.FileAppendResourceUsage;
import com.grame.services.fees.calculation.file.txns.FileCreateResourceUsage;
import com.grame.services.fees.calculation.file.txns.FileDeleteResourceUsage;
import com.grame.services.fees.calculation.file.txns.FileUpdateResourceUsage;
import com.grame.services.fees.calculation.file.txns.SystemDeleteFileResourceUsage;
import com.grame.services.fees.calculation.file.txns.SystemUndeleteFileResourceUsage;
import com.grame.services.fees.calculation.system.txns.FreezeResourceUsage;
import com.grame.services.queries.answering.AnswerFunctions;
import com.grame.services.records.RecordCache;
import com.grame.services.usage.crypto.CryptoOpsUsage;
import com.grame.services.usage.file.FileOpsUsage;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.fee.CryptoFeeBuilder;
import com.gramegrame.fee.FileFeeBuilder;
import com.gramegrame.fee.SmartContractFeeBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.grame.test.mocks.TestExchangeRates.TEST_EXCHANGE;
import static com.grame.test.mocks.TestFeeMultiplierSource.MULTIPLIER_SOURCE;
import static com.grame.test.mocks.TestUsagePricesProvider.TEST_USAGE_PRICES;
import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusCreateTopic;
import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusDeleteTopic;
import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusSubmitMessage;
import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusUpdateTopic;
import static com.gramegrame.api.proto.java.grameFunctionality.ContractCall;
import static com.gramegrame.api.proto.java.grameFunctionality.ContractCreate;
import static com.gramegrame.api.proto.java.grameFunctionality.ContractDelete;
import static com.gramegrame.api.proto.java.grameFunctionality.ContractUpdate;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoCreate;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoDelete;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoTransfer;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoUpdate;
import static com.gramegrame.api.proto.java.grameFunctionality.FileAppend;
import static com.gramegrame.api.proto.java.grameFunctionality.FileCreate;
import static com.gramegrame.api.proto.java.grameFunctionality.FileDelete;
import static com.gramegrame.api.proto.java.grameFunctionality.FileUpdate;
import static com.gramegrame.api.proto.java.grameFunctionality.Freeze;
import static com.gramegrame.api.proto.java.grameFunctionality.SystemDelete;
import static com.gramegrame.api.proto.java.grameFunctionality.SystemUndelete;
import static java.util.Map.entry;

public enum TestFeesFactory {
	FEES_FACTORY;

	public FeeCalculator get() {
		return getWithExchange(TEST_EXCHANGE);
	}

	public FeeCalculator getWithExchange(HbarCentExchange exchange) {
		CryptoOpsUsage cryptoOpsUsage = new CryptoOpsUsage();
		FileOpsUsage fileOpsUsage = new FileOpsUsage();
		FileFeeBuilder fileFees = new FileFeeBuilder();
		CryptoFeeBuilder cryptoFees = new CryptoFeeBuilder();
		SmartContractFeeBuilder contractFees = new SmartContractFeeBuilder();
		PropertySource properties =
				new StandardizedPropertySources(new BootstrapProperties(), ignore -> true).asResolvingSource();
		AnswerFunctions answerFunctions = new AnswerFunctions();
		RecordCache recordCache = new RecordCache(
				null,
				CacheBuilder.newBuilder().build(),
				new HashMap<>());

		return new UsageBasedFeeCalculator(
				exchange,
				TEST_USAGE_PRICES,
				MULTIPLIER_SOURCE,
				List.of(
						/* Meta */
						new GetTxnRecordResourceUsage(recordCache, answerFunctions, cryptoFees),
						/* Crypto */
						new GetAccountInfoResourceUsage(cryptoOpsUsage),
						new GetAccountRecordsResourceUsage(answerFunctions, cryptoFees),
						/* Consensus */
						new GetTopicInfoResourceUsage()
				),
				txnUsageFn(cryptoOpsUsage, fileOpsUsage, fileFees, cryptoFees, contractFees)
		);
	}

	private Function<grameFunctionality, List<TxnResourceUsageEstimator>> txnUsageFn(
			CryptoOpsUsage cryptoOpsUsage,
			FileOpsUsage fileOpsUsage,
			FileFeeBuilder fileFees,
			CryptoFeeBuilder cryptoFees,
			SmartContractFeeBuilder contractFees
	) {
		return Map.ofEntries(
				/* Crypto */
				entry(CryptoCreate, List.<TxnResourceUsageEstimator>of(new CryptoCreateResourceUsage(cryptoOpsUsage))),
				entry(CryptoDelete, List.<TxnResourceUsageEstimator>of(new CryptoDeleteResourceUsage(cryptoFees))),
				entry(CryptoUpdate, List.<TxnResourceUsageEstimator>of(new CryptoUpdateResourceUsage(cryptoOpsUsage))),
				entry(CryptoTransfer, List.<TxnResourceUsageEstimator>of(new CryptoTransferResourceUsage(new MockGlobalDynamicProps()))),
				/* Contract */
				entry(ContractCall, List.<TxnResourceUsageEstimator>of(new ContractCallResourceUsage(contractFees))),
				entry(ContractCreate, List.<TxnResourceUsageEstimator>of(new ContractCreateResourceUsage(contractFees))),
				entry(ContractDelete, List.<TxnResourceUsageEstimator>of(new ContractDeleteResourceUsage(contractFees))),
				entry(ContractUpdate, List.<TxnResourceUsageEstimator>of(new ContractUpdateResourceUsage(contractFees))),
				/* File */
				entry(FileCreate, List.<TxnResourceUsageEstimator>of(new FileCreateResourceUsage(fileOpsUsage))),
				entry(FileDelete, List.<TxnResourceUsageEstimator>of(new FileDeleteResourceUsage(fileFees))),
				entry(FileUpdate, List.<TxnResourceUsageEstimator>of(new FileUpdateResourceUsage(fileOpsUsage))),
				entry(FileAppend, List.<TxnResourceUsageEstimator>of(new FileAppendResourceUsage(fileFees))),
				/* Consensus */
				entry(ConsensusCreateTopic, List.<TxnResourceUsageEstimator>of(new CreateTopicResourceUsage())),
				entry(ConsensusUpdateTopic, List.<TxnResourceUsageEstimator>of(new UpdateTopicResourceUsage())),
				entry(ConsensusDeleteTopic, List.<TxnResourceUsageEstimator>of(new DeleteTopicResourceUsage())),
				entry(ConsensusSubmitMessage, List.<TxnResourceUsageEstimator>of(new SubmitMessageResourceUsage())),
				/* System */
				entry(Freeze, List.<TxnResourceUsageEstimator>of(new FreezeResourceUsage())),
				entry(SystemDelete, List.<TxnResourceUsageEstimator>of(new SystemDeleteFileResourceUsage(fileFees))),
				entry(SystemUndelete, List.<TxnResourceUsageEstimator>of(new SystemUndeleteFileResourceUsage(fileFees)))
		)::get;
	}
}
