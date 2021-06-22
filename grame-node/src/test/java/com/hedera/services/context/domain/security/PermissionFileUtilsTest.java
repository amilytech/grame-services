package com.grame.services.context.domain.security;

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

import com.gramegrame.api.proto.java.ConsensusCreateTopicTransactionBody;
import com.gramegrame.api.proto.java.ConsensusDeleteTopicTransactionBody;
import com.gramegrame.api.proto.java.ConsensusGetTopicInfoQuery;
import com.gramegrame.api.proto.java.ConsensusSubmitMessageTransactionBody;
import com.gramegrame.api.proto.java.ConsensusUpdateTopicTransactionBody;
import com.gramegrame.api.proto.java.ContractCallLocalQuery;
import com.gramegrame.api.proto.java.ContractCallTransactionBody;
import com.gramegrame.api.proto.java.ContractCreateTransactionBody;
import com.gramegrame.api.proto.java.ContractDeleteTransactionBody;
import com.gramegrame.api.proto.java.ContractGetBytecodeQuery;
import com.gramegrame.api.proto.java.ContractGetInfoQuery;
import com.gramegrame.api.proto.java.ContractGetRecordsQuery;
import com.gramegrame.api.proto.java.ContractUpdateTransactionBody;
import com.gramegrame.api.proto.java.CryptoAddLiveHashTransactionBody;
import com.gramegrame.api.proto.java.CryptoCreateTransactionBody;
import com.gramegrame.api.proto.java.CryptoDeleteLiveHashTransactionBody;
import com.gramegrame.api.proto.java.CryptoDeleteTransactionBody;
import com.gramegrame.api.proto.java.CryptoGetAccountBalanceQuery;
import com.gramegrame.api.proto.java.CryptoGetAccountRecordsQuery;
import com.gramegrame.api.proto.java.CryptoGetLiveHashQuery;
import com.gramegrame.api.proto.java.CryptoGetInfoQuery;
import com.gramegrame.api.proto.java.CryptoTransferTransactionBody;
import com.gramegrame.api.proto.java.CryptoUpdateTransactionBody;
import com.gramegrame.api.proto.java.FileAppendTransactionBody;
import com.gramegrame.api.proto.java.FileCreateTransactionBody;
import com.gramegrame.api.proto.java.FileDeleteTransactionBody;
import com.gramegrame.api.proto.java.FileGetContentsQuery;
import com.gramegrame.api.proto.java.FileGetInfoQuery;
import com.gramegrame.api.proto.java.FileUpdateTransactionBody;
import com.gramegrame.api.proto.java.FreezeTransactionBody;
import com.gramegrame.api.proto.java.GetBySolidityIDQuery;
import com.gramegrame.api.proto.java.NetworkGetVersionInfoQuery;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.ScheduleCreateTransactionBody;
import com.gramegrame.api.proto.java.ScheduleDeleteTransactionBody;
import com.gramegrame.api.proto.java.ScheduleSignTransactionBody;
import com.gramegrame.api.proto.java.SystemDeleteTransactionBody;
import com.gramegrame.api.proto.java.SystemUndeleteTransactionBody;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionGetFastRecordQuery;
import com.gramegrame.api.proto.java.TransactionGetReceiptQuery;
import com.gramegrame.api.proto.java.TransactionGetRecordQuery;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.grame.services.context.domain.security.PermissionFileUtils.permissionFileKeyForTxn;
import static com.grame.services.context.domain.security.PermissionFileUtils.permissionFileKeyForQuery;

class PermissionFileUtilsTest {
	@Test
	public void returnsEmptyKeyForBlankTxn() {
		assertEquals("", permissionFileKeyForTxn(TransactionBody.getDefaultInstance()));
	}

	@Test
	public void returnsEmptyKeyForBlankQuery() {
		assertEquals("", permissionFileKeyForQuery(Query.getDefaultInstance()));
	}

