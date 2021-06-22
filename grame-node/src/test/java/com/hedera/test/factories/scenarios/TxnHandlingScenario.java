package com.grame.test.factories.scenarios;

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

import com.grame.services.legacy.unit.serialization.HFileMetaSerdeTest;
import com.grame.services.state.merkle.MerkleSchedule;
import com.grame.services.state.merkle.MerkleScheduleTest;
import com.grame.services.state.merkle.MerkleToken;
import com.grame.services.state.merkle.MerkleTopic;
import com.grame.services.files.grameFs;
import com.grame.services.state.submerkle.EntityId;
import com.grame.services.state.submerkle.RichInstant;
import com.grame.services.store.schedule.ScheduleStore;
import com.grame.services.store.tokens.TokenStore;
import com.grame.services.utils.MiscUtils;
import com.grame.services.utils.PlatformTxnAccessor;
import com.grame.test.factories.keys.KeyFactory;
import com.grame.test.factories.keys.KeyTree;
import com.grame.test.factories.keys.OverlappingKeyGenerator;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.ContractID;
import com.gramegrame.api.proto.java.Duration;
import com.gramegrame.api.proto.java.FileGetInfoResponse;
import com.gramegrame.api.proto.java.FileID;
import com.gramegrame.api.proto.java.Key;
import com.gramegrame.api.proto.java.ScheduleID;
import com.gramegrame.api.proto.java.Timestamp;
import com.gramegrame.api.proto.java.TokenID;
import com.gramegrame.api.proto.java.TopicID;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.state.merkle.MerkleBlobMeta;
import com.grame.services.state.merkle.MerkleOptionalBlob;
import com.gramegrame.api.proto.java.TransactionBody;
import com.swirlds.fcmap.FCMap;

import java.time.Instant;

import static com.grame.test.factories.keys.KeyTree.withRoot;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static com.grame.test.factories.accounts.MockFCMapFactory.newAccounts;
import static com.grame.test.utils.IdUtils.*;
import static com.grame.test.factories.txns.SignedTxnFactory.*;
import static com.grame.test.factories.accounts.MerkleAccountFactory.newAccount;
import static com.grame.test.factories.accounts.MerkleAccountFactory.newContract;
import static com.grame.test.factories.keys.NodeFactory.*;

public interface TxnHandlingScenario {
	PlatformTxnAccessor platformTxn() throws Throwable;

	KeyFactory overlapFactory = new KeyFactory(OverlappingKeyGenerator.withDefaultOverlaps());

