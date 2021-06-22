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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grame.services.ServicesState;
import com.grame.services.config.AccountNumbers;
import com.grame.services.config.EntityNumbers;
import com.grame.services.config.FileNumbers;
import com.grame.services.config.grameNumbers;
import com.grame.services.context.domain.security.HapiOpPermissions;
import com.grame.services.context.domain.trackers.ConsensusStatusCounts;
import com.grame.services.context.domain.trackers.IssEventInfo;
import com.grame.services.context.primitives.StateView;
import com.grame.services.context.properties.GlobalDynamicProperties;
import com.grame.services.context.properties.NodeLocalProperties;
import com.grame.services.context.properties.PropertySource;
import com.grame.services.context.properties.PropertySources;
import com.grame.services.context.properties.SemanticVersions;
import com.grame.services.context.properties.StandardizedPropertySources;
import com.grame.services.contracts.execution.SolidityLifecycle;
import com.grame.services.contracts.execution.SoliditySigsVerifier;
import com.grame.services.contracts.execution.TxnAwareSoliditySigsVerifier;
import com.grame.services.contracts.persistence.BlobStoragePersistence;
import com.grame.services.contracts.sources.BlobStorageSource;
import com.grame.services.contracts.sources.LedgerAccountsSource;
import com.grame.services.fees.AwareHbarCentExchange;
import com.grame.services.fees.FeeCalculator;
import com.grame.services.fees.FeeExemptions;
import com.grame.services.fees.FeeMultiplierSource;
import com.grame.services.fees.HbarCentExchange;
import com.grame.services.fees.StandardExemptions;
import com.grame.services.fees.TxnRateFeeMultiplierSource;
import com.grame.services.fees.calculation.AwareFcfsUsagePrices;
import com.grame.services.fees.calculation.TxnResourceUsageEstimator;
import com.grame.services.fees.calculation.UsageBasedFeeCalculator;
import com.grame.services.fees.calculation.UsagePricesProvider;
import com.grame.services.fees.calculation.consensus.queries.GetTopicInfoResourceUsage;
import com.grame.services.fees.calculation.consensus.txns.CreateTopicResourceUsage;
import com.grame.services.fees.calculation.consensus.txns.DeleteTopicResourceUsage;
import com.grame.services.fees.calculation.consensus.txns.SubmitMessageResourceUsage;
import com.grame.services.fees.calculation.consensus.txns.UpdateTopicResourceUsage;
import com.grame.services.fees.calculation.contract.queries.ContractCallLocalResourceUsage;
import com.grame.services.fees.calculation.contract.queries.GetBytecodeResourceUsage;
import com.grame.services.fees.calculation.contract.queries.GetContractInfoResourceUsage;
import com.grame.services.fees.calculation.contract.queries.GetContractRecordsResourceUsage;
import com.grame.services.fees.calculation.contract.txns.ContractCallResourceUsage;
import com.grame.services.fees.calculation.contract.txns.ContractCreateResourceUsage;
import com.grame.services.fees.calculation.contract.txns.ContractDeleteResourceUsage;
import com.grame.services.fees.calculation.contract.txns.ContractUpdateResourceUsage;
import com.grame.services.fees.calculation.crypto.queries.GetAccountInfoResourceUsage;
import com.grame.services.fees.calculation.crypto.queries.GetAccountRecordsResourceUsage;
import com.grame.services.fees.calculation.crypto.queries.GetTxnRecordResourceUsage;
import com.grame.services.fees.calculation.crypto.txns.CryptoCreateResourceUsage;
import com.grame.services.fees.calculation.crypto.txns.CryptoDeleteResourceUsage;
import com.grame.services.fees.calculation.crypto.txns.CryptoTransferResourceUsage;
import com.grame.services.fees.calculation.crypto.txns.CryptoUpdateResourceUsage;
import com.grame.services.fees.calculation.file.queries.GetFileContentsResourceUsage;
import com.grame.services.fees.calculation.file.queries.GetFileInfoResourceUsage;
import com.grame.services.fees.calculation.file.txns.FileAppendResourceUsage;
import com.grame.services.fees.calculation.file.txns.FileCreateResourceUsage;
import com.grame.services.fees.calculation.file.txns.FileDeleteResourceUsage;
import com.grame.services.fees.calculation.file.txns.FileUpdateResourceUsage;
import com.grame.services.fees.calculation.file.txns.SystemDeleteFileResourceUsage;
import com.grame.services.fees.calculation.file.txns.SystemUndeleteFileResourceUsage;
import com.grame.services.fees.calculation.meta.queries.GetVersionInfoResourceUsage;
import com.grame.services.fees.calculation.schedule.queries.GetScheduleInfoResourceUsage;
import com.grame.services.fees.calculation.schedule.txns.ScheduleCreateResourceUsage;
import com.grame.services.fees.calculation.schedule.txns.ScheduleDeleteResourceUsage;
import com.grame.services.fees.calculation.schedule.txns.ScheduleSignResourceUsage;
import com.grame.services.fees.calculation.system.txns.FreezeResourceUsage;
import com.grame.services.fees.calculation.token.queries.GetTokenInfoResourceUsage;
import com.grame.services.fees.calculation.token.txns.TokenAssociateResourceUsage;
import com.grame.services.fees.calculation.token.txns.TokenBurnResourceUsage;
import com.grame.services.fees.calculation.token.txns.TokenCreateResourceUsage;
import com.grame.services.fees.calculation.token.txns.TokenDeleteResourceUsage;
import com.grame.services.fees.calculation.token.txns.TokenDissociateResourceUsage;
import com.grame.services.fees.calculation.token.txns.TokenFreezeResourceUsage;
import com.grame.services.fees.calculation.token.txns.TokenGrantKycResourceUsage;
import com.grame.services.fees.calculation.token.txns.TokenMintResourceUsage;
import com.grame.services.fees.calculation.token.txns.TokenRevokeKycResourceUsage;
import com.grame.services.fees.calculation.token.txns.TokenUnfreezeResourceUsage;
import com.grame.services.fees.calculation.token.txns.TokenUpdateResourceUsage;
import com.grame.services.fees.calculation.token.txns.TokenWipeResourceUsage;
import com.grame.services.fees.charging.ItemizableFeeCharging;
import com.grame.services.fees.charging.TxnFeeChargingPolicy;
import com.grame.services.files.DataMapFactory;
import com.grame.services.files.EntityExpiryMapFactory;
import com.grame.services.files.FileUpdateInterceptor;
import com.grame.services.files.grameFs;
import com.grame.services.files.MetadataMapFactory;
import com.grame.services.files.SysFileCallbacks;
import com.grame.services.files.TieredgrameFs;
import com.grame.services.files.interceptors.ConfigListUtils;
import com.grame.services.files.interceptors.FeeSchedulesManager;
import com.grame.services.files.interceptors.ThrottleDefsManager;
import com.grame.services.files.interceptors.TxnAwareRatesManager;
import com.grame.services.files.interceptors.ValidatingCallbackInterceptor;
import com.grame.services.files.store.FcBlobsBytesStore;
import com.grame.services.files.sysfiles.ConfigCallbacks;
import com.grame.services.files.sysfiles.CurrencyCallbacks;
import com.grame.services.files.sysfiles.ThrottlesCallback;
import com.grame.services.grpc.ConfigDrivenNettyFactory;
import com.grame.services.grpc.GrpcServerManager;
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
import com.grame.services.keys.StandardSyncActivationCheck;
import com.grame.services.ledger.grameLedger;
import com.grame.services.ledger.TransactionalLedger;
import com.grame.services.ledger.accounts.BackingStore;
import com.grame.services.ledger.accounts.BackingTokenRels;
import com.grame.services.ledger.accounts.FCMapBackingAccounts;
import com.grame.services.ledger.accounts.PureFCMapBackingAccounts;
import com.grame.services.ledger.ids.EntityIdSource;
import com.grame.services.ledger.ids.SeqNoEntityIdSource;
import com.grame.services.ledger.properties.AccountProperty;
import com.grame.services.ledger.properties.ChangeSummaryManager;
import com.grame.services.ledger.properties.TokenRelProperty;
import com.grame.services.legacy.handler.FreezeHandler;
import com.grame.services.legacy.handler.SmartContractRequestHandler;
import com.grame.services.legacy.handler.TransactionHandler;
import com.grame.services.legacy.initialization.ExportExistingAccounts;
import com.grame.services.legacy.services.state.AwareProcessLogic;
import com.grame.services.queries.AnswerFlow;
import com.grame.services.queries.answering.AnswerFunctions;
import com.grame.services.queries.answering.QueryResponseHelper;
import com.grame.services.queries.answering.StakedAnswerFlow;
import com.grame.services.queries.answering.ZeroStakeAnswerFlow;
import com.grame.services.queries.consensus.GetTopicInfoAnswer;
import com.grame.services.queries.consensus.HcsAnswers;
import com.grame.services.queries.contract.ContractAnswers;
import com.grame.services.queries.contract.ContractCallLocalAnswer;
import com.grame.services.queries.contract.GetBySolidityIdAnswer;
import com.grame.services.queries.contract.GetBytecodeAnswer;
import com.grame.services.queries.contract.GetContractInfoAnswer;
import com.grame.services.queries.contract.GetContractRecordsAnswer;
import com.grame.services.queries.crypto.CryptoAnswers;
import com.grame.services.queries.crypto.GetAccountBalanceAnswer;
import com.grame.services.queries.crypto.GetAccountInfoAnswer;
import com.grame.services.queries.crypto.GetAccountRecordsAnswer;
import com.grame.services.queries.crypto.GetLiveHashAnswer;
import com.grame.services.queries.crypto.GetStakersAnswer;
import com.grame.services.queries.file.FileAnswers;
import com.grame.services.queries.file.GetFileContentsAnswer;
import com.grame.services.queries.file.GetFileInfoAnswer;
import com.grame.services.queries.meta.GetFastTxnRecordAnswer;
import com.grame.services.queries.meta.GetTxnReceiptAnswer;
import com.grame.services.queries.meta.GetTxnRecordAnswer;
import com.grame.services.queries.meta.GetVersionInfoAnswer;
import com.grame.services.queries.meta.MetaAnswers;
import com.grame.services.queries.schedule.GetScheduleInfoAnswer;
import com.grame.services.queries.schedule.ScheduleAnswers;
import com.grame.services.queries.token.GetTokenInfoAnswer;
import com.grame.services.queries.token.TokenAnswers;
import com.grame.services.queries.validation.QueryFeeCheck;
import com.grame.services.records.AccountRecordsHistorian;
import com.grame.services.records.RecordCache;
import com.grame.services.records.RecordCacheFactory;
import com.grame.services.records.TxnAwareRecordsHistorian;
import com.grame.services.records.TxnIdRecentHistory;
import com.grame.services.security.ops.SystemOpPolicies;
import com.grame.services.sigs.factories.SigFactoryCreator;
import com.grame.services.sigs.metadata.DelegatingSigMetadataLookup;
import com.grame.services.sigs.order.grameSigningOrder;
import com.grame.services.sigs.sourcing.DefaultSigBytesProvider;
import com.grame.services.sigs.verification.PrecheckKeyReqs;
import com.grame.services.sigs.verification.PrecheckVerifier;
import com.grame.services.sigs.verification.SyncVerifier;
import com.grame.services.state.expiry.ExpiringCreations;
import com.grame.services.state.expiry.ExpiryManager;
import com.grame.services.state.exports.AccountsExporter;
import com.grame.services.state.exports.BalancesExporter;
import com.grame.services.state.exports.SignedStateBalancesExporter;
import com.grame.services.state.initialization.BackedSystemAccountsCreator;
import com.grame.services.state.initialization.HfsSystemFilesManager;
import com.grame.services.state.initialization.SystemAccountsCreator;
import com.grame.services.state.initialization.SystemFilesManager;
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
import com.grame.services.state.migration.StateMigrations;
import com.grame.services.state.migration.StdStateMigrations;
import com.grame.services.state.submerkle.EntityId;
import com.grame.services.state.submerkle.ExchangeRates;
import com.grame.services.state.submerkle.SequenceNumber;
import com.grame.services.state.validation.BasedLedgerValidator;
import com.grame.services.state.validation.LedgerValidator;
import com.grame.services.stats.CounterFactory;
import com.grame.services.stats.HapiOpCounters;
import com.grame.services.stats.HapiOpSpeedometers;
import com.grame.services.stats.MiscRunningAvgs;
import com.grame.services.stats.MiscSpeedometers;
import com.grame.services.stats.RunningAvgFactory;
import com.grame.services.stats.ServicesStatsManager;
import com.grame.services.stats.SpeedometerFactory;
import com.grame.services.store.schedule.grameScheduleStore;
import com.grame.services.store.schedule.ScheduleStore;
import com.grame.services.store.tokens.grameTokenStore;
import com.grame.services.store.tokens.TokenStore;
import com.grame.services.stream.RecordStreamManager;
import com.grame.services.throttling.DeterministicThrottling;
import com.grame.services.throttling.FunctionalityThrottling;
import com.grame.services.throttling.HapiThrottling;
import com.grame.services.throttling.TransactionThrottling;
import com.grame.services.throttling.TxnAwareHandleThrottling;
import com.grame.services.txns.ProcessLogic;
import com.grame.services.txns.SubmissionFlow;
import com.grame.services.txns.TransitionLogic;
import com.grame.services.txns.TransitionLogicLookup;
import com.grame.services.txns.consensus.SubmitMessageTransitionLogic;
import com.grame.services.txns.consensus.TopicCreateTransitionLogic;
import com.grame.services.txns.consensus.TopicDeleteTransitionLogic;
import com.grame.services.txns.consensus.TopicUpdateTransitionLogic;
import com.grame.services.txns.contract.ContractCallTransitionLogic;
import com.grame.services.txns.contract.ContractCreateTransitionLogic;
import com.grame.services.txns.contract.ContractDeleteTransitionLogic;
import com.grame.services.txns.contract.ContractSysDelTransitionLogic;
import com.grame.services.txns.contract.ContractSysUndelTransitionLogic;
import com.grame.services.txns.contract.ContractUpdateTransitionLogic;
import com.grame.services.txns.crypto.CryptoCreateTransitionLogic;
import com.grame.services.txns.crypto.CryptoDeleteTransitionLogic;
import com.grame.services.txns.crypto.CryptoTransferTransitionLogic;
import com.grame.services.txns.crypto.CryptoUpdateTransitionLogic;
import com.grame.services.txns.file.FileAppendTransitionLogic;
import com.grame.services.txns.file.FileCreateTransitionLogic;
import com.grame.services.txns.file.FileDeleteTransitionLogic;
import com.grame.services.txns.file.FileSysDelTransitionLogic;
import com.grame.services.txns.file.FileSysUndelTransitionLogic;
import com.grame.services.txns.file.FileUpdateTransitionLogic;
import com.grame.services.txns.network.FreezeTransitionLogic;
import com.grame.services.txns.network.UncheckedSubmitTransitionLogic;
import com.grame.services.txns.schedule.ScheduleCreateTransitionLogic;
import com.grame.services.txns.schedule.ScheduleDeleteTransitionLogic;
import com.grame.services.txns.schedule.ScheduleSignTransitionLogic;
import com.grame.services.txns.submission.PlatformSubmissionManager;
import com.grame.services.txns.submission.TxnHandlerSubmissionFlow;
import com.grame.services.txns.submission.TxnResponseHelper;
import com.grame.services.txns.token.TokenAssociateTransitionLogic;
import com.grame.services.txns.token.TokenBurnTransitionLogic;
import com.grame.services.txns.token.TokenCreateTransitionLogic;
import com.grame.services.txns.token.TokenDeleteTransitionLogic;
import com.grame.services.txns.token.TokenDissociateTransitionLogic;
import com.grame.services.txns.token.TokenFreezeTransitionLogic;
import com.grame.services.txns.token.TokenGrantKycTransitionLogic;
import com.grame.services.txns.token.TokenMintTransitionLogic;
import com.grame.services.txns.token.TokenRevokeKycTransitionLogic;
import com.grame.services.txns.token.TokenUnfreezeTransitionLogic;
import com.grame.services.txns.token.TokenUpdateTransitionLogic;
import com.grame.services.txns.token.TokenWipeTransitionLogic;
import com.grame.services.txns.validation.BasicPrecheck;
import com.grame.services.txns.validation.ContextOptionValidator;
import com.grame.services.txns.validation.OptionValidator;
import com.grame.services.usage.crypto.CryptoOpsUsage;
import com.grame.services.usage.file.FileOpsUsage;
import com.grame.services.usage.schedule.ScheduleOpsUsage;
import com.grame.services.utils.EntityIdUtils;
import com.grame.services.utils.MiscUtils;
import com.grame.services.utils.Pause;
import com.grame.services.utils.SleepingPause;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.TokenID;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.api.proto.java.TransactionID;
import com.gramegrame.fee.CryptoFeeBuilder;
import com.gramegrame.fee.FileFeeBuilder;
import com.gramegrame.fee.SmartContractFeeBuilder;
import com.swirlds.common.Address;
import com.swirlds.common.AddressBook;
import com.swirlds.common.Console;
import com.swirlds.common.NodeId;
import com.swirlds.common.Platform;
import com.swirlds.common.crypto.DigestType;
import com.swirlds.common.crypto.Hash;
import com.swirlds.common.crypto.ImmutableHash;
import com.swirlds.common.crypto.RunningHash;
import com.swirlds.fcmap.FCMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.core.AccountState;
import org.ethereum.datasource.Source;
import org.ethereum.datasource.StoragePersistence;
import org.ethereum.db.ServicesRepositoryRoot;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.grame.services.context.ServicesNodeType.STAKED_NODE;
import static com.grame.services.context.ServicesNodeType.ZERO_STAKE_NODE;
import static com.grame.services.contracts.sources.AddressKeyedMapFactory.bytecodeMapFrom;
import static com.grame.services.contracts.sources.AddressKeyedMapFactory.storageMapFrom;
import static com.grame.services.files.interceptors.ConfigListUtils.uncheckedParse;
import static com.grame.services.files.interceptors.PureRatesValidation.isNormalIntradayChange;
import static com.grame.services.ledger.grameLedger.ACCOUNT_ID_COMPARATOR;
import static com.grame.services.ledger.accounts.BackingTokenRels.REL_CMP;
import static com.grame.services.ledger.ids.ExceptionalEntityIdSource.NOOP_ID_SOURCE;
import static com.grame.services.records.NoopRecordsHistorian.NOOP_RECORDS_HISTORIAN;
import static com.grame.services.security.ops.SystemOpAuthorization.AUTHORIZED;
import static com.grame.services.sigs.metadata.DelegatingSigMetadataLookup.backedLookupsFor;
import static com.grame.services.sigs.metadata.DelegatingSigMetadataLookup.defaultAccountRetryingLookupsFor;
import static com.grame.services.sigs.metadata.DelegatingSigMetadataLookup.defaultLookupsFor;
import static com.grame.services.sigs.metadata.SigMetadataLookup.REF_LOOKUP_FACTORY;
import static com.grame.services.sigs.metadata.SigMetadataLookup.SCHEDULE_REF_LOOKUP_FACTORY;
import static com.grame.services.sigs.utils.PrecheckUtils.queryPaymentTestFor;
import static com.grame.services.state.expiry.NoopExpiringCreations.NOOP_EXPIRING_CREATIONS;
import static com.grame.services.store.tokens.ExceptionalTokenStore.NOOP_TOKEN_STORE;
import static com.grame.services.utils.EntityIdUtils.accountParsedFromString;
import static com.grame.services.utils.MiscUtils.lookupInCustomStore;
import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusCreateTopic;
import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusDeleteTopic;
import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusSubmitMessage;
import static com.gramegrame.api.proto.java.grameFunctionality.ConsensusUpdateTopic;
import static com.gramegrame.api.proto.java.grameFunctionality.ContractCall;
import static com.gramegrame.api.proto.java.grameFunctionality.ContractCreate;
import static com.gramegrame.api.proto.java.grameFunctionality.ContractDelete;
import static com.gramegrame.api.proto.java.grameFunctionality.ContractUpdate;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoCreate;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoDelete;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoTransfer;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoUpdate;
import static com.gramegrame.api.proto.java.grameFunctionality.FileAppend;
import static com.gramegrame.api.proto.java.grameFunctionality.FileCreate;
import static com.gramegrame.api.proto.java.grameFunctionality.FileDelete;
import static com.gramegrame.api.proto.java.grameFunctionality.FileUpdate;
import static com.gramegrame.api.proto.java.grameFunctionality.Freeze;
import static com.gramegrame.api.proto.java.grameFunctionality.ScheduleCreate;
import static com.gramegrame.api.proto.java.grameFunctionality.ScheduleDelete;
import static com.gramegrame.api.proto.java.grameFunctionality.ScheduleSign;
import static com.gramegrame.api.proto.java.grameFunctionality.SystemDelete;
import static com.gramegrame.api.proto.java.grameFunctionality.SystemUndelete;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenAccountWipe;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenAssociateToAccount;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenBurn;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenCreate;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenDelete;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenDissociateFromAccount;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenFreezeAccount;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenGrantKycToAccount;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenMint;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenRevokeKycFromAccount;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenUnfreezeAccount;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenUpdate;
import static com.gramegrame.api.proto.java.grameFunctionality.UncheckedSubmit;
import static java.util.Map.entry;

