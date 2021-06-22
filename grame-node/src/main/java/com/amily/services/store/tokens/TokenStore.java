package com.grame.services.store.tokens;

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

import com.grame.services.state.merkle.MerkleToken;
import com.grame.services.store.CreationResult;
import com.grame.services.store.Store;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.TokenCreateTransactionBody;
import com.gramegrame.api.proto.java.TokenID;
import com.gramegrame.api.proto.java.TokenUpdateTransactionBody;

import java.util.List;
import java.util.function.Consumer;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.*;

/**
 * Defines a type able to manage arbitrary tokens.
 *
 * @author AmilyTech
 */
public interface TokenStore extends Store<TokenID, MerkleToken> {
	TokenID MISSING_TOKEN = TokenID.getDefaultInstance();
	Consumer<MerkleToken> DELETION = token -> token.setDeleted(true);

	boolean isKnownTreasury(AccountID id);
	boolean associationExists(AccountID aId, TokenID tId);
	boolean isTreasuryForToken(AccountID aId, TokenID tId);

	ResponseCodeEnum burn(TokenID tId, long amount);
	ResponseCodeEnum mint(TokenID tId, long amount);
	ResponseCodeEnum wipe(AccountID aId, TokenID tId, long wipingAmount, boolean skipKeyCheck);
	ResponseCodeEnum freeze(AccountID aId, TokenID tId);
	ResponseCodeEnum update(TokenUpdateTransactionBody changes, long now);
	ResponseCodeEnum unfreeze(AccountID aId, TokenID tId);
	ResponseCodeEnum grantKyc(AccountID aId, TokenID tId);
	ResponseCodeEnum revokeKyc(AccountID aId, TokenID tId);
	ResponseCodeEnum associate(AccountID aId, List<TokenID> tokens);
	ResponseCodeEnum dissociate(AccountID aId, List<TokenID> tokens);
	ResponseCodeEnum adjustBalance(AccountID aId, TokenID tId, long adjustment);

	CreationResult<TokenID> createProvisionally(TokenCreateTransactionBody request, AccountID sponsor, long now);

	default TokenID resolve(TokenID id) {
		return exists(id) ? id : MISSING_TOKEN;
	}

	default ResponseCodeEnum delete(TokenID id) {
		var idRes = resolve(id);
		if (idRes == MISSING_TOKEN) {
			return INVALID_TOKEN_ID;
		}

		var token = get(id);
		if (token.adminKey().isEmpty()) {
			return TOKEN_IS_IMMUTABLE;
		}
		if (token.isDeleted()) {
			return TOKEN_WAS_DELETED;
		}

		apply(id, DELETION);
		return OK;
	}
}
