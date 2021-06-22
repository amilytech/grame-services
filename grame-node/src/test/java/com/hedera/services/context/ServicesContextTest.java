package com.grame.services.context;

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

import com.grame.services.ServicesState;
import com.grame.services.config.AccountNumbers;
import com.grame.services.config.EntityNumbers;
import com.grame.services.config.FileNumbers;
import com.grame.services.context.domain.security.HapiOpPermissions;
import com.grame.services.context.domain.trackers.ConsensusStatusCounts;
import com.grame.services.context.domain.trackers.IssEventInfo;
import com.grame.services.context.primitives.StateView;
import com.grame.services.context.properties.GlobalDynamicProperties;
import com.grame.services.context.properties.NodeLocalProperties;
import com.grame.services.context.properties.PropertySource;
import com.grame.services.context.properties.SemanticVersions;
import com.grame.services.context.properties.StandardizedPropertySources;
import com.grame.services.contracts.execution.SolidityLifecycle;
import com.grame.services.contracts.execution.TxnAwareSoliditySigsVerifier;
import com.grame.services.contracts.persistence.BlobStoragePersistence;
import com.grame.services.contracts.sources.BlobStorageSource;
import com.grame.services.contracts.sources.LedgerAccountsSource;
import com.grame.services.fees.AwareHbarCentExchange;
import com.grame.services.fees.StandardExemptions;
import com.grame.services.fees.TxnRateFeeMultiplierSource;
import com.grame.services.fees.calculation.AwareFcfsUsagePrices;
import com.grame.services.fees.calculation.UsageBasedFeeCalculator;
import com.grame.services.fees.charging.ItemizableFeeCharging;
import com.grame.services.fees.charging.TxnFeeChargingPolicy;
import com.grame.services.files.HFileMeta;
import com.grame.services.files.SysFileCallbacks;
import com.grame.services.files.TieredgrameFs;
import com.grame.services.files.interceptors.FeeSchedulesManager;
import com.grame.services.files.interceptors.ThrottleDefsManager;
import com.grame.services.files.interceptors.TxnAwareRatesManager;
import com.grame.services.files.interceptors.ValidatingCallbackInterceptor;
import com.grame.services.files.store.FcBlobsBytesStore;
import com.grame.services.grpc.NettyGrpcServerManager;
import com.grame.services.grpc.controllers.ConsensusController;
import com.grame.services.grpc.controllers.ContractController;
import com.grame.services.grpc.controllers.CryptoController;
import com.grame.services.grpc.controllers.FileController;
import com.grame.services.grpc.controllers.FreezeController;
import com.grame.services.grpc.controllers.NetworkController;
import com.grame.services.grpc.controllers.ScheduleController;
import com.grame.services.grpc.controllers.TokenController;
import com.grame.services.keys.CharacteristicsFactory;
import com.grame.services.keys.InHandleActivationHelper;
import com.grame.services.keys.LegacyEd25519KeyReader;
import com.grame.services.ledger.grameLedger;
import com.grame.services.ledger.accounts.BackingTokenRels;
import com.grame.services.ledger.accounts.FCMapBackingAccounts;
import com.grame.services.ledger.ids.SeqNoEntityIdSource;
import com.grame.services.legacy.handler.FreezeHandler;
import com.grame.services.legacy.handler.SmartContractRequestHandler;
import com.grame.services.legacy.handler.TransactionHandler;
import com.grame.services.legacy.services.state.AwareProcessLogic;
import com.grame.services.queries.answering.AnswerFunctions;
import com.grame.services.queries.answering.QueryResponseHelper;
import com.grame.services.queries.answering.StakedAnswerFlow;
import com.grame.services.queries.answering.ZeroStakeAnswerFlow;
import com.grame.services.queries.consensus.HcsAnswers;
import com.grame.services.queries.contract.ContractAnswers;
import com.grame.services.queries.crypto.CryptoAnswers;
import com.grame.services.queries.meta.MetaAnswers;
import com.grame.services.queries.schedule.ScheduleAnswers;
import com.grame.services.queries.token.TokenAnswers;
import com.grame.services.queries.validation.QueryFeeCheck;
import com.grame.services.records.RecordCache;
import com.grame.services.records.TxnAwareRecordsHistorian;
import com.grame.services.security.ops.SystemOpPolicies;
import com.grame.services.sigs.factories.SigFactoryCreator;
import com.grame.services.sigs.order.grameSigningOrder;
import com.grame.services.sigs.verification.PrecheckVerifier;
import com.grame.services.sigs.verification.SyncVerifier;
import com.grame.services.state.expiry.ExpiringCreations;
import com.grame.services.state.expiry.ExpiryManager;
import com.grame.services.state.exports.SignedStateBalancesExporter;
import com.grame.services.state.initialization.BackedSystemAccountsCreator;
import com.grame.services.state.initialization.HfsSystemFilesManager;
import com.grame.services.state.logic.AwareNodeDiligenceScreen;
import com.grame.services.state.logic.NetworkCtxManager;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.state.merkle.MerkleBlobMeta;
import com.grame.services.state.merkle.MerkleDiskFs;
import com.grame.services.state.merkle.MerkleEntityAssociation;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.state.merkle.MerkleNetworkContext;
import com.grame.services.state.merkle.MerkleOptionalBlob;
import com.grame.services.state.merkle.MerkleSchedule;
import com.grame.services.state.merkle.MerkleToken;
import com.grame.services.state.merkle.MerkleTokenRelStatus;
import com.grame.services.state.merkle.MerkleTopic;
import com.grame.services.state.migration.StdStateMigrations;
import com.grame.services.state.submerkle.ExchangeRates;
import com.grame.services.state.submerkle.RichInstant;
import com.grame.services.state.submerkle.SequenceNumber;
import com.grame.services.state.validation.BasedLedgerValidator;
import com.grame.services.stats.HapiOpCounters;
import com.grame.services.stats.MiscRunningAvgs;
import com.grame.services.stats.MiscSpeedometers;
import com.grame.services.stats.ServicesStatsManager;
import com.grame.services.store.schedule.ScheduleStore;
import com.grame.services.store.tokens.grameTokenStore;
import com.grame.services.store.tokens.TokenStore;
import com.grame.services.stream.RecordStreamManager;
import com.grame.services.stream.RecordsRunningHashLeaf;
import com.grame.services.throttling.HapiThrottling;
import com.grame.services.throttling.TransactionThrottling;
import com.grame.services.throttling.TxnAwareHandleThrottling;
import com.grame.services.txns.TransitionLogicLookup;
import com.grame.services.txns.submission.PlatformSubmissionManager;
import com.grame.services.txns.submission.TxnHandlerSubmissionFlow;
import com.grame.services.txns.submission.TxnResponseHelper;
import com.grame.services.txns.validation.ContextOptionValidator;
import com.grame.services.utils.SleepingPause;
import com.gramegrame.api.proto.java.AccountID;
import com.swirlds.common.Address;
import com.swirlds.common.AddressBook;
import com.swirlds.common.Console;
import com.swirlds.common.NodeId;
import com.swirlds.common.Platform;
import com.swirlds.common.PlatformStatus;
import com.swirlds.common.crypto.Cryptography;
import com.swirlds.common.crypto.Hash;
import com.swirlds.common.crypto.RunningHash;
import com.swirlds.fcmap.FCMap;
import org.ethereum.db.ServicesRepositoryRoot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static com.grame.services.stream.RecordStreamManagerTest.INITIAL_RANDOM_HASH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.inOrder;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.when;