/**
 * Provide a trivial implementation of the inversion-of-control pattern,
 * isolating secondary responsibilities of dependency creation and
 * injection in a single component.
 *
 * @author AmilyTech
 */
public class ServicesContext {
	static Logger log = LogManager.getLogger(ServicesContext.class);

	/* Injected dependencies. */
	ServicesState state;

	private final NodeId id;
	private final Platform platform;
	private final PropertySources propertySources;

	/* Context-sensitive singletons. */
	/** the directory to which we writes .rcd and .rcd_sig files */
	private String recordStreamDir;
	/** the initialHash of RecordStreamManager */
	private Hash recordsInitialHash = new ImmutableHash(new byte[DigestType.SHA_384.digestLength()]);
	private Address address;
	private Console console;
	private grameFs hfs;
	private StateView currentView;
	private AccountID accountId;
	private AnswerFlow answerFlow;
	private HcsAnswers hcsAnswers;
	private FileNumbers fileNums;
	private FileAnswers fileAnswers;
	private MetaAnswers metaAnswers;
	private RecordCache recordCache;
	private TokenStore tokenStore;
	private TokenAnswers tokenAnswers;
	private grameLedger ledger;
	private SyncVerifier syncVerifier;
	private IssEventInfo issEventInfo;
	private ProcessLogic logic;
	private QueryFeeCheck queryFeeCheck;
	private grameNumbers grameNums;
	private ExpiryManager expiries;
	private FeeCalculator fees;
	private FeeExemptions exemptions;
	private EntityNumbers entityNums;
	private FreezeHandler freeze;
	private CryptoAnswers cryptoAnswers;
	private ScheduleStore scheduleStore;
	private AccountNumbers accountNums;
	private SubmissionFlow submissionFlow;
	private PropertySource properties;
	private EntityIdSource ids;
	private FileController fileGrpc;
	private HapiOpCounters opCounters;
	private AnswerFunctions answerFunctions;
	private ContractAnswers contractAnswers;
	private OptionValidator validator;
	private LedgerValidator ledgerValidator;
	private TokenController tokenGrpc;
	private MiscRunningAvgs runningAvgs;
	private ScheduleAnswers scheduleAnswers;
	private MiscSpeedometers speedometers;
	private ServicesNodeType nodeType;
	private SystemOpPolicies systemOpPolicies;
	private CryptoController cryptoGrpc;
	private HbarCentExchange exchange;
	private SemanticVersions semVers;
	private PrecheckVerifier precheckVerifier;
	private BackingTokenRels backingTokenRels;
	private FreezeController freezeGrpc;
	private BalancesExporter balancesExporter;
	private SysFileCallbacks sysFileCallbacks;
	private NetworkCtxManager networkCtxManager;
	private SolidityLifecycle solidityLifecycle;
	private ExpiringCreations creator;
	private NetworkController networkGrpc;
	private GrpcServerManager grpc;
	private TxnResponseHelper txnResponseHelper;
	private SigFactoryCreator sigFactoryCreator;
	private BlobStorageSource bytecodeDb;
	private HapiOpPermissions hapiOpPermissions;
	private TransactionContext txnCtx;
	private TransactionHandler txns;
	private ContractController contractsGrpc;
	private grameSigningOrder keyOrder;
	private grameSigningOrder backedKeyOrder;
	private grameSigningOrder lookupRetryingKeyOrder;
	private StoragePersistence storagePersistence;
	private ScheduleController scheduleGrpc;
	private ConsensusController consensusGrpc;
	private QueryResponseHelper queryResponseHelper;
	private UsagePricesProvider usagePrices;
	private Supplier<StateView> stateViews;
	private FeeSchedulesManager feeSchedulesManager;
	private RecordStreamManager recordStreamManager;
	private ThrottleDefsManager throttleDefsManager;
	private Map<String, byte[]> blobStore;
	private Map<EntityId, Long> entityExpiries;
	private FeeMultiplierSource feeMultiplierSource;
	private NodeLocalProperties nodeLocalProperties;
	private TxnFeeChargingPolicy txnChargingPolicy;
	private TxnAwareRatesManager exchangeRatesManager;
	private ServicesStatsManager statsManager;
	private LedgerAccountsSource accountSource;
	private FCMapBackingAccounts backingAccounts;
	private TransitionLogicLookup transitionLogic;
	private TransactionThrottling txnThrottling;
	private ConsensusStatusCounts statusCounts;
	private HfsSystemFilesManager systemFilesManager;
	private CurrentPlatformStatus platformStatus;
	private SystemAccountsCreator systemAccountsCreator;
	private ItemizableFeeCharging itemizableFeeCharging;
	private ServicesRepositoryRoot repository;
	private CharacteristicsFactory characteristics;
	private AccountRecordsHistorian recordsHistorian;
	private GlobalDynamicProperties globalDynamicProperties;
	private FunctionalityThrottling hapiThrottling;
	private FunctionalityThrottling handleThrottling;
	private AwareNodeDiligenceScreen nodeDiligenceScreen;
	private InHandleActivationHelper activationHelper;
	private PlatformSubmissionManager submissionManager;
	private SmartContractRequestHandler contracts;
	private TxnAwareSoliditySigsVerifier soliditySigsVerifier;
	private ValidatingCallbackInterceptor apiPermissionsReloading;
	private ValidatingCallbackInterceptor applicationPropertiesReloading;
	private Supplier<ServicesRepositoryRoot> newPureRepo;
	private Map<TransactionID, TxnIdRecentHistory> txnHistories;
	private AtomicReference<FCMap<MerkleEntityId, MerkleTopic>> queryableTopics;
	private AtomicReference<FCMap<MerkleEntityId, MerkleToken>> queryableTokens;
	private AtomicReference<FCMap<MerkleEntityId, MerkleAccount>> queryableAccounts;
	private AtomicReference<FCMap<MerkleEntityId, MerkleSchedule>> queryableSchedules;
	private AtomicReference<FCMap<MerkleBlobMeta, MerkleOptionalBlob>> queryableStorage;
	private AtomicReference<FCMap<MerkleEntityAssociation, MerkleTokenRelStatus>> queryableTokenAssociations;

