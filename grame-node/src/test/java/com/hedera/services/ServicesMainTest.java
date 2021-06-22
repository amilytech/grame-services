package com.grame.services;

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

import com.grame.services.context.CurrentPlatformStatus;
import com.grame.services.context.ServicesContext;
import com.grame.services.context.properties.GlobalDynamicProperties;
import com.grame.services.context.properties.NodeLocalProperties;
import com.grame.services.context.properties.Profile;
import com.grame.services.context.properties.PropertySource;
import com.grame.services.context.properties.PropertySources;
import com.grame.services.fees.FeeCalculator;
import com.grame.services.fees.FeeMultiplierSource;
import com.grame.services.grpc.GrpcServerManager;
import com.grame.services.ledger.accounts.BackingStore;
import com.grame.services.records.AccountRecordsHistorian;
import com.grame.services.state.exports.AccountsExporter;
import com.grame.services.state.exports.BalancesExporter;
import com.grame.services.state.forensics.IssListener;
import com.grame.services.state.initialization.SystemAccountsCreator;
import com.grame.services.state.initialization.SystemFilesManager;
import com.grame.services.state.logic.NetworkCtxManager;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.state.merkle.MerkleNetworkContext;
import com.grame.services.state.migration.StateMigrations;
import com.grame.services.state.validation.LedgerValidator;
import com.grame.services.stats.ServicesStatsManager;
import com.grame.services.stream.RecordStreamManager;
import com.grame.services.throttling.FunctionalityThrottling;
import com.grame.services.utils.Pause;
import com.grame.services.utils.SystemExits;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.AccountID;
import com.swirlds.common.Address;
import com.swirlds.common.AddressBook;
import com.swirlds.common.Console;
import com.swirlds.common.NodeId;
import com.swirlds.common.Platform;
import com.swirlds.common.PlatformStatus;
import com.swirlds.common.SwirldState;
import com.swirlds.common.notification.NotificationEngine;
import com.swirlds.common.notification.NotificationFactory;
import com.swirlds.common.notification.listeners.ReconnectCompleteListener;
import com.swirlds.common.notification.listeners.ReconnectCompleteNotification;
import com.swirlds.fcmap.FCMap;
import org.apache.commons.lang3.RandomUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static com.grame.services.context.SingletonContextsManager.CONTEXTS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.inOrder;
import static org.mockito.BDDMockito.intThat;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.verifyNoInteractions;
import static org.mockito.BDDMockito.willThrow;

public class ServicesMainTest {
	final long NODE_ID = 1L;
	final String PATH = "/this/was/mr/bleaneys/room";

	FCMap topics;
	FCMap accounts;
	FCMap storage;
	Pause pause;
	Logger mockLog;
	Console console;
	Platform platform;
	SystemExits systemExits;
	AddressBook addressBook;
	PrintStream consoleOut;
	FeeCalculator fees;
	ServicesMain subject;
	ServicesContext ctx;
	PropertySource properties;
	LedgerValidator ledgerValidator;
	AccountsExporter accountsExporter;
	PropertySources propertySources;
	BalancesExporter balancesExporter;
	StateMigrations stateMigrations;
	MerkleNetworkContext networkCtx;
	FeeMultiplierSource feeMultiplierSource;
	ServicesStatsManager statsManager;
	GrpcServerManager grpc;
	NodeLocalProperties nodeLocalProps;
	SystemFilesManager systemFilesManager;
	SystemAccountsCreator systemAccountsCreator;
	CurrentPlatformStatus platformStatus;
	AccountRecordsHistorian recordsHistorian;
	GlobalDynamicProperties globalDynamicProperties;
	BackingStore<AccountID, MerkleAccount> backingAccounts;
	RecordStreamManager recordStreamManager;
	NetworkCtxManager networkCtxManager;