public class ServicesContextTest {
	private final long id = 1L;
	private final NodeId nodeId = new NodeId(false, id);
	private static final String recordStreamDir = "somePath/recordStream";

	RichInstant consensusTimeOfLastHandledTxn = RichInstant.fromJava(Instant.now());
	Platform platform;
	SequenceNumber seqNo;
	ExchangeRates midnightRates;
	MerkleNetworkContext networkCtx;
	ServicesState state;
	Cryptography crypto;
	PropertySource properties;
	StandardizedPropertySources propertySources;
	FCMap<MerkleEntityId, MerkleTopic> topics;
	FCMap<MerkleEntityId, MerkleToken> tokens;
	FCMap<MerkleEntityId, MerkleAccount> accounts;
	FCMap<MerkleBlobMeta, MerkleOptionalBlob> storage;
	FCMap<MerkleEntityAssociation, MerkleTokenRelStatus> tokenAssociations;
	FCMap<MerkleEntityId, MerkleSchedule> schedules;

	@BeforeEach
	void setup() {
		topics = mock(FCMap.class);
		tokens = mock(FCMap.class);
		tokenAssociations = mock(FCMap.class);
		schedules = mock(FCMap.class);
		storage = mock(FCMap.class);
		accounts = mock(FCMap.class);
		seqNo = mock(SequenceNumber.class);
		midnightRates = mock(ExchangeRates.class);
		networkCtx = new MerkleNetworkContext(consensusTimeOfLastHandledTxn, seqNo, midnightRates);
		state = mock(ServicesState.class);
		given(state.networkCtx()).willReturn(networkCtx);
		given(state.accounts()).willReturn(accounts);
		given(state.storage()).willReturn(storage);
		given(state.topics()).willReturn(topics);
		given(state.tokens()).willReturn(tokens);
		given(state.tokenAssociations()).willReturn(tokenAssociations);
		given(state.scheduleTxs()).willReturn(schedules);
		crypto = mock(Cryptography.class);
		platform = mock(Platform.class);
		given(platform.getCryptography()).willReturn(crypto);
		properties = mock(PropertySource.class);
		propertySources = mock(StandardizedPropertySources.class);
		given(propertySources.asResolvingSource()).willReturn(properties);
	}

