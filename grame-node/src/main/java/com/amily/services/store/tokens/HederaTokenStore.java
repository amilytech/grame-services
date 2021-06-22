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

import com.grame.services.context.properties.GlobalDynamicProperties;
import com.grame.services.ledger.grameLedger;
import com.grame.services.ledger.TransactionalLedger;
import com.grame.services.ledger.ids.EntityIdSource;
import com.grame.services.ledger.properties.TokenRelProperty;
import com.grame.services.legacy.core.jproto.JKey;
import com.grame.services.sigs.utils.ImmutableKeyUtils;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.state.merkle.MerkleToken;
import com.grame.services.state.merkle.MerkleTokenRelStatus;
import com.grame.services.store.CreationResult;
import com.grame.services.store.grameStore;
import com.grame.services.txns.validation.OptionValidator;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.Duration;
import com.gramegrame.api.proto.java.Key;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.TokenCreateTransactionBody;
import com.gramegrame.api.proto.java.TokenID;
import com.gramegrame.api.proto.java.TokenUpdateTransactionBody;
import com.swirlds.fcmap.FCMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.grame.services.ledger.accounts.BackingTokenRels.asTokenRel;
import static com.grame.services.ledger.properties.TokenRelProperty.IS_FROZEN;
import static com.grame.services.ledger.properties.TokenRelProperty.IS_KYC_GRANTED;
import static com.grame.services.ledger.properties.TokenRelProperty.TOKEN_BALANCE;
import static com.grame.services.state.merkle.MerkleEntityId.fromTokenId;
import static com.grame.services.state.merkle.MerkleToken.UNUSED_KEY;
import static com.grame.services.state.submerkle.EntityId.ofNullableAccountId;
import static com.grame.services.store.CreationResult.failure;
import static com.grame.services.store.CreationResult.success;
import static com.grame.services.utils.EntityIdUtils.readableId;
import static com.grame.services.utils.MiscUtils.asFcKeyUnchecked;
import static com.grame.services.utils.MiscUtils.asUsableFcKey;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.ACCOUNT_FROZEN_FOR_TOKEN;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.ACCOUNT_IS_TREASURY;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.ACCOUNT_KYC_NOT_GRANTED_FOR_TOKEN;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.CANNOT_WIPE_TOKEN_TREASURY_ACCOUNT;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INSUFFICIENT_TOKEN_BALANCE;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_AUTORENEW_ACCOUNT;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_EXPIRATION_TIME;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_RENEWAL_PERIOD;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TOKEN_BURN_AMOUNT;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TOKEN_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TOKEN_MINT_AMOUNT;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_TREASURY_ACCOUNT_FOR_TOKEN;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_WIPING_AMOUNT;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.TOKENS_PER_ACCOUNT_LIMIT_EXCEEDED;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.TOKEN_ALREADY_ASSOCIATED_TO_ACCOUNT;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.TOKEN_HAS_NO_FREEZE_KEY;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.TOKEN_HAS_NO_KYC_KEY;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.TOKEN_HAS_NO_SUPPLY_KEY;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.TOKEN_HAS_NO_WIPE_KEY;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.TOKEN_IS_IMMUTABLE;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.TOKEN_NOT_ASSOCIATED_TO_ACCOUNT;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.TOKEN_WAS_DELETED;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.TRANSACTION_REQUIRES_ZERO_TOKEN_BALANCES;

/**
 * Provides a managing store for arbitrary tokens.
 *
 * @author AmilyTech
 */
public class grameTokenStore extends grameStore implements TokenStore {
	private static final Logger log = LogManager.getLogger(grameTokenStore.class);

	static final TokenID NO_PENDING_ID = TokenID.getDefaultInstance();

	static Predicate<Key> REMOVES_ADMIN_KEY = ImmutableKeyUtils::signalsKeyRemoval;