	@BeforeEach
	private void setup() {
		fees = mock(FeeCalculator.class);
		grpc = mock(GrpcServerManager.class);
		pause = mock(Pause.class);
		accounts = mock(FCMap.class);
		topics = mock(FCMap.class);
		storage = mock(FCMap.class);
		mockLog = mock(Logger.class);
		console = mock(Console.class);
		consoleOut = mock(PrintStream.class);
		platform = mock(Platform.class);
		systemExits = mock(SystemExits.class);
		recordStreamManager = mock(RecordStreamManager.class);
		backingAccounts = (BackingStore<AccountID, MerkleAccount>) mock(BackingStore.class);
		statsManager = mock(ServicesStatsManager.class);
		stateMigrations = mock(StateMigrations.class);
		balancesExporter = mock(BalancesExporter.class);
		nodeLocalProps = mock(NodeLocalProperties.class);
		recordsHistorian = mock(AccountRecordsHistorian.class);
		ledgerValidator = mock(LedgerValidator.class);
		accountsExporter = mock(AccountsExporter.class);
		platformStatus = mock(CurrentPlatformStatus.class);
		properties = mock(PropertySource.class);
		propertySources = mock(PropertySources.class);
		addressBook = mock(AddressBook.class);
		systemFilesManager = mock(SystemFilesManager.class);
		systemAccountsCreator = mock(SystemAccountsCreator.class);
		globalDynamicProperties = mock(GlobalDynamicProperties.class);
		networkCtx = mock(MerkleNetworkContext.class);
		feeMultiplierSource = mock(FeeMultiplierSource.class);
		networkCtxManager = mock(NetworkCtxManager.class);

		ctx = mock(ServicesContext.class);

		ServicesMain.log = mockLog;

		given(nodeLocalProps.devListeningAccount()).willReturn("0.0.3");
		given(nodeLocalProps.port()).willReturn(50211);
		given(nodeLocalProps.tlsPort()).willReturn(50212);
		given(ctx.fees()).willReturn(fees);
		given(ctx.grpc()).willReturn(grpc);
		given(ctx.globalDynamicProperties()).willReturn(globalDynamicProperties);
		given(ctx.pause()).willReturn(pause);
		given(ctx.nodeLocalProperties()).willReturn(nodeLocalProps);
		given(ctx.accounts()).willReturn(accounts);
		given(ctx.id()).willReturn(new NodeId(false, NODE_ID));
		given(ctx.nodeAccount()).willReturn(IdUtils.asAccount("0.0.3"));
		given(ctx.console()).willReturn(console);
		given(ctx.consoleOut()).willReturn(consoleOut);
		given(ctx.addressBook()).willReturn(addressBook);
		given(ctx.platform()).willReturn(platform);
		given(ctx.recordStreamManager()).willReturn(recordStreamManager);
		given(ctx.platformStatus()).willReturn(platformStatus);
		given(ctx.ledgerValidator()).willReturn(ledgerValidator);
		given(ctx.propertySources()).willReturn(propertySources);
		given(ctx.properties()).willReturn(properties);
		given(ctx.recordStreamManager()).willReturn(recordStreamManager);
		given(ctx.stateMigrations()).willReturn(stateMigrations);
		given(ctx.recordsHistorian()).willReturn(recordsHistorian);
		given(ctx.backingAccounts()).willReturn(backingAccounts);
		given(ctx.systemFilesManager()).willReturn(systemFilesManager);
		given(ctx.systemAccountsCreator()).willReturn(systemAccountsCreator);
		given(ctx.accountsExporter()).willReturn(accountsExporter);
		given(ctx.balancesExporter()).willReturn(balancesExporter);
		given(ctx.statsManager()).willReturn(statsManager);
		given(ctx.consensusTimeOfLastHandledTxn()).willReturn(Instant.ofEpochSecond(33L, 0));
		given(ctx.networkCtx()).willReturn(networkCtx);
		given(ctx.networkCtxManager()).willReturn(networkCtxManager);
		given(ctx.feeMultiplierSource()).willReturn(feeMultiplierSource);
		given(ledgerValidator.hasExpectedTotalBalance(any())).willReturn(true);
		given(properties.getIntProperty("timer.stats.dump.value")).willReturn(123);

		subject = new ServicesMain();
		subject.systemExits = systemExits;
		subject.defaultCharset = () -> StandardCharsets.UTF_8;
		CONTEXTS.store(ctx);
	}

	@AfterEach
	public void cleanup() {
		ServicesMain.log = LogManager.getLogger(ServicesMain.class);
	}

	@Test
	public void failsFastOnNonUtf8DefaultCharset() {
		// setup:
		subject.defaultCharset = () -> StandardCharsets.US_ASCII;

		// when:
		subject.init(null, new NodeId(false, NODE_ID));

		// then:
		verify(systemExits).fail(1);
	}

	@Test
	public void failsFastOnMissingNodeAccountIdIfNotSkippingExits() {
		given(ctx.nodeAccount()).willReturn(null);

		// when:
		subject.init(null, new NodeId(false, NODE_ID));

		// then:
		verify(systemExits).fail(1);
	}