	@Test
	public void updatesStateAsExpected() {
		// setup:
		var newState = mock(ServicesState.class);
		var newAccounts = mock(FCMap.class);
		var newTopics = mock(FCMap.class);
		var newStorage = mock(FCMap.class);
		var newTokens = mock(FCMap.class);
		var newTokenRels = mock(FCMap.class);
		var newSchedules = mock(FCMap.class);

		given(newState.accounts()).willReturn(newAccounts);
		given(newState.topics()).willReturn(newTopics);
		given(newState.tokens()).willReturn(newTokens);
		given(newState.storage()).willReturn(newStorage);
		given(newState.tokenAssociations()).willReturn(newTokenRels);
		given(newState.scheduleTxs()).willReturn(newSchedules);
		// given:
		var subject = new ServicesContext(nodeId, platform, state, propertySources);
		// and:
		var accountsRef = subject.queryableAccounts();
		var topicsRef = subject.queryableTopics();
		var storageRef = subject.queryableStorage();
		var tokensRef = subject.queryableTokens();
		var tokenRelsRef = subject.queryableTokenAssociations();
		var schedulesRef = subject.queryableSchedules();

		// when:
		subject.update(newState);

		// then:
		assertSame(newState, subject.state);
		assertSame(accountsRef, subject.queryableAccounts());
		assertSame(topicsRef, subject.queryableTopics());
		assertSame(storageRef, subject.queryableStorage());
		assertSame(tokensRef, subject.queryableTokens());
		assertSame(tokenRelsRef, subject.queryableTokenAssociations());
		assertSame(schedulesRef, subject.queryableSchedules());
		// and:
		assertSame(newAccounts, subject.queryableAccounts().get());
		assertSame(newTopics, subject.queryableTopics().get());
		assertSame(newStorage, subject.queryableStorage().get());
		assertSame(newTokens, subject.queryableTokens().get());
		assertSame(newTokenRels, subject.queryableTokenAssociations().get());
		assertSame(newSchedules, subject.queryableSchedules().get());
	}

