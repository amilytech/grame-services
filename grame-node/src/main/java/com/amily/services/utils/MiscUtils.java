package com.grame.services.utils;

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

import com.grame.services.exceptions.UnknowngrameFunctionality;

import static com.grame.services.grpc.controllers.ContractController.CALL_CONTRACT_METRIC;
import static com.grame.services.grpc.controllers.ContractController.CREATE_CONTRACT_METRIC;
import static com.grame.services.grpc.controllers.ContractController.DELETE_CONTRACT_METRIC;
import static com.grame.services.grpc.controllers.ContractController.GET_CONTRACT_BYTECODE_METRIC;
import static com.grame.services.grpc.controllers.ContractController.GET_CONTRACT_INFO_METRIC;
import static com.grame.services.grpc.controllers.ContractController.GET_CONTRACT_RECORDS_METRIC;
import static com.grame.services.grpc.controllers.ContractController.GET_SOLIDITY_ADDRESS_INFO_METRIC;
import static com.grame.services.grpc.controllers.ContractController.LOCALCALL_CONTRACT_METRIC;
import static com.grame.services.grpc.controllers.ContractController.UPDATE_CONTRACT_METRIC;
import static com.grame.services.grpc.controllers.CryptoController.*;
import static com.grame.services.grpc.controllers.ConsensusController.*;
import static com.grame.services.grpc.controllers.NetworkController.GET_VERSION_INFO_METRIC;
import static com.grame.services.grpc.controllers.NetworkController.UNCHECKED_SUBMIT_METRIC;
import static com.grame.services.grpc.controllers.FileController.*;

import com.grame.services.keys.LegacyEd25519KeyReader;
import com.grame.services.ledger.grameLedger;
import com.gramegrame.api.proto.java.AccountAmount;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Key;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.QueryHeader;
import com.gramegrame.api.proto.java.SchedulableTransactionBody;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransferList;
import com.grame.services.legacy.core.jproto.JEd25519Key;
import com.grame.services.legacy.core.jproto.JKey;
import com.grame.services.state.submerkle.ExpirableTxnRecord;
import com.swirlds.common.AddressBook;
import com.swirlds.fcqueue.FCQueue;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.grame.services.grpc.controllers.FreezeController.FREEZE_METRIC;
import static com.grame.services.stats.ServicesStatsConfig.SYSTEM_DELETE_METRIC;
import static com.grame.services.stats.ServicesStatsConfig.SYSTEM_UNDELETE_METRIC;
import static com.grame.services.utils.EntityIdUtils.accountParsedFromString;
import static com.gramegrame.api.proto.java.Query.QueryCase.CONSENSUSGETTOPICINFO;
import static com.gramegrame.api.proto.java.Query.QueryCase.CONTRACTCALLLOCAL;
import static com.gramegrame.api.proto.java.Query.QueryCase.CONTRACTGETBYTECODE;
import static com.gramegrame.api.proto.java.Query.QueryCase.CONTRACTGETINFO;
import static com.gramegrame.api.proto.java.Query.QueryCase.CONTRACTGETRECORDS;
import static com.gramegrame.api.proto.java.Query.QueryCase.CRYPTOGETACCOUNTBALANCE;
import static com.gramegrame.api.proto.java.Query.QueryCase.CRYPTOGETACCOUNTRECORDS;
import static com.gramegrame.api.proto.java.Query.QueryCase.CRYPTOGETLIVEHASH;
import static com.gramegrame.api.proto.java.Query.QueryCase.CRYPTOGETINFO;
import static com.gramegrame.api.proto.java.Query.QueryCase.FILEGETCONTENTS;
import static com.gramegrame.api.proto.java.Query.QueryCase.FILEGETINFO;
import static com.gramegrame.api.proto.java.Query.QueryCase.GETBYKEY;
import static com.gramegrame.api.proto.java.Query.QueryCase.GETBYSOLIDITYID;
import static com.gramegrame.api.proto.java.Query.QueryCase.NETWORKGETVERSIONINFO;
import static com.gramegrame.api.proto.java.Query.QueryCase.SCHEDULEGETINFO;
import static com.gramegrame.api.proto.java.Query.QueryCase.TOKENGETINFO;
import static com.gramegrame.api.proto.java.Query.QueryCase.TRANSACTIONGETRECEIPT;
import static com.gramegrame.api.proto.java.Query.QueryCase.TRANSACTIONGETRECORD;
import static com.grame.services.legacy.core.jproto.JKey.mapJKey;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static com.gramegrame.api.proto.java.grameFunctionality.*;
import static java.util.stream.Collectors.toSet;