	/* Context-free infrastructure. */
	private static Pause pause;
	private static StateMigrations stateMigrations;
	private static AccountsExporter accountsExporter;
	private static LegacyEd25519KeyReader b64KeyReader;

	static {
		pause = SleepingPause.SLEEPING_PAUSE;
		b64KeyReader = new LegacyEd25519KeyReader();
		stateMigrations = new StdStateMigrations(SleepingPause.SLEEPING_PAUSE);
		accountsExporter = ExportExistingAccounts::exportAccounts;
	}

	public ServicesContext(
			NodeId id,
			Platform platform,
			ServicesState state,
			PropertySources propertySources
	) {
		this.id = id;
		this.platform = platform;
		this.state = state;
		this.propertySources = propertySources;
	}

	public void update(ServicesState state) {
		this.state = state;

		queryableAccounts().set(accounts());
		queryableTopics().set(topics());
		queryableStorage().set(storage());
		queryableTokens().set(tokens());
		queryableTokenAssociations().set(tokenAssociations());
		queryableSchedules().set(schedules());
	}

	public void rebuildBackingStoresIfPresent() {
		if (backingTokenRels != null) {
			backingTokenRels.rebuildFromSources();
		}
		if (backingAccounts != null) {
			backingAccounts.rebuildFromSources();
		}
	}

