package com.grame.services.legacy.unit.handler;

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

import com.grame.services.legacy.unit.GlobalFlag;
import com.gramegrame.api.proto.java.AccountAmount;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.LiveHash;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionID;
import com.gramegrame.api.proto.java.TransactionReceipt;
import com.gramegrame.api.proto.java.TransactionRecord;
import com.gramegrame.api.proto.java.TransferList;
import com.gramegrame.builder.RequestBuilder;
import com.grame.services.legacy.exception.InvalidAccountIDException;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.exceptions.NegativeAccountBalanceException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import com.swirlds.fcmap.FCMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.grame.services.legacy.unit.handler.AccountOperations.deletingAccountFrom;
import static com.grame.services.legacy.unit.handler.AccountOperations.recordOf;
import static java.util.stream.Collectors.joining;

public class CryptoHandlerTestHelper extends CryptoHandler {
	public static final String INSUFFICIENT_BAL = "INSUFFICIENT_BALANCE";
	public static final String INSUFFICIENT_FEE = "INSUFFICIENT_FEE";
	public static final String PAYER_ACCOUNT_NOT_FOUND = "PAYER_ACCOUNT_NOT_FOUND";
	public static final String BAD_ENCODING = "BAD_ENCODING";
	public static final String KEY_REQUIRED = "KEY_REQUIRED";
	public static final String INSUFFICIENT_PAYER_BALANCE = "INSUFFICIENT_PAYER_BALANCE";
	public static final String INSUFFICIENT_ACCOUNT_BALANCE = "INSUFFICIENT_ACCOUNT_BALANCE";
	public static final String ACCOUNT_ETHADDRESS_NOT_FOUND = "ACCOUNT_ETHADDRESS_NOT_FOUND";
	public static final String ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
	private static final Logger log = LogManager.getLogger(CryptoHandlerTestHelper.class);
	private FCMap<MerkleEntityId, MerkleAccount> map;
	private GlobalFlag globalFlag;

	public CryptoHandlerTestHelper(FCMap<MerkleEntityId, MerkleAccount> map) {
		this.map = map;
		this.globalFlag = GlobalFlag.getInstance();
	}

	/**
	 * This util translates application constant to ResponseCodeEnum
	 */
	public static ResponseCodeEnum translateResponseCode(Exception ex) {
	  if (ex == null) {
		return ResponseCodeEnum.FAIL_INVALID;
	  }

	  if (ex.getMessage() == null) {
		return ResponseCodeEnum.FAIL_INVALID;
	  }

	  switch (ex.getMessage()) {
		case (INSUFFICIENT_PAYER_BALANCE):
		  return ResponseCodeEnum.INSUFFICIENT_PAYER_BALANCE;

		case (INSUFFICIENT_BAL):
		  return ResponseCodeEnum.INSUFFICIENT_ACCOUNT_BALANCE;

		case (BAD_ENCODING):
		  return ResponseCodeEnum.BAD_ENCODING;

		case (KEY_REQUIRED):
		  return ResponseCodeEnum.KEY_REQUIRED;

		case (INSUFFICIENT_FEE):
		  return ResponseCodeEnum.INSUFFICIENT_TX_FEE;

		case (PAYER_ACCOUNT_NOT_FOUND):
		  return ResponseCodeEnum.PAYER_ACCOUNT_NOT_FOUND;

		case (INSUFFICIENT_ACCOUNT_BALANCE):
		  return ResponseCodeEnum.INSUFFICIENT_ACCOUNT_BALANCE;

		case (ACCOUNT_ETHADDRESS_NOT_FOUND):
		  return ResponseCodeEnum.INVALID_SOLIDITY_ADDRESS;

		case (ACCOUNT_NOT_FOUND):
		  return ResponseCodeEnum.INVALID_ACCOUNT_ID;

		default:
		  return ResponseCodeEnum.FAIL_INVALID;
	  }
	}