	@Test
	public void delegatesPrimitivesToState() {
		// setup:
		InOrder inOrder = inOrder(state);

		// given:
		var subject = new ServicesContext(nodeId, platform, state, propertySources);

		// when:
		subject.addressBook();
		var actualSeqNo = subject.seqNo();
		var actualMidnightRates = subject.midnightRates();
		var actualLastHandleTime = subject.consensusTimeOfLastHandledTxn();
		subject.topics();
		subject.storage();
		subject.accounts();

		// then:
		inOrder.verify(state).addressBook();
		assertEquals(seqNo, actualSeqNo);
		assertEquals(midnightRates, actualMidnightRates);
		assertEquals(consensusTimeOfLastHandledTxn.toJava(), actualLastHandleTime);
		inOrder.verify(state).topics();
		inOrder.verify(state).storage();
		inOrder.verify(state).accounts();
	}

	@Test
	public void hasExpectedNodeAccount() {
		// setup:
		Address address = mock(Address.class);
		AddressBook book = mock(AddressBook.class);

		given(address.getMemo()).willReturn("0.0.3");
		given(book.getAddress(1L)).willReturn(address);
		given(state.addressBook()).willReturn(book);

		// when:
		ServicesContext ctx = new ServicesContext(nodeId, platform, state, propertySources);

		// then:
		assertEquals(ctx.address(), address);
		assertEquals(AccountID.newBuilder().setAccountNum(3L).build(), ctx.nodeAccount());
	}

	@Test
	public void canOverrideLastHandledConsensusTime() {
		// given:
		Instant dataDrivenNow = Instant.now();
		ServicesContext ctx =
				new ServicesContext(
						nodeId,
						platform,
						state,
						propertySources);

		// when:
		ctx.updateConsensusTimeOfLastHandledTxn(dataDrivenNow);

		// then:
		assertEquals(dataDrivenNow, ctx.consensusTimeOfLastHandledTxn());
	}

	@Test
	public void hasExpectedConsole() {
		// setup:
		Console console = mock(Console.class);
		given(platform.createConsole(true)).willReturn(console);

		// when:
		ServicesContext ctx = new ServicesContext(nodeId, platform, state, propertySources);

		// then:
		assertEquals(console, ctx.console());
		assertNull(ctx.consoleOut());
	}

	@Test
	public void hasExpectedZeroStakeInfrastructure() {
		// setup:
		Address address = mock(Address.class);
		AddressBook book = mock(AddressBook.class);
		given(address.getMemo()).willReturn("0.0.3");
		given(address.getStake()).willReturn(0L);
		given(book.getAddress(1L)).willReturn(address);
		given(state.addressBook()).willReturn(book);

		// given:
		ServicesContext ctx = new ServicesContext(nodeId, platform, state, propertySources);

		// expect:
		assertEquals(ServicesNodeType.ZERO_STAKE_NODE, ctx.nodeType());
		// and:
		assertThat(ctx.answerFlow(), instanceOf(ZeroStakeAnswerFlow.class));
	}

	@Test
	public void rebuildsStoreViewsIfNonNull() {
		// setup:
		ScheduleStore scheduleStore = mock(ScheduleStore.class);
		TokenStore tokenStore = mock(TokenStore.class);

		// given:
		ServicesContext ctx = new ServicesContext(nodeId, platform, state, propertySources);

		// expect:
		assertDoesNotThrow(ctx::rebuildStoreViewsIfPresent);

		// and given:
		ctx.setTokenStore(tokenStore);
		ctx.setScheduleStore(scheduleStore);

		// when:
		ctx.rebuildStoreViewsIfPresent();

		// then:
		verify(tokenStore).rebuildViews();
		verify(scheduleStore).rebuildViews();
	}

