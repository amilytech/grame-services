package com.grame.services.queries.crypto;

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

import com.grame.services.context.primitives.StateView;
import com.grame.services.state.merkle.MerkleToken;
import com.grame.services.txns.validation.OptionValidator;
import com.grame.services.queries.AnswerService;
import com.grame.services.utils.SignedTxnAccessor;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.CryptoGetAccountBalanceQuery;
import com.gramegrame.api.proto.java.CryptoGetAccountBalanceResponse;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.state.merkle.MerkleAccount;
import com.gramegrame.api.proto.java.TokenBalance;
import com.gramegrame.api.proto.java.TokenID;
import com.swirlds.fcmap.FCMap;

import static com.grame.services.state.merkle.MerkleEntityAssociation.fromAccountTokenRel;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoGetAccountBalance;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_ACCOUNT_ID;

import java.util.Optional;

import static com.grame.services.utils.EntityIdUtils.asAccount;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;

public class GetAccountBalanceAnswer implements AnswerService {
	private final OptionValidator optionValidator;

	public GetAccountBalanceAnswer(OptionValidator optionValidator) {
		this.optionValidator = optionValidator;
	}

	@Override
	public ResponseCodeEnum checkValidity(Query query, StateView view) {
		FCMap<MerkleEntityId, MerkleAccount> accounts = view.accounts();
		CryptoGetAccountBalanceQuery op = query.getCryptogetAccountBalance();
		return validityOf(op, accounts);
	}

	@Override
	public boolean requiresNodePayment(Query query) {
		return false;
	}

	@Override
	public boolean needsAnswerOnlyCost(Query query) {
		return false;
	}

	@Override
	public Response responseGiven(Query query, StateView view, ResponseCodeEnum validity, long cost) {
		FCMap<MerkleEntityId, MerkleAccount> accounts = view.accounts();
		CryptoGetAccountBalanceQuery op = query.getCryptogetAccountBalance();

		AccountID id = targetOf(op);
		CryptoGetAccountBalanceResponse.Builder opAnswer = CryptoGetAccountBalanceResponse.newBuilder()
				.setHeader(answerOnlyHeader(validity))
				.setAccountID(id);

		if (validity == OK) {
			var key = MerkleEntityId.fromAccountId(id);
			var account = accounts.get(key);
			opAnswer.setBalance(account.getBalance());
			for (TokenID tId : account.tokens().asIds()) {
				var relKey = fromAccountTokenRel(id, tId);
				var relationship = view.tokenAssociations().get().get(relKey);
				var decimals = view.tokenWith(tId).map(MerkleToken::decimals).orElse(0);
				opAnswer.addTokenBalances(TokenBalance.newBuilder()
						.setTokenId(tId)
						.setBalance(relationship.getBalance())
						.setDecimals(decimals)
						.build());
			}
		}

		return Response.newBuilder().setCryptogetAccountBalance(opAnswer).build();
	}

	@Override
	public Optional<SignedTxnAccessor> extractPaymentFrom(Query query) {
		return Optional.empty();
	}

	private AccountID targetOf(CryptoGetAccountBalanceQuery op) {
		return op.hasAccountID()
				? op.getAccountID()
				: (op.hasContractID() ? asAccount(op.getContractID()) : AccountID.getDefaultInstance());
	}

	private ResponseCodeEnum validityOf(
			CryptoGetAccountBalanceQuery op,
			FCMap<MerkleEntityId, MerkleAccount> accounts
	) {
		if (op.hasContractID()) {
			return optionValidator.queryableContractStatus(op.getContractID(), accounts);
		} else if (op.hasAccountID()) {
			return optionValidator.queryableAccountStatus(op.getAccountID(), accounts);
		} else {
			return INVALID_ACCOUNT_ID;
		}
	}

	@Override
	public ResponseCodeEnum extractValidityFrom(Response response) {
		return response.getCryptogetAccountBalance().getHeader().getNodeTransactionPrecheckCode();
	}

	@Override
	public grameFunctionality canonicalFunction() {
		return CryptoGetAccountBalance;
	}
}