	private final OptionValidator validator;
	private final GlobalDynamicProperties properties;
	private final Supplier<FCMap<MerkleEntityId, MerkleToken>> tokens;
	private final TransactionalLedger<
			Pair<AccountID, TokenID>,
			TokenRelProperty,
			MerkleTokenRelStatus> tokenRelsLedger;
	Map<AccountID, Set<TokenID>> knownTreasuries = new HashMap<>();

	TokenID pendingId = NO_PENDING_ID;
	MerkleToken pendingCreation;

	public grameTokenStore(
			EntityIdSource ids,
			OptionValidator validator,
			GlobalDynamicProperties properties,
			Supplier<FCMap<MerkleEntityId, MerkleToken>> tokens,
			TransactionalLedger<Pair<AccountID, TokenID>, TokenRelProperty, MerkleTokenRelStatus> tokenRelsLedger
	) {
		super(ids);
		this.tokens = tokens;
		this.validator = validator;
		this.properties = properties;
		this.tokenRelsLedger = tokenRelsLedger;
		rebuildViewOfKnownTreasuries();
	}

	@Override
	public void rebuildViews() {
		knownTreasuries.clear();
		rebuildViewOfKnownTreasuries();
	}

	private void rebuildViewOfKnownTreasuries() {
		tokens.get().forEach((key, value) ->
				addKnownTreasury(value.treasury().toGrpcAccountId(), key.toTokenId()));
	}

	@Override
	public boolean isCreationPending() {
		return pendingId != NO_PENDING_ID;
	}

	@Override
	public void setgrameLedger(grameLedger grameLedger) {
		grameLedger.setTokenRelsLedger(tokenRelsLedger);
		super.setgrameLedger(grameLedger);
	}

	@Override
	public ResponseCodeEnum associate(AccountID aId, List<TokenID> tokens) {
		return fullySanityChecked(true, aId, tokens, (account, tokenIds) -> {
			var accountTokens = grameLedger.getAssociatedTokens(aId);
			for (TokenID id : tokenIds) {
				if (accountTokens.includes(id)) {
					return TOKEN_ALREADY_ASSOCIATED_TO_ACCOUNT;
				}
			}
			var validity = OK;
			if ((accountTokens.numAssociations() + tokenIds.size()) > properties.maxTokensPerAccount()) {
				validity = TOKENS_PER_ACCOUNT_LIMIT_EXCEEDED;
			} else {
				accountTokens.associateAll(new HashSet<>(tokenIds));
				for (TokenID id : tokenIds) {
					var relationship = asTokenRel(aId, id);
					tokenRelsLedger.create(relationship);
					var token = get(id);
					tokenRelsLedger.set(
							relationship,
							TokenRelProperty.IS_FROZEN,
							token.hasFreezeKey() && token.accountsAreFrozenByDefault());
					tokenRelsLedger.set(
							relationship,
							TokenRelProperty.IS_KYC_GRANTED,
							!token.hasKycKey());
				}
			}
			grameLedger.setAssociatedTokens(aId, accountTokens);
			return validity;
		});
	}

	@Override
	public ResponseCodeEnum dissociate(AccountID aId, List<TokenID> targetTokens) {
		return fullySanityChecked(false, aId, targetTokens, (account, tokenIds) -> {
			var accountTokens = grameLedger.getAssociatedTokens(aId);
			for (TokenID tId : tokenIds) {
				if (!accountTokens.includes(tId)) {
					return TOKEN_NOT_ASSOCIATED_TO_ACCOUNT;
				}
				if (!tokens.get().containsKey(fromTokenId(tId))) {
					/* Expired tokens that have been removed from state (either because they
					were also deleted, or their grace period ended) should be dissociated
					with no additional checks. */
					continue;
				}
				var token = get(tId);
				var isTokenDeleted = token.isDeleted();
				/* Once a token is deleted, this always returns false. */
				if (isTreasuryForToken(aId, tId)) {
					return ACCOUNT_IS_TREASURY;
				}
				var relationship = asTokenRel(aId, tId);
				if (!isTokenDeleted && (boolean) tokenRelsLedger.get(relationship, IS_FROZEN)) {
					return ACCOUNT_FROZEN_FOR_TOKEN;
				}
				long balance = (long) tokenRelsLedger.get(relationship, TOKEN_BALANCE);
				if (balance > 0) {
					var expiry = Timestamp.newBuilder().setSeconds(token.expiry()).build();
					var isTokenExpired = !validator.isValidExpiry(expiry);
					if (!isTokenDeleted && !isTokenExpired) {
						return TRANSACTION_REQUIRES_ZERO_TOKEN_BALANCES;
					}
					if (!isTokenDeleted) {
						/* Must be expired; return balance to treasury account. */
						grameLedger.doTokenTransfer(tId, aId, token.treasury().toGrpcAccountId(), balance);
					}
				}
			}
			accountTokens.dissociateAll(new HashSet<>(tokenIds));
			tokenIds.forEach(id -> tokenRelsLedger.destroy(asTokenRel(aId, id)));
			grameLedger.setAssociatedTokens(aId, accountTokens);
			return OK;
		});
	}