	@Test
	public void rebuildsBackingAccountsIfNonNull() {
		// setup:
		BackingTokenRels tokenRels = mock(BackingTokenRels.class);
		FCMapBackingAccounts backingAccounts = mock(FCMapBackingAccounts.class);

		// given:
		ServicesContext ctx = new ServicesContext(nodeId, platform, state, propertySources);

		// expect:
		assertDoesNotThrow(ctx::rebuildBackingStoresIfPresent);

		// and given:
		ctx.setBackingAccounts(backingAccounts);
		ctx.setBackingTokenRels(tokenRels);

		// when:
		ctx.rebuildBackingStoresIfPresent();

		// then:
		verify(tokenRels).rebuildFromSources();
		verify(backingAccounts).rebuildFromSources();
	}

	@Test
	public void hasExpectedStakedInfrastructure() {
		// setup:
		Address address = mock(Address.class);
		AddressBook book = mock(AddressBook.class);
		given(address.getMemo()).willReturn("0.0.3");
		given(address.getStake()).willReturn(1_234_567L);
		given(book.getAddress(1L)).willReturn(address);
		given(state.addressBook()).willReturn(book);

		// given:
		ServicesContext ctx = new ServicesContext(nodeId, platform, state, propertySources);
		// and:
		ctx.platformStatus().set(PlatformStatus.DISCONNECTED);

		// expect:
		assertEquals(SleepingPause.SLEEPING_PAUSE, ctx.pause());
		assertEquals(PlatformStatus.DISCONNECTED, ctx.platformStatus().get());
		assertEquals(ctx.properties(), properties);
		assertEquals(ctx.propertySources(), propertySources);
		// and expect TDD:
		assertThat(ctx.hfs(), instanceOf(TieredgrameFs.class));
		assertThat(ctx.ids(), instanceOf(SeqNoEntityIdSource.class));
		assertThat(ctx.fees(), instanceOf(UsageBasedFeeCalculator.class));
		assertThat(ctx.grpc(), instanceOf(NettyGrpcServerManager.class));
		assertThat(ctx.ledger(), instanceOf(grameLedger.class));
		assertThat(ctx.txnCtx(), instanceOf(AwareTransactionContext.class));
		assertThat(ctx.keyOrder(), instanceOf(grameSigningOrder.class));
		assertThat(ctx.backedKeyOrder(), instanceOf(grameSigningOrder.class));
		assertThat(ctx.validator(), instanceOf(ContextOptionValidator.class));
		assertThat(ctx.hcsAnswers(), instanceOf(HcsAnswers.class));
		assertThat(ctx.issEventInfo(), instanceOf(IssEventInfo.class));
		assertThat(ctx.cryptoGrpc(), instanceOf(CryptoController.class));
		assertThat(ctx.answerFlow(), instanceOf(StakedAnswerFlow.class));
		assertThat(ctx.recordCache(), instanceOf(RecordCache.class));
		assertThat(ctx.topics(), instanceOf(FCMap.class));
		assertThat(ctx.storage(), instanceOf(FCMap.class));
		assertThat(ctx.metaAnswers(), instanceOf(MetaAnswers.class));
		assertThat(ctx.stateViews().get(), instanceOf(StateView.class));
		assertThat(ctx.fileNums(), instanceOf(FileNumbers.class));
		assertThat(ctx.accountNums(), instanceOf(AccountNumbers.class));
		assertThat(ctx.usagePrices(), instanceOf(AwareFcfsUsagePrices.class));
		assertThat(ctx.currentView(), instanceOf(StateView.class));
		assertThat(ctx.blobStore(), instanceOf(FcBlobsBytesStore.class));
		assertThat(ctx.entityExpiries(), instanceOf(Map.class));
		assertThat(ctx.syncVerifier(), instanceOf(SyncVerifier.class));
		assertThat(ctx.txnThrottling(), instanceOf(TransactionThrottling.class));
		assertThat(ctx.accountSource(), instanceOf(LedgerAccountsSource.class));
		assertThat(ctx.bytecodeDb(), instanceOf(BlobStorageSource.class));
		assertThat(ctx.cryptoAnswers(), instanceOf(CryptoAnswers.class));
		assertThat(ctx.tokenAnswers(), instanceOf(TokenAnswers.class));
		assertThat(ctx.scheduleAnswers(), instanceOf(ScheduleAnswers.class));
		assertThat(ctx.consensusGrpc(), instanceOf(ConsensusController.class));
		assertThat(ctx.storagePersistence(), instanceOf(BlobStoragePersistence.class));
		assertThat(ctx.filesGrpc(), instanceOf(FileController.class));
		assertThat(ctx.networkGrpc(), instanceOf(NetworkController.class));
		assertThat(ctx.entityNums(), instanceOf(EntityNumbers.class));
		assertThat(ctx.feeSchedulesManager(), instanceOf(FeeSchedulesManager.class));
		assertThat(ctx.submissionFlow(), instanceOf(TxnHandlerSubmissionFlow.class));
		assertThat(ctx.answerFunctions(), instanceOf(AnswerFunctions.class));
		assertThat(ctx.queryFeeCheck(), instanceOf(QueryFeeCheck.class));
		assertThat(ctx.queryableTopics(), instanceOf(AtomicReference.class));
		assertThat(ctx.transitionLogic(), instanceOf(TransitionLogicLookup.class));
		assertThat(ctx.precheckVerifier(), instanceOf(PrecheckVerifier.class));
		assertThat(ctx.apiPermissionsReloading(), instanceOf(ValidatingCallbackInterceptor.class));
		assertThat(ctx.applicationPropertiesReloading(), instanceOf(ValidatingCallbackInterceptor.class));
		assertThat(ctx.recordsHistorian(), instanceOf(TxnAwareRecordsHistorian.class));
		assertThat(ctx.queryableAccounts(), instanceOf(AtomicReference.class));
		assertThat(ctx.txnChargingPolicy(), instanceOf(TxnFeeChargingPolicy.class));
		assertThat(ctx.txnResponseHelper(), instanceOf(TxnResponseHelper.class));
		assertThat(ctx.statusCounts(), instanceOf(ConsensusStatusCounts.class));
		assertThat(ctx.queryableStorage(), instanceOf(AtomicReference.class));
		assertThat(ctx.systemFilesManager(), instanceOf(HfsSystemFilesManager.class));
		assertThat(ctx.queryResponseHelper(), instanceOf(QueryResponseHelper.class));
		assertThat(ctx.solidityLifecycle(), instanceOf(SolidityLifecycle.class));
		assertThat(ctx.charging(), instanceOf(ItemizableFeeCharging.class));
		assertThat(ctx.repository(), instanceOf(ServicesRepositoryRoot.class));
		assertThat(ctx.newPureRepo(), instanceOf(Supplier.class));
		assertThat(ctx.exchangeRatesManager(), instanceOf(TxnAwareRatesManager.class));
		assertThat(ctx.lookupRetryingKeyOrder(), instanceOf(grameSigningOrder.class));
		assertThat(ctx.soliditySigsVerifier(), instanceOf(TxnAwareSoliditySigsVerifier.class));
		assertThat(ctx.expiries(), instanceOf(ExpiryManager.class));
		assertThat(ctx.creator(), instanceOf(ExpiringCreations.class));
		assertThat(ctx.txnHistories(), instanceOf(Map.class));
		assertThat(ctx.backingAccounts(), instanceOf(FCMapBackingAccounts.class));
		assertThat(ctx.backingTokenRels(), instanceOf(BackingTokenRels.class));
		assertThat(ctx.systemAccountsCreator(), instanceOf(BackedSystemAccountsCreator.class));
		assertThat(ctx.b64KeyReader(), instanceOf(LegacyEd25519KeyReader.class));
		assertThat(ctx.ledgerValidator(), instanceOf(BasedLedgerValidator.class));
		assertThat(ctx.systemOpPolicies(), instanceOf(SystemOpPolicies.class));
		assertThat(ctx.exemptions(), instanceOf(StandardExemptions.class));
		assertThat(ctx.submissionManager(), instanceOf(PlatformSubmissionManager.class));
		assertThat(ctx.platformStatus(), instanceOf(ContextPlatformStatus.class));
		assertThat(ctx.contractAnswers(), instanceOf(ContractAnswers.class));
		assertThat(ctx.tokenStore(), instanceOf(grameTokenStore.class));
		assertThat(ctx.globalDynamicProperties(), instanceOf(GlobalDynamicProperties.class));
		assertThat(ctx.tokenGrpc(), instanceOf(TokenController.class));
		assertThat(ctx.scheduleGrpc(), instanceOf(ScheduleController.class));
		assertThat(ctx.nodeLocalProperties(), instanceOf(NodeLocalProperties.class));
		assertThat(ctx.balancesExporter(), instanceOf(SignedStateBalancesExporter.class));
		assertThat(ctx.exchange(), instanceOf(AwareHbarCentExchange.class));
		assertThat(ctx.stateMigrations(), instanceOf(StdStateMigrations.class));
		assertThat(ctx.opCounters(), instanceOf(HapiOpCounters.class));
		assertThat(ctx.runningAvgs(), instanceOf(MiscRunningAvgs.class));
		assertThat(ctx.speedometers(), instanceOf(MiscSpeedometers.class));
		assertThat(ctx.statsManager(), instanceOf(ServicesStatsManager.class));
		assertThat(ctx.semVers(), instanceOf(SemanticVersions.class));
		assertThat(ctx.freezeGrpc(), instanceOf(FreezeController.class));
		assertThat(ctx.contractsGrpc(), instanceOf(ContractController.class));
		assertThat(ctx.sigFactoryCreator(), instanceOf(SigFactoryCreator.class));
		assertThat(ctx.activationHelper(), instanceOf(InHandleActivationHelper.class));
		assertThat(ctx.characteristics(), instanceOf(CharacteristicsFactory.class));
		assertThat(ctx.nodeDiligenceScreen(), instanceOf(AwareNodeDiligenceScreen.class));
		assertThat(ctx.feeMultiplierSource(), instanceOf(TxnRateFeeMultiplierSource.class));
		assertThat(ctx.hapiThrottling(), instanceOf(HapiThrottling.class));
		assertThat(ctx.handleThrottling(), instanceOf(TxnAwareHandleThrottling.class));
		assertThat(ctx.throttleDefsManager(), instanceOf(ThrottleDefsManager.class));
		assertThat(ctx.sysFileCallbacks(), instanceOf(SysFileCallbacks.class));
		assertThat(ctx.networkCtxManager(), instanceOf(NetworkCtxManager.class));
		assertThat(ctx.hapiOpPermissions(), instanceOf(HapiOpPermissions.class));
		// and:
		assertEquals(ServicesNodeType.STAKED_NODE, ctx.nodeType());
		// and expect legacy:
		assertThat(ctx.txns(), instanceOf(TransactionHandler.class));
		assertThat(ctx.contracts(), instanceOf(SmartContractRequestHandler.class));
		assertThat(ctx.freeze(), instanceOf(FreezeHandler.class));
		assertThat(ctx.logic(), instanceOf(AwareProcessLogic.class));
		assertNotNull(ctx.accountsExporter());
	}

