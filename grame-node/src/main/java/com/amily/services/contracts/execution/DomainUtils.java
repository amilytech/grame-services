package com.grame.services.contracts.execution;

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

import com.google.protobuf.ByteString;
import com.gramegrame.api.proto.java.ContractFunctionResult;
import com.gramegrame.api.proto.java.ContractID;
import com.gramegrame.api.proto.java.ContractLoginfo;
import org.apache.commons.lang3.StringUtils;
import org.ethereum.core.Block;
import org.ethereum.core.Bloom;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.db.ServicesRepositoryImpl;
import org.ethereum.util.ByteUtil;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;
import org.ethereum.vm.program.ProgramResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.grame.services.utils.EntityIdUtils.accountParsedFromSolidityAddress;
import static com.grame.services.utils.EntityIdUtils.contractParsedFromSolidityAddress;
import static java.util.Collections.emptyList;
import static org.ethereum.core.BlockchainImpl.EMPTY_LIST_HASH;
import static org.ethereum.util.ByteUtil.longToBytesNoLeadZeroes;

public class DomainUtils {
	public static Block fakeBlock(Instant at) {
		return new Block(
				EMPTY_LIST_HASH,
				EMPTY_LIST_HASH,
				new byte[32],
				new byte[32],
				new byte[0],
				0,
				longToBytesNoLeadZeroes(Long.MAX_VALUE),
				0,
				at.getEpochSecond(),
				new byte[0],
				new byte[0],
				new byte[0],
				new byte[32],
				new byte[32],
				new byte[32],
				emptyList(),
				emptyList());
	}

	public static Consumer<byte[]> newScopedAccountInitializer(
			long startTimeEpochSecs,
			long contractDurationSecs,
			byte[] sponsorAddress,
			ServicesRepositoryImpl repository
	) {
		return address -> {
			var id = accountParsedFromSolidityAddress(address);
			var sponsor = repository.getAccount(sponsorAddress);

			repository.setSmartContract(address, true);
			repository.setRealmId(address, sponsor.getRealmId());
			repository.setShardId(address, sponsor.getShardId());
			repository.setAccountNum(address, id.getAccountNum());

			repository.setCreateTimeMs(address, startTimeEpochSecs * 1_000L);
			repository.setExpirationTime(address, startTimeEpochSecs + contractDurationSecs);
		};
	}

	public static TransactionReceipt asReceipt(
			long cumulativeGas,
			String errorMsg,
			Transaction solidityTxn,
			List<LogInfo> vmLogs,
			ProgramResult result
	) {
		var receipt = new TransactionReceipt();
		receipt.setCumulativeGas(cumulativeGas);
		receipt.setTransaction(solidityTxn);
		receipt.setLogInfoList(vmLogs);
		receipt.setGasUsed(cumulativeGas);
		receipt.setExecutionResult(result.getHReturn());
		receipt.setError(errorMsg);
		return receipt;
	}

	public static ContractFunctionResult asHapiResult(
			TransactionReceipt receipt,
			Optional<List<ContractID>> created
	) {
		var result = ContractFunctionResult.newBuilder();

		result.setGasUsed(ByteUtil.byteArrayToLong(receipt.getGasUsed()));
		result.setErrorMessage(receipt.getError());
		created.map(result::addAllCreatedContractIDs);

		if (!isFailed(receipt)) {
			if (isCreation(receipt)) {
				result.setContractID(contractParsedFromSolidityAddress(receipt.getTransaction().getContractAddress()));
			} else {
				Optional.ofNullable(receipt.getExecutionResult())
						.map(ByteString::copyFrom)
						.ifPresent(result::setContractCallResult);
			}
			Optional.ofNullable(receipt.getLogInfoList()).ifPresent(logs ->
					logs.stream().map(DomainUtils::asHapiLog).forEach(result::addLogInfo));
		}

		return result.build();
	}

	private static boolean isCreation(TransactionReceipt receipt) {
		return receipt.getTransaction().getContractAddress() != null;
	}

	private static boolean isFailed(TransactionReceipt receipt) {
		return StringUtils.isNotEmpty(receipt.getError());
	}

	public static ContractLoginfo asHapiLog(LogInfo logInfo) {
		var log = ContractLoginfo.newBuilder();

		log.setContractID(contractParsedFromSolidityAddress(logInfo.getAddress()));
		Optional.ofNullable(logInfo.getBloom())
				.map(Bloom::getData)
				.map(ByteString::copyFrom)
				.ifPresent(log::setBloom);
		Optional.ofNullable(logInfo.getData())
				.map(ByteString::copyFrom)
				.ifPresent(log::setData);
		Optional.ofNullable(logInfo.getTopics())
				.stream()
				.flatMap(List::stream)
				.map(DataWord::getData)
				.map(ByteString::copyFrom)
				.forEach(log::addTopic);

		return log.build();
	}

}