	@Override
	public boolean associationExists(AccountID aId, TokenID tId) {
		return checkExistence(aId, tId) == OK && tokenRelsLedger.exists(asTokenRel(aId, tId));
	}

	@Override
	public boolean exists(TokenID id) {
		return (isCreationPending() && pendingId.equals(id)) || tokens.get().containsKey(fromTokenId(id));
	}

	@Override
	public MerkleToken get(TokenID id) {
		throwIfMissing(id);

		return pendingId.equals(id) ? pendingCreation : tokens.get().get(fromTokenId(id));
	}

	@Override
	public void apply(TokenID id, Consumer<MerkleToken> change) {
		throwIfMissing(id);

		var key = fromTokenId(id);
		var token = tokens.get().getForModify(key);
		try {
			change.accept(token);
		} catch (Exception internal) {
			throw new IllegalArgumentException("Token change failed unexpectedly!", internal);
		} finally {
			tokens.get().replace(key, token);
		}
	}

	@Override
	public ResponseCodeEnum grantKyc(AccountID aId, TokenID tId) {
		return setHasKyc(aId, tId, true);
	}

	@Override
	public ResponseCodeEnum revokeKyc(AccountID aId, TokenID tId) {
		return setHasKyc(aId, tId, false);
	}

	@Override
	public ResponseCodeEnum unfreeze(AccountID aId, TokenID tId) {
		return setIsFrozen(aId, tId, false);
	}

	@Override
	public ResponseCodeEnum freeze(AccountID aId, TokenID tId) {
		return setIsFrozen(aId, tId, true);
	}

	private ResponseCodeEnum setHasKyc(
			AccountID aId,
			TokenID tId,
			boolean value
	) {
		return manageFlag(
				aId,
				tId,
				value,
				TOKEN_HAS_NO_KYC_KEY,
				TokenRelProperty.IS_KYC_GRANTED,
				MerkleToken::kycKey);
	}

	private ResponseCodeEnum setIsFrozen(
			AccountID aId,
			TokenID tId,
			boolean value
	) {
		return manageFlag(
				aId,
				tId,
				value,
				TOKEN_HAS_NO_FREEZE_KEY,
				TokenRelProperty.IS_FROZEN,
				MerkleToken::freezeKey);
	}

	@Override
	public ResponseCodeEnum adjustBalance(AccountID aId, TokenID tId, long adjustment) {
		return sanityChecked(aId, tId, token -> tryAdjustment(aId, tId, adjustment));
	}

	@Override
	public ResponseCodeEnum wipe(AccountID aId, TokenID tId, long amount, boolean skipKeyCheck) {
		return sanityChecked(aId, tId, token -> {
			if (!skipKeyCheck && !token.hasWipeKey()) {
				return TOKEN_HAS_NO_WIPE_KEY;
			}
			if (ofNullableAccountId(aId).equals(token.treasury())) {
				return CANNOT_WIPE_TOKEN_TREASURY_ACCOUNT;
			}

			var relationship = asTokenRel(aId, tId);
			long balance = (long) tokenRelsLedger.get(relationship, TOKEN_BALANCE);
			if (amount > balance) {
				return INVALID_WIPING_AMOUNT;
			}
			tokenRelsLedger.set(relationship, TOKEN_BALANCE, balance - amount);
			grameLedger.updateTokenXfers(tId, aId, -amount);
			apply(tId, t -> t.adjustTotalSupplyBy(-amount));

			return OK;
		});
	}