	@Test
	public void shouldInitFees() throws Exception {
		// setup:
		MerkleNetworkContext networkCtx = new MerkleNetworkContext();

		given(properties.getLongProperty("files.feeSchedules")).willReturn(111L);
		given(properties.getIntProperty("cache.records.ttl")).willReturn(180);
		var book = mock(AddressBook.class);
		var diskFs = mock(MerkleDiskFs.class);
		var blob = mock(MerkleOptionalBlob.class);
		byte[] fileInfo = new HFileMeta(false, StateView.EMPTY_WACL, 1_234_567L).serialize();
		byte[] fileContents = new byte[0];
		given(state.networkCtx()).willReturn(networkCtx);
		given(state.addressBook()).willReturn(book);
		given(state.diskFs()).willReturn(diskFs);
		given(storage.containsKey(any())).willReturn(true);
		given(storage.get(any())).willReturn(blob);
		given(blob.getData()).willReturn(fileInfo);
		given(diskFs.contains(any())).willReturn(true);
		given(diskFs.contentsOf(any())).willReturn(fileContents);

		ServicesContext ctx = new ServicesContext(nodeId, platform, state, propertySources);
		var subject = ctx.systemFilesManager();

		assertSame(networkCtx, ctx.networkCtx());
		assertDoesNotThrow(() -> subject.loadFeeSchedules());
	}

