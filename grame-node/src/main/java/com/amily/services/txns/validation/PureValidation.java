package com.grame.services.txns.validation;

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

import com.grame.services.legacy.core.jproto.JKey;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.context.primitives.StateView;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.ContractID;
import com.gramegrame.api.proto.java.FileGetInfoResponse;
import com.gramegrame.api.proto.java.FileID;
import com.gramegrame.api.proto.java.Key;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.Timestamp;
import com.grame.services.state.merkle.MerkleEntityId;
import com.swirlds.fcmap.FCMap;
import org.apache.commons.codec.DecoderException;

import java.time.Instant;
import java.util.Optional;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.ACCOUNT_DELETED;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.CONTRACT_DELETED;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_ACCOUNT_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_CONTRACT_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_FILE_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TRANSACTION_START;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.TRANSACTION_EXPIRED;
import static com.grame.services.state.merkle.MerkleEntityId.fromContractId;

public class PureValidation {
	public static ResponseCodeEnum queryableFileStatus(FileID id, StateView view) {
		Optional<FileGetInfoResponse.FileInfo> info = view.infoForFile(id);
		if (info.isEmpty()) {
			return INVALID_FILE_ID;
		} else {
			return  OK;
		}
	}

	public static ResponseCodeEnum queryableAccountStatus(AccountID id, FCMap<MerkleEntityId, MerkleAccount> accounts) {
		MerkleAccount account = accounts.get(MerkleEntityId.fromAccountId(id));

		return Optional.ofNullable(account)
				.map(v -> v.isDeleted()
						? ACCOUNT_DELETED
						: (v.isSmartContract() ? INVALID_ACCOUNT_ID : OK))
				.orElse(INVALID_ACCOUNT_ID);
	}

	public static ResponseCodeEnum queryableContractStatus(ContractID cid, FCMap<MerkleEntityId, MerkleAccount> contracts) {
		MerkleAccount contract = contracts.get(fromContractId(cid));

		return Optional.ofNullable(contract)
				.map(v -> v.isDeleted()
						? CONTRACT_DELETED
						: (!v.isSmartContract() ? INVALID_CONTRACT_ID : OK))
				.orElse(INVALID_CONTRACT_ID);
	}

	public static ResponseCodeEnum chronologyStatus(Instant consensusTime, Instant validStart, long validDuration) {
		validDuration = Math.min(validDuration, Instant.MAX.getEpochSecond() - validStart.getEpochSecond());
		if (validStart.plusSeconds(validDuration).isBefore(consensusTime)) {
			return TRANSACTION_EXPIRED;
		} else if (!validStart.isBefore(consensusTime)) {
			return INVALID_TRANSACTION_START;
		} else {
			return OK;
		}
	}

	public static Instant asCoercedInstant(Timestamp when) {
		return Instant.ofEpochSecond(
			Math.min(Math.max(Instant.MIN.getEpochSecond(), when.getSeconds()), Instant.MAX.getEpochSecond()),
			Math.min(Math.max(Instant.MIN.getNano(), when.getNanos()), Instant.MAX.getNano()));
	}

	public static ResponseCodeEnum checkKey(Key key, ResponseCodeEnum failure) {
		try {
			var fcKey = JKey.mapKey(key);
			if (!fcKey.isValid()) {
				return failure;
			}
			return OK;
		} catch (DecoderException ignore) {
			return failure;
		}
	}
}