	/**
	 * Transfer some accounts to many other accounts. The accounts list can contain up to 10 accounts.
	 * The amounts list must be the same length as the accounts list. Each negative amount is
	 * withdrawn from the corresponding account (a sender), Each positive one is added to the
	 * corresponding account (a receiver).
	 *
	 * @return TransactionRecord
	 */
	public TransactionRecord cryptoTransfer(TransactionBody cryptoTransferTx, Instant consensusTime) {
		log.debug("In CryptoHandler :: cryptoTransfer()");
		TransactionReceipt receipt;
		long txFee = cryptoTransferTx.getTransactionFee();
		TransferList transferList = cryptoTransferTx.getCryptoTransfer().getTransfers();
		TransferList.Builder completedTransfers = TransferList.newBuilder();
		try {
			List<AccountAmount> accountAmounts = transferList.getAccountAmountsList();
			log.debug("Retrieved  accountAmounts size " + accountAmounts.size());
			AccountID acctId;
			boolean isSmartContractAccount = false; // variable to check whether Crypto Account or not
			boolean isAccountSetForDeletion = false; // variable to check whether Crypto Account is set for delete or
			// not
			// check account has sufficient balance
			for (AccountAmount actAmt : accountAmounts) {
				acctId = actAmt.getAccountID();
				if (!validateAccountID(acctId)) {
					isSmartContractAccount = true;
					break;
				}
				if (isAccountSetForDelete(acctId)) {
					isAccountSetForDeletion = true;
					break;
				}
				MerkleEntityId merkleEntityId = MerkleEntityId.fromAccountId(acctId);
				MerkleAccount mapValue = map.get(merkleEntityId);
				if (mapValue == null) {
					if (log.isDebugEnabled()) {
						log.debug("@@ Null values detected: actAmt=" + actAmt + "; mapValue=" + mapValue + "; "
								+ " mapKey :: " + merkleEntityId.toString() + "cryptoTransferTx=" + cryptoTransferTx);
					}
					throw new InvalidTransactionException(ACCOUNT_NOT_FOUND);
				}
				if (actAmt.getAmount() < 0) {
					if (log.isDebugEnabled()) {
						log.debug(
								"Map Value of Account ID :: " + acctId.getAccountNum() + "  is :: " + mapValue);
					}
					long balance = mapValue.getBalance();
					if (balance < -actAmt.getAmount() || actAmt.getAmount() == Long.MIN_VALUE) {
						if (log.isDebugEnabled()) {
							log.debug("Insufficient balance in account ID  :: " + acctId.getAccountNum());
						}
						throw new InsufficientBalanceException(INSUFFICIENT_BAL);
					}
				}
			}
			if (isSmartContractAccount) {
				receipt = RequestBuilder.getTransactionReceipt(
						ResponseCodeEnum.INVALID_ACCOUNT_ID, globalFlag.getExchangeRateSet());
			} else if (isAccountSetForDeletion) {
				receipt = RequestBuilder.getTransactionReceipt(
						ResponseCodeEnum.ACCOUNT_DELETED, globalFlag.getExchangeRateSet());
			} else {
				for (AccountAmount actAmt : accountAmounts) {
					acctId = actAmt.getAccountID();
					MerkleEntityId merkleEntityId = MerkleEntityId.fromAccountId(acctId);
					MerkleAccount mapValue = map.getForModify(merkleEntityId);
					if (log.isDebugEnabled()) {
						log.debug(
								"Map Value of Account ID :: " + acctId.getAccountNum() + "  is :: " + mapValue);
						log.debug("Going to add Amount for Account Id " + acctId.getAccountNum() + "  amount "
								+ actAmt.getAmount());
					}
					mapValue.setBalance(Math.addExact(mapValue.getBalance(), actAmt.getAmount()));
					map.replace(merkleEntityId, mapValue);
					completedTransfers.addAccountAmounts(actAmt);
				}
				receipt = RequestBuilder.getTransactionReceipt(
						ResponseCodeEnum.SUCCESS, globalFlag.getExchangeRateSet());
			}

		} catch (InvalidTransactionException te) {
			if (log.isDebugEnabled()) {
				log.debug("InsufficientBalanceException: cryptoTransferTx=" + cryptoTransferTx, te);
			}
			receipt = RequestBuilder.getTransactionReceipt(
					translateResponseCode(te), globalFlag.getExchangeRateSet());
		} catch (InsufficientBalanceException be) {
			if (log.isDebugEnabled()) {
				log.debug("InsufficientBalanceException: cryptoTransferTx=" + cryptoTransferTx, be);
			}
			receipt = RequestBuilder.getTransactionReceipt(
					ResponseCodeEnum.INSUFFICIENT_ACCOUNT_BALANCE, globalFlag.getExchangeRateSet());
		} catch (NegativeAccountBalanceException nbe) {
			if (log.isDebugEnabled()) {
				log.debug("NegativeAccountBalanceException: cryptoTransferTx=" + cryptoTransferTx, nbe);
			}
			receipt = RequestBuilder.getTransactionReceipt(
					ResponseCodeEnum.SETTING_NEGATIVE_ACCOUNT_BALANCE, globalFlag.getExchangeRateSet());
		} catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.debug("Error occurred in Transfer: cryptoTransferTx=" + cryptoTransferTx, e);
			}
			receipt = RequestBuilder.getTransactionReceipt(
					ResponseCodeEnum.FAIL_INVALID, globalFlag.getExchangeRateSet());
		}
		TransactionRecord.Builder transactionRecord = RequestBuilder.getTransactionRecord(txFee,
				cryptoTransferTx.getMemo(), cryptoTransferTx.getTransactionID(),
				RequestBuilder.getTimestamp(consensusTime), receipt);
		transactionRecord.setTransferList(completedTransfers.build());
		return transactionRecord.build();
	}


	/**
	 * Adds a claim to the file system and returns the Transaction Record.
	 */
	public TransactionRecord cryptoAddLiveHash(TransactionBody cryptoAddLiveHashTx, Instant consensusTime) {

		log.debug("In CryptoHandler::cryptoAddLiveHash()");
		AccountID accountId = cryptoAddLiveHashTx.getTransactionID().getAccountID();
		TransactionID transactionID = cryptoAddLiveHashTx.getTransactionID();
		Instant startTime =
				RequestBuilder.convertProtoTimeStamp(transactionID.getTransactionValidStart());

		LiveHash claim = cryptoAddLiveHashTx.getCryptoAddLiveHash().getLiveHash();
		AccountID attachedAccountID = cryptoAddLiveHashTx.getCryptoAddLiveHash().getLiveHash().getAccountId();
		log.debug("The following claim :: " + claim + " needs to be attached with the account ID ::"
				+ attachedAccountID);
//    TimestampSeconds timestampSeconds = claim.getLiveHashExpiration();
		long durationSeconds = claim.getDuration().getSeconds();
		TransactionReceipt transactionReceipt = null;
		if (!validateAccountID(attachedAccountID)) {
			transactionReceipt = TransactionReceipt.newBuilder().setAccountID(accountId)
					.setStatus(ResponseCodeEnum.INVALID_ACCOUNT_ID).build();
		} else if (isAccountSetForDelete(attachedAccountID)) {
			transactionReceipt = TransactionReceipt.newBuilder().setAccountID(accountId)
					.setStatus(ResponseCodeEnum.ACCOUNT_DELETED).build();
		} else {
			try {
				transactionReceipt = TransactionReceipt.newBuilder().setAccountID(accountId)
						.setStatus(ResponseCodeEnum.SUCCESS).build();
			} catch (Exception e) {
				transactionReceipt = TransactionReceipt.newBuilder().setAccountID(accountId)
						.setStatus(ResponseCodeEnum.FAIL_INVALID).build();
			}
		}
		return TransactionRecord.newBuilder().setReceipt(transactionReceipt)
				.setMemo(cryptoAddLiveHashTx.getMemo())
				.setConsensusTimestamp(Timestamp.newBuilder().setSeconds(consensusTime.toEpochMilli()))
				.build();
	}


	public TransactionRecord cryptoDelete(TransactionBody txn, Instant consensusTime) {
		FCMap<MerkleEntityId, MerkleAccount> ledger = map;

		return recordOf(deletingAccountFrom(ledger).via(txn).at(consensusTime));
	}


	public boolean validateAccountID(AccountID accountID) {
		boolean isValid = false;
		MerkleEntityId merkleEntityId = MerkleEntityId.fromAccountId(accountID);
		if (map.containsKey(merkleEntityId)) {
			MerkleAccount mapValue = map.get(merkleEntityId);
			if (mapValue != null) {
				//not contract ID
				isValid = !mapValue.isSmartContract();
			}
		}
		return isValid;
	}

	/**
	 * The method checks whether an account is set for deletion or not
	 */
	public boolean isAccountSetForDelete(AccountID accountID) {
		MerkleEntityId accountKey = MerkleEntityId.fromAccountId(accountID);
		if (map.containsKey(accountKey)) {
			MerkleAccount accountValue = map.get(accountKey);
			return accountValue.isDeleted();
		}
		return false;
	}

}

