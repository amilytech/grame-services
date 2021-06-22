package com.grame.services.txns.contract;

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
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.txns.TransitionLogic;
import com.grame.services.txns.validation.OptionValidator;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionRecord;
import com.swirlds.fcmap.FCMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.FAIL_INVALID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_CONTRACT_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;

public class ContractSysUndelTransitionLogic implements TransitionLogic {
	private static final Logger log = LogManager.getLogger(ContractSysUndelTransitionLogic.class);

	private final OptionValidator validator;
	private final TransactionContext txnCtx;
	private final LegacySystemUndeleter delegate;
	private final Supplier<FCMap<MerkleEntityId, MerkleAccount>> contracts;

	private final Function<TransactionBody, ResponseCodeEnum> SYNTAX_CHECK = this::validate;

	public ContractSysUndelTransitionLogic(
			OptionValidator validator,
			TransactionContext txnCtx,
			LegacySystemUndeleter delegate,
			Supplier<FCMap<MerkleEntityId, MerkleAccount>> contracts
	) {
		this.validator = validator;
		this.txnCtx = txnCtx;
		this.delegate = delegate;
		this.contracts = contracts;
	}

	@FunctionalInterface
	public interface LegacySystemUndeleter {
		TransactionRecord perform(TransactionBody txn, Instant consensusTime);
	}

	@Override
	public void doStateTransition() {
		try {
			var contractSysUndelTxn = txnCtx.accessor().getTxn();

			var legacyRecord = delegate.perform(contractSysUndelTxn, txnCtx.consensusTime());

			txnCtx.setStatus(legacyRecord.getReceipt().getStatus());
		} catch (Exception e) {
			log.warn("Avoidable exception!", e);
			txnCtx.setStatus(FAIL_INVALID);
		}
	}

	@Override
	public Predicate<TransactionBody> applicability() {
		return txn -> txn.hasSystemUndelete() && txn.getSystemUndelete().hasContractID();
	}

	@Override
	public Function<TransactionBody, ResponseCodeEnum> syntaxCheck() {
		return SYNTAX_CHECK;
	}

	public ResponseCodeEnum validate(TransactionBody contractSysUndelTxn) {
		var op = contractSysUndelTxn.getSystemUndelete();
		var status = validator.queryableContractStatus(op.getContractID(), contracts.get());
		return (status != INVALID_CONTRACT_ID) ? OK : INVALID_CONTRACT_ID;
	}
}