public class MiscUtils {
	public static final EnumSet<grameFunctionality> QUERY_FUNCTIONS = EnumSet.of(
			ConsensusGetTopicInfo,
			GetBySolidityID,
			ContractCallLocal,
			ContractGetInfo,
			ContractGetBytecode,
			ContractGetRecords,
			CryptoGetAccountBalance,
			CryptoGetAccountRecords,
			CryptoGetInfo,
			CryptoGetLiveHash,
			FileGetContents,
			FileGetInfo,
			TransactionGetReceipt,
			TransactionGetRecord,
			GetVersionInfo,
			TokenGetInfo,
			ScheduleGetInfo
	);

	static final String TOKEN_MINT_METRIC = "mintToken";
	static final String TOKEN_BURN_METRIC = "burnToken";
	static final String TOKEN_CREATE_METRIC = "createToken";
	static final String TOKEN_DELETE_METRIC = "deleteToken";
	static final String TOKEN_UPDATE_METRIC = "updateToken";
	static final String TOKEN_FREEZE_METRIC = "freezeTokenAccount";
	static final String TOKEN_UNFREEZE_METRIC = "unfreezeTokenAccount";
	static final String TOKEN_GRANT_KYC_METRIC = "grantKycToTokenAccount";
	static final String TOKEN_REVOKE_KYC_METRIC = "revokeKycFromTokenAccount";
	static final String TOKEN_WIPE_ACCOUNT_METRIC = "wipeTokenAccount";
	static final String TOKEN_ASSOCIATE_METRIC = "associateTokens";
	static final String TOKEN_DISSOCIATE_METRIC = "dissociateTokens";
	static final String TOKEN_GET_INFO_METRIC = "getTokenInfo";

	static final String SCHEDULE_CREATE_METRIC = "createSchedule";
	static final String SCHEDULE_DELETE_METRIC = "deleteSchedule";
	static final String SCHEDULE_SIGN_METRIC = "signSchedule";
	static final String SCHEDULE_GET_INFO_METRIC = "getScheduleInfo";

	private static final EnumMap<Query.QueryCase, grameFunctionality> queryFunctions =
			new EnumMap<>(Query.QueryCase.class);
	static {
		queryFunctions.put(NETWORKGETVERSIONINFO, GetVersionInfo);
		queryFunctions.put(GETBYKEY, GetByKey);
		queryFunctions.put(CONSENSUSGETTOPICINFO, ConsensusGetTopicInfo);
		queryFunctions.put(GETBYSOLIDITYID, GetBySolidityID);
		queryFunctions.put(CONTRACTCALLLOCAL, ContractCallLocal);
		queryFunctions.put(CONTRACTGETINFO, ContractGetInfo);
		queryFunctions.put(CONTRACTGETBYTECODE, ContractGetBytecode);
		queryFunctions.put(CONTRACTGETRECORDS, ContractGetRecords);
		queryFunctions.put(CRYPTOGETACCOUNTBALANCE, CryptoGetAccountBalance);
		queryFunctions.put(CRYPTOGETACCOUNTRECORDS, CryptoGetAccountRecords);
		queryFunctions.put(CRYPTOGETINFO, CryptoGetInfo);
		queryFunctions.put(CRYPTOGETLIVEHASH, CryptoGetLiveHash);
		queryFunctions.put(FILEGETCONTENTS, FileGetContents);
		queryFunctions.put(FILEGETINFO, FileGetInfo);
		queryFunctions.put(TRANSACTIONGETRECEIPT, TransactionGetReceipt);
		queryFunctions.put(TRANSACTIONGETRECORD, TransactionGetRecord);
		queryFunctions.put(TOKENGETINFO, TokenGetInfo);
		queryFunctions.put(SCHEDULEGETINFO, ScheduleGetInfo);
	}