	default FCMap<MerkleEntityId, MerkleAccount> accounts() throws Exception {
		return newAccounts()
				.withAccount(FIRST_TOKEN_SENDER_ID,
						newAccount()
								.balance(10_000L)
								.accountKeys(FIRST_TOKEN_SENDER_KT).get())
				.withAccount(SECOND_TOKEN_SENDER_ID,
						newAccount()
								.balance(10_000L)
								.accountKeys(SECOND_TOKEN_SENDER_KT).get())
				.withAccount(TOKEN_RECEIVER_ID,
						newAccount()
								.balance(0L).get())
				.withAccount(DEFAULT_NODE_ID,
						newAccount()
								.balance(0L)
								.accountKeys(DEFAULT_PAYER_KT).get())
				.withAccount(
						DEFAULT_PAYER_ID,
						newAccount()
								.balance(DEFAULT_PAYER_BALANCE)
								.accountKeys(DEFAULT_PAYER_KT).get())
				.withAccount(
						MASTER_PAYER_ID,
						newAccount()
								.balance(DEFAULT_PAYER_BALANCE)
								.accountKeys(DEFAULT_PAYER_KT).get())
				.withAccount(
						TREASURY_PAYER_ID,
						newAccount()
								.balance(DEFAULT_PAYER_BALANCE)
								.accountKeys(DEFAULT_PAYER_KT).get())
				.withAccount(
						NO_RECEIVER_SIG_ID,
						newAccount()
								.receiverSigRequired(false)
								.balance(DEFAULT_BALANCE)
								.accountKeys(NO_RECEIVER_SIG_KT).get()
				).withAccount(
						RECEIVER_SIG_ID,
						newAccount()
								.receiverSigRequired(true)
								.balance(DEFAULT_BALANCE)
								.accountKeys(RECEIVER_SIG_KT).get()
				).withAccount(
						MISC_ACCOUNT_ID,
						newAccount()
								.balance(DEFAULT_BALANCE)
								.accountKeys(MISC_ACCOUNT_KT).get()
				).withAccount(
						COMPLEX_KEY_ACCOUNT_ID,
						newAccount()
								.balance(DEFAULT_BALANCE)
								.accountKeys(COMPLEX_KEY_ACCOUNT_KT).get()
				).withAccount(
						TOKEN_TREASURY_ID,
						newAccount()
								.balance(DEFAULT_BALANCE)
								.accountKeys(TOKEN_TREASURY_KT).get()
				).withAccount(
						DILIGENT_SIGNING_PAYER_ID,
						newAccount()
								.balance(DEFAULT_BALANCE)
								.accountKeys(DILIGENT_SIGNING_PAYER_KT).get()
				).withAccount(
						FROM_OVERLAP_PAYER_ID,
						newAccount()
								.balance(DEFAULT_BALANCE)
								.keyFactory(overlapFactory)
								.accountKeys(FROM_OVERLAP_PAYER_KT).get()
				).withContract(
						MISC_RECIEVER_SIG_CONTRACT_ID,
						newContract()
								.receiverSigRequired(true)
								.balance(DEFAULT_BALANCE)
								.accountKeys(DILIGENT_SIGNING_PAYER_KT).get()
				).withContract(
						MISC_CONTRACT_ID,
						newContract()
								.balance(DEFAULT_BALANCE)
								.accountKeys(MISC_ADMIN_KT).get()
				).get();
	}

	default grameFs hfs() throws Exception {
		grameFs hfs = mock(grameFs.class);
		given(hfs.exists(MISC_FILE)).willReturn(true);
		given(hfs.exists(SYS_FILE)).willReturn(true);
		given(hfs.getattr(MISC_FILE)).willReturn(HFileMetaSerdeTest.convert(MISC_FILE_INFO));
		given(hfs.getattr(SYS_FILE)).willReturn(HFileMetaSerdeTest.convert(SYS_FILE_INFO));
		given(hfs.exists(IMMUTABLE_FILE)).willReturn(true);
		given(hfs.getattr(IMMUTABLE_FILE)).willReturn(HFileMetaSerdeTest.convert(IMMUTABLE_FILE_INFO));
		return hfs;
	}

	default FCMap<MerkleBlobMeta, MerkleOptionalBlob> storage() {
		@SuppressWarnings("unchecked")
		FCMap<MerkleBlobMeta, MerkleOptionalBlob> storage = (FCMap<MerkleBlobMeta, MerkleOptionalBlob>) mock(
				FCMap.class);

		return storage;
	}

	default FCMap<MerkleEntityId, MerkleTopic> topics() {
		var topics = (FCMap<MerkleEntityId, MerkleTopic>) mock(FCMap.class);
		given(topics.get(EXISTING_TOPIC)).willReturn(new MerkleTopic());
		return topics;
	}

