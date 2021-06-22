package com.grame.services.contracts.sources;

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
import com.grame.services.ledger.accounts.grameAccountCustomizer;
import com.grame.services.state.submerkle.EntityId;
import com.grame.services.legacy.core.jproto.JContractIDKey;
import com.gramegrame.api.proto.java.AccountID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.core.AccountState;
import org.ethereum.datasource.Source;
import org.ethereum.util.ALock;

import java.math.BigInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.grame.services.utils.EntityIdUtils.accountParsedFromSolidityAddress;
import static com.grame.services.utils.EntityIdUtils.asContract;
import static com.grame.services.utils.EntityIdUtils.asLiteralString;

public class LedgerAccountsSource implements Source<byte[], AccountState> {
	static Logger log = LogManager.getLogger(LedgerAccountsSource.class);

	private final grameLedger ledger;
	private final GlobalDynamicProperties properties;
	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private final ALock rLock = new ALock(rwLock.readLock());
	private final ALock wLock = new ALock(rwLock.writeLock());

	public LedgerAccountsSource(grameLedger ledger, GlobalDynamicProperties properties) {
		this.ledger = ledger;
		this.properties = properties;
	}

	@Override
	public void delete(byte[] key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AccountState get(byte[] key) {
		try (ALock ignored = rLock.lock()) {
			var id = accountParsedFromSolidityAddress(key);
			if (!ledger.exists(id)) {
				return null;
			}

			var grameAccount = ledger.get(id);
			var evmState = new AccountState(
					BigInteger.ZERO,
					BigInteger.valueOf(grameAccount.getBalance()));

			evmState.setShardId(id.getShardNum());
			evmState.setRealmId(id.getRealmNum());
			evmState.setAccountNum(id.getAccountNum());
			evmState.setAutoRenewPeriod(grameAccount.getAutoRenewSecs());
			if (grameAccount.getProxy() != null) {
				var proxy = grameAccount.getProxy();
				evmState.setProxyAccountShard(proxy.shard());
				evmState.setProxyAccountRealm(proxy.realm());
				evmState.setProxyAccountNum(proxy.num());
			}
			evmState.setReceiverSigRequired(grameAccount.isReceiverSigRequired());
			evmState.setDeleted(grameAccount.isDeleted());
			evmState.setExpirationTime(grameAccount.getExpiry());
			evmState.setSmartContract(grameAccount.isSmartContract());

			return evmState;
		}
	}

	@Override
	public void put(byte[] key, AccountState evmState) {
		var id = accountParsedFromSolidityAddress(key);

		if (evmState == null) {
			String id_str = asLiteralString(id);
			log.warn("Ignoring null state put to account {}!", id_str);
			return;
		}

		try (ALock ignored = wLock.lock()) {
			if (ledger.exists(id)) {
				updateForEvm(id, evmState);
			} else {
				createForEvm(id, evmState);
			}
		}
	}

	private void updateForEvm(AccountID id, AccountState evmState) {
		long oldBalance = ledger.getBalance(id);
		long newBalance = evmState.getBalance().longValue();
		long adjustment = newBalance - oldBalance;

		ledger.adjustBalance(id, adjustment);
		grameAccountCustomizer customizer = new grameAccountCustomizer()
				.expiry(evmState.getExpirationTime())
				.isDeleted(evmState.isDeleted());
		ledger.customize(id, customizer);
	}

	private void createForEvm(AccountID id, AccountState evmState) {
		var proxy = new EntityId(
				evmState.getProxyAccountShard(),
				evmState.getProxyAccountRealm(),
				evmState.getProxyAccountNum());
		var key = new JContractIDKey(asContract(id));
		grameAccountCustomizer customizer = new grameAccountCustomizer()
				.key(key)
				.memo("")
				.proxy(proxy)
				.expiry(evmState.getExpirationTime())
				.autoRenewPeriod(evmState.getAutoRenewPeriod())
				.isSmartContract(true);

		long balance = evmState.getBalance().longValue();
		ledger.spawn(id, balance, customizer);
	}

	@Override
	public boolean flush() {
		return false;
	}
}
