package com.grame.services.state.merkle;

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

import com.google.common.base.MoreObjects;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.grame.services.legacy.core.jproto.JKey;
import com.grame.services.state.serdes.DomainSerdes;
import com.grame.services.state.submerkle.EntityId;
import com.grame.services.state.submerkle.RichInstant;
import com.grame.services.utils.MiscUtils;
import com.gramegrame.api.proto.java.Key;
import com.gramegrame.api.proto.java.SchedulableTransactionBody;
import com.gramegrame.api.proto.java.SignedTransaction;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.Transaction;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionID;
import com.swirlds.common.FCMValue;
import com.swirlds.common.io.SerializableDataInputStream;
import com.swirlds.common.io.SerializableDataOutputStream;
import com.swirlds.common.merkle.utility.AbstractMerkleLeaf;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.protobuf.ByteString.copyFrom;
import static com.grame.services.utils.MiscUtils.asOrdinary;
import static com.grame.services.utils.MiscUtils.asTimestamp;
import static com.grame.services.utils.MiscUtils.describe;
import static com.swirlds.common.CommonUtils.hex;
import static java.util.stream.Collectors.toList;

public class MerkleSchedule extends AbstractMerkleLeaf implements FCMValue {
	static final int MERKLE_VERSION = 1;

	static final int NUM_ED25519_PUBKEY_BYTES = 32;

	static final long RUNTIME_CONSTRUCTABLE_ID = 0x8d2b7d9e673285fcL;
	static DomainSerdes serdes = new DomainSerdes();

	public static final Key UNUSED_GRPC_KEY = null;
	public static final JKey UNUSED_KEY = null;
	public static final EntityId UNUSED_PAYER = null;
	public static final RichInstant UNRESOLVED_TIME = null;

	private Key grpcAdminKey = UNUSED_GRPC_KEY;
	private JKey adminKey = UNUSED_KEY;
	private String memo;
	private boolean deleted = false;
	private boolean executed = false;
	private EntityId payer = UNUSED_PAYER;
	private EntityId schedulingAccount;
	private RichInstant schedulingTXValidStart;
	private long expiry;
	private RichInstant resolutionTime = UNRESOLVED_TIME;

	private byte[] bodyBytes;
	private TransactionBody ordinaryScheduledTxn;
	private SchedulableTransactionBody scheduledTxn;

	private Set<ByteString> notary = ConcurrentHashMap.newKeySet();
	private List<byte[]> signatories = new ArrayList<>();

	public MerkleSchedule() {
	}

	public static MerkleSchedule from(byte[] bodyBytes, long consensusExpiry) {
		var to = new MerkleSchedule();
		to.expiry = consensusExpiry;
		to.bodyBytes = bodyBytes;
		to.initFromBodyBytes();

		return to;
	}

	/* Notary functions */
	public boolean witnessValidEd25519Signature(byte[] key) {
		var usableKey = copyFrom(key);
		if (notary.contains(usableKey)) {
			return false;
		} else {
			signatories.add(key);
			notary.add(usableKey);
			return true;
		}
	}

	public Transaction asSignedTxn() {
		return Transaction.newBuilder()
				.setSignedTransactionBytes(
						SignedTransaction.newBuilder()
								.setBodyBytes(
										TransactionBody.newBuilder()
												.mergeFrom(ordinaryScheduledTxn)
												.setTransactionID(scheduledTransactionId())
												.build()
												.toByteString())
								.build()
								.toByteString())
				.build();
	}

	public TransactionID scheduledTransactionId() {
		if (schedulingAccount == null || schedulingTXValidStart == null) {
			throw new IllegalStateException("Cannot invoke scheduledTransactionId on a content-addressable view!");
		}
		return TransactionID.newBuilder()
				.setAccountID(schedulingAccount.toGrpcAccountId())
				.setTransactionValidStart(asTimestamp(schedulingTXValidStart.toJava()))
				.setScheduled(true)
				.build();
	}

	public boolean hasValidEd25519Signature(byte[] key) {
		return notary.contains(copyFrom(key));
	}

	/* Object */