	@Override
	public ResponseCodeEnum burn(TokenID tId, long amount) {
		return changeSupply(tId, amount, -1, INVALID_TOKEN_BURN_AMOUNT);
	}

	@Override
	public ResponseCodeEnum mint(TokenID tId, long amount) {
		return changeSupply(tId, amount, +1, INVALID_TOKEN_MINT_AMOUNT);
	}

	private ResponseCodeEnum changeSupply(
			TokenID tId,
			long amount,
			long sign,
			ResponseCodeEnum failure
	) {
		return tokenSanityCheck(tId, token -> {
			if (!token.hasSupplyKey()) {
				return TOKEN_HAS_NO_SUPPLY_KEY;
			}

			var change = sign * amount;
			var toBeUpdatedTotalSupply = token.totalSupply() + change;
			if (toBeUpdatedTotalSupply < 0) {
				return failure;
			}

			var aId = token.treasury().toGrpcAccountId();
			var validity = tryAdjustment(aId, tId, change);
			if (validity != OK) {
				return validity;
			}

			apply(tId, t -> t.adjustTotalSupplyBy(change));

			return OK;
		});
	}

	@Override
	public CreationResult<TokenID> createProvisionally(
			TokenCreateTransactionBody request,
			AccountID sponsor,
			long now
	) {
		var validity = accountCheck(request.getTreasury(), INVALID_TREASURY_ACCOUNT_FOR_TOKEN);
		if (validity != OK) {
			return failure(validity);
		}
		if (request.hasAutoRenewAccount()) {
			validity = accountCheck(request.getAutoRenewAccount(), INVALID_AUTORENEW_ACCOUNT);
			if (validity != OK) {
				return failure(validity);
			}
		}

		var freezeKey = asUsableFcKey(request.getFreezeKey());
		var adminKey = asUsableFcKey(request.getAdminKey());
		var kycKey = asUsableFcKey(request.getKycKey());
		var wipeKey = asUsableFcKey(request.getWipeKey());
		var supplyKey = asUsableFcKey(request.getSupplyKey());

		var expiry = expiryOf(request, now);
		pendingId = ids.newTokenId(sponsor);
		pendingCreation = new MerkleToken(
				expiry,
				request.getInitialSupply(),
				request.getDecimals(),
				request.getSymbol(),
				request.getName(),
				request.getFreezeDefault(),
				kycKey.isEmpty(),
				ofNullableAccountId(request.getTreasury()));
		pendingCreation.setMemo(request.getMemo());
		adminKey.ifPresent(pendingCreation::setAdminKey);
		kycKey.ifPresent(pendingCreation::setKycKey);
		wipeKey.ifPresent(pendingCreation::setWipeKey);
		freezeKey.ifPresent(pendingCreation::setFreezeKey);
		supplyKey.ifPresent(pendingCreation::setSupplyKey);
		if (request.hasAutoRenewAccount()) {
			pendingCreation.setAutoRenewAccount(ofNullableAccountId(request.getAutoRenewAccount()));
			pendingCreation.setAutoRenewPeriod(request.getAutoRenewPeriod().getSeconds());
		}

		return success(pendingId);
	}


	public void addKnownTreasury(AccountID aId, TokenID tId) {
		knownTreasuries.computeIfAbsent(aId, ignore -> new HashSet<>()).add(tId);
	}

	public void removeKnownTreasuryForToken(AccountID aId, TokenID tId) {
		throwIfKnownTreasuryIsMissing(aId);
		knownTreasuries.get(aId).remove(tId);
		if (knownTreasuries.get(aId).isEmpty()) {
			knownTreasuries.remove(aId);
		}
	}