	public static final EnumMap<grameFunctionality, String> BASE_STAT_NAMES =
			new EnumMap<>(grameFunctionality.class);
	static {
		/* Transactions */
		BASE_STAT_NAMES.put(CryptoCreate, CRYPTO_CREATE_METRIC);
		BASE_STAT_NAMES.put(CryptoTransfer, CRYPTO_TRANSFER_METRIC);
		BASE_STAT_NAMES.put(CryptoUpdate, CRYPTO_UPDATE_METRIC);
		BASE_STAT_NAMES.put(CryptoDelete, CRYPTO_DELETE_METRIC);
		BASE_STAT_NAMES.put(CryptoAddLiveHash, ADD_LIVE_HASH_METRIC);
		BASE_STAT_NAMES.put(CryptoDeleteLiveHash, DELETE_LIVE_HASH_METRIC);
		BASE_STAT_NAMES.put(FileCreate, CREATE_FILE_METRIC);
		BASE_STAT_NAMES.put(FileUpdate, UPDATE_FILE_METRIC);
		BASE_STAT_NAMES.put(FileDelete, DELETE_FILE_METRIC);
		BASE_STAT_NAMES.put(FileAppend, FILE_APPEND_METRIC);
		BASE_STAT_NAMES.put(ContractCreate, CREATE_CONTRACT_METRIC);
		BASE_STAT_NAMES.put(ContractUpdate, UPDATE_CONTRACT_METRIC);
		BASE_STAT_NAMES.put(ContractCall, CALL_CONTRACT_METRIC);
		BASE_STAT_NAMES.put(ContractDelete, DELETE_CONTRACT_METRIC);
		BASE_STAT_NAMES.put(ConsensusCreateTopic, CREATE_TOPIC_METRIC);
		BASE_STAT_NAMES.put(ConsensusUpdateTopic, UPDATE_TOPIC_METRIC);
		BASE_STAT_NAMES.put(ConsensusDeleteTopic, DELETE_TOPIC_METRIC);
		BASE_STAT_NAMES.put(ConsensusSubmitMessage, SUBMIT_MESSAGE_METRIC);
		BASE_STAT_NAMES.put(TokenCreate, TOKEN_CREATE_METRIC);
		BASE_STAT_NAMES.put(TokenFreezeAccount, TOKEN_FREEZE_METRIC);
		BASE_STAT_NAMES.put(TokenUnfreezeAccount, TOKEN_UNFREEZE_METRIC);
		BASE_STAT_NAMES.put(TokenGrantKycToAccount, TOKEN_GRANT_KYC_METRIC);
		BASE_STAT_NAMES.put(TokenRevokeKycFromAccount, TOKEN_REVOKE_KYC_METRIC);
		BASE_STAT_NAMES.put(TokenDelete, TOKEN_DELETE_METRIC);
		BASE_STAT_NAMES.put(TokenMint, TOKEN_MINT_METRIC);
		BASE_STAT_NAMES.put(TokenBurn, TOKEN_BURN_METRIC);
		BASE_STAT_NAMES.put(TokenAccountWipe, TOKEN_WIPE_ACCOUNT_METRIC);
		BASE_STAT_NAMES.put(TokenUpdate, TOKEN_UPDATE_METRIC);
		BASE_STAT_NAMES.put(TokenAssociateToAccount, TOKEN_ASSOCIATE_METRIC);
		BASE_STAT_NAMES.put(TokenDissociateFromAccount, TOKEN_DISSOCIATE_METRIC);
		BASE_STAT_NAMES.put(ScheduleCreate, SCHEDULE_CREATE_METRIC);
		BASE_STAT_NAMES.put(ScheduleSign, SCHEDULE_SIGN_METRIC);
		BASE_STAT_NAMES.put(ScheduleDelete, SCHEDULE_DELETE_METRIC);
		BASE_STAT_NAMES.put(UncheckedSubmit, UNCHECKED_SUBMIT_METRIC);
		BASE_STAT_NAMES.put(Freeze, FREEZE_METRIC);
		BASE_STAT_NAMES.put(SystemDelete, SYSTEM_DELETE_METRIC);
		BASE_STAT_NAMES.put(SystemUndelete, SYSTEM_UNDELETE_METRIC);
		/* Queries */
		BASE_STAT_NAMES.put(ConsensusGetTopicInfo, GET_TOPIC_INFO_METRIC);
		BASE_STAT_NAMES.put(GetBySolidityID, GET_SOLIDITY_ADDRESS_INFO_METRIC);
		BASE_STAT_NAMES.put(ContractCallLocal, LOCALCALL_CONTRACT_METRIC);
		BASE_STAT_NAMES.put(ContractGetInfo, GET_CONTRACT_INFO_METRIC);
		BASE_STAT_NAMES.put(ContractGetBytecode, GET_CONTRACT_BYTECODE_METRIC);
		BASE_STAT_NAMES.put(ContractGetRecords, GET_CONTRACT_RECORDS_METRIC);
		BASE_STAT_NAMES.put(CryptoGetAccountBalance, GET_ACCOUNT_BALANCE_METRIC);
		BASE_STAT_NAMES.put(CryptoGetAccountRecords, GET_ACCOUNT_RECORDS_METRIC);
		BASE_STAT_NAMES.put(CryptoGetInfo, GET_ACCOUNT_INFO_METRIC);
		BASE_STAT_NAMES.put(CryptoGetLiveHash, GET_LIVE_HASH_METRIC);
		BASE_STAT_NAMES.put(FileGetContents, GET_FILE_CONTENT_METRIC);
		BASE_STAT_NAMES.put(FileGetInfo, GET_FILE_INFO_METRIC);
		BASE_STAT_NAMES.put(TransactionGetReceipt, GET_RECEIPT_METRIC);
		BASE_STAT_NAMES.put(TransactionGetRecord, GET_RECORD_METRIC);
		BASE_STAT_NAMES.put(GetVersionInfo, GET_VERSION_INFO_METRIC);
		BASE_STAT_NAMES.put(TokenGetInfo, TOKEN_GET_INFO_METRIC);
		BASE_STAT_NAMES.put(ScheduleGetInfo, SCHEDULE_GET_INFO_METRIC);
	}