	default TokenStore tokenStore() {
		var tokenStore = mock(TokenStore.class);

		var adminKey = TOKEN_ADMIN_KT.asJKeyUnchecked();
		var optionalKycKey = TOKEN_KYC_KT.asJKeyUnchecked();
		var optionalWipeKey = TOKEN_WIPE_KT.asJKeyUnchecked();
		var optionalSupplyKey = TOKEN_SUPPLY_KT.asJKeyUnchecked();
		var optionalFreezeKey = TOKEN_FREEZE_KT.asJKeyUnchecked();

		var immutableToken = new MerkleToken(
				Long.MAX_VALUE, 100, 1,
				"ImmutableToken", "ImmutableTokenName", false, false,
				new EntityId(1, 2, 3));
		given(tokenStore.resolve(KNOWN_TOKEN_IMMUTABLE))
				.willReturn(KNOWN_TOKEN_IMMUTABLE);
		given(tokenStore.get(KNOWN_TOKEN_IMMUTABLE)).willReturn(immutableToken);

		var vanillaToken = new MerkleToken(
				Long.MAX_VALUE, 100, 1,
				"VanillaToken", "TOKENNAME", false, false,
				new EntityId(1, 2, 3));
		vanillaToken.setAdminKey(adminKey);
		given(tokenStore.resolve(KNOWN_TOKEN_NO_SPECIAL_KEYS))
				.willReturn(KNOWN_TOKEN_NO_SPECIAL_KEYS);
		given(tokenStore.get(KNOWN_TOKEN_NO_SPECIAL_KEYS)).willReturn(vanillaToken);

		var frozenToken = new MerkleToken(
				Long.MAX_VALUE, 100, 1,
				"FrozenToken", "FRZNTKN", true, false,
				new EntityId(1, 2, 4));
		frozenToken.setAdminKey(adminKey);
		frozenToken.setFreezeKey(optionalFreezeKey);
		given(tokenStore.resolve(KNOWN_TOKEN_WITH_FREEZE))
				.willReturn(KNOWN_TOKEN_WITH_FREEZE);
		given(tokenStore.get(KNOWN_TOKEN_WITH_FREEZE)).willReturn(frozenToken);

		var kycToken = new MerkleToken(
				Long.MAX_VALUE, 100, 1,
				"KycToken", "KYCTOKENNAME", false, true,
				new EntityId(1, 2, 4));
		kycToken.setAdminKey(adminKey);
		kycToken.setKycKey(optionalKycKey);
		given(tokenStore.resolve(KNOWN_TOKEN_WITH_KYC))
				.willReturn(KNOWN_TOKEN_WITH_KYC);
		given(tokenStore.get(KNOWN_TOKEN_WITH_KYC)).willReturn(kycToken);

		var supplyToken = new MerkleToken(
				Long.MAX_VALUE, 100, 1,
				"SupplyToken", "SUPPLYTOKENNAME", false, false,
				new EntityId(1, 2, 4));
		supplyToken.setAdminKey(adminKey);
		supplyToken.setSupplyKey(optionalSupplyKey);
		given(tokenStore.resolve(KNOWN_TOKEN_WITH_SUPPLY))
				.willReturn(KNOWN_TOKEN_WITH_SUPPLY);
		given(tokenStore.get(KNOWN_TOKEN_WITH_SUPPLY)).willReturn(supplyToken);

		var wipeToken = new MerkleToken(
				Long.MAX_VALUE, 100, 1,
				"WipeToken", "WIPETOKENNAME", false, false,
				new EntityId(1, 2, 4));
		wipeToken.setAdminKey(adminKey);
		wipeToken.setWipeKey(optionalWipeKey);
		given(tokenStore.resolve(KNOWN_TOKEN_WITH_WIPE))
				.willReturn(KNOWN_TOKEN_WITH_WIPE);
		given(tokenStore.get(KNOWN_TOKEN_WITH_WIPE)).willReturn(wipeToken);

		given(tokenStore.resolve(UNKNOWN_TOKEN))
				.willReturn(TokenStore.MISSING_TOKEN);

		return tokenStore;
	}

	default byte[] extantSchedulingBodyBytes() throws Throwable {
		return MerkleScheduleTest.scheduleCreateTxnWith(
				Key.getDefaultInstance(),
				"",
				MISC_ACCOUNT,
				MISC_ACCOUNT,
				MiscUtils.asTimestamp(Instant.ofEpochSecond(1L))
		)
				.toByteArray();
	}

