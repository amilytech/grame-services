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
import com.grame.services.ledger.accounts.grameAccountCustomizer;
import com.grame.services.ledger.grameLedger;
import com.grame.services.state.submerkle.EntityId;
import com.grame.services.txns.TransitionLogic;
import com.grame.services.txns.validation.OptionValidator;
import com.grame.services.utils.MiscUtils;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.CryptoUpdateTransactionBody;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.TransactionBody;
import com.grame.services.legacy.core.jproto.JKey;
import org.apache.commons.codec.DecoderException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.grame.services.utils.MiscUtils.asFcKeyUnchecked;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.*;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Implements the {@link TransitionLogic} for a HAPI CryptoUpdate transaction,
 * and the conditions under which such logic is syntactically correct. (It is
 * possible that the <i>semantics</i> of the transaction will still be wrong;
 * for example, if the target account was deleted before this transaction
 * reached consensus.)
 *
 * @author AmilyTech
 */
public class CryptoUpdateTransitionLogic implements TransitionLogic {
	private static final Logger log = LogManager.getLogger(CryptoUpdateTransitionLogic.class);

	private final Function<TransactionBody, ResponseCodeEnum> SYNTAX_CHECK = this::validate;

	private final grameLedger ledger;
	private final OptionValidator validator;
	private final TransactionContext txnCtx;

	public CryptoUpdateTransitionLogic(
			grameLedger ledger,
			OptionValidator validator,
			TransactionContext txnCtx
	) {
		this.ledger = ledger;
		this.validator = validator;
		this.txnCtx = txnCtx;
	}

	@Override
	public void doStateTransition() {
		try {
			CryptoUpdateTransactionBody op = txnCtx.accessor().getTxn().getCryptoUpdateAccount();
			AccountID target = op.getAccountIDToUpdate();

			ledger.customize(target, asCustomizer(op));
			txnCtx.setStatus(SUCCESS);
		} catch (MissingAccountException mae) {
			txnCtx.setStatus(INVALID_ACCOUNT_ID);
		} catch (DeletedAccountException aide) {
			txnCtx.setStatus(ACCOUNT_DELETED);
		} catch (Exception e) {
			txnCtx.setStatus(FAIL_INVALID);
		}
	}

	private grameAccountCustomizer asCustomizer(CryptoUpdateTransactionBody op) {
		grameAccountCustomizer customizer = new grameAccountCustomizer();

		if (op.hasKey()) {
			/* Note that {@code this.validate(TransactionBody)} will have rejected any txn with an invalid key. */
			var fcKey = asFcKeyUnchecked(op.getKey());
			customizer.key(fcKey);
		}
		if (op.hasExpirationTime()) {
			customizer.expiry(op.getExpirationTime().getSeconds());
		}
		if (op.hasProxyAccountID()) {
			customizer.proxy(EntityId.ofNullableAccountId(op.getProxyAccountID()));
		}
		if (op.hasReceiverSigRequiredWrapper()) {
			customizer.isReceiverSigRequired(op.getReceiverSigRequiredWrapper().getValue());
		} else if (op.getReceiverSigRequired()) {
			customizer.isReceiverSigRequired(true);
		}
		if (op.hasAutoRenewPeriod()) {
			customizer.autoRenewPeriod(op.getAutoRenewPeriod().getSeconds());
		}
		if (op.hasMemo()) {
			customizer.memo(op.getMemo().getValue());
		}

		return customizer;
	}

	@Override
	public Predicate<TransactionBody> applicability() {
		return TransactionBody::hasCryptoUpdateAccount;
	}

	@Override
	public Function<TransactionBody, ResponseCodeEnum> syntaxCheck() {
		return SYNTAX_CHECK;
	}

	private ResponseCodeEnum validate(TransactionBody cryptoUpdateTxn) {
		CryptoUpdateTransactionBody op = cryptoUpdateTxn.getCryptoUpdateAccount();

		var memoValidity = !op.hasMemo() ? OK : validator.memoCheck(op.getMemo().getValue());
		if (memoValidity != OK) {
			return memoValidity;
		}

		if (op.hasKey()) {
			try {
				JKey fcKey = JKey.mapKey(op.getKey());
				/* Note that an empty key is never valid. */
				if (!fcKey.isValid()) {
					return BAD_ENCODING;
				}
			} catch (DecoderException e) {
				return BAD_ENCODING;
			}
		}

		if (op.hasAutoRenewPeriod() && !validator.isValidAutoRenewPeriod(op.getAutoRenewPeriod())) {
			return AUTORENEW_DURATION_NOT_IN_RANGE;
		}
		if (op.hasExpirationTime() && !validator.isValidExpiry(op.getExpirationTime())) {
			return INVALID_EXPIRATION_TIME;
		}

		return OK;
	}
}