class AccountOperations {

	public static TransactionRecord recordOf(Supplier<TransactionRecord> txn) {
		return txn.get();
	}

	public static AccountOperations.DeleteSpec.Txn deletingAccountFrom(FCMap<MerkleEntityId, MerkleAccount> ledger) {
		return txn -> consensusTime -> new AccountOperations.DeletionRecord(consensusTime, txn, ledger);
	}

	final static class DeletionRecord implements Supplier<TransactionRecord> {

		private static final Logger deletionLog = LogManager.getLogger(
				AccountOperations.DeletionRecord.class);

		Instant consensusTime;
		TransactionBody txn;
		FCMap<MerkleEntityId, MerkleAccount> ledger;
		AccountID target, payer, transfer;

		public DeletionRecord(Instant consensusTime, TransactionBody txn,
				FCMap<MerkleEntityId, MerkleAccount> ledger) {
			this.consensusTime = consensusTime;
			this.txn = txn;
			this.ledger = ledger;
		}

		@Override
		public TransactionRecord get() {
			TransactionRecord record;

			try {
				setAccountsFromTxn();
				long balance = balanceOf(target, ledger);
				TransferList transfers = asTransferList(hbarsFromTo(balance, target, transfer));
				if (deletionLog.isDebugEnabled()) {
					deletionLog.debug(printable(transfers));
				}
				doTransfers(transfers, ledger);
				markDeleted(target, ledger);
				record = ofSuccessUsing(transfers);
			} catch (Exception e) {
				deletionLog.debug("CryptoDelete failed for " + txn, e);
				record = ofFailureBecause(e);
			}

			return record;
		}