	default ScheduleStore scheduleStore() {
		var scheduleStore = mock(ScheduleStore.class);

		given(scheduleStore.resolve(KNOWN_SCHEDULE_IMMUTABLE))
				.willReturn(KNOWN_SCHEDULE_IMMUTABLE);
		given(scheduleStore.get(KNOWN_SCHEDULE_IMMUTABLE))
				.willAnswer(inv -> {
					var entity = MerkleSchedule.from(extantSchedulingBodyBytes(), 1801L);
					entity.setPayer(MerkleSchedule.UNUSED_PAYER);
					return entity;
				});

		given(scheduleStore.resolve(KNOWN_SCHEDULE_WITH_ADMIN))
				.willReturn(KNOWN_SCHEDULE_WITH_ADMIN);
		given(scheduleStore.get(KNOWN_SCHEDULE_WITH_ADMIN))
				.willAnswer(inv -> {
					var adminKey = SCHEDULE_ADMIN_KT.asJKeyUnchecked();
					var entity = MerkleSchedule.from(extantSchedulingBodyBytes(), 1801L);
					entity.setPayer(MerkleSchedule.UNUSED_PAYER);
					entity.setAdminKey(adminKey);
					return entity;
				});

		given(scheduleStore.resolve(KNOWN_SCHEDULE_WITH_EXPLICIT_PAYER))
				.willReturn(KNOWN_SCHEDULE_WITH_EXPLICIT_PAYER);
		given(scheduleStore.get(KNOWN_SCHEDULE_WITH_EXPLICIT_PAYER))
				.willAnswer(inv -> {
					var entity = MerkleSchedule.from(extantSchedulingBodyBytes(), 1801L);
					entity.setPayer(EntityId.ofNullableAccountId(DILIGENT_SIGNING_PAYER));
					return entity;
				});

		given(scheduleStore.resolve(KNOWN_SCHEDULE_WITH_NOW_INVALID_PAYER))
				.willReturn(KNOWN_SCHEDULE_WITH_NOW_INVALID_PAYER);
		given(scheduleStore.get(KNOWN_SCHEDULE_WITH_NOW_INVALID_PAYER))
				.willAnswer(inv -> {
					var entity = MerkleSchedule.from(extantSchedulingBodyBytes(), 1801L);
					entity.setPayer(EntityId.ofNullableAccountId(MISSING_ACCOUNT));
					return entity;
				});

		given(scheduleStore.resolve(UNKNOWN_SCHEDULE))
				.willReturn(ScheduleStore.MISSING_SCHEDULE);

		return scheduleStore;
	}

	String MISSING_ACCOUNT_ID = "1.2.3";
	AccountID MISSING_ACCOUNT = asAccount(MISSING_ACCOUNT_ID);

	String NO_RECEIVER_SIG_ID = "0.0.1337";
	AccountID NO_RECEIVER_SIG = asAccount(NO_RECEIVER_SIG_ID);
	KeyTree NO_RECEIVER_SIG_KT = withRoot(ed25519());

	String RECEIVER_SIG_ID = "0.0.1338";
	AccountID RECEIVER_SIG = asAccount(RECEIVER_SIG_ID);
	KeyTree RECEIVER_SIG_KT = withRoot(list(ed25519(), threshold(1, ed25519(), ed25519())));

	String MISC_ACCOUNT_ID = "0.0.1339";
	AccountID MISC_ACCOUNT = asAccount(MISC_ACCOUNT_ID);
	KeyTree MISC_ACCOUNT_KT = withRoot(ed25519());

	String SYS_ACCOUNT_ID = "0.0.666";
	AccountID SYS_ACCOUNT = asAccount(SYS_ACCOUNT_ID);

	String DILIGENT_SIGNING_PAYER_ID = "0.0.1340";
	AccountID DILIGENT_SIGNING_PAYER = asAccount(DILIGENT_SIGNING_PAYER_ID);
	KeyTree DILIGENT_SIGNING_PAYER_KT = withRoot(threshold(2, ed25519(true), ed25519(true), ed25519(false)));

	String TOKEN_TREASURY_ID = "0.0.1341";
	AccountID TOKEN_TREASURY = asAccount(TOKEN_TREASURY_ID);
	KeyTree TOKEN_TREASURY_KT = withRoot(threshold(2, ed25519(false), ed25519(true), ed25519(false)));

	String COMPLEX_KEY_ACCOUNT_ID = "0.0.1342";
	AccountID COMPLEX_KEY_ACCOUNT = asAccount(COMPLEX_KEY_ACCOUNT_ID);
	KeyTree COMPLEX_KEY_ACCOUNT_KT = withRoot(
			list(
					ed25519(),
					threshold(1,
							list(list(ed25519(), ed25519()), ed25519()), ed25519()),
					ed25519(),
					list(
							threshold(2,
									ed25519(), ed25519(), ed25519()))));