	public void rebuildStoreViewsIfPresent() {
		if (scheduleStore != null) {
			scheduleStore.rebuildViews();
		}
		if (tokenStore != null) {
			tokenStore.rebuildViews();
		}
	}

	public SigFactoryCreator sigFactoryCreator() {
		if (sigFactoryCreator == null) {
			sigFactoryCreator = new SigFactoryCreator();
		}
		return sigFactoryCreator;
	}

	public HapiOpCounters opCounters() {
		if (opCounters == null) {
			opCounters = new HapiOpCounters(new CounterFactory() {
			}, runningAvgs(), txnCtx(), MiscUtils::baseStatNameOf);
		}
		return opCounters;
	}

	public MiscRunningAvgs runningAvgs() {
		if (runningAvgs == null) {
			runningAvgs = new MiscRunningAvgs(new RunningAvgFactory() {
			}, nodeLocalProperties());
		}
		return runningAvgs;
	}

	public FeeMultiplierSource feeMultiplierSource() {
		if (feeMultiplierSource == null) {
			feeMultiplierSource = new TxnRateFeeMultiplierSource(globalDynamicProperties(), handleThrottling());
		}
		return feeMultiplierSource;
	}

	public MiscSpeedometers speedometers() {
		if (speedometers == null) {
			speedometers = new MiscSpeedometers(new SpeedometerFactory() {
			}, nodeLocalProperties());
		}
		return speedometers;
	}

	public FunctionalityThrottling hapiThrottling() {
		if (hapiThrottling == null) {
			hapiThrottling = new HapiThrottling(new DeterministicThrottling(() -> addressBook().getSize()));
		}
		return hapiThrottling;
	}

	public FunctionalityThrottling handleThrottling() {
		if (handleThrottling == null) {
			handleThrottling = new TxnAwareHandleThrottling(txnCtx(), new DeterministicThrottling(() -> 1));
		}
		return handleThrottling;
	}

	public AwareNodeDiligenceScreen nodeDiligenceScreen() {
		if (nodeDiligenceScreen == null) {
			nodeDiligenceScreen = new AwareNodeDiligenceScreen(validator(), txnCtx(), backingAccounts());
		}
		return nodeDiligenceScreen;
	}

	public SemanticVersions semVers() {
		if (semVers == null) {
			semVers = new SemanticVersions();
		}
		return semVers;
	}

	public ServicesStatsManager statsManager() {
		if (statsManager == null) {
			var opSpeedometers = new HapiOpSpeedometers(
					opCounters(),
					new SpeedometerFactory() {
					},
					nodeLocalProperties(),
					MiscUtils::baseStatNameOf);
			statsManager = new ServicesStatsManager(
					opCounters(),
					runningAvgs(),
					speedometers(),
					opSpeedometers,
					nodeLocalProperties());
		}
		return statsManager;
	}

	public CurrentPlatformStatus platformStatus() {
		if (platformStatus == null) {
			platformStatus = new ContextPlatformStatus();
		}
		return platformStatus;
	}

	public LedgerValidator ledgerValidator() {
		if (ledgerValidator == null) {
			ledgerValidator = new BasedLedgerValidator(grameNums(), properties(), globalDynamicProperties());
		}
		return ledgerValidator;
	}

	public InHandleActivationHelper activationHelper() {
		if (activationHelper == null) {
			activationHelper = new InHandleActivationHelper(
					backedKeyOrder(),
					characteristics(),
					txnCtx()::accessor);
		}
		return activationHelper;
	}

	public IssEventInfo issEventInfo() {
		if (issEventInfo == null) {
			issEventInfo = new IssEventInfo(properties());
		}
		return issEventInfo;
	}

	public Map<String, byte[]> blobStore() {
		if (blobStore == null) {
			blobStore = new FcBlobsBytesStore(MerkleOptionalBlob::new, this::storage);
		}
		return blobStore;
	}

	public Supplier<StateView> stateViews() {
		if (stateViews == null) {
			stateViews = () -> new StateView(
					tokenStore(),
					scheduleStore(),
					() -> queryableTopics().get(),
					() -> queryableAccounts().get(),
					() -> queryableStorage().get(),
					() -> queryableTokenAssociations().get(),
					this::diskFs,
					nodeLocalProperties());
		}
		return stateViews;
	}

	public StateView currentView() {
		if (currentView == null) {
			currentView = new StateView(
					tokenStore(),
					scheduleStore(),
					this::topics,
					this::accounts,
					this::storage,
					this::tokenAssociations,
					this::diskFs,
					nodeLocalProperties());
		}
		return currentView;
	}

	public grameNumbers grameNums() {
		if (grameNums == null) {
			grameNums = new grameNumbers(properties());
		}
		return grameNums;
	}

	public FileNumbers fileNums() {
		if (fileNums == null) {
			fileNums = new FileNumbers(grameNums(), properties());
		}
		return fileNums;
	}

	public AccountNumbers accountNums() {
		if (accountNums == null) {
			accountNums = new AccountNumbers(properties());
		}
		return accountNums;
	}

	public TxnResponseHelper txnResponseHelper() {
		if (txnResponseHelper == null) {
			txnResponseHelper = new TxnResponseHelper(submissionFlow(), opCounters());
		}
		return txnResponseHelper;
	}

	public TransactionThrottling txnThrottling() {
		if (txnThrottling == null) {
			txnThrottling = new TransactionThrottling(hapiThrottling());
		}
		return txnThrottling;
	}

	public ItemizableFeeCharging charging() {
		if (itemizableFeeCharging == null) {
			itemizableFeeCharging = new ItemizableFeeCharging(
					ledger(),
					exemptions(),
					globalDynamicProperties());
		}
		return itemizableFeeCharging;
	}

	public SubmissionFlow submissionFlow() {
		if (submissionFlow == null) {
			submissionFlow = new TxnHandlerSubmissionFlow(
					nodeType(),
					txns(),
					transitionLogic(),
					submissionManager());
		}
		return submissionFlow;
	}

	public QueryResponseHelper queryResponseHelper() {
		if (queryResponseHelper == null) {
			queryResponseHelper = new QueryResponseHelper(answerFlow(), opCounters());
		}
		return queryResponseHelper;
	}

	public FileAnswers fileAnswers() {
		if (fileAnswers == null) {
			fileAnswers = new FileAnswers(
					new GetFileInfoAnswer(validator()),
					new GetFileContentsAnswer(validator())
			);
		}
		return fileAnswers;
	}

	public ContractAnswers contractAnswers() {
		if (contractAnswers == null) {
			contractAnswers = new ContractAnswers(
					new GetBytecodeAnswer(validator()),
					new GetContractInfoAnswer(validator()),
					new GetBySolidityIdAnswer(),
					new GetContractRecordsAnswer(validator()),
					new ContractCallLocalAnswer(contracts()::contractCallLocal, validator())
			);
		}
		return contractAnswers;
	}

	public HcsAnswers hcsAnswers() {
		if (hcsAnswers == null) {
			hcsAnswers = new HcsAnswers(
					new GetTopicInfoAnswer(validator())
			);
		}
		return hcsAnswers;
	}

	public MetaAnswers metaAnswers() {
		if (metaAnswers == null) {
			metaAnswers = new MetaAnswers(
					new GetTxnRecordAnswer(recordCache(), validator(), answerFunctions()),
					new GetTxnReceiptAnswer(recordCache()),
					new GetVersionInfoAnswer(semVers()),
					new GetFastTxnRecordAnswer()
			);
		}
		return metaAnswers;
	}

	public EntityNumbers entityNums() {
		if (entityNums == null) {
			entityNums = new EntityNumbers(fileNums(), grameNums(), accountNums());
		}
		return entityNums;
	}

	public TokenAnswers tokenAnswers() {
		if (tokenAnswers == null) {
			tokenAnswers = new TokenAnswers(
					new GetTokenInfoAnswer()
			);
		}
		return tokenAnswers;
	}

	public ScheduleAnswers scheduleAnswers() {
		if (scheduleAnswers == null) {
			scheduleAnswers = new ScheduleAnswers(
					new GetScheduleInfoAnswer()
			);
		}
		return scheduleAnswers;
	}

	public CryptoAnswers cryptoAnswers() {
		if (cryptoAnswers == null) {
			cryptoAnswers = new CryptoAnswers(
					new GetLiveHashAnswer(),
					new GetStakersAnswer(),
					new GetAccountInfoAnswer(validator()),
					new GetAccountBalanceAnswer(validator()),
					new GetAccountRecordsAnswer(answerFunctions(), validator())
			);
		}
		return cryptoAnswers;
	}

	public AnswerFunctions answerFunctions() {
		if (answerFunctions == null) {
			answerFunctions = new AnswerFunctions();
		}
		return answerFunctions;
	}

	public QueryFeeCheck queryFeeCheck() {
		if (queryFeeCheck == null) {
			queryFeeCheck = new QueryFeeCheck(this::accounts);
		}
		return queryFeeCheck;
	}