	@Test
	public void getRecordStreamDirectoryTest() {
		String expectedDir = "/here/we/are";

		NodeLocalProperties sourceProps = mock(NodeLocalProperties.class);
		given(sourceProps.recordLogDir()).willReturn(expectedDir);
		final AddressBook book = mock(AddressBook.class);
		final Address address = mock(Address.class);
		given(state.addressBook()).willReturn(book);
		given(book.getAddress(id)).willReturn(address);
		given(address.getMemo()).willReturn("0.0.3");

		ServicesContext ctx = new ServicesContext(nodeId, platform, state, propertySources);
		assertEquals(expectedDir + "/record0.0.3", ctx.getRecordStreamDirectory(sourceProps));
	}

	@Test
	public void updateRecordRunningHashTest() {
		// given:
		final RunningHash runningHash = mock(RunningHash.class);
		final RecordsRunningHashLeaf runningHashLeaf = new RecordsRunningHashLeaf();
		when(state.runningHashLeaf()).thenReturn(runningHashLeaf);

		ServicesContext ctx =
				new ServicesContext(
						nodeId,
						platform,
						state,
						propertySources);

		// when:
		ctx.updateRecordRunningHash(runningHash);

		// then:
		assertEquals(runningHash, ctx.state.runningHashLeaf().getRunningHash());
	}

