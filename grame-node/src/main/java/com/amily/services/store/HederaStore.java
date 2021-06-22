package com.grame.services.store;

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

import com.grame.services.ledger.grameLedger;
import com.grame.services.ledger.TransactionalLedger;
import com.grame.services.ledger.ids.EntityIdSource;
import com.grame.services.ledger.properties.AccountProperty;
import com.grame.services.state.merkle.MerkleAccount;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.ResponseCodeEnum;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.ACCOUNT_DELETED;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_ACCOUNT_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;

/**
 * Provides an abstract store, having common functionality related to {@link EntityIdSource}, {@link grameLedger}
 * and {@link TransactionalLedger} for accounts.
 *
 */
public abstract class grameStore {
    protected final EntityIdSource ids;

    protected grameLedger grameLedger;
    protected TransactionalLedger<AccountID, AccountProperty, MerkleAccount> accountsLedger;

    protected grameStore(
            EntityIdSource ids
    ) {
        this.ids = ids;
    }

    public void setAccountsLedger(TransactionalLedger<AccountID, AccountProperty, MerkleAccount> accountsLedger) {
        this.accountsLedger = accountsLedger;
    }

    public void setgrameLedger(grameLedger grameLedger) {
        this.grameLedger = grameLedger;
    }

    public void rollbackCreation() {
        ids.reclaimLastId();
    }

    protected ResponseCodeEnum accountCheck(AccountID id, ResponseCodeEnum failure) {
        if (!accountsLedger.exists(id) || (boolean) accountsLedger.get(id, AccountProperty.IS_DELETED)) {
            return failure;
        }
        return OK;
    }

    protected ResponseCodeEnum checkAccountExistence(AccountID aId) {
        return accountsLedger.exists(aId)
                ? (grameLedger.isDeleted(aId) ? ACCOUNT_DELETED : OK)
                : INVALID_ACCOUNT_ID;
    }
}