	private void throwIfKnownTreasuryIsMissing(AccountID aId) {
		if (!knownTreasuries.containsKey(aId)) {
			throw new IllegalArgumentException(String.format(
					"Argument 'aId=%s' does not refer to a known treasury!",
					readableId(aId)));
		}
	}

	private ResponseCodeEnum tryAdjustment(AccountID aId, TokenID tId, long adjustment) {
		var relationship = asTokenRel(aId, tId);
		if ((boolean) tokenRelsLedger.get(relationship, IS_FROZEN)) {
			return ACCOUNT_FROZEN_FOR_TOKEN;
		}
		if (!(boolean) tokenRelsLedger.get(relationship, IS_KYC_GRANTED)) {
			return ACCOUNT_KYC_NOT_GRANTED_FOR_TOKEN;
		}
		long balance = (long) tokenRelsLedger.get(relationship, TOKEN_BALANCE);
		long newBalance = balance + adjustment;
		if (newBalance < 0) {
			return INSUFFICIENT_TOKEN_BALANCE;
		}
		tokenRelsLedger.set(relationship, TOKEN_BALANCE, newBalance);
		grameLedger.updateTokenXfers(tId, aId, adjustment);
		return OK;
	}

	private boolean isValidAutoRenewPeriod(long secs) {
		return validator.isValidAutoRenewPeriod(Duration.newBuilder().setSeconds(secs).build());
	}

	private long expiryOf(TokenCreateTransactionBody request, long now) {
		return request.hasAutoRenewAccount()
				? now + request.getAutoRenewPeriod().getSeconds()
				: request.getExpiry().getSeconds();
	}

	@Override
	public void commitCreation() {
		throwIfNoCreationPending();

		tokens.get().put(fromTokenId(pendingId), pendingCreation);
		addKnownTreasury(pendingCreation.treasury().toGrpcAccountId(), pendingId);

		resetPendingCreation();
	}

	@Override
	public void rollbackCreation() {
		throwIfNoCreationPending();

		ids.reclaimLastId();
		resetPendingCreation();
	}

	@Override
	public ResponseCodeEnum delete(TokenID tId) {
		var outcome = TokenStore.super.delete(tId);
		if (outcome != OK) {
			return outcome;
		}

		var treasury = tokens.get().get(fromTokenId(tId)).treasury().toGrpcAccountId();
		var tokensServed = knownTreasuries.get(treasury);
		tokensServed.remove(tId);
		if (tokensServed.isEmpty()) {
			knownTreasuries.remove(treasury);
		}
		return OK;
	}