	public FeeCalculator fees() {
		if (fees == null) {
			FileOpsUsage fileOpsUsage = new FileOpsUsage();
			CryptoOpsUsage cryptoOpsUsage = new CryptoOpsUsage();
			FileFeeBuilder fileFees = new FileFeeBuilder();
			CryptoFeeBuilder cryptoFees = new CryptoFeeBuilder();
			ScheduleOpsUsage scheduleOpsUsage = new ScheduleOpsUsage();
			SmartContractFeeBuilder contractFees = new SmartContractFeeBuilder();

			fees = new UsageBasedFeeCalculator(
					exchange(),
					usagePrices(),
					feeMultiplierSource(),
					List.of(
							/* Meta */
							new GetVersionInfoResourceUsage(),
							new GetTxnRecordResourceUsage(recordCache(), answerFunctions(), cryptoFees),
							/* Crypto */
							new GetAccountInfoResourceUsage(cryptoOpsUsage),
							new GetAccountRecordsResourceUsage(answerFunctions(), cryptoFees),
							/* File */
							new GetFileInfoResourceUsage(fileOpsUsage),
							new GetFileContentsResourceUsage(fileFees),
							/* Consensus */
							new GetTopicInfoResourceUsage(),
							/* Smart Contract */
							new GetBytecodeResourceUsage(contractFees),
							new GetContractInfoResourceUsage(),
							new GetContractRecordsResourceUsage(contractFees),
							new ContractCallLocalResourceUsage(
									contracts()::contractCallLocal, contractFees, globalDynamicProperties()),
							/* Token */
							new GetTokenInfoResourceUsage(),
							/* Schedule */
							new GetScheduleInfoResourceUsage(scheduleOpsUsage)
					),
					txnUsageEstimators(
							cryptoOpsUsage, fileOpsUsage, fileFees, cryptoFees, contractFees, scheduleOpsUsage)
			);
		}
		return fees;
	}

	private Function<grameFunctionality, List<TxnResourceUsageEstimator>> txnUsageEstimators(
			CryptoOpsUsage cryptoOpsUsage,
			FileOpsUsage fileOpsUsage,
			FileFeeBuilder fileFees,
			CryptoFeeBuilder cryptoFees,
			SmartContractFeeBuilder contractFees,
			ScheduleOpsUsage scheduleOpsUsage
	) {
		var props = globalDynamicProperties();

		Map<grameFunctionality, List<TxnResourceUsageEstimator>> estimatorsMap = Map.ofEntries(
				/* Crypto */
				entry(CryptoCreate, List.of(new CryptoCreateResourceUsage(cryptoOpsUsage))),
				entry(CryptoDelete, List.of(new CryptoDeleteResourceUsage(cryptoFees))),
				entry(CryptoUpdate, List.of(new CryptoUpdateResourceUsage(cryptoOpsUsage))),
				entry(CryptoTransfer, List.of(new CryptoTransferResourceUsage(globalDynamicProperties()))),
				/* Contract */
				entry(ContractCall, List.of(new ContractCallResourceUsage(contractFees))),
				entry(ContractCreate, List.of(new ContractCreateResourceUsage(contractFees))),
				entry(ContractDelete, List.of(new ContractDeleteResourceUsage(contractFees))),
				entry(ContractUpdate, List.of(new ContractUpdateResourceUsage(contractFees))),
				/* File */
				entry(FileCreate, List.of(new FileCreateResourceUsage(fileOpsUsage))),
				entry(FileDelete, List.of(new FileDeleteResourceUsage(fileFees))),
				entry(FileUpdate, List.of(new FileUpdateResourceUsage(fileOpsUsage))),
				entry(FileAppend, List.of(new FileAppendResourceUsage(fileFees))),
				/* Consensus */
				entry(ConsensusCreateTopic, List.of(new CreateTopicResourceUsage())),
				entry(ConsensusUpdateTopic, List.of(new UpdateTopicResourceUsage())),
				entry(ConsensusDeleteTopic, List.of(new DeleteTopicResourceUsage())),
				entry(ConsensusSubmitMessage, List.of(new SubmitMessageResourceUsage())),
				/* Token */
				entry(TokenCreate, List.of(new TokenCreateResourceUsage())),
				entry(TokenUpdate, List.of(new TokenUpdateResourceUsage())),
				entry(TokenFreezeAccount, List.of(new TokenFreezeResourceUsage())),
				entry(TokenUnfreezeAccount, List.of(new TokenUnfreezeResourceUsage())),
				entry(TokenGrantKycToAccount, List.of(new TokenGrantKycResourceUsage())),
				entry(TokenRevokeKycFromAccount, List.of(new TokenRevokeKycResourceUsage())),
				entry(TokenDelete, List.of(new TokenDeleteResourceUsage())),
				entry(TokenMint, List.of(new TokenMintResourceUsage())),
				entry(TokenBurn, List.of(new TokenBurnResourceUsage())),
				entry(TokenAccountWipe, List.of(new TokenWipeResourceUsage())),
				entry(TokenAssociateToAccount, List.of(new TokenAssociateResourceUsage())),
				entry(TokenDissociateFromAccount, List.of(new TokenDissociateResourceUsage())),
				/* Schedule */
				entry(ScheduleCreate, List.of(new ScheduleCreateResourceUsage(scheduleOpsUsage, props))),
				entry(ScheduleDelete, List.of(new ScheduleDeleteResourceUsage(scheduleOpsUsage, props))),
				entry(ScheduleSign, List.of(new ScheduleSignResourceUsage(scheduleOpsUsage, props))),
				/* System */
				entry(Freeze, List.of(new FreezeResourceUsage())),
				entry(SystemDelete, List.of(new SystemDeleteFileResourceUsage(fileFees))),
				entry(SystemUndelete, List.of(new SystemUndeleteFileResourceUsage(fileFees)))
		);

		return estimatorsMap::get;
	}

	public AnswerFlow answerFlow() {
		if (answerFlow == null) {
			if (nodeType() == STAKED_NODE) {
				answerFlow = new StakedAnswerFlow(
						fees(),
						txns(),
						stateViews(),
						usagePrices(),
						hapiThrottling(),
						submissionManager());
			} else {
				answerFlow = new ZeroStakeAnswerFlow(txns(), stateViews(), hapiThrottling());
			}
		}
		return answerFlow;
	}

	public grameSigningOrder keyOrder() {
		if (keyOrder == null) {
			var lookups = defaultLookupsFor(
					hfs(),
					this::accounts,
					this::topics,
					REF_LOOKUP_FACTORY.apply(tokenStore()),
					SCHEDULE_REF_LOOKUP_FACTORY.apply(scheduleStore()));
			keyOrder = keyOrderWith(lookups);
		}
		return keyOrder;
	}

	public grameSigningOrder backedKeyOrder() {
		if (backedKeyOrder == null) {
			var lookups = backedLookupsFor(
					hfs(),
					backingAccounts(),
					this::topics,
					this::accounts,
					REF_LOOKUP_FACTORY.apply(tokenStore()),
					SCHEDULE_REF_LOOKUP_FACTORY.apply(scheduleStore()));
			backedKeyOrder = keyOrderWith(lookups);
		}
		return backedKeyOrder;
	}

	public grameSigningOrder lookupRetryingKeyOrder() {
		if (lookupRetryingKeyOrder == null) {
			var lookups = defaultAccountRetryingLookupsFor(
					hfs(),
					nodeLocalProperties(),
					this::accounts,
					this::topics,
					REF_LOOKUP_FACTORY.apply(tokenStore()),
					SCHEDULE_REF_LOOKUP_FACTORY.apply(scheduleStore()),
					runningAvgs(),
					speedometers());
			lookupRetryingKeyOrder = keyOrderWith(lookups);
		}
		return lookupRetryingKeyOrder;
	}

	public ServicesNodeType nodeType() {
		if (nodeType == null) {
			nodeType = (address().getStake() > 0) ? STAKED_NODE : ZERO_STAKE_NODE;
		}
		return nodeType;
	}

	private grameSigningOrder keyOrderWith(DelegatingSigMetadataLookup lookups) {
		var policies = systemOpPolicies();
		var properties = globalDynamicProperties();
		return new grameSigningOrder(
				entityNums(),
				lookups,
				txn -> policies.check(txn, CryptoUpdate) != AUTHORIZED,
				(txn, function) -> policies.check(txn, function) != AUTHORIZED,
				properties);
	}

	public StoragePersistence storagePersistence() {
		if (storagePersistence == null) {
			storagePersistence = new BlobStoragePersistence(storageMapFrom(blobStore()));
		}
		return storagePersistence;
	}

	public SyncVerifier syncVerifier() {
		if (syncVerifier == null) {
			syncVerifier = platform().getCryptography()::verifySync;
		}
		return syncVerifier;
	}

	public PrecheckVerifier precheckVerifier() {
		if (precheckVerifier == null) {
			Predicate<TransactionBody> isQueryPayment = queryPaymentTestFor(nodeAccount());
			PrecheckKeyReqs reqs = new PrecheckKeyReqs(keyOrder(), lookupRetryingKeyOrder(), isQueryPayment);
			precheckVerifier = new PrecheckVerifier(syncVerifier(), reqs, DefaultSigBytesProvider.DEFAULT_SIG_BYTES);
		}
		return precheckVerifier;
	}

	public PrintStream consoleOut() {
		return Optional.ofNullable(console()).map(c -> c.out).orElse(null);
	}

	public BalancesExporter balancesExporter() {
		if (balancesExporter == null) {
			balancesExporter = new SignedStateBalancesExporter(
					properties(),
					platform()::sign,
					globalDynamicProperties());
		}
		return balancesExporter;
	}

	public Map<EntityId, Long> entityExpiries() {
		if (entityExpiries == null) {
			entityExpiries = EntityExpiryMapFactory.entityExpiryMapFrom(blobStore());
		}
		return entityExpiries;
	}

	public grameFs hfs() {
		if (hfs == null) {
			hfs = new TieredgrameFs(
					ids(),
					globalDynamicProperties(),
					txnCtx()::consensusTime,
					DataMapFactory.dataMapFrom(blobStore()),
					MetadataMapFactory.metaMapFrom(blobStore()),
					this::getCurrentSpecialFileSystem);
			hfs.register(feeSchedulesManager());
			hfs.register(exchangeRatesManager());
			hfs.register(apiPermissionsReloading());
			hfs.register(applicationPropertiesReloading());
			hfs.register(throttleDefsManager());
		}
		return hfs;
	}

