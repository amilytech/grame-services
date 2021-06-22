package com.grame.services.state.submerkle;

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
import com.grame.services.state.merkle.MerkleEntityId;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.ContractID;
import com.gramegrame.api.proto.java.FileID;
import com.gramegrame.api.proto.java.ScheduleID;
import com.gramegrame.api.proto.java.TokenID;
import com.gramegrame.api.proto.java.TopicID;
import com.swirlds.common.io.SelfSerializable;
import com.swirlds.common.io.SerializableDataInputStream;
import com.swirlds.common.io.SerializableDataOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Objects;

public class EntityId implements SelfSerializable {
	private static final Logger log = LogManager.getLogger(EntityId.class);

	static final int MERKLE_VERSION = 1;
	static final long RUNTIME_CONSTRUCTABLE_ID = 0xf35ba643324efa37L;

	public static final EntityId MISSING_ENTITY_ID = new EntityId(0, 0, 0);

	private long shard;
	private long realm;
	private long num;

	public EntityId() { }

	public EntityId(long shard, long realm, long num) {
		this.shard = shard;
		this.realm = realm;
		this.num = num;
	}

	public EntityId(EntityId that) {
		this(that.shard, that.realm, that.num);
	}

	/* --- SelfSerializable --- */

	@Override
	public long getClassId() {
		return RUNTIME_CONSTRUCTABLE_ID;
	}

	@Override
	public int getVersion() {
		return MERKLE_VERSION;
	}

	@Override
	public void deserialize(SerializableDataInputStream in, int version) throws IOException {
try {
		shard = in.readLong();
		realm = in.readLong();
		num = in.readLong();
} catch (Throwable t123) {
    t123.printStackTrace();
    throw t123;
}
	}

	@Override
	public void serialize(SerializableDataOutputStream out) throws IOException {
		out.writeLong(shard);
		out.writeLong(realm);
		out.writeLong(num);
	}

	/* --- Object --- */

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || EntityId.class != o.getClass()) {
			return false;
		}
		EntityId that = (EntityId)o;
		return shard == that.shard && realm == that.realm && num == that.num;
	}

	@Override
	public int hashCode() {
		return Objects.hash(shard, realm, num);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("shard", shard)
				.add("realm", realm)
				.add("num", num)
				.toString();
	}

	public String toAbbrevString() {
		return String.format("%d.%d.%d", shard, realm, num);
	}

	public EntityId copy() {
		return new EntityId(this);
	}

	public long shard() {
		return shard;
	}

	public long realm() {
		return realm;
	}

	public long num() {
		return num;
	}

	/* --- Helpers --- */

	public static EntityId ofNullableAccountId(AccountID accountId) {
		return (accountId == null )
				? null
				: new EntityId(accountId.getShardNum(), accountId.getRealmNum(), accountId.getAccountNum());
	}

	public static EntityId ofNullableFileId(FileID fileId) {
		return (fileId == null )
				? null
				: new EntityId(fileId.getShardNum(), fileId.getRealmNum(), fileId.getFileNum());
	}

	public static EntityId ofNullableTopicId(TopicID topicId) {
		return (topicId == null )
				? null
				: new EntityId(topicId.getShardNum(), topicId.getRealmNum(), topicId.getTopicNum());
	}

	public static EntityId ofNullableTokenId(TokenID tokenId) {
		return (tokenId == null )
				? null
				: new EntityId(tokenId.getShardNum(), tokenId.getRealmNum(), tokenId.getTokenNum());
	}

	public static EntityId ofNullableScheduleId(ScheduleID scheduleID) {
		return (scheduleID == null )
				? null
				: new EntityId(scheduleID.getShardNum(), scheduleID.getRealmNum(), scheduleID.getScheduleNum());
	}

	public static EntityId ofNullableContractId(ContractID contractId) {
		return (contractId == null )
				? null
				: new EntityId(contractId.getShardNum(), contractId.getRealmNum(), contractId.getContractNum());
	}

	public ContractID toGrpcContractId() {
		return ContractID.newBuilder()
				.setShardNum(shard)
				.setRealmNum(realm)
				.setContractNum(num)
				.build();
	}

	public TokenID toGrpcTokenId() {
		return TokenID.newBuilder()
				.setShardNum(shard)
				.setRealmNum(realm)
				.setTokenNum(num)
				.build();
	}

	public ScheduleID toGrpcScheduleId() {
		return ScheduleID.newBuilder()
				.setShardNum(shard)
				.setRealmNum(realm)
				.setScheduleNum(num)
				.build();
	}

	public AccountID toGrpcAccountId() {
		return AccountID.newBuilder()
				.setShardNum(shard)
				.setRealmNum(realm)
				.setAccountNum(num)
				.build();
	}

	public MerkleEntityId asMerkle() {
		return new MerkleEntityId(shard, realm, num);
	}
}
