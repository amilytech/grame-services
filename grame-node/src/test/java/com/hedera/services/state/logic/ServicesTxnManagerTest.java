package com.grame.services.state.logic;

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

import com.grame.services.context.ServicesContext;
import com.grame.services.context.TransactionContext;
import com.grame.services.ledger.grameLedger;
import com.grame.services.records.RecordCache;
import com.grame.services.utils.PlatformTxnAccessor;
import com.grame.test.utils.IdUtils;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.function.BiConsumer;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ServicesTxnManagerTest {
	PlatformTxnAccessor accessor;
	Instant consensusTime = Instant.now();
	long submittingMember = 1;
	AccountID effectivePayer = IdUtils.asAccount("0.0.75231");

	Runnable processLogic;
	Runnable recordStreaming;
	Runnable triggeredProcessLogic;
	BiConsumer<Exception, String> warning;

	grameLedger ledger;
	RecordCache recordCache;
	TransactionContext txnCtx;
	ServicesContext ctx;

	ServicesTxnManager subject;

	@BeforeEach
	public void setup() {
		accessor = mock(PlatformTxnAccessor.class);

		processLogic = mock(Runnable.class);
		recordCache = mock(RecordCache.class);
		recordStreaming = mock(Runnable.class);
		triggeredProcessLogic = mock(Runnable.class);
		warning = mock(BiConsumer.class);

		subject = new ServicesTxnManager(processLogic, recordStreaming, triggeredProcessLogic, warning);

		ledger = mock(grameLedger.class);
		txnCtx = mock(TransactionContext.class);
		ctx = mock(ServicesContext.class);
		given(ctx.ledger()).willReturn(ledger);
		given(ctx.txnCtx()).willReturn(txnCtx);
		given(txnCtx.effectivePayer()).willReturn(effectivePayer);
		given(ctx.recordCache()).willReturn(recordCache);
	}

	@Test
	public void managesHappyPath() {
		// setup:
		InOrder inOrder = inOrder(ledger, txnCtx, processLogic, recordStreaming);

		// when:
		subject.process(accessor, consensusTime, submittingMember, ctx);

		// then:
		inOrder.verify(ledger).begin();
		inOrder.verify(txnCtx).resetFor(accessor, consensusTime, submittingMember);
		inOrder.verify(processLogic).run();
		inOrder.verify(ledger).commit();
		inOrder.verify(recordStreaming).run();
	}

	@Test
	public void warnsOnFailedRecordStreaming() {
		willThrow(IllegalStateException.class).given(recordStreaming).run();

		// when:
		subject.process(accessor, consensusTime, submittingMember, ctx);

		// then:
		verify(warning).accept(any(IllegalStateException.class), argThat("record streaming"::equals));
	}

	@Test
	public void setsFailInvalidAndWarnsOnProcessFailure() {
		// setup:
		InOrder inOrder = inOrder(ledger, txnCtx, processLogic, recordStreaming, warning);

		willThrow(IllegalStateException.class).given(ledger).begin();

		// when:
		subject.process(accessor, consensusTime, submittingMember, ctx);

		// then:
		inOrder.verify(ledger).begin();
		inOrder.verify(warning).accept(any(IllegalStateException.class), argThat("txn processing"::equals));
		inOrder.verify(txnCtx).setStatus(ResponseCodeEnum.FAIL_INVALID);
		inOrder.verify(ledger).commit();
		inOrder.verify(recordStreaming).run();
	}

	@Test
	public void retriesRecordCreationOnCommitFailureThenRollbacks() {
		// setup:
		InOrder inOrder = inOrder(ledger, txnCtx, processLogic, recordStreaming, warning, recordCache);

		willThrow(IllegalStateException.class).given(ledger).commit();

		// when:
		subject.process(accessor, consensusTime, submittingMember, ctx);

		// then:
		inOrder.verify(ledger).begin();
		inOrder.verify(txnCtx).resetFor(accessor, consensusTime, submittingMember);
		inOrder.verify(processLogic).run();
		inOrder.verify(ledger).commit();
		inOrder.verify(warning).accept(any(IllegalStateException.class), argThat("txn commit"::equals));
		inOrder.verify(recordCache).setFailInvalid(effectivePayer, accessor, consensusTime, submittingMember);
		inOrder.verify(ledger).rollback();
		inOrder.verify(recordStreaming, never()).run();
	}

	@Test
	public void warnsOnFailedRecordRecreate() {
		// setup:
		InOrder inOrder = inOrder(ledger, txnCtx, processLogic, recordStreaming, warning, recordCache);

		willThrow(IllegalStateException.class).given(ledger).commit();
		willThrow(IllegalStateException.class).given(recordCache).setFailInvalid(any(), any(), any(), anyLong());

		// when:
		subject.process(accessor, consensusTime, submittingMember, ctx);

		// then:
		inOrder.verify(ledger).begin();
		inOrder.verify(txnCtx).resetFor(accessor, consensusTime, submittingMember);
		inOrder.verify(processLogic).run();
		inOrder.verify(ledger).commit();
		inOrder.verify(warning).accept(any(IllegalStateException.class), argThat("txn commit"::equals));
		inOrder.verify(recordCache).setFailInvalid(effectivePayer, accessor, consensusTime, submittingMember);
		inOrder.verify(warning).accept(any(IllegalStateException.class), argThat("creating failure record"::equals));
		inOrder.verify(ledger).rollback();
		inOrder.verify(recordStreaming, never()).run();
	}

	@Test
	public void warnsOnFailedRollback() {
		// setup:
		InOrder inOrder = inOrder(ledger, txnCtx, processLogic, recordStreaming, warning, recordCache);

		willThrow(IllegalStateException.class).given(ledger).commit();
		willThrow(IllegalStateException.class).given(ledger).rollback();

		// when:
		subject.process(accessor, consensusTime, submittingMember, ctx);

		// then:
		inOrder.verify(ledger).begin();
		inOrder.verify(txnCtx).resetFor(accessor, consensusTime, submittingMember);
		inOrder.verify(processLogic).run();
		inOrder.verify(ledger).commit();
		inOrder.verify(warning).accept(any(IllegalStateException.class), argThat("txn commit"::equals));
		inOrder.verify(recordCache).setFailInvalid(effectivePayer, accessor, consensusTime, submittingMember);
		inOrder.verify(ledger).rollback();
		inOrder.verify(warning).accept(any(IllegalStateException.class), argThat("txn rollback"::equals));
		inOrder.verify(recordStreaming, never()).run();
	}
}