	@Test
	public void exitsOnAddressBookCreationFailure() {
		willThrow(IllegalStateException.class)
				.given(systemFilesManager).createAddressBookIfMissing();

		// when:
		subject.init(null, new NodeId(false, NODE_ID));

		// then:
		verify(systemExits).fail(1);
	}

	@Test
	public void exitsOnCreationFailure() {
		willThrow(IllegalStateException.class)
				.given(systemAccountsCreator).ensureSystemAccounts(any(), any());

		// when:
		subject.init(null, new NodeId(false, NODE_ID));

		// then:
		verify(systemExits).fail(1);
	}

	@Test
	public void initializesSanelyGivenPreconditions() {
		var throttling = mock(FunctionalityThrottling.class);

		// given:
		InOrder inOrder = inOrder(
				systemFilesManager,
				networkCtxManager,
				propertySources,
				platform,
				stateMigrations,
				ledgerValidator,
				recordsHistorian,
				fees,
				grpc,
				statsManager,
				ctx,
				networkCtx,
				feeMultiplierSource);
		given(ctx.handleThrottling()).willReturn(throttling);

		// when:
		subject.init(null, new NodeId(false, NODE_ID));

		// then:
		inOrder.verify(propertySources).assertSourcesArePresent();
		inOrder.verify(networkCtxManager).loadObservableSysFilesIfNeeded();
		inOrder.verify(stateMigrations).runAllFor(ctx);
		inOrder.verify(ledgerValidator).assertIdsAreValid(accounts);
		inOrder.verify(ledgerValidator).hasExpectedTotalBalance(accounts);
		inOrder.verify(platform).setSleepAfterSync(0L);
		inOrder.verify(platform).addSignedStateListener(any(IssListener.class));
		inOrder.verify(statsManager).initializeFor(platform);
		inOrder.verify(ctx).initRecordStreamManager();
	}

	@Test
	public void runsOnDefaultPortInProduction() {
		given(nodeLocalProps.activeProfile()).willReturn(Profile.PROD);

		// when:
		subject.init(null, new NodeId(false, NODE_ID));

		// then:
		verify(grpc).start(intThat(i -> i == 50211), intThat(i -> i == 50212), any());
	}

	@Test
	public void runsOnDefaultPortInDevIfBlessedInSingleNodeListeningNode() {
		// setup:
		Address address = mock(Address.class);

		given(nodeLocalProps.activeProfile()).willReturn(Profile.DEV);
		given(address.getMemo()).willReturn("0.0.3");
		given(addressBook.getAddress(NODE_ID)).willReturn(address);
		given(nodeLocalProps.devOnlyDefaultNodeListens()).willReturn(true);

		// when:
		subject.init(null, new NodeId(false, NODE_ID));

		// then:
		verify(grpc).start(intThat(i -> i == 50211), intThat(i -> i == 50212), any());
	}

	@Test
	public void doesntRunInDevIfNotBlessedInSingleNodeListeningNode() {
		// setup:
		Address address = mock(Address.class);

		given(nodeLocalProps.activeProfile()).willReturn(Profile.DEV);
		given(address.getMemo()).willReturn("0.0.4");
		given(addressBook.getAddress(NODE_ID)).willReturn(address);
		given(nodeLocalProps.devOnlyDefaultNodeListens()).willReturn(true);

		// when:
		subject.init(null, new NodeId(false, NODE_ID));

		// then:
		verifyNoInteractions(grpc);
	}

	@Test
	public void runsOnDefaultPortInDevIfNotInSingleNodeListeningNodeAndDefault() {
		// setup:
		Address address = mock(Address.class);

		given(nodeLocalProps.activeProfile()).willReturn(Profile.DEV);
		given(address.getMemo()).willReturn("0.0.3");
		given(address.getPortExternalIpv4()).willReturn(50001);
		given(addressBook.getAddress(NODE_ID)).willReturn(address);
		given(nodeLocalProps.devOnlyDefaultNodeListens()).willReturn(false);

		// when:
		subject.init(null, new NodeId(false, NODE_ID));

		// then:
		verify(grpc).start(intThat(i -> i == 50211), intThat(i -> i == 50212), any());
	}