	/**
	 * Two {@code MerkleSchedule}s are identical as long as they agree on
	 * the transaction being scheduled, the admin key used to manage it,
	 * and the memo to accompany it.
	 *
	 * @param o the object to check for equality
	 * @return whether {@code this} and {@code o} are identical
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || MerkleSchedule.class != o.getClass()) {
			return false;
		}

		var that = (MerkleSchedule) o;
		return Objects.equals(this.memo, that.memo) &&
				Objects.equals(this.scheduledTxn, that.scheduledTxn) &&
				Objects.equals(this.grpcAdminKey, that.grpcAdminKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(memo, grpcAdminKey, scheduledTxn);
	}

	@Override
	public String toString() {
		var helper = MoreObjects.toStringHelper(MerkleSchedule.class)
				.add("scheduledTxn", scheduledTxn)
				.add("expiry", expiry)
				.add("executed", executed)
				.add("deleted", deleted)
				.add("memo", memo)
				.add("payer", readablePayer())
				.add("schedulingAccount", schedulingAccount)
				.add("schedulingTXValidStart", schedulingTXValidStart)
				.add("signatories", signatories.stream().map(Hex::encodeHexString).collect(toList()))
				.add("adminKey", describe(adminKey));
		if (resolutionTime != null) {
			helper.add("resolutionTime", resolutionTime);
		}
		return helper.toString();
	}

	private String readablePayer() {
		return Optional.ofNullable(effectivePayer()).map(EntityId::toAbbrevString).orElse("<N/A>");
	}

	@Override
	public void deserialize(SerializableDataInputStream in, int version) throws IOException {
try {
		expiry = in.readLong();
		bodyBytes = in.readByteArray(Integer.MAX_VALUE);
		executed = in.readBoolean();
		deleted = in.readBoolean();
		resolutionTime = serdes.readNullableInstant(in);
		int numSignatories = in.readInt();
		while (numSignatories-- > 0) {
			witnessValidEd25519Signature(in.readByteArray(NUM_ED25519_PUBKEY_BYTES));
		}

		initFromBodyBytes();
} catch (Throwable t123) {
    t123.printStackTrace();
    throw t123;
}
	}

	@Override
	public void serialize(SerializableDataOutputStream out) throws IOException {
		out.writeLong(expiry);
		out.writeByteArray(bodyBytes);
		out.writeBoolean(executed);
		out.writeBoolean(deleted);
		serdes.writeNullableInstant(resolutionTime, out);
		out.writeInt(signatories.size());
		for (byte[] key : signatories) {
			out.writeByteArray(key);
		}
	}

	@Override
	public long getClassId() {
		return RUNTIME_CONSTRUCTABLE_ID;
	}

	@Override
	public int getVersion() {
		return MERKLE_VERSION;
	}

	@Override
	public MerkleSchedule copy() {
		var fc = new MerkleSchedule();

		/* These fields are all immutable or effectively immutable, we can share them between copies */
		fc.grpcAdminKey = grpcAdminKey;
		fc.adminKey = adminKey;
		fc.memo = memo;
		fc.deleted = deleted;
		fc.executed = executed;
		fc.payer = payer;
		fc.schedulingAccount = schedulingAccount;
		fc.schedulingTXValidStart = schedulingTXValidStart;
		fc.expiry = expiry;
		fc.bodyBytes = bodyBytes;
		fc.scheduledTxn = scheduledTxn;
		fc.ordinaryScheduledTxn = ordinaryScheduledTxn;
		fc.resolutionTime = resolutionTime;

		/* Signatories are mutable */
		for (byte[] signatory : signatories) {
			fc.witnessValidEd25519Signature(signatory);
		}

		return fc;
	}

	public MerkleSchedule toContentAddressableView() {
		var cav = new MerkleSchedule();

		cav.memo = memo;
		cav.grpcAdminKey = grpcAdminKey;
		cav.scheduledTxn = scheduledTxn;

		return cav;
	}

	public Optional<String> memo() {
		return Optional.ofNullable(this.memo);
	}

	public void setMemo(String memo) {
		this.memo = memo;
	}

	public boolean hasAdminKey() {
		return adminKey != UNUSED_KEY;
	}

	public Optional<JKey> adminKey() {
		return Optional.ofNullable(adminKey);
	}

	public void setAdminKey(JKey adminKey) {
		this.adminKey = adminKey;
	}

	public void setPayer(EntityId payer) {
		this.payer = payer;
	}

	public EntityId payer() {
		return payer;
	}

	public EntityId effectivePayer() {
		return hasExplicitPayer() ? payer : schedulingAccount;
	}

	public boolean hasExplicitPayer() {
		return payer != UNUSED_PAYER;
	}

	public EntityId schedulingAccount() {
		return schedulingAccount;
	}

	public RichInstant schedulingTXValidStart() {
		return this.schedulingTXValidStart;
	}

	public List<byte[]> signatories() {
		return signatories;
	}

	public void setExpiry(long expiry) {
		this.expiry = expiry;
	}

	public long expiry() {
		return expiry;
	}

	public void markDeleted(Instant at) {
		resolutionTime = RichInstant.fromJava(at);
		deleted = true;
	}

	public void markExecuted(Instant at) {
		resolutionTime = RichInstant.fromJava(at);
		executed = true;
	}

	public boolean isExecuted() {
		return executed;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public Timestamp deletionTime() {
		if (!deleted) {
			throw new IllegalStateException("Schedule not deleted, cannot return deletion time!");
		}
		return resolutionTime.toGrpc();
	}

	public Timestamp executionTime() {
		if (!executed) {
			throw new IllegalStateException("Schedule not executed, cannot return execution time!");
		}
		return resolutionTime.toGrpc();
	}

	public TransactionBody ordinaryViewOfScheduledTxn() {
		return ordinaryScheduledTxn;
	}

	public SchedulableTransactionBody scheduledTxn() {
		return scheduledTxn;
	}

	public byte[] bodyBytes() {
		return bodyBytes;
	}

	public Key grpcAdminKey() {
		return grpcAdminKey;
	}

	private void initFromBodyBytes() {
		try {
			var parentTxn = TransactionBody.parseFrom(bodyBytes);
			var creationOp = parentTxn.getScheduleCreate();

			if (!creationOp.getMemo().isEmpty()) {
				memo = creationOp.getMemo();
			}
			if (creationOp.hasPayerAccountID()) {
				payer = EntityId.ofNullableAccountId(creationOp.getPayerAccountID());
			}
			if (creationOp.hasAdminKey()) {
				MiscUtils.asUsableFcKey(creationOp.getAdminKey()).ifPresent(this::setAdminKey);
				if (adminKey != UNUSED_KEY) {
					grpcAdminKey = creationOp.getAdminKey();
				}
			}
			scheduledTxn = parentTxn.getScheduleCreate().getScheduledTransactionBody();
			schedulingAccount = EntityId.ofNullableAccountId(parentTxn.getTransactionID().getAccountID());
			ordinaryScheduledTxn = MiscUtils.asOrdinary(scheduledTxn);
			schedulingTXValidStart = RichInstant.fromGrpc(parentTxn.getTransactionID().getTransactionValidStart());
		} catch (InvalidProtocolBufferException e) {
			throw new IllegalArgumentException(String.format(
					"Argument bodyBytes=0x%s was not a TransactionBody!", Hex.encodeHexString(bodyBytes)));
		}
	}
}