	MerkleDiskFs getCurrentSpecialFileSystem() {
		return this.state.diskFs();
	}

	public SoliditySigsVerifier soliditySigsVerifier() {
		if (soliditySigsVerifier == null) {
			soliditySigsVerifier = new TxnAwareSoliditySigsVerifier(
					syncVerifier(),
					txnCtx(),
					StandardSyncActivationCheck::allKeysAreActive,
					this::accounts);
		}
		return soliditySigsVerifier;
	}

	public FileUpdateInterceptor applicationPropertiesReloading() {
		if (applicationPropertiesReloading == null) {
			var propertiesCb = sysFileCallbacks().propertiesCb();
			applicationPropertiesReloading = new ValidatingCallbackInterceptor(
					0,
					"files.networkProperties",
					properties(),
					contents -> propertiesCb.accept(uncheckedParse(contents)),
					ConfigListUtils::isConfigList
			);
		}
		return applicationPropertiesReloading;
	}

	public FileUpdateInterceptor apiPermissionsReloading() {
		if (apiPermissionsReloading == null) {
			var permissionsCb = sysFileCallbacks().permissionsCb();
			apiPermissionsReloading = new ValidatingCallbackInterceptor(
					0,
					"files.hapiPermissions",
					properties(),
					contents -> permissionsCb.accept(uncheckedParse(contents)),
					ConfigListUtils::isConfigList
			);
		}
		return apiPermissionsReloading;
	}

	public TransitionLogicLookup transitionLogic() {
		if (transitionLogic == null) {
			transitionLogic = new TransitionLogicLookup(transitions());
		}
		return transitionLogic;
	}

	private Function<grameFunctionality, List<TransitionLogic>> transitions() {
		Map<grameFunctionality, List<TransitionLogic>> transitionsMap = Map.ofEntries(
				/* Crypto */
				entry(CryptoCreate,
						List.of(new CryptoCreateTransitionLogic(ledger(), validator(), txnCtx()))),
				entry(CryptoUpdate,
						List.of(new CryptoUpdateTransitionLogic(ledger(), validator(), txnCtx()))),
				entry(CryptoDelete,
						List.of(new CryptoDeleteTransitionLogic(ledger(), txnCtx()))),
				entry(CryptoTransfer,
						List.of(new CryptoTransferTransitionLogic(ledger(), validator(), txnCtx()))),
				/* File */
				entry(FileUpdate,
						List.of(new FileUpdateTransitionLogic(hfs(), entityNums(), validator(), txnCtx()))),
				entry(FileCreate,
						List.of(new FileCreateTransitionLogic(hfs(), validator(), txnCtx()))),
				entry(FileDelete,
						List.of(new FileDeleteTransitionLogic(hfs(), txnCtx()))),
				entry(FileAppend,
						List.of(new FileAppendTransitionLogic(hfs(), txnCtx()))),
				/* Contract */
				entry(ContractCreate,
						List.of(new ContractCreateTransitionLogic(
								hfs(), contracts()::createContract, this::seqNo, validator(), txnCtx()))),
				entry(ContractUpdate,
						List.of(new ContractUpdateTransitionLogic(
								contracts()::updateContract, validator(), txnCtx(), this::accounts))),
				entry(ContractDelete,
						List.of(new ContractDeleteTransitionLogic(
								contracts()::deleteContract, validator(), txnCtx(), this::accounts))),
				entry(ContractCall,
						List.of(new ContractCallTransitionLogic(
								contracts()::contractCall, validator(), txnCtx(), this::seqNo, this::accounts))),
				/* Consensus */
				entry(ConsensusCreateTopic,
						List.of(new TopicCreateTransitionLogic(
								this::accounts, this::topics, ids(), validator(), txnCtx()))),
				entry(ConsensusUpdateTopic,
						List.of(new TopicUpdateTransitionLogic(
								this::accounts, this::topics, validator(), txnCtx()))),
				entry(ConsensusDeleteTopic,
						List.of(new TopicDeleteTransitionLogic(
								this::topics, validator(), txnCtx()))),
				entry(ConsensusSubmitMessage,
						List.of(new SubmitMessageTransitionLogic(
								this::topics, validator(), txnCtx(), globalDynamicProperties()))),
				/* Token */
				entry(TokenCreate,
						List.of(new TokenCreateTransitionLogic(validator(), tokenStore(), ledger(), txnCtx()))),
				entry(TokenUpdate,
						List.of(new TokenUpdateTransitionLogic(
								validator(), tokenStore(), ledger(), txnCtx(), grameTokenStore::affectsExpiryAtMost))),
				entry(TokenFreezeAccount,
						List.of(new TokenFreezeTransitionLogic(tokenStore(), ledger(), txnCtx()))),
				entry(TokenUnfreezeAccount,
						List.of(new TokenUnfreezeTransitionLogic(tokenStore(), ledger(), txnCtx()))),
				entry(TokenGrantKycToAccount,
						List.of(new TokenGrantKycTransitionLogic(tokenStore(), ledger(), txnCtx()))),
				entry(TokenRevokeKycFromAccount,
						List.of(new TokenRevokeKycTransitionLogic(tokenStore(), ledger(), txnCtx()))),
				entry(TokenDelete,
						List.of(new TokenDeleteTransitionLogic(tokenStore(), txnCtx()))),
				entry(TokenMint,
						List.of(new TokenMintTransitionLogic(tokenStore(), txnCtx()))),
				entry(TokenBurn,
						List.of(new TokenBurnTransitionLogic(tokenStore(), txnCtx()))),
				entry(TokenAccountWipe,
						List.of(new TokenWipeTransitionLogic(tokenStore(), txnCtx()))),
				entry(TokenAssociateToAccount,
						List.of(new TokenAssociateTransitionLogic(tokenStore(), txnCtx()))),
				entry(TokenDissociateFromAccount,
						List.of(new TokenDissociateTransitionLogic(tokenStore(), txnCtx()))),
				/* Schedule */
				entry(ScheduleCreate,
						List.of(new ScheduleCreateTransitionLogic(
								scheduleStore(), txnCtx(), activationHelper(), validator()))),
				entry(ScheduleSign,
						List.of(new ScheduleSignTransitionLogic(scheduleStore(), txnCtx(), activationHelper()))),
				entry(ScheduleDelete,
						List.of(new ScheduleDeleteTransitionLogic(scheduleStore(), txnCtx()))),
				/* System */
				entry(SystemDelete,
						List.of(
								new FileSysDelTransitionLogic(hfs(), entityExpiries(), txnCtx()),
								new ContractSysDelTransitionLogic(
										validator(), txnCtx(), contracts()::systemDelete, this::accounts))),
				entry(SystemUndelete,
						List.of(
								new FileSysUndelTransitionLogic(hfs(), entityExpiries(), txnCtx()),
								new ContractSysUndelTransitionLogic(
										validator(), txnCtx(), contracts()::systemUndelete, this::accounts))),
				/* Network */
				entry(Freeze,
						List.of(new FreezeTransitionLogic(fileNums(), freeze()::freeze, txnCtx()))),
				entry(UncheckedSubmit,
						List.of(new UncheckedSubmitTransitionLogic()))
		);
		return transitionsMap::get;
	}

	public EntityIdSource ids() {
		if (ids == null) {
			ids = new SeqNoEntityIdSource(this::seqNo);
		}
		return ids;
	}

	public TransactionContext txnCtx() {
		if (txnCtx == null) {
			txnCtx = new AwareTransactionContext(this);
		}
		return txnCtx;
	}

	public Map<TransactionID, TxnIdRecentHistory> txnHistories() {
		if (txnHistories == null) {
			txnHistories = new ConcurrentHashMap<>();
		}
		return txnHistories;
	}

	public RecordCache recordCache() {
		if (recordCache == null) {
			recordCache = new RecordCache(
					this,
					new RecordCacheFactory(properties()).getRecordCache(),
					txnHistories());
		}
		return recordCache;
	}

	public CharacteristicsFactory characteristics() {
		if (characteristics == null) {
			characteristics = new CharacteristicsFactory(hfs());
		}
		return characteristics;
	}

	public AccountRecordsHistorian recordsHistorian() {
		if (recordsHistorian == null) {
			recordsHistorian = new TxnAwareRecordsHistorian(
					recordCache(),
					txnCtx(),
					this::accounts,
					expiries());
		}
		return recordsHistorian;
	}

	public FeeExemptions exemptions() {
		if (exemptions == null) {
			exemptions = new StandardExemptions(accountNums(), systemOpPolicies());
		}
		return exemptions;
	}

	public HbarCentExchange exchange() {
		if (exchange == null) {
			exchange = new AwareHbarCentExchange(txnCtx());
		}
		return exchange;
	}

	public BackingStore<Pair<AccountID, TokenID>, MerkleTokenRelStatus> backingTokenRels() {
		if (backingTokenRels == null) {
			backingTokenRels = new BackingTokenRels(this::tokenAssociations);
		}
		return backingTokenRels;
	}

	public BackingStore<AccountID, MerkleAccount> backingAccounts() {
		if (backingAccounts == null) {
			backingAccounts = new FCMapBackingAccounts(this::accounts);
		}
		return backingAccounts;
	}

	public NodeLocalProperties nodeLocalProperties() {
		if (nodeLocalProperties == null) {
			nodeLocalProperties = new NodeLocalProperties(properties());
		}
		return nodeLocalProperties;
	}