	@Test
	public void runsOnOffsetPortInDevIfNotInSingleNodeListeningNodeAndNotDefault() {
		// setup:
		Address address = mock(Address.class);

		given(nodeLocalProps.activeProfile()).willReturn(Profile.DEV);
		given(address.getMemo()).willReturn("0.0.4");
		given(address.getPortExternalIpv4()).willReturn(50001);
		given(addressBook.getAddress(NODE_ID)).willReturn(address);
		given(nodeLocalProps.devOnlyDefaultNodeListens()).willReturn(false);

		// when:
		subject.init(null, new NodeId(false, NODE_ID));

		// then:
		verify(grpc).start(intThat(i -> i == 50212), intThat(i -> i == 50213), any());
	}

	@Test
	public void loadsSystemFilesIfNotAlreadyDone() {
		given(systemFilesManager.areObservableFilesLoaded()).willReturn(true);

		// when:
		subject.init(null, new NodeId(false, NODE_ID));

		// then:
		verify(systemFilesManager).createAddressBookIfMissing();
		verify(systemFilesManager).createNodeDetailsIfMissing();
		verify(systemFilesManager).createUpdateZipFileIfMissing();
		// and:
		verify(systemFilesManager, never()).loadObservableSystemFiles();
	}

	@Test
	public void managesSystemFiles() {
		given(systemFilesManager.areObservableFilesLoaded()).willReturn(false);

		// when:
		subject.init(null, new NodeId(false, NODE_ID));

		// then:
		verify(systemFilesManager).createAddressBookIfMissing();
		verify(systemFilesManager).createNodeDetailsIfMissing();
		verify(systemFilesManager).createUpdateZipFileIfMissing();
		// and:
		verify(networkCtxManager).loadObservableSysFilesIfNeeded();
	}

	@Test
	public void createsSystemAccountsIfRequested() {
		// when:
		subject.init(null, new NodeId(false, NODE_ID));

		// then:
		verify(systemAccountsCreator).ensureSystemAccounts(backingAccounts, addressBook);
		verify(pause).forMs(ServicesMain.SUGGESTED_POST_CREATION_PAUSE_MS);
	}

	@Test
	public void rethrowsAccountsCreationFailureAsIse() {
		given(ctx.systemAccountsCreator()).willReturn(null);

		// when:
		subject.init(null, new NodeId(false, NODE_ID));

		// then:
		verify(systemExits).fail(1);
	}

	@Test
	public void exportsAccountsIfRequested() throws Exception {
		given(nodeLocalProps.accountsExportPath()).willReturn(PATH);
		given(nodeLocalProps.exportAccountsOnStartup()).willReturn(true);

		// when:
		subject.init(null, new NodeId(false, NODE_ID));

		// then:
		verify(accountsExporter).toFile(PATH, accounts);
	}

	@Test
	public void rethrowsAccountsExportFailureAsIse() {
		given(nodeLocalProps.accountsExportPath()).willReturn(PATH);
		given(nodeLocalProps.exportAccountsOnStartup()).willReturn(true);
		given(ctx.accountsExporter()).willReturn(null);

		// when:
		subject.init(null, new NodeId(false, NODE_ID));

		// then:
		verify(systemExits).fail(1);
	}

	@Test
	public void updatesCurrentStatusOnChangeOnlyIfBehind() {
		// given:
		subject.ctx = ctx;
		PlatformStatus newStatus = PlatformStatus.BEHIND;

		// when:
		subject.platformStatusChange(newStatus);

		// then:
		verify(platformStatus).set(newStatus);
		verifyNoInteractions(recordStreamManager);
	}

	@Test
	public void updatesCurrentStatusAndFreezesRecordStreamOnMaintenance() {
		// given:
		subject.ctx = ctx;
		PlatformStatus newStatus = PlatformStatus.MAINTENANCE;

		// when:
		subject.platformStatusChange(newStatus);

		// then:
		verify(platformStatus).set(newStatus);
		verify(recordStreamManager).setInFreeze(true);
	}

	@Test
	public void updatesCurrentStatusAndFreezesRecordStreamOnActive() {
		// given:
		subject.ctx = ctx;
		PlatformStatus newStatus = PlatformStatus.ACTIVE;

		// when:
		subject.platformStatusChange(newStatus);

		// then:
		verify(platformStatus).set(newStatus);
		verify(recordStreamManager).setInFreeze(false);
	}