		private void setAccountsFromTxn() {
			payer = txn.getTransactionID().getAccountID();
			target = txn.getCryptoDelete().getDeleteAccountID();
			transfer = txn.getCryptoDelete().getTransferAccountID();
		}

		private TransactionRecord ofSuccessUsing(TransferList transfers) {
			TransactionRecord.Builder recordBuilder = builderWithReceiptStatus(ResponseCodeEnum.SUCCESS);
			recordBuilder.setTransferList(transfers);
			return recordBuilder.build();
		}

		private TransactionRecord ofFailureBecause(Exception e) {
			ResponseCodeEnum bestErrorStatus = ResponseCodeEnum.INVALID_TRANSACTION;
			if (e instanceof InvalidAccountIDException) {
				bestErrorStatus = ResponseCodeEnum.INVALID_ACCOUNT_ID;
			} else if (e instanceof NegativeAccountBalanceException) {
				bestErrorStatus = ResponseCodeEnum.SETTING_NEGATIVE_ACCOUNT_BALANCE;
			}
			return builderWithReceiptStatus(bestErrorStatus).build();
		}

		private TransactionRecord.Builder builderWithReceiptStatus(ResponseCodeEnum status) {
			TransactionReceipt receipt = TransactionReceipt.newBuilder()
					.setAccountID(payer).setStatus(status).build();
			return RequestBuilder.getTransactionRecord(
					txn.getTransactionFee(),
					txn.getMemo(),
					txn.getTransactionID(),
					RequestBuilder.getTimestamp(consensusTime),
					receipt);
		}
	}

