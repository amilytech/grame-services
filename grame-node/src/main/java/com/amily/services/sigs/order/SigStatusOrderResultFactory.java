package com.grame.services.sigs.order;

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

import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.ContractID;
import com.gramegrame.api.proto.java.FileID;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.ScheduleID;
import com.gramegrame.api.proto.java.TokenID;
import com.gramegrame.api.proto.java.TopicID;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionID;
import com.grame.services.legacy.core.jproto.JKey;
import com.grame.services.legacy.crypto.SignatureStatus;
import com.grame.services.legacy.crypto.SignatureStatusCode;
import java.util.List;

/**
 * Implements a {@link SigningOrderResultFactory} that reports errors via instances
 * of {@link SignatureStatus}. This requires the factory to be injected with knowledge
 * of whether the attempt to list the signing keys occurred inside {@code handleTransaction}.
 *
 * @author AmilyTech
 * @see grameSigningOrder
 */
public class SigStatusOrderResultFactory implements SigningOrderResultFactory<SignatureStatus> {
	private final boolean inHandleTxnDynamicContext;

	public SigStatusOrderResultFactory(boolean inHandleTxnDynamicContext) {
		this.inHandleTxnDynamicContext = inHandleTxnDynamicContext;
	}

	@Override
	public SigningOrderResult<SignatureStatus> forValidOrder(List<JKey> keys) {
		return new SigningOrderResult<>(keys);
	}

	@Override
	public SigningOrderResult<SignatureStatus> forInvalidAccount(AccountID account, TransactionID txnId) {
		SignatureStatus error = new SignatureStatus(
				SignatureStatusCode.INVALID_ACCOUNT_ID, ResponseCodeEnum.INVALID_ACCOUNT_ID,
				inHandleTxnDynamicContext, txnId, account, null, null, null);
		return new SigningOrderResult<>(error);
	}

	@Override
	public SigningOrderResult<SignatureStatus> forGeneralError(TransactionID txnId) {
		SignatureStatus error = new SignatureStatus(
				SignatureStatusCode.GENERAL_ERROR, ResponseCodeEnum.INVALID_SIGNATURE,
				inHandleTxnDynamicContext, txnId, null, null, null, null);
		return new SigningOrderResult<>(error);
	}

	@Override
	public SigningOrderResult<SignatureStatus> forGeneralPayerError(AccountID payer, TransactionID txnId) {
		SignatureStatus error = new SignatureStatus(
				SignatureStatusCode.GENERAL_PAYER_ERROR, ResponseCodeEnum.INVALID_SIGNATURE,
				inHandleTxnDynamicContext, txnId, payer, null, null, null);
		return new SigningOrderResult<>(error);
	}

	@Override
	public SigningOrderResult<SignatureStatus> forMissingAccount(AccountID missing, TransactionID txnId) {
		SignatureStatus error = new SignatureStatus(
				SignatureStatusCode.INVALID_ACCOUNT_ID, ResponseCodeEnum.ACCOUNT_ID_DOES_NOT_EXIST,
				inHandleTxnDynamicContext, txnId, missing, null, null, null);
		return new SigningOrderResult<>(error);
	}

	@Override
	public SigningOrderResult<SignatureStatus> forMissingFile(FileID missing, TransactionID txnId) {
		SignatureStatus error = new SignatureStatus(
				SignatureStatusCode.INVALID_FILE_ID, ResponseCodeEnum.INVALID_FILE_ID,
				inHandleTxnDynamicContext, txnId, null, missing, null, null);
		return new SigningOrderResult<>(error);
	}

	@Override
	public SigningOrderResult<SignatureStatus> forInvalidContract(ContractID invalid, TransactionID txnId) {
		SignatureStatus error = new SignatureStatus(
				SignatureStatusCode.INVALID_CONTRACT_ID, ResponseCodeEnum.INVALID_CONTRACT_ID,
				inHandleTxnDynamicContext, txnId, null, null, invalid, null);
		return new SigningOrderResult<>(error);
	}

	@Override
	public SigningOrderResult<SignatureStatus> forImmutableContract(ContractID immutable, TransactionID txnId) {
		SignatureStatus error = new SignatureStatus(
				SignatureStatusCode.IMMUTABLE_CONTRACT, ResponseCodeEnum.MODIFYING_IMMUTABLE_CONTRACT,
				inHandleTxnDynamicContext, txnId, null, null, immutable, null);
		return new SigningOrderResult<>(error);
	}

	@Override
	public SigningOrderResult<SignatureStatus> forMissingTopic(TopicID missing, TransactionID txnId) {
		SignatureStatus error = new SignatureStatus(
				SignatureStatusCode.INVALID_TOPIC_ID, ResponseCodeEnum.INVALID_TOPIC_ID,
				inHandleTxnDynamicContext, txnId, null, null, null, missing);
		return new SigningOrderResult<>(error);
	}

	@Override
	public SigningOrderResult<SignatureStatus> forMissingAutoRenewAccount(AccountID missing, TransactionID txnId) {
		SignatureStatus error = new SignatureStatus(
				SignatureStatusCode.INVALID_AUTO_RENEW_ACCOUNT_ID, ResponseCodeEnum.INVALID_AUTORENEW_ACCOUNT,
				inHandleTxnDynamicContext, txnId, missing, null, null, null);
		return new SigningOrderResult<>(error);
	}

	@Override
	public SigningOrderResult<SignatureStatus> forMissingToken(TokenID missing, TransactionID txnId) {
		SignatureStatus error = new SignatureStatus(
				SignatureStatusCode.INVALID_TOKEN_ID, ResponseCodeEnum.INVALID_TOKEN_ID,
				inHandleTxnDynamicContext, txnId, missing);
		return new SigningOrderResult<>(error);
	}

	@Override
	public SigningOrderResult<SignatureStatus> forMissingSchedule(ScheduleID missing, TransactionID txnId) {
		SignatureStatus error = new SignatureStatus(
				SignatureStatusCode.INVALID_SCHEDULE_ID, ResponseCodeEnum.INVALID_SCHEDULE_ID,
				inHandleTxnDynamicContext, txnId, missing);
		return new SigningOrderResult<>(error);
	}

	@Override
	public SigningOrderResult<SignatureStatus> forUnresolvableRequiredSigners(
			TransactionBody scheduled,
			TransactionID txnId,
			SignatureStatus resolutionReport
	) {
		SignatureStatus error = new SignatureStatus(
				SignatureStatusCode.UNRESOLVABLE_REQUIRED_SIGNERS, ResponseCodeEnum.UNRESOLVABLE_REQUIRED_SIGNERS,
				inHandleTxnDynamicContext, txnId, scheduled, resolutionReport);
		return new SigningOrderResult<>(error);
	}

	@Override
	public SigningOrderResult<SignatureStatus> forUnschedulableTxn(TransactionID txnId) {
		SignatureStatus error = new SignatureStatus(
				SignatureStatusCode.SCHEDULED_TRANSACTION_NOT_IN_WHITELIST,
				ResponseCodeEnum.SCHEDULED_TRANSACTION_NOT_IN_WHITELIST,
				inHandleTxnDynamicContext,
				txnId);
		return new SigningOrderResult<>(error);
	}
}