	@Test
	public void doesLogSummaryIfNotInMaintenance() {
		// setup:
		subject.ctx = ctx;
		var signedState = mock(ServicesState.class);
		var currentPlatformStatus = mock(CurrentPlatformStatus.class);

		given(currentPlatformStatus.get()).willReturn(PlatformStatus.DISCONNECTED);
		given(ctx.platformStatus()).willReturn(currentPlatformStatus);

		// when:
		subject.newSignedState(signedState, Instant.now(), 1L);

		// then:
		verify(signedState, never()).logSummary();
	}

	@Test
	public void onlyLogsSummary() {
		// setup:
		subject.ctx = ctx;
		var signedState = mock(ServicesState.class);
		var currentPlatformStatus = mock(CurrentPlatformStatus.class);

		given(currentPlatformStatus.get()).willReturn(PlatformStatus.MAINTENANCE);
		given(ctx.platformStatus()).willReturn(currentPlatformStatus);

		// when:
		subject.newSignedState(signedState, Instant.now(), 1L);

		// then:
		verify(signedState).logSummary();
	}

	@Test
	public void doesntExportBalanceIfNotTime() throws Exception {
		// setup:
		subject.ctx = ctx;
		Instant when = Instant.now();
		ServicesState signedState = mock(ServicesState.class);

		given(properties.getBooleanProperty("grame.exportBalancesOnNewSignedState")).willReturn(true);
		given(balancesExporter.isTimeToExport(when)).willReturn(false);

		// when:
		subject.newSignedState(signedState, when, 1L);

		// then:
		verify(balancesExporter, never()).exportBalancesFrom(any(), any());
	}

	@Test
	public void exportsBalancesIfPropertySet() throws Exception {
		// setup:
		subject.ctx = ctx;
		Instant when = Instant.now();
		ServicesState signedState = mock(ServicesState.class);

		given(globalDynamicProperties.shouldExportBalances()).willReturn(true);
		given(balancesExporter.isTimeToExport(when)).willReturn(true);

		// when:
		subject.newSignedState(signedState, when, 1L);

		// then:
		verify(balancesExporter).exportBalancesFrom(signedState, when);
	}

	@Test
	public void doesntExportBalancesIfPropertyNotSet() {
		// setup:
		subject.ctx = ctx;
		Instant when = Instant.now();
		ServicesState signedState = mock(ServicesState.class);

		given(globalDynamicProperties.shouldExportBalances()).willReturn(false);

		// when:
		subject.newSignedState(signedState, when, 1L);

		// then:
		verifyNoInteractions(balancesExporter);
	}

	@Test
	public void failsFastIfBalanceExportDetectedInvalidState() throws Exception {
		// setup:
		subject.ctx = ctx;
		Instant when = Instant.now();
		ServicesState signedState = mock(ServicesState.class);

		given(globalDynamicProperties.shouldExportBalances()).willReturn(true);
		given(balancesExporter.isTimeToExport(when)).willReturn(true);
		willThrow(IllegalStateException.class)
				.given(balancesExporter)
				.exportBalancesFrom(signedState, when);

		// when:
		subject.newSignedState(signedState, when, 1L);

		// then:
		verify(systemExits).fail(1);
	}

	@Test
	public void noOpsRun() {
		// expect:
		assertDoesNotThrow(() -> {
			subject.run();
			subject.preEvent();
		});
	}

	@Test
	public void returnsAppState() {
		// expect:
		assertTrue(subject.newState() instanceof ServicesState);
	}

	@Test
	public void registerReconnectCompleteListenerTest() {
		NotificationEngine engineMock = mock(NotificationEngine.class);
		subject.registerReconnectCompleteListener(engineMock);
		verify(engineMock).register(eq(ReconnectCompleteListener.class), any());
	}

	@Test
	public void reconnectCompleteListenerTest() {
		// setup
		subject.ctx = ctx;
		// register
		subject.registerReconnectCompleteListener(NotificationFactory.getEngine());
		final long roundNumber = RandomUtils.nextLong();
		final Instant consensusTimestamp = Instant.now();
		final SwirldState state = mock(ServicesState.class);
		// dispatch a notification
		final ReconnectCompleteNotification notification = new ReconnectCompleteNotification(roundNumber,
				consensusTimestamp, state);
		NotificationFactory.getEngine().dispatch(ReconnectCompleteListener.class, notification);
		// should receive this notification
		verify(recordStreamManager).setStartWriteAtCompleteWindow(true);
	}
}
