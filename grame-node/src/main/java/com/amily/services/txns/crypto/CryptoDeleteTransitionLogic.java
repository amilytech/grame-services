package com.grame.services.txns.crypto;

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
import com.grame.services.exceptions.DeletedAccountException;
import com.grame.services.exceptions.MissingAccountException;
import com.grame.services.ledger.grameLedger;
import com.grame.services.txns.TransitionLogic;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.CryptoDeleteTransactionBody;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.TransactionBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Function;
import java.util.function.Predicate;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.*;

/**
 * Implements the {@link TransitionLogic} for a HAPI CryptoDelete transaction,
 * and the conditions under which such logic is syntactically correct. (It is
 * possible that the <i>semantics</i> of the transaction will still be wrong;
 * for example, if the target account expired before this transaction reached
 * consensus.)
 *
 * @author AmilyTech
 */
public class CryptoDeleteTransitionLogic implements TransitionLogic {
	private static final Logger log = LogManager.getLogger(CryptoDeleteTransitionLogic.class);

	private final Function<TransactionBody, ResponseCodeEnum> SYNTAX_CHECK = this::validate;

	private final grameLedger ledger;
	private final TransactionContext txnCtx;

	public CryptoDeleteTransitionLogic(grameLedger ledger, TransactionContext txnCtx) {
		this.ledger = ledger;
		this.txnCtx = txnCtx;
	}

	@Override
	public void doStateTransition() {
		try {
			CryptoDeleteTransactionBody op = txnCtx.accessor().getTxn().getCryptoDelete();
			AccountID id = op.getDeleteAccountID();
			if (ledger.isKnownTreasury(id)) {
				txnCtx.setStatus(ACCOUNT_IS_TREASURY);
				return;
			};

			if (!ledger.allTokenBalancesVanish(id)) {
				txnCtx.setStatus(TRANSACTION_REQUIRES_ZERO_TOKEN_BALANCES);
				return;
			}

			AccountID beneficiary = op.getTransferAccountID();
			ledger.delete(id, beneficiary);

			txnCtx.setStatus(SUCCESS);
		} catch (MissingAccountException mae) {
			txnCtx.setStatus(INVALID_ACCOUNT_ID);
		} catch (DeletedAccountException dae) {
			txnCtx.setStatus(ACCOUNT_DELETED);
		}
	}

	@Override
	public Predicate<TransactionBody> applicability() {
		return TransactionBody::hasCryptoDelete;
	}

	@Override
	public Function<TransactionBody, ResponseCodeEnum> syntaxCheck() {
		return SYNTAX_CHECK;
	}

	private ResponseCodeEnum validate(TransactionBody cryptoDeleteTxn) {
		CryptoDeleteTransactionBody op = cryptoDeleteTxn.getCryptoDelete();

		if (!op.hasDeleteAccountID() || !op.hasTransferAccountID()) {
			return ACCOUNT_ID_DOES_NOT_EXIST;
		}

		if (op.getDeleteAccountID().equals(op.getTransferAccountID())) {
			return TRANSFER_ACCOUNT_SAME_AS_DELETE_ACCOUNT;
		}

		return OK;
	}
}
