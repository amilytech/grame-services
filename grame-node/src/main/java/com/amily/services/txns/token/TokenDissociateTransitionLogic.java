package com.grame.services.txns.token;

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
import com.grame.services.store.tokens.TokenStore;
import com.grame.services.txns.TransitionLogic;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.TokenDissociateTransactionBody;
import com.gramegrame.api.proto.java.TransactionBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Function;
import java.util.function.Predicate;

import static com.grame.services.txns.validation.TokenListChecks.repeatsItself;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.FAIL_INVALID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_ACCOUNT_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.SUCCESS;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.TOKEN_ID_REPEATED_IN_TOKEN_LIST;

/**
 * Provides the state transition for dissociating tokens from an account.
 *
 * @author AmilyTech
 */
public class TokenDissociateTransitionLogic implements TransitionLogic {
	private static final Logger log = LogManager.getLogger(TokenDissociateTransitionLogic.class);

	private final Function<TransactionBody, ResponseCodeEnum> SYNTAX_CHECK = this::validate;

	private final TokenStore store;
	private final TransactionContext txnCtx;

	public TokenDissociateTransitionLogic(
			TokenStore store,
			TransactionContext txnCtx
	) {
		this.store = store;
		this.txnCtx = txnCtx;
	}

	@Override
	public void doStateTransition() {
		try {
			var op = txnCtx.accessor().getTxn().getTokenDissociate();
			var outcome = store.dissociate(op.getAccount(), op.getTokensList());
			txnCtx.setStatus((outcome == OK) ? SUCCESS : outcome);
		} catch (Exception e) {
			log.warn("Unhandled error while processing :: {}!", txnCtx.accessor().getSignedTxn4Log(), e);
			txnCtx.setStatus(FAIL_INVALID);
		}
	}

	@Override
	public Predicate<TransactionBody> applicability() {
		return TransactionBody::hasTokenDissociate;
	}

	@Override
	public Function<TransactionBody, ResponseCodeEnum> syntaxCheck() {
		return SYNTAX_CHECK;
	}

	public ResponseCodeEnum validate(TransactionBody txnBody) {
		TokenDissociateTransactionBody op = txnBody.getTokenDissociate();

		if (!op.hasAccount()) {
			return INVALID_ACCOUNT_ID;
		}

		if (repeatsItself(op.getTokensList())) {
			return TOKEN_ID_REPEATED_IN_TOKEN_LIST;
		}

		return OK;
	}
}