	public static String baseStatNameOf(grameFunctionality function) {
		return BASE_STAT_NAMES.getOrDefault(function, function.toString());
	}

	public static List<AccountAmount> canonicalDiffRepr(List<AccountAmount> a, List<AccountAmount> b) {
		return canonicalRepr(Stream.concat(a.stream(), b.stream().map(MiscUtils::negationOf)).collect(toList()));
	}

	private static AccountAmount negationOf(AccountAmount adjustment) {
		return adjustment.toBuilder().setAmount(-1 * adjustment.getAmount()).build();
	}

	public static List<AccountAmount> canonicalRepr(List<AccountAmount> transfers) {
		return transfers.stream()
				.collect(toMap(AccountAmount::getAccountID, AccountAmount::getAmount, Math::addExact))
				.entrySet().stream()
				.filter(e -> e.getValue() != 0)
				.sorted(comparing(Map.Entry::getKey, grameLedger.ACCOUNT_ID_COMPARATOR))
				.map(e -> AccountAmount.newBuilder().setAccountID(e.getKey()).setAmount(e.getValue()).build())
				.collect(toList());
	}

	public static String readableTransferList(TransferList accountAmounts) {
		return accountAmounts.getAccountAmountsList()
				.stream()
				.map(aa -> String.format(
						"%s %s %s%s",
						EntityIdUtils.readableId(aa.getAccountID()),
						aa.getAmount() < 0 ? "->" : "<-",
						aa.getAmount() < 0 ? "-" : "+",
						BigInteger.valueOf(aa.getAmount()).abs().toString()))
				.collect(toList())
				.toString();
	}

	public static JKey lookupInCustomStore(LegacyEd25519KeyReader b64Reader, String storeLoc, String kpId) {
		try {
			return new JEd25519Key(commonsHexToBytes(b64Reader.hexedABytesFrom(storeLoc, kpId)));
		} catch (DecoderException e) {
			var msg = String.format("Arguments 'storeLoc=%s' and 'kpId=%s' did not denote a valid key!", storeLoc, kpId);
			throw new IllegalArgumentException(msg, e);
		}
	}

	public static String readableProperty(Object o) {
		if (o instanceof FCQueue) {
			return ExpirableTxnRecord.allToGrpc(new ArrayList<>((FCQueue<ExpirableTxnRecord>) o)).toString();
		} else {
			return o.toString();
		}
	}

	public static JKey asFcKeyUnchecked(Key key) {
		try {
			return JKey.mapKey(key);
		} catch (DecoderException impermissible) {
			throw new IllegalArgumentException("Key " + key + " should have been decode-able!", impermissible);
		}
	}