	@Test
	public void initRecordStreamManagerTest() {
		// given:
		final AddressBook book = mock(AddressBook.class);
		final Address address = mock(Address.class);
		given(state.addressBook()).willReturn(book);
		given(book.getAddress(id)).willReturn(address);
		given(address.getMemo()).willReturn("0.0.3");
		given(properties.getStringProperty("grame.recordStream.logDir")).willReturn(recordStreamDir);
		given(properties.getIntProperty("grame.recordStream.queueCapacity")).willReturn(123);
		given(properties.getLongProperty("grame.recordStream.logPeriod")).willReturn(1L);
		given(properties.getBooleanProperty("grame.recordStream.isEnabled")).willReturn(true);
		final Hash initialHash = INITIAL_RANDOM_HASH;

		ServicesContext ctx =
				new ServicesContext(
						nodeId,
						platform,
						state,
						propertySources);

		assertNull(ctx.recordStreamManager());

		// when:
		ctx.setRecordsInitialHash(initialHash);
		ctx.initRecordStreamManager();

		// then:
		assertEquals(initialHash, ctx.getRecordsInitialHash());
		assertNotNull(ctx.recordStreamManager());
		assertEquals(initialHash, ctx.recordStreamManager().getInitialHash());
	}

	@Test
	public void setRecordsInitialHashTest() {
		// given:
		final Hash initialHash = INITIAL_RANDOM_HASH;

		ServicesContext ctx = spy(new ServicesContext(
				nodeId,
				platform,
				state,
				propertySources));
		RecordStreamManager recordStreamManager = mock(RecordStreamManager.class);

		when(ctx.recordStreamManager()).thenReturn(recordStreamManager);

		// when:
		ctx.setRecordsInitialHash(initialHash);

		// then:
		verify(recordStreamManager).setInitialHash(initialHash);
	}
}