	@Test
	public void worksForScheduleCreate() {
		var op = ScheduleCreateTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setScheduleCreate(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForScheduleDelete() {
		var op = ScheduleDeleteTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setScheduleDelete(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForScheduleSign() {
		var op = ScheduleSignTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setScheduleSign(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForCryptoCreateAccount() {
		var op = CryptoCreateTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setCryptoCreateAccount(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForCryptoTransfer() {
		var op = CryptoTransferTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setCryptoTransfer(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForCryptoUpdateAccount() {
		var op = CryptoUpdateTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setCryptoUpdateAccount(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForCryptoDelete() {
		var op = CryptoDeleteTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setCryptoDelete(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForCryptoAddLiveHash() {
		var op = CryptoAddLiveHashTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setCryptoAddLiveHash(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForCryptoDeleteLiveHash() {
		var op = CryptoDeleteLiveHashTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setCryptoDeleteLiveHash(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForFileCreate() {
		var op = FileCreateTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setFileCreate(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForFileUpdate() {
		var op = FileUpdateTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setFileUpdate(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForFileDelete() {
		var op = FileDeleteTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setFileDelete(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForFileAppend() {
		var op = FileAppendTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setFileAppend(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForContractCreateInstance() {
		var op = ContractCreateTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setContractCreateInstance(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForContractUpdateInstance() {
		var op = ContractUpdateTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setContractUpdateInstance(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForContractCall() {
		var op = ContractCallTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setContractCall(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForSystemDelete() {
		var op = SystemDeleteTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setSystemDelete(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForSystemUndelete() {
		var op = SystemUndeleteTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setSystemUndelete(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForContractDeleteInstance() {
		var op = ContractDeleteTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setContractDeleteInstance(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForFreeze() {
		var op = FreezeTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setFreeze(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForConsensusCreateTopic() {
		var op = ConsensusCreateTopicTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setConsensusCreateTopic(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForConsensusUpdateTopic() {
		var op = ConsensusUpdateTopicTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setConsensusUpdateTopic(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForConsensusDeleteTopic() {
		var op = ConsensusDeleteTopicTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setConsensusDeleteTopic(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForConsensusSubmitMessage() {
		var op = ConsensusSubmitMessageTransactionBody.getDefaultInstance();
		var txn = TransactionBody.newBuilder()
				.setConsensusSubmitMessage(op)
				.build();
		assertEquals(permissionFileKeyForTxn(txn), legacyKeyForTxn(txn));
	}

	@Test
	public void worksForGetTopicInfo() {
		var op = ConsensusGetTopicInfoQuery.getDefaultInstance();
		var query = Query.newBuilder()
				.setConsensusGetTopicInfo(op)
				.build();
		assertEquals(legacyKeyForQuery(query), permissionFileKeyForQuery(query));
	}

	@Test
	public void worksForGetVersionInfo() {
		var op = NetworkGetVersionInfoQuery.getDefaultInstance();
		var query = Query.newBuilder()
				.setNetworkGetVersionInfo(op)
				.build();
		assertEquals(legacyKeyForQuery(query), permissionFileKeyForQuery(query));
	}

	@Test
	public void worksForGetSolidityId() {
		var op = GetBySolidityIDQuery.getDefaultInstance();
		var query = Query.newBuilder()
				.setGetBySolidityID(op)
				.build();
		assertEquals(legacyKeyForQuery(query), permissionFileKeyForQuery(query));
	}

	@Test
	public void worksForGetContractCallLocal() {
		var op = ContractCallLocalQuery.getDefaultInstance();
		var query = Query.newBuilder()
				.setContractCallLocal(op)
				.build();
		assertEquals(legacyKeyForQuery(query), permissionFileKeyForQuery(query));
	}

	@Test
	public void worksForGetContractInfo() {
		var op = ContractGetInfoQuery.getDefaultInstance();
		var query = Query.newBuilder()
				.setContractGetInfo(op)
				.build();
		assertEquals(legacyKeyForQuery(query), permissionFileKeyForQuery(query));
	}

	@Test
	public void worksForGetContractBytecode() {
		var op = ContractGetBytecodeQuery.getDefaultInstance();
		var query = Query.newBuilder()
				.setContractGetBytecode(op)
				.build();
		assertEquals(legacyKeyForQuery(query), permissionFileKeyForQuery(query));
	}

	@Test
	public void worksForGetContractRecords() {
		var op = ContractGetRecordsQuery.getDefaultInstance();
		var query = Query.newBuilder()
				.setContractGetRecords(op)
				.build();
		assertEquals(legacyKeyForQuery(query), permissionFileKeyForQuery(query));
	}

	@Test
	public void worksForGetCryptoBalance() {
		var op = CryptoGetAccountBalanceQuery.getDefaultInstance();
		var query = Query.newBuilder()
				.setCryptogetAccountBalance(op)
				.build();
		assertEquals(legacyKeyForQuery(query), permissionFileKeyForQuery(query));
	}

	@Test
	public void worksForGetCryptoRecords() {
		var op = CryptoGetAccountRecordsQuery.getDefaultInstance();
		var query = Query.newBuilder()
				.setCryptoGetAccountRecords(op)
				.build();
		assertEquals(legacyKeyForQuery(query), permissionFileKeyForQuery(query));
	}

	@Test
	public void worksForGetCryptoInfo() {
		var op = CryptoGetInfoQuery.getDefaultInstance();
		var query = Query.newBuilder()
				.setCryptoGetInfo(op)
				.build();
		assertEquals(legacyKeyForQuery(query), permissionFileKeyForQuery(query));
	}

	@Test
	public void worksForGetLiveHash() {
		var op = CryptoGetLiveHashQuery.getDefaultInstance();
		var query = Query.newBuilder()
				.setCryptoGetLiveHash(op)
				.build();
		assertEquals(legacyKeyForQuery(query), permissionFileKeyForQuery(query));
	}

	@Test
	public void worksForGetFileContents() {
		var op = FileGetContentsQuery.getDefaultInstance();
		var query = Query.newBuilder()
				.setFileGetContents(op)
				.build();
		assertEquals(legacyKeyForQuery(query), permissionFileKeyForQuery(query));
	}

	@Test
	public void worksForGetFileinfo() {
		var op = FileGetInfoQuery.getDefaultInstance();
		var query = Query.newBuilder()
				.setFileGetInfo(op)
				.build();
		assertEquals(legacyKeyForQuery(query), permissionFileKeyForQuery(query));
	}

	@Test
	public void worksForReceipt() {
		var op = TransactionGetReceiptQuery.getDefaultInstance();
		var query = Query.newBuilder()
				.setTransactionGetReceipt(op)
				.build();
		assertEquals(legacyKeyForQuery(query), permissionFileKeyForQuery(query));
	}

	@Test
	public void worksForRecord() {
		var op = TransactionGetRecordQuery.getDefaultInstance();
		var query = Query.newBuilder()
				.setTransactionGetRecord(op)
				.build();
		assertEquals(legacyKeyForQuery(query), permissionFileKeyForQuery(query));
	}

	@Test
	public void worksForFastRecord() {
		var op = TransactionGetFastRecordQuery.getDefaultInstance();
		var query = Query.newBuilder()
				.setTransactionGetFastRecord(op)
				.build();
		assertEquals(legacyKeyForQuery(query), permissionFileKeyForQuery(query));
	}

	private String legacyKeyForQuery(Query request) {
		String queryBody = null;
		switch (request.getQueryCase()) {
			case NETWORKGETVERSIONINFO:
				queryBody = "getVersionInfo";
				break;
			case GETBYKEY:
				break;
			case CONSENSUSGETTOPICINFO:
				queryBody = "getTopicInfo";
				break;
			case GETBYSOLIDITYID:
				queryBody = "getBySolidityID";
				break;
			case CONTRACTCALLLOCAL:
				queryBody = "contractCallLocalMethod";
				break;
			case CONTRACTGETINFO:
				queryBody = "getContractInfo";
				break;
			case SCHEDULEGETINFO:
				queryBody = "getScheduleInfo";
				break;
			case CONTRACTGETBYTECODE:
				queryBody = "contractGetBytecode";
				break;
			case CONTRACTGETRECORDS:
				queryBody = "getTxRecordByContractID";
				break;
			case CRYPTOGETACCOUNTBALANCE:
				queryBody = "cryptoGetBalance";
				break;
			case CRYPTOGETACCOUNTRECORDS:
				queryBody = "getAccountRecords";
				break;
			case CRYPTOGETINFO:
				queryBody = "getAccountInfo";
				break;
			case CRYPTOGETLIVEHASH:
				queryBody = "getLiveHash";
				break;
			case CRYPTOGETPROXYSTAKERS:
				break;
			case FILEGETCONTENTS:
				queryBody = "getFileContent";
				break;
			case FILEGETINFO:
				queryBody = "getFileInfo";
				break;
			case TRANSACTIONGETRECEIPT:
				queryBody = "getTransactionReceipts";
				break;
			case TRANSACTIONGETRECORD:
				queryBody = "getTxRecordByTxID";
				break;
			case TRANSACTIONGETFASTRECORD:
				queryBody = "getFastTransactionRecord";
				break;
			case QUERY_NOT_SET:
				break;
			default:
				queryBody = null;
		}
		return queryBody;
	}

	private String legacyKeyForTxn(TransactionBody txn) {
		String key = "";
		if (txn.hasCryptoCreateAccount()) {
			key = "createAccount";
		} else if (txn.hasCryptoTransfer()) {
			key = "cryptoTransfer";
		} else if (txn.hasCryptoUpdateAccount()) {
			key = "updateAccount";
		} else if (txn.hasCryptoDelete()) {
			key = "cryptoDelete";
		} else if (txn.hasCryptoAddLiveHash()) {
			key = "addLiveHash";
		} else if (txn.hasCryptoDeleteLiveHash()) {
			key = "deleteLiveHash";
		} else if (txn.hasFileCreate()) {
			key = "createFile";
		} else if (txn.hasFileUpdate()) {
			key = "updateFile";
		} else if (txn.hasFileDelete()) {
			key = "deleteFile";
		} else if (txn.hasFileAppend()) {
			key = "appendContent";
		} else if (txn.hasContractCreateInstance()) {
			key = "createContract";
		} else if (txn.hasContractUpdateInstance()) {
			key = "updateContract";
		} else if (txn.hasContractCall()) {
			key = "contractCallMethod";
		} else if (txn.hasSystemDelete()) {
			key = "systemDelete";
		} else if (txn.hasSystemUndelete()) {
			key = "systemUndelete";
		} else if (txn.hasContractDeleteInstance()) {
			key = "deleteContract";
		} else if (txn.hasFreeze()) {
			key = "freeze";
		} else if (txn.hasConsensusCreateTopic()) {
			key = "createTopic";
		} else if (txn.hasConsensusUpdateTopic()) {
			key = "updateTopic";
		} else if (txn.hasConsensusDeleteTopic()) {
			key = "deleteTopic";
		} else if (txn.hasScheduleCreate()) {
			key = "scheduleCreate";
		} else if (txn.hasScheduleDelete()) {
			key = "scheduleDelete";
		} else if (txn.hasScheduleSign()) {
			key = "scheduleSign";
		} else if (txn.hasConsensusSubmitMessage()) {
			key = "submitMessage";
		}
		return key;
	}
}