	public GlobalDynamicProperties globalDynamicProperties() {
		if (globalDynamicProperties == null) {
			globalDynamicProperties = new GlobalDynamicProperties(grameNums(), properties());
		}
		return globalDynamicProperties;
	}

	public TokenStore tokenStore() {
		if (tokenStore == null) {
			TransactionalLedger<Pair<AccountID, TokenID>, TokenRelProperty, MerkleTokenRelStatus> tokenRelsLedger =
					new TransactionalLedger<>(
							TokenRelProperty.class,
							MerkleTokenRelStatus::new,
							backingTokenRels(),
							new ChangeSummaryManager<>());
			tokenRelsLedger.setKeyComparator(REL_CMP);
			tokenRelsLedger.setKeyToString(BackingTokenRels::readableTokenRel);
			tokenStore = new grameTokenStore(
					ids(),
					validator(),
					globalDynamicProperties(),
					this::tokens,
					tokenRelsLedger);
		}
		return tokenStore;
	}

	public ScheduleStore scheduleStore() {
		if (scheduleStore == null) {
			scheduleStore = new grameScheduleStore(globalDynamicProperties(), ids(), txnCtx(), this::schedules);
		}
		return scheduleStore;
	}

	public grameLedger ledger() {
		if (ledger == null) {
			TransactionalLedger<AccountID, AccountProperty, MerkleAccount> accountsLedger =
					new TransactionalLedger<>(
							AccountProperty.class,
							MerkleAccount::new,
							backingAccounts(),
							new ChangeSummaryManager<>());
			accountsLedger.setKeyComparator(ACCOUNT_ID_COMPARATOR);
			ledger = new grameLedger(
					tokenStore(),
					ids(),
					creator(),
					recordsHistorian(),
					accountsLedger);
			scheduleStore().setAccountsLedger(accountsLedger);
			scheduleStore().setgrameLedger(ledger);
		}
		return ledger;
	}

	public ExpiryManager expiries() {
		if (expiries == null) {
			var histories = txnHistories();
			expiries = new ExpiryManager(recordCache(), histories, scheduleStore(), schedules());
		}
		return expiries;
	}

	public ExpiringCreations creator() {
		if (creator == null) {
			creator = new ExpiringCreations(expiries(), globalDynamicProperties());
			creator.setRecordCache(recordCache());
		}
		return creator;
	}

	public OptionValidator validator() {
		if (validator == null) {
			validator = new ContextOptionValidator(txnCtx(), globalDynamicProperties());
		}
		return validator;
	}

	public ProcessLogic logic() {
		if (logic == null) {
			logic = new AwareProcessLogic(this);
		}
		return logic;
	}

	public FreezeHandler freeze() {
		if (freeze == null) {
			freeze = new FreezeHandler(hfs(), platform(), exchange());
		}
		return freeze;
	}