	String FROM_OVERLAP_PAYER_ID = "0.0.1343";
	KeyTree FROM_OVERLAP_PAYER_KT = withRoot(threshold(2, ed25519(true), ed25519(true), ed25519(false)));

	KeyTree NEW_ACCOUNT_KT = withRoot(list(ed25519(), threshold(1, ed25519(), ed25519())));
	KeyTree SYS_ACCOUNT_KT =  withRoot(list(ed25519(), threshold(1, ed25519(), ed25519())));
	KeyTree LONG_THRESHOLD_KT = withRoot(threshold(1, ed25519(), ed25519(), ed25519(), ed25519()));

	String MISSING_FILE_ID = "1.2.3";
	FileID MISSING_FILE = asFile(MISSING_FILE_ID);

	String SYS_FILE_ID = "0.0.111";
	FileID SYS_FILE = asFile(SYS_FILE_ID);
	KeyTree SYS_FILE_WACL_KT = withRoot(list(ed25519()));
	FileGetInfoResponse.FileInfo SYS_FILE_INFO = FileGetInfoResponse.FileInfo.newBuilder()
			.setKeys(SYS_FILE_WACL_KT.asKey().getKeyList())
			.setFileID(SYS_FILE)
			.build();

	String MISC_FILE_ID = "0.0.2337";
	FileID MISC_FILE = asFile(MISC_FILE_ID);
	KeyTree MISC_FILE_WACL_KT = withRoot(list(ed25519()));
	FileGetInfoResponse.FileInfo MISC_FILE_INFO = FileGetInfoResponse.FileInfo.newBuilder()
			.setKeys(MISC_FILE_WACL_KT.asKey().getKeyList())
			.setFileID(MISC_FILE)
			.build();

	String IMMUTABLE_FILE_ID = "0.0.2338";
	FileID IMMUTABLE_FILE = asFile(IMMUTABLE_FILE_ID);
	FileGetInfoResponse.FileInfo IMMUTABLE_FILE_INFO = FileGetInfoResponse.FileInfo.newBuilder()
			.setFileID(IMMUTABLE_FILE)
			.build();

	KeyTree SIMPLE_NEW_WACL_KT = withRoot(list(ed25519()));

	String MISSING_CONTRACT_ID = "1.2.3";

	String MISC_RECIEVER_SIG_CONTRACT_ID = "0.0.7337";
	ContractID MISC_RECIEVER_SIG_CONTRACT = asContract(MISC_RECIEVER_SIG_CONTRACT_ID);

	String MISC_CONTRACT_ID = "0.0.3337";
	ContractID MISC_CONTRACT = asContract(MISC_CONTRACT_ID);
	KeyTree MISC_ADMIN_KT = withRoot(ed25519());

	KeyTree SIMPLE_NEW_ADMIN_KT = withRoot(ed25519());

	Long DEFAULT_BALANCE = 1_000L;
	Long DEFAULT_PAYER_BALANCE = 1_000_000_000_000L;