	public static Optional<JKey> asUsableFcKey(Key key) {
		try {
			var fcKey = JKey.mapKey(key);
			if (!fcKey.isValid()) {
				return Optional.empty();
			}
			return Optional.of(fcKey);
		} catch (DecoderException ignore) {
			return Optional.empty();
		}
	}

	public static Key asKeyUnchecked(JKey fcKey) {
		try {
			return mapJKey(fcKey);
		} catch (Exception impossible) {
			return Key.getDefaultInstance();
		}
	}

	public static Timestamp asTimestamp(Instant when) {
		return Timestamp.newBuilder()
				.setSeconds(when.getEpochSecond())
				.setNanos(when.getNano())
				.build();
	}

	public static Instant timestampToInstant(Timestamp timestamp) {
		return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
	}

	public static Optional<QueryHeader> activeHeaderFrom(Query query) {
		switch (query.getQueryCase()) {
			case TOKENGETINFO:
				return Optional.of(query.getTokenGetInfo().getHeader());
			case SCHEDULEGETINFO:
				return Optional.of(query.getScheduleGetInfo().getHeader());
			case CONSENSUSGETTOPICINFO:
				return Optional.of(query.getConsensusGetTopicInfo().getHeader());
			case GETBYSOLIDITYID:
				return Optional.of(query.getGetBySolidityID().getHeader());
			case CONTRACTCALLLOCAL:
				return Optional.of(query.getContractCallLocal().getHeader());
			case CONTRACTGETINFO:
				return Optional.of(query.getContractGetInfo().getHeader());
			case CONTRACTGETBYTECODE:
				return Optional.of(query.getContractGetBytecode().getHeader());
			case CONTRACTGETRECORDS:
				return Optional.of(query.getContractGetRecords().getHeader());
			case CRYPTOGETACCOUNTBALANCE:
				return Optional.of(query.getCryptogetAccountBalance().getHeader());
			case CRYPTOGETACCOUNTRECORDS:
				return Optional.of(query.getCryptoGetAccountRecords().getHeader());
			case CRYPTOGETINFO:
				return Optional.of(query.getCryptoGetInfo().getHeader());
			case CRYPTOGETLIVEHASH:
				return Optional.of(query.getCryptoGetLiveHash().getHeader());
			case CRYPTOGETPROXYSTAKERS:
				return Optional.of(query.getCryptoGetProxyStakers().getHeader());
			case FILEGETCONTENTS:
				return Optional.of(query.getFileGetContents().getHeader());
			case FILEGETINFO:
				return Optional.of(query.getFileGetInfo().getHeader());
			case TRANSACTIONGETRECEIPT:
				return Optional.of(query.getTransactionGetReceipt().getHeader());
			case TRANSACTIONGETRECORD:
				return Optional.of(query.getTransactionGetRecord().getHeader());
			case TRANSACTIONGETFASTRECORD:
				return Optional.of(query.getTransactionGetFastRecord().getHeader());
			case NETWORKGETVERSIONINFO:
				return Optional.of(query.getNetworkGetVersionInfo().getHeader());
			default:
				return Optional.empty();
		}
	}

	public static String getTxnStat(TransactionBody txn) {
		try {
			return BASE_STAT_NAMES.get(functionOf(txn));
		} catch (UnknowngrameFunctionality unknowngrameFunctionality) {
			return "NotImplemented";
		}
	}