	static void markDeleted(AccountID target, FCMap<MerkleEntityId, MerkleAccount> ledger) {
		MerkleEntityId key = MerkleEntityId.fromAccountId(target);
		MerkleAccount account = ledger.getForModify(key);
		account.setDeleted(true);
		ledger.replace(key, account);
	}

	static void doTransfers(
			TransferList transferList,
			FCMap<MerkleEntityId, MerkleAccount> ledger)
			throws InvalidAccountIDException, NegativeAccountBalanceException {
		assertValidAccounts(transferList, ledger);
		for (AccountAmount transfer : transferList.getAccountAmountsList()) {
			doTransfer(transfer, ledger);
		}
	}

	static void assertValidAccounts(
			TransferList transferList,
			FCMap<MerkleEntityId, MerkleAccount> ledger) throws InvalidAccountIDException {
		for (AccountAmount transfer : transferList.getAccountAmountsList()) {
			if (!isValid(transfer.getAccountID(), ledger)) {
				throw new InvalidAccountIDException(
						"Account " + transfer.getAccountID() + " is not a valid transfer target!",
						transfer.getAccountID());
			}
		}
	}

	static boolean isValid(AccountID account, FCMap<MerkleEntityId, MerkleAccount> ledger) {
		return Optional
				.ofNullable(ledger.get(MerkleEntityId.fromAccountId(account)))
				.map(a -> !a.getKey().hasContractID())
				.orElse(false);
	}

	static void doTransfer(
			AccountAmount transfer, FCMap<MerkleEntityId, MerkleAccount> ledger)
			throws NegativeAccountBalanceException {
		MerkleEntityId key = MerkleEntityId.fromAccountId(transfer.getAccountID());
		MerkleAccount account = ledger.getForModify(key);
		long adjustedBalance = Math.addExact(account.getBalance(), transfer.getAmount());
		account.setBalance(adjustedBalance);
		ledger.replace(key, account);
	}

	static TransferList asTransferList(List<AccountAmount>... specifics) {
		TransferList.Builder builder = TransferList.newBuilder();
		Arrays.stream(specifics).forEach(builder::addAllAccountAmounts);
		return builder.build();
	}

	static String printable(TransferList transfers) {
		return transfers
				.getAccountAmountsList()
				.stream()
				.map(adjust -> String.format(
						"%d %s %d",
						adjust.getAmount(),
						adjust.getAmount() < 0L ? "from" : "to",
						adjust.getAccountID().getAccountNum()))
				.collect(joining(", "));
	}

	static List<AccountAmount> hbarsFromTo(long amount, AccountID from, AccountID to) {
		return Arrays.asList(
				AccountAmount
						.newBuilder()
						.setAccountID(from)
						.setAmount(-1L * amount)
						.build(),
				AccountAmount
						.newBuilder()
						.setAccountID(to)
						.setAmount(amount)
						.build());
	}

	static long balanceOf(AccountID account, FCMap<MerkleEntityId, MerkleAccount> ledger) {
		MerkleEntityId key = MerkleEntityId.fromAccountId(account);
		return ledger.get(key).getBalance();
	}


	interface DeleteSpec {

		@FunctionalInterface
		interface ConsensusTime {

			Supplier<TransactionRecord> at(Instant consensusTime);
		}

		@FunctionalInterface
		interface Txn {

			AccountOperations.DeleteSpec.ConsensusTime via(TransactionBody txn);
		}
	}
}
