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

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.grame.services.exceptions.UnknowngrameFunctionality;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.ScheduleID;
import com.gramegrame.api.proto.java.SignatureMap;
import com.gramegrame.api.proto.java.SignedTransaction;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionID;

import java.util.function.Function;

import static com.grame.services.legacy.proto.utils.CommonUtils.sha384HashOf;
import static com.grame.services.utils.MiscUtils.functionOf;
import static com.gramegrame.api.proto.java.grameFunctionality.NONE;

/**
 * Encapsulates access to several commonly referenced parts of a gRPC {@link Transaction}.
 *
 * @author AmilyTech
 */
public class SignedTxnAccessor implements TxnAccessor {
	private byte[] txnBytes;
	private byte[] backwardCompatibleSignedTxnBytes;
	private Transaction backwardCompatibleSignedTxn;
	private SignatureMap sigMap;
	private TransactionID txnId;
	private TransactionBody txn;
	private grameFunctionality function;
	private ByteString hash;

	static Function<TransactionBody, grameFunctionality> functionExtractor = txn -> {
		try {
			return functionOf(txn);
		} catch (UnknowngrameFunctionality ignore) {
			return NONE;
		}
	};

	public static SignedTxnAccessor uncheckedFrom(Transaction validSignedTxn) {
		try {
			return new SignedTxnAccessor(validSignedTxn);
		} catch (Exception impossible) { }
		return null;
	}

	public SignedTxnAccessor(byte[] backwardCompatibleSignedTxnBytes) throws InvalidProtocolBufferException {
		this.backwardCompatibleSignedTxnBytes = backwardCompatibleSignedTxnBytes;
		backwardCompatibleSignedTxn = Transaction.parseFrom(backwardCompatibleSignedTxnBytes);

		if (!backwardCompatibleSignedTxn.getSignedTransactionBytes().isEmpty()) {
			var signedTxn = SignedTransaction.parseFrom(backwardCompatibleSignedTxn.getSignedTransactionBytes());
			txnBytes = signedTxn.getBodyBytes().toByteArray();
			sigMap = signedTxn.getSigMap();
		} else {
			txnBytes = backwardCompatibleSignedTxn.getBodyBytes().toByteArray();
			sigMap = backwardCompatibleSignedTxn.getSigMap();
		}

		txn = TransactionBody.parseFrom(txnBytes);
		txnId = txn.getTransactionID();
		hash = sha384HashOf(backwardCompatibleSignedTxn);
	}

	public SignedTxnAccessor(Transaction backwardCompatibleSignedTxn) throws InvalidProtocolBufferException {
		this(backwardCompatibleSignedTxn.toByteArray());
	}

	public SignatureMap getSigMap() {
		return sigMap;
	}

	public grameFunctionality getFunction() {
		if (function == null) {
			function = functionExtractor.apply(getTxn());
		}
		return function;
	}

	public Transaction getSignedTxn4Log() {
		return backwardCompatibleSignedTxn;
	}

	public byte[] getTxnBytes() {
		return txnBytes;
	}

	public Transaction getBackwardCompatibleSignedTxn() {
		return backwardCompatibleSignedTxn;
	}

	public TransactionBody getTxn() {
		return txn;
	}

	public TransactionID getTxnId() {
		return txnId;
	}

	public AccountID getPayer() {
		return getTxnId().getAccountID();
	}

	public byte[] getBackwardCompatibleSignedTxnBytes() {
		return backwardCompatibleSignedTxnBytes;
	}

	public ByteString getHash() {
		return hash;
	}

	@Override
	public boolean canTriggerTxn() {
		return getTxn().hasScheduleCreate() || getTxn().hasScheduleSign();
	}

	public boolean isTriggeredTxn() { return false; }

	public ScheduleID getScheduleRef() {
		throw new UnsupportedOperationException("Only the TriggeredTxnAccessor implementation can refer to a schedule");
	}
}