	@Override
	public ResponseCodeEnum update(TokenUpdateTransactionBody changes, long now) {
		var tId = resolve(changes.getToken());
		if (tId == MISSING_TOKEN) {
			return INVALID_TOKEN_ID;
		}
		var validity = OK;
		var isExpiryOnly = affectsExpiryAtMost(changes);
		var hasNewSymbol = changes.getSymbol().length() > 0;
		var hasNewTokenName = changes.getName().length() > 0;
		var hasAutoRenewAccount = changes.hasAutoRenewAccount();
		if (hasAutoRenewAccount) {
			validity = accountCheck(changes.getAutoRenewAccount(), INVALID_AUTORENEW_ACCOUNT);
			if (validity != OK) {
				return validity;
			}
		}

		Optional<JKey> newKycKey = changes.hasKycKey() ? asUsableFcKey(changes.getKycKey()) : Optional.empty();
		Optional<JKey> newWipeKey = changes.hasWipeKey() ? asUsableFcKey(changes.getWipeKey()) : Optional.empty();
		Optional<JKey> newSupplyKey = changes.hasSupplyKey() ? asUsableFcKey(changes.getSupplyKey()) : Optional.empty();
		Optional<JKey> newFreezeKey = changes.hasFreezeKey() ? asUsableFcKey(changes.getFreezeKey()) : Optional.empty();

		var appliedValidity = new AtomicReference<>(OK);
		apply(tId, token -> {
			var candidateExpiry = changes.getExpiry().getSeconds();
			if (candidateExpiry != 0 && candidateExpiry < token.expiry()) {
				appliedValidity.set(INVALID_EXPIRATION_TIME);
			}
			if (hasAutoRenewAccount || token.hasAutoRenewAccount()) {
				long changedAutoRenewPeriod = changes.getAutoRenewPeriod().getSeconds();
				if ((changedAutoRenewPeriod != 0 || !token.hasAutoRenewAccount()) &&
						!isValidAutoRenewPeriod(changedAutoRenewPeriod)) {
					appliedValidity.set(INVALID_RENEWAL_PERIOD);
				}
			}
			if (!token.hasKycKey() && newKycKey.isPresent()) {
				appliedValidity.set(TOKEN_HAS_NO_KYC_KEY);
			}
			if (!token.hasFreezeKey() && newFreezeKey.isPresent()) {
				appliedValidity.set(TOKEN_HAS_NO_FREEZE_KEY);
			}
			if (!token.hasWipeKey() && newWipeKey.isPresent()) {
				appliedValidity.set(TOKEN_HAS_NO_WIPE_KEY);
			}
			if (!token.hasSupplyKey() && newSupplyKey.isPresent()) {
				appliedValidity.set(TOKEN_HAS_NO_SUPPLY_KEY);
			}
			if (!token.hasAdminKey() && !isExpiryOnly) {
				appliedValidity.set(TOKEN_IS_IMMUTABLE);
			}
			if (OK != appliedValidity.get()) {
				return;
			}
			if (changes.hasAdminKey()) {
				var newAdminKey = changes.getAdminKey();
				if (REMOVES_ADMIN_KEY.test(newAdminKey)) {
					token.setAdminKey(UNUSED_KEY);
				} else {
					token.setAdminKey(asFcKeyUnchecked(changes.getAdminKey()));
				}
			}
			if (changes.hasAutoRenewAccount()) {
				token.setAutoRenewAccount(ofNullableAccountId(changes.getAutoRenewAccount()));
			}
			if (token.hasAutoRenewAccount()) {
				long changedAutoRenewPeriod = changes.getAutoRenewPeriod().getSeconds();
				if (changedAutoRenewPeriod > 0) {
					token.setAutoRenewPeriod(changedAutoRenewPeriod);
				}
			}
			if (changes.hasFreezeKey()) {
				token.setFreezeKey(asFcKeyUnchecked(changes.getFreezeKey()));
			}
			if (changes.hasKycKey()) {
				token.setKycKey(asFcKeyUnchecked(changes.getKycKey()));
			}
			if (changes.hasSupplyKey()) {
				token.setSupplyKey(asFcKeyUnchecked(changes.getSupplyKey()));
			}
			if (changes.hasWipeKey()) {
				token.setWipeKey(asFcKeyUnchecked(changes.getWipeKey()));
			}
			if (hasNewSymbol) {
				var newSymbol = changes.getSymbol();
				token.setSymbol(newSymbol);
			}
			if (hasNewTokenName) {
				var newName = changes.getName();
				token.setName(newName);
			}
			if (changes.hasTreasury() && !changes.getTreasury().equals(token.treasury().toGrpcAccountId())) {
				var treasuryId = ofNullableAccountId(changes.getTreasury());
				removeKnownTreasuryForToken(token.treasury().toGrpcAccountId(), tId);
				token.setTreasury(treasuryId);
				addKnownTreasury(changes.getTreasury(), tId);
			}
			if (changes.hasMemo()) {
				token.setMemo(changes.getMemo().getValue());
			}
			var expiry = changes.getExpiry().getSeconds();
			if (expiry != 0) {
				token.setExpiry(expiry);
			}
		});
		return appliedValidity.get();
	}