	public void updateFeature() {
		if (freeze != null) {
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("mac")) {
				if (platform.getSelfId().getId() == 0) {
					freeze.handleUpdateFeature();
				}
			} else {
				freeze.handleUpdateFeature();
			}
		}
	}

	public NetworkCtxManager networkCtxManager() {
		if (networkCtxManager == null) {
			networkCtxManager = new NetworkCtxManager(
					issEventInfo(),
					properties(),
					opCounters(),
					exchange(),
					systemFilesManager(),
					feeMultiplierSource(),
					handleThrottling(),
					this::networkCtx);
		}
		return networkCtxManager;
	}

	public ThrottleDefsManager throttleDefsManager() {
		if (throttleDefsManager == null) {
			throttleDefsManager = new ThrottleDefsManager(
					fileNums(), this::addressBook, sysFileCallbacks().throttlesCb());
		}
		return throttleDefsManager;
	}

	public RecordStreamManager recordStreamManager() {
		return recordStreamManager;
	}

	/**
	 * RecordStreamManager should only be initialized after system files have been loaded,
	 * which means enableRecordStreaming has been read from file
	 */
	public void initRecordStreamManager() {
		try {
			var nodeLocalProps = nodeLocalProperties();
			var nodeScopedRecordLogDir = getRecordStreamDirectory(nodeLocalProps);
			recordStreamManager = new RecordStreamManager(
					platform,
					runningAvgs(),
					nodeLocalProps,
					nodeScopedRecordLogDir,
					getRecordsInitialHash());
		} catch (IOException | NoSuchAlgorithmException ex) {
			log.error("Fail to initialize RecordStreamManager.", ex);
		}
	}

	public FileUpdateInterceptor exchangeRatesManager() {
		if (exchangeRatesManager == null) {
			exchangeRatesManager = new TxnAwareRatesManager(
					fileNums(),
					accountNums(),
					globalDynamicProperties(),
					txnCtx(),
					this::midnightRates,
					exchange()::updateRates,
					limitPercent -> (base, proposed) -> isNormalIntradayChange(base, proposed, limitPercent));
		}
		return exchangeRatesManager;
	}

	public FileUpdateInterceptor feeSchedulesManager() {
		if (feeSchedulesManager == null) {
			feeSchedulesManager = new FeeSchedulesManager(fileNums(), fees());
		}
		return feeSchedulesManager;
	}

	public FreezeController freezeGrpc() {
		if (freezeGrpc == null) {
			freezeGrpc = new FreezeController(txnResponseHelper());
		}
		return freezeGrpc;
	}

	public NetworkController networkGrpc() {
		if (networkGrpc == null) {
			networkGrpc = new NetworkController(metaAnswers(), txnResponseHelper(), queryResponseHelper());
		}
		return networkGrpc;
	}

	public FileController filesGrpc() {
		if (fileGrpc == null) {
			fileGrpc = new FileController(fileAnswers(), txnResponseHelper(), queryResponseHelper());
		}
		return fileGrpc;
	}

	public SystemOpPolicies systemOpPolicies() {
		if (systemOpPolicies == null) {
			systemOpPolicies = new SystemOpPolicies(entityNums());
		}
		return systemOpPolicies;
	}

	public TokenController tokenGrpc() {
		if (tokenGrpc == null) {
			tokenGrpc = new TokenController(tokenAnswers(), txnResponseHelper(), queryResponseHelper());
		}
		return tokenGrpc;
	}

	public ScheduleController scheduleGrpc() {
		if (scheduleGrpc == null) {
			scheduleGrpc = new ScheduleController(scheduleAnswers(), txnResponseHelper(), queryResponseHelper());
		}
		return scheduleGrpc;
	}

	public CryptoController cryptoGrpc() {
		if (cryptoGrpc == null) {
			cryptoGrpc = new CryptoController(
					metaAnswers(),
					cryptoAnswers(),
					txnResponseHelper(),
					queryResponseHelper());
		}
		return cryptoGrpc;
	}

	public ContractController contractsGrpc() {
		if (contractsGrpc == null) {
			contractsGrpc = new ContractController(contractAnswers(), txnResponseHelper(), queryResponseHelper());
		}
		return contractsGrpc;
	}

	public PlatformSubmissionManager submissionManager() {
		if (submissionManager == null) {
			submissionManager = new PlatformSubmissionManager(platform(), recordCache(), speedometers());
		}
		return submissionManager;
	}

	public ConsensusController consensusGrpc() {
		if (null == consensusGrpc) {
			consensusGrpc = new ConsensusController(hcsAnswers(), txnResponseHelper(), queryResponseHelper());
		}
		return consensusGrpc;
	}

	public GrpcServerManager grpc() {
		if (grpc == null) {
			grpc = new NettyGrpcServerManager(
					Runtime.getRuntime()::addShutdownHook,
					List.of(
							cryptoGrpc(),
							filesGrpc(),
							freezeGrpc(),
							contractsGrpc(),
							consensusGrpc(),
							networkGrpc(),
							tokenGrpc(),
							scheduleGrpc()),
					new ConfigDrivenNettyFactory(nodeLocalProperties()),
					Collections.emptyList());
		}
		return grpc;
	}

	public SmartContractRequestHandler contracts() {
		if (contracts == null) {
			contracts = new SmartContractRequestHandler(
					repository(),
					ledger(),
					this::accounts,
					txnCtx(),
					exchange(),
					usagePrices(),
					newPureRepo(),
					solidityLifecycle(),
					soliditySigsVerifier(),
					entityExpiries(),
					globalDynamicProperties());
		}
		return contracts;
	}

	public SysFileCallbacks sysFileCallbacks() {
		if (sysFileCallbacks == null) {
			var configCallbacks = new ConfigCallbacks(
					hapiOpPermissions(),
					globalDynamicProperties(),
					(StandardizedPropertySources) propertySources());
			var currencyCallbacks = new CurrencyCallbacks(fees(), exchange(), this::midnightRates);
			var throttlesCallback = new ThrottlesCallback(feeMultiplierSource(), hapiThrottling(), handleThrottling());
			sysFileCallbacks = new SysFileCallbacks(configCallbacks, throttlesCallback, currencyCallbacks);
		}
		return sysFileCallbacks;
	}

	public SolidityLifecycle solidityLifecycle() {
		if (solidityLifecycle == null) {
			solidityLifecycle = new SolidityLifecycle(globalDynamicProperties());
		}
		return solidityLifecycle;
	}

	public PropertySource properties() {
		if (properties == null) {
			properties = propertySources().asResolvingSource();
		}
		return properties;
	}

	public SystemFilesManager systemFilesManager() {
		if (systemFilesManager == null) {
			systemFilesManager = new HfsSystemFilesManager(
					addressBook(),
					fileNums(),
					properties(),
					(TieredgrameFs) hfs(),
					() -> lookupInCustomStore(
							b64KeyReader(),
							properties.getStringProperty("bootstrap.genesisB64Keystore.path"),
							properties.getStringProperty("bootstrap.genesisB64Keystore.keyName")),
					sysFileCallbacks());
		}
		return systemFilesManager;
	}

	public HapiOpPermissions hapiOpPermissions() {
		if (hapiOpPermissions == null) {
			hapiOpPermissions = new HapiOpPermissions(accountNums());
		}
		return hapiOpPermissions;
	}

	public ServicesRepositoryRoot repository() {
		if (repository == null) {
			repository = new ServicesRepositoryRoot(accountSource(), bytecodeDb());
			repository.setStoragePersistence(storagePersistence());
		}
		return repository;
	}

	public Supplier<ServicesRepositoryRoot> newPureRepo() {
		if (newPureRepo == null) {
			TransactionalLedger<AccountID, AccountProperty, MerkleAccount> pureDelegate = new TransactionalLedger<>(
					AccountProperty.class,
					MerkleAccount::new,
					new PureFCMapBackingAccounts(this::accounts),
					new ChangeSummaryManager<>());
			grameLedger pureLedger = new grameLedger(
					NOOP_TOKEN_STORE,
					NOOP_ID_SOURCE,
					NOOP_EXPIRING_CREATIONS,
					NOOP_RECORDS_HISTORIAN,
					pureDelegate);
			Source<byte[], AccountState> pureAccountSource = new LedgerAccountsSource(
					pureLedger,
					globalDynamicProperties());
			newPureRepo = () -> {
				var pureRepository = new ServicesRepositoryRoot(pureAccountSource, bytecodeDb());
				pureRepository.setStoragePersistence(storagePersistence());
				return pureRepository;
			};
		}
		return newPureRepo;
	}

	public ConsensusStatusCounts statusCounts() {
		if (statusCounts == null) {
			statusCounts = new ConsensusStatusCounts(new ObjectMapper());
		}
		return statusCounts;
	}

	public LedgerAccountsSource accountSource() {
		if (accountSource == null) {
			accountSource = new LedgerAccountsSource(ledger(), globalDynamicProperties());
		}
		return accountSource;
	}

	public BlobStorageSource bytecodeDb() {
		if (bytecodeDb == null) {
			bytecodeDb = new BlobStorageSource(bytecodeMapFrom(blobStore()));
		}
		return bytecodeDb;
	}

	public TransactionHandler txns() {
		if (txns == null) {
			txns = new TransactionHandler(
					recordCache(),
					precheckVerifier(),
					this::accounts,
					nodeAccount(),
					txnThrottling(),
					fees(),
					stateViews(),
					new BasicPrecheck(validator(), globalDynamicProperties()),
					queryFeeCheck(),
					accountNums(),
					systemOpPolicies(),
					exemptions(),
					platformStatus(),
					hapiOpPermissions());
		}
		return txns;
	}

	public Console console() {
		if (console == null) {
			console = platform().createConsole(true);
		}
		return console;
	}

	public AccountID nodeAccount() {
		if (accountId == null) {
			try {
				String memoOfAccountId = address().getMemo();
				accountId = accountParsedFromString(memoOfAccountId);
			} catch (Exception ignore) {
			}
		}
		return accountId;
	}

	public Address address() {
		if (address == null) {
			address = addressBook().getAddress(id.getId());
		}
		return address;
	}

	public AtomicReference<FCMap<MerkleBlobMeta, MerkleOptionalBlob>> queryableStorage() {
		if (queryableStorage == null) {
			queryableStorage = new AtomicReference<>(storage());
		}
		return queryableStorage;
	}

	public AtomicReference<FCMap<MerkleEntityId, MerkleAccount>> queryableAccounts() {
		if (queryableAccounts == null) {
			queryableAccounts = new AtomicReference<>(accounts());
		}
		return queryableAccounts;
	}

	public AtomicReference<FCMap<MerkleEntityId, MerkleTopic>> queryableTopics() {
		if (queryableTopics == null) {
			queryableTopics = new AtomicReference<>(topics());
		}
		return queryableTopics;
	}

	public AtomicReference<FCMap<MerkleEntityId, MerkleToken>> queryableTokens() {
		if (queryableTokens == null) {
			queryableTokens = new AtomicReference<>(tokens());
		}
		return queryableTokens;
	}

	public AtomicReference<FCMap<MerkleEntityAssociation, MerkleTokenRelStatus>> queryableTokenAssociations() {
		if (queryableTokenAssociations == null) {
			queryableTokenAssociations = new AtomicReference<>(tokenAssociations());
		}
		return queryableTokenAssociations;
	}

	public AtomicReference<FCMap<MerkleEntityId, MerkleSchedule>> queryableSchedules() {
		if (queryableSchedules == null) {
			queryableSchedules = new AtomicReference<>(schedules());
		}

		return queryableSchedules;
	}

	public UsagePricesProvider usagePrices() {
		if (usagePrices == null) {
			usagePrices = new AwareFcfsUsagePrices(hfs(), fileNums(), txnCtx());
		}
		return usagePrices;
	}

	public TxnFeeChargingPolicy txnChargingPolicy() {
		if (txnChargingPolicy == null) {
			txnChargingPolicy = new TxnFeeChargingPolicy();
		}
		return txnChargingPolicy;
	}

	public SystemAccountsCreator systemAccountsCreator() {
		if (systemAccountsCreator == null) {
			systemAccountsCreator = new BackedSystemAccountsCreator(
					grameNums(),
					accountNums(),
					properties(),
					b64KeyReader());
		}
		return systemAccountsCreator;
	}

	/* Context-free infrastructure. */
	public LegacyEd25519KeyReader b64KeyReader() {
		return b64KeyReader;
	}

	public Pause pause() {
		return pause;
	}

	public StateMigrations stateMigrations() {
		return stateMigrations;
	}

	public AccountsExporter accountsExporter() {
		return accountsExporter;
	}

	/* Injected dependencies. */
	public NodeId id() {
		return id;
	}

	public Platform platform() {
		return platform;
	}

	public PropertySources propertySources() {
		return propertySources;
	}

	public Instant consensusTimeOfLastHandledTxn() {
		return state.networkCtx().consensusTimeOfLastHandledTxn();
	}

	public void updateConsensusTimeOfLastHandledTxn(Instant dataDrivenNow) {
		state.networkCtx().setConsensusTimeOfLastHandledTxn(dataDrivenNow);
	}

	public AddressBook addressBook() {
		return state.addressBook();
	}

	public SequenceNumber seqNo() {
		return state.networkCtx().seqNo();
	}

	public ExchangeRates midnightRates() {
		return state.networkCtx().midnightRates();
	}

	public FCMap<MerkleEntityId, MerkleAccount> accounts() {
		return state.accounts();
	}

	public FCMap<MerkleEntityId, MerkleTopic> topics() {
		return state.topics();
	}

	public FCMap<MerkleBlobMeta, MerkleOptionalBlob> storage() {
		return state.storage();
	}

	public FCMap<MerkleEntityId, MerkleToken> tokens() {
		return state.tokens();
	}

	public FCMap<MerkleEntityAssociation, MerkleTokenRelStatus> tokenAssociations() {
		return state.tokenAssociations();
	}

	public FCMap<MerkleEntityId, MerkleSchedule> schedules() {
		return state.scheduleTxs();
	}

	public MerkleDiskFs diskFs() {
		return state.diskFs();
	}

	public MerkleNetworkContext networkCtx() {
		return state.networkCtx();
	}

	/**
	 * return the directory to which record stream files should be write
	 *
	 * @return the direct file folder for writing record stream files
	 */
	public String getRecordStreamDirectory(NodeLocalProperties source) {
		if (recordStreamDir == null) {
			final String nodeAccountString = EntityIdUtils.asLiteralString(nodeAccount());
			String parentDir = source.recordLogDir();
			if (!parentDir.endsWith(File.separator)) {
				parentDir += File.separator;
			}
			recordStreamDir = parentDir + "record" + nodeAccountString;
		}
		return recordStreamDir;
	}

	/**
	 * update the runningHash instance saved in runningHashLeaf
	 *
	 * @param runningHash
	 * 		new runningHash instance
	 */
	public void updateRecordRunningHash(final RunningHash runningHash) {
		state.runningHashLeaf().setRunningHash(runningHash);
	}

	/**
	 * set recordsInitialHash, which will be set to RecordStreamManager as initialHash.
	 * recordsInitialHash is read at restart, either from the state's runningHashLeaf,
	 * or from the last old .rcd_sig file in migration.
	 * When recordsInitialHash is read, the RecordStreamManager might not be initialized yet,
	 * because RecordStreamManager can only be initialized after system files are loaded so that enableRecordStream
	 * setting is read.
	 * Thus we save the initialHash in the context, and use it when initializing RecordStreamManager
	 *
	 * @param recordsInitialHash
	 * 		initial running Hash of records
	 */
	public void setRecordsInitialHash(final Hash recordsInitialHash) {
		this.recordsInitialHash = recordsInitialHash;
		if (recordStreamManager() != null) {
			recordStreamManager().setInitialHash(recordsInitialHash);
		}
	}

	Hash getRecordsInitialHash() {
		return recordsInitialHash;
	}

	void setBackingTokenRels(BackingTokenRels backingTokenRels) {
		this.backingTokenRels = backingTokenRels;
	}

	void setBackingAccounts(FCMapBackingAccounts backingAccounts) {
		this.backingAccounts = backingAccounts;
	}

	public void setTokenStore(TokenStore tokenStore) {
		this.tokenStore = tokenStore;
	}

	public void setScheduleStore(ScheduleStore scheduleStore) {
		this.scheduleStore = scheduleStore;
	}
}