	public static grameFunctionality functionOf(TransactionBody txn) throws UnknowngrameFunctionality {
		if (txn.hasSystemDelete()) {
			return SystemDelete;
		} else if (txn.hasSystemUndelete()) {
			return SystemUndelete;
		} else if (txn.hasContractCall()) {
			return ContractCall;
		} else if (txn.hasContractCreateInstance()) {
			return ContractCreate;
		} else if (txn.hasContractUpdateInstance()) {
			return ContractUpdate;
		} else if (txn.hasCryptoAddLiveHash()) {
			return CryptoAddLiveHash;
		} else if (txn.hasCryptoCreateAccount()) {
			return CryptoCreate;
		} else if (txn.hasCryptoDelete()) {
			return CryptoDelete;
		} else if (txn.hasCryptoDeleteLiveHash()) {
			return CryptoDeleteLiveHash;
		} else if (txn.hasCryptoTransfer()) {
			return CryptoTransfer;
		} else if (txn.hasCryptoUpdateAccount()) {
			return CryptoUpdate;
		} else if (txn.hasFileAppend()) {
			return FileAppend;
		} else if (txn.hasFileCreate()) {
			return FileCreate;
		} else if (txn.hasFileDelete()) {
			return FileDelete;
		} else if (txn.hasFileUpdate()) {
			return FileUpdate;
		} else if (txn.hasContractDeleteInstance()) {
			return ContractDelete;
		} else if (txn.hasFreeze()) {
			return Freeze;
		} else if (txn.hasConsensusCreateTopic()) {
			return ConsensusCreateTopic;
		} else if (txn.hasConsensusUpdateTopic()) {
			return ConsensusUpdateTopic;
		} else if (txn.hasConsensusDeleteTopic()) {
			return ConsensusDeleteTopic;
		} else if (txn.hasConsensusSubmitMessage()) {
			return ConsensusSubmitMessage;
		} else if (txn.hasTokenCreation()) {
			return TokenCreate;
		} else if (txn.hasTokenFreeze()) {
			return TokenFreezeAccount;
		} else if (txn.hasTokenUnfreeze()) {
			return TokenUnfreezeAccount;
		} else if (txn.hasTokenGrantKyc()) {
			return TokenGrantKycToAccount;
		} else if (txn.hasTokenRevokeKyc()) {
			return TokenRevokeKycFromAccount;
		} else if (txn.hasTokenDeletion()) {
			return TokenDelete;
		} else if (txn.hasTokenUpdate()) {
			return TokenUpdate;
		} else if (txn.hasTokenMint()) {
			return TokenMint;
		} else if (txn.hasTokenBurn()) {
			return TokenBurn;
		} else if (txn.hasTokenWipe()) {
			return TokenAccountWipe;
		} else if (txn.hasTokenAssociate()) {
			return TokenAssociateToAccount;
		} else if (txn.hasTokenDissociate()) {
			return TokenDissociateFromAccount;
		} else if (txn.hasScheduleCreate()) {
			return ScheduleCreate;
		} else if (txn.hasScheduleSign()) {
			return ScheduleSign;
		} else if (txn.hasScheduleDelete()) {
			return ScheduleDelete;
		} else if (txn.hasUncheckedSubmit()) {
			return UncheckedSubmit;
		} else {
			throw new UnknowngrameFunctionality();
		}
	}

	public static Optional<grameFunctionality> functionalityOfQuery(Query query) {
		return Optional.ofNullable(queryFunctions.get(query.getQueryCase()));
	}

	public static String commonsBytesToHex(byte[] data) {
		return Hex.encodeHexString(data);
	}

	public static byte[] commonsHexToBytes(String literal) throws DecoderException {
		return Hex.decodeHex(literal);
	}

	public static String describe(JKey k) {
		if (k == null) {
			return "<N/A>";
		} else {
			Key readable = null;
			try {
				readable = mapJKey(k);
			} catch (Exception ignore) { }
			return String.valueOf(readable);
		}
	}

	public static Set<AccountID> getNodeAccounts(AddressBook addressBook) {
		return IntStream.range(0, addressBook.getSize())
				.mapToObj(addressBook::getAddress)
				.map(address -> accountParsedFromString(address.getMemo()))
				.collect(toSet());
	}