	public static boolean affectsExpiryAtMost(TokenUpdateTransactionBody op) {
		return !op.hasAdminKey() &&
				!op.hasKycKey() &&
				!op.hasWipeKey() &&
				!op.hasFreezeKey() &&
				!op.hasSupplyKey() &&
				!op.hasTreasury() &&
				!op.hasAutoRenewAccount() &&
				op.getSymbol().length() == 0 &&
				op.getName().length() == 0 &&
				op.getAutoRenewPeriod().getSeconds() == 0;
	}

	private ResponseCodeEnum fullySanityChecked(
			boolean strictTokenCheck,
			AccountID aId,
			List<TokenID> tokens,
			BiFunction<AccountID, List<TokenID>, ResponseCodeEnum> action
	) {
		var validity = checkAccountExistence(aId);
		if (validity != OK) {
			return validity;
		}
		if (strictTokenCheck) {
			for (TokenID tID : tokens) {
				var id = resolve(tID);
				if (id == MISSING_TOKEN) {
					return INVALID_TOKEN_ID;
				}
				var token = get(id);
				if (token.isDeleted()) {
					return TOKEN_WAS_DELETED;
				}
			}
		}
		return action.apply(aId, tokens);
	}

	private void resetPendingCreation() {
		pendingId = NO_PENDING_ID;
		pendingCreation = null;
	}

	private void throwIfNoCreationPending() {
		if (pendingId == NO_PENDING_ID) {
			throw new IllegalStateException("No pending token creation!");
		}
	}

	private void throwIfMissing(TokenID id) {
		if (!exists(id)) {
			throw new IllegalArgumentException(String.format(
					"Argument 'id=%s' does not refer to a known token!",
					readableId(id)));
		}
	}

	public boolean isKnownTreasury(AccountID aid) {
		return knownTreasuries.containsKey(aid);
	}

	@Override
	public boolean isTreasuryForToken(AccountID aId, TokenID tId) {
		if (!knownTreasuries.containsKey(aId)) {
			return false;
		}
		return knownTreasuries.get(aId).contains(tId);
	}

	private ResponseCodeEnum manageFlag(
			AccountID aId,
			TokenID tId,
			boolean value,
			ResponseCodeEnum keyFailure,
			TokenRelProperty flagProperty,
			Function<MerkleToken, Optional<JKey>> controlKeyFn
	) {
		return sanityChecked(aId, tId, token -> {
			if (controlKeyFn.apply(token).isEmpty()) {
				return keyFailure;
			}
			var relationship = asTokenRel(aId, tId);
			tokenRelsLedger.set(relationship, flagProperty, value);
			return OK;
		});
	}

	private ResponseCodeEnum sanityChecked(
			AccountID aId,
			TokenID tId,
			Function<MerkleToken, ResponseCodeEnum> action
	) {
		var validity = checkExistence(aId, tId);
		if (validity != OK) {
			return validity;
		}

		var token = get(tId);
		if (token.isDeleted()) {
			return TOKEN_WAS_DELETED;
		}

		var key = asTokenRel(aId, tId);
		if (!tokenRelsLedger.exists(key)) {
			return TOKEN_NOT_ASSOCIATED_TO_ACCOUNT;
		}

		return action.apply(token);
	}

	private ResponseCodeEnum tokenSanityCheck(
			TokenID tId,
			Function<MerkleToken, ResponseCodeEnum> action
	) {
		var validity = exists(tId) ? OK : INVALID_TOKEN_ID;
		if (validity != OK) {
			return validity;
		}

		var token = get(tId);
		if (token.isDeleted()) {
			return TOKEN_WAS_DELETED;
		}

		return action.apply(token);
	}

	private ResponseCodeEnum checkExistence(AccountID aId, TokenID tId) {
		var validity = checkAccountExistence(aId);
		if (validity != OK) {
			return validity;
		}
		return exists(tId) ? OK : INVALID_TOKEN_ID;
	}

	Map<AccountID, Set<TokenID>> getKnownTreasuries() {
		return knownTreasuries;
	}
}