	String DEFAULT_MEMO = "This is something else.";
	Duration DEFAULT_PERIOD = Duration.newBuilder().setSeconds(1_000L).build();
	Timestamp DEFAULT_EXPIRY = Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1_000L + 86_400L).build();

	String EXISTING_TOPIC_ID = "0.0.7890";
	TopicID EXISTING_TOPIC = asTopic(EXISTING_TOPIC_ID);

	String MISSING_TOPIC_ID = "0.0.12121";
	TopicID MISSING_TOPIC = asTopic(MISSING_TOPIC_ID);

	String KNOWN_TOKEN_IMMUTABLE_ID = "0.0.534";
	TokenID KNOWN_TOKEN_IMMUTABLE = asToken(KNOWN_TOKEN_IMMUTABLE_ID);
	String KNOWN_TOKEN_NO_SPECIAL_KEYS_ID = "0.0.535";
	TokenID KNOWN_TOKEN_NO_SPECIAL_KEYS = asToken(KNOWN_TOKEN_NO_SPECIAL_KEYS_ID);
	String KNOWN_TOKEN_WITH_FREEZE_ID = "0.0.777";
	TokenID KNOWN_TOKEN_WITH_FREEZE = asToken(KNOWN_TOKEN_WITH_FREEZE_ID);
	String KNOWN_TOKEN_WITH_KYC_ID = "0.0.776";
	TokenID KNOWN_TOKEN_WITH_KYC = asToken(KNOWN_TOKEN_WITH_KYC_ID);
	String KNOWN_TOKEN_WITH_SUPPLY_ID = "0.0.775";
	TokenID KNOWN_TOKEN_WITH_SUPPLY = asToken(KNOWN_TOKEN_WITH_SUPPLY_ID);
	String KNOWN_TOKEN_WITH_WIPE_ID = "0.0.774";
	TokenID KNOWN_TOKEN_WITH_WIPE = asToken(KNOWN_TOKEN_WITH_WIPE_ID);

	String FIRST_TOKEN_SENDER_ID = "0.0.888";
	AccountID FIRST_TOKEN_SENDER = asAccount(FIRST_TOKEN_SENDER_ID);
	String SECOND_TOKEN_SENDER_ID = "0.0.999";
	AccountID SECOND_TOKEN_SENDER = asAccount(SECOND_TOKEN_SENDER_ID);
	String TOKEN_RECEIVER_ID = "0.0.1111";
	AccountID TOKEN_RECEIVER = asAccount(TOKEN_RECEIVER_ID);

	String UNKNOWN_TOKEN_ID = "0.0.666";
	TokenID UNKNOWN_TOKEN = asToken(UNKNOWN_TOKEN_ID);

	KeyTree FIRST_TOKEN_SENDER_KT = withRoot(ed25519());
	KeyTree SECOND_TOKEN_SENDER_KT = withRoot(ed25519());
	KeyTree TOKEN_ADMIN_KT = withRoot(ed25519());
	KeyTree TOKEN_FREEZE_KT = withRoot(ed25519());
	KeyTree TOKEN_SUPPLY_KT = withRoot(ed25519());
	KeyTree TOKEN_WIPE_KT = withRoot(ed25519());
	KeyTree TOKEN_KYC_KT = withRoot(ed25519());
	KeyTree TOKEN_REPLACE_KT = withRoot(ed25519());
	KeyTree MISC_TOPIC_SUBMIT_KT = withRoot(ed25519());
	KeyTree MISC_TOPIC_ADMIN_KT = withRoot(ed25519());
	KeyTree UPDATE_TOPIC_ADMIN_KT = withRoot(ed25519());

	String KNOWN_SCHEDULE_IMMUTABLE_ID = "0.0.789";
	ScheduleID KNOWN_SCHEDULE_IMMUTABLE = asSchedule(KNOWN_SCHEDULE_IMMUTABLE_ID);

	String KNOWN_SCHEDULE_WITH_ADMIN_ID = "0.0.456";
	ScheduleID KNOWN_SCHEDULE_WITH_ADMIN = asSchedule(KNOWN_SCHEDULE_WITH_ADMIN_ID);

	String KNOWN_SCHEDULE_WITH_PAYER_ID = "0.0.456456";
	ScheduleID KNOWN_SCHEDULE_WITH_EXPLICIT_PAYER = asSchedule(KNOWN_SCHEDULE_WITH_PAYER_ID);

	String KNOWN_SCHEDULE_WITH_NOW_INVALID_PAYER_ID = "0.0.654654";
	ScheduleID KNOWN_SCHEDULE_WITH_NOW_INVALID_PAYER = asSchedule(KNOWN_SCHEDULE_WITH_NOW_INVALID_PAYER_ID);

	String UNKNOWN_SCHEDULE_ID = "0.0.123";
	ScheduleID UNKNOWN_SCHEDULE = asSchedule(UNKNOWN_SCHEDULE_ID);

	KeyTree SCHEDULE_ADMIN_KT = withRoot(ed25519());
}