	public static TransactionBody asOrdinary(SchedulableTransactionBody scheduledTxn) {
		var ordinary = TransactionBody.newBuilder();
		ordinary.setTransactionFee(scheduledTxn.getTransactionFee())
				.setMemo(scheduledTxn.getMemo());
		if (scheduledTxn.hasContractCall()) {
			ordinary.setContractCall(scheduledTxn.getContractCall());
		} else if (scheduledTxn.hasContractCreateInstance()) {
			ordinary.setContractCreateInstance(scheduledTxn.getContractCreateInstance());
		} else if (scheduledTxn.hasContractUpdateInstance()) {
			ordinary.setContractUpdateInstance(scheduledTxn.getContractUpdateInstance());
		} else if (scheduledTxn.hasContractDeleteInstance()) {
			ordinary.setContractDeleteInstance(scheduledTxn.getContractDeleteInstance());
		} else if (scheduledTxn.hasCryptoCreateAccount()) {
			ordinary.setCryptoCreateAccount(scheduledTxn.getCryptoCreateAccount());
		} else if (scheduledTxn.hasCryptoDelete()) {
			ordinary.setCryptoDelete(scheduledTxn.getCryptoDelete());
		} else if (scheduledTxn.hasCryptoTransfer()) {
			ordinary.setCryptoTransfer(scheduledTxn.getCryptoTransfer());
		} else if (scheduledTxn.hasCryptoUpdateAccount()) {
			ordinary.setCryptoUpdateAccount(scheduledTxn.getCryptoUpdateAccount());
		} else if (scheduledTxn.hasFileAppend()) {
			ordinary.setFileAppend(scheduledTxn.getFileAppend());
		} else if (scheduledTxn.hasFileCreate()) {
			ordinary.setFileCreate(scheduledTxn.getFileCreate());
		} else if (scheduledTxn.hasFileDelete()) {
			ordinary.setFileDelete(scheduledTxn.getFileDelete());
		} else if (scheduledTxn.hasFileUpdate()) {
			ordinary.setFileUpdate(scheduledTxn.getFileUpdate());
		} else if (scheduledTxn.hasSystemDelete()) {
			ordinary.setSystemDelete(scheduledTxn.getSystemDelete());
		} else if (scheduledTxn.hasSystemUndelete()) {
			ordinary.setSystemUndelete(scheduledTxn.getSystemUndelete());
		} else if (scheduledTxn.hasFreeze()) {
			ordinary.setFreeze(scheduledTxn.getFreeze());
		} else if (scheduledTxn.hasConsensusCreateTopic()) {
			ordinary.setConsensusCreateTopic(scheduledTxn.getConsensusCreateTopic());
		} else if (scheduledTxn.hasConsensusUpdateTopic()) {
			ordinary.setConsensusUpdateTopic(scheduledTxn.getConsensusUpdateTopic());
		} else if (scheduledTxn.hasConsensusDeleteTopic()) {
			ordinary.setConsensusDeleteTopic(scheduledTxn.getConsensusDeleteTopic());
		} else if (scheduledTxn.hasConsensusSubmitMessage()) {
			ordinary.setConsensusSubmitMessage(scheduledTxn.getConsensusSubmitMessage());
		} else if (scheduledTxn.hasTokenCreation()) {
			ordinary.setTokenCreation(scheduledTxn.getTokenCreation());
		} else if (scheduledTxn.hasTokenFreeze()) {
			ordinary.setTokenFreeze(scheduledTxn.getTokenFreeze());
		} else if (scheduledTxn.hasTokenUnfreeze()) {
			ordinary.setTokenUnfreeze(scheduledTxn.getTokenUnfreeze());
		} else if (scheduledTxn.hasTokenGrantKyc()) {
			ordinary.setTokenGrantKyc(scheduledTxn.getTokenGrantKyc());
		} else if (scheduledTxn.hasTokenRevokeKyc()) {
			ordinary.setTokenRevokeKyc(scheduledTxn.getTokenRevokeKyc());
		} else if (scheduledTxn.hasTokenDeletion()) {
			ordinary.setTokenDeletion(scheduledTxn.getTokenDeletion());
		} else if (scheduledTxn.hasTokenUpdate()) {
			ordinary.setTokenUpdate(scheduledTxn.getTokenUpdate());
		} else if (scheduledTxn.hasTokenMint()) {
			ordinary.setTokenMint(scheduledTxn.getTokenMint());
		} else if (scheduledTxn.hasTokenBurn()) {
			ordinary.setTokenBurn(scheduledTxn.getTokenBurn());
		} else if (scheduledTxn.hasTokenWipe()) {
			ordinary.setTokenWipe(scheduledTxn.getTokenWipe());
		} else if (scheduledTxn.hasTokenAssociate()) {
			ordinary.setTokenAssociate(scheduledTxn.getTokenAssociate());
		} else if (scheduledTxn.hasTokenDissociate()) {
			ordinary.setTokenDissociate(scheduledTxn.getTokenDissociate());
		} else if (scheduledTxn.hasScheduleDelete()) {
			ordinary.setScheduleDelete(scheduledTxn.getScheduleDelete());
		}
		return ordinary.build();
	}
}
