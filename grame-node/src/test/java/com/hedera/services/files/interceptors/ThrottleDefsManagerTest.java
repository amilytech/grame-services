package com.grame.services.files.interceptors;

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

import com.grame.services.config.FileNumbers;
import com.grame.services.sysfiles.domain.throttling.ThrottleBucket;
import com.grame.services.sysfiles.validation.ErrorCodeUtils;
import com.grame.test.utils.SerdeUtils;
import com.gramegrame.api.proto.java.FileID;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.ThrottleDefinitions;
import com.swirlds.common.AddressBook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.EnumSet;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.gramegrame.api.proto.java.grameFunctionality.ContractCall;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoCreate;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoGetAccountBalance;
import static com.gramegrame.api.proto.java.grameFunctionality.CryptoTransfer;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenAssociateToAccount;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenCreate;
import static com.gramegrame.api.proto.java.grameFunctionality.TokenMint;
import static com.gramegrame.api.proto.java.grameFunctionality.TransactionGetReceipt;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_THROTTLE_DEFINITIONS;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.NODE_CAPACITY_NOT_SUFFICIENT_FOR_OPERATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ThrottleDefsManagerTest {
	FileNumbers fileNums = new MockFileNumbers();
	FileID throttleDefs = fileNums.toFid(123L);

	EnumSet<grameFunctionality> pretendExpectedOps = EnumSet.of(
			CryptoCreate, CryptoTransfer, ContractCall,
			TokenCreate, TokenAssociateToAccount, TokenMint,
			CryptoGetAccountBalance, TransactionGetReceipt
	);

	@Mock
	AddressBook book;
	@Mock
	ThrottleBucket bucket;
	@Mock
	Function<ThrottleDefinitions, com.grame.services.sysfiles.domain.throttling.ThrottleDefinitions> mockToPojo;
	@Mock
	Consumer<ThrottleDefinitions> postUpdateCb;

	ThrottleDefsManager subject;

	@BeforeEach
	void setUp() {
		subject = new ThrottleDefsManager(fileNums, () -> book, postUpdateCb);
		subject.expectedOps = pretendExpectedOps;
	}

	@Test
	void rubberstampsAllUpdates() {
		// expect:
		assertEquals(ThrottleDefsManager.YES_VERDICT, subject.preAttrChange(throttleDefs, null));
	}

	@Test
	void throwsUnsupportedOnDelete() {
		// expect:
		assertThrows(UnsupportedOperationException.class, () -> subject.preDelete(throttleDefs));
	}

	@Test
	void saysYesWhenAcceptable() throws IOException {
		var ok = SerdeUtils.protoDefs("bootstrap/throttles.json");

		given(book.getSize()).willReturn(2);

		// when:
		var verdict = subject.preUpdate(throttleDefs, ok.toByteArray());

		// then:
		assertEquals(ThrottleDefsManager.YES_VERDICT, verdict);
	}

	@Test
	void detectsMissingExpectedOp() throws IOException {
		var missingMint = SerdeUtils.protoDefs("bootstrap/throttles-sans-mint.json");

		given(book.getSize()).willReturn(2);

		// when:
		var verdict = subject.preUpdate(throttleDefs, missingMint.toByteArray());

		// then:
		assertEquals(ThrottleDefsManager.YES_BUT_MISSING_OP_VERDICT, verdict);
	}

	@Test
	void invokesPostUpdateCbAsExpected() {
		// given:
		var newDef = ThrottleDefinitions.getDefaultInstance();

		// when:
		subject.postUpdate(throttleDefs, newDef.toByteArray());

		// then:
		verify(postUpdateCb).accept(newDef);
	}

	@Test
	public void reusesResponseCodeFromMapperFailure() {
		// setup:
		int nodes = 7;
		var pojoDefs = new com.grame.services.sysfiles.domain.throttling.ThrottleDefinitions();
		pojoDefs.getBuckets().add(bucket);

		given(book.getSize()).willReturn(nodes);
		given(bucket.asThrottleMapping(nodes)).willThrow(new IllegalStateException(
				ErrorCodeUtils.exceptionMsgFor(
						NODE_CAPACITY_NOT_SUFFICIENT_FOR_OPERATION, "YIKES!"
				)));
		given(mockToPojo.apply(any())).willReturn(pojoDefs);
		// and:
		subject.toPojo = mockToPojo;

		// when:
		var verdict = subject.preUpdate(throttleDefs, ThrottleDefinitions.getDefaultInstance().toByteArray());

		// then:
		assertEquals(NODE_CAPACITY_NOT_SUFFICIENT_FOR_OPERATION, verdict.getKey());
		assertFalse(verdict.getValue());
	}

	@Test
	public void fallsBackToDefaultInvalidIfNoDetailsFromMapperFailure() {
		// setup:
		int nodes = 7;
		var pojoDefs = new com.grame.services.sysfiles.domain.throttling.ThrottleDefinitions();
		pojoDefs.getBuckets().add(bucket);

		given(book.getSize()).willReturn(nodes);
		given(bucket.asThrottleMapping(nodes)).willThrow(new IllegalStateException("YIKES!"));
		given(mockToPojo.apply(any())).willReturn(pojoDefs);
		// and:
		subject.toPojo = mockToPojo;

		// when:
		var verdict = subject.preUpdate(throttleDefs, ThrottleDefinitions.getDefaultInstance().toByteArray());

		// then:
		assertEquals(INVALID_THROTTLE_DEFINITIONS, verdict.getKey());
		assertFalse(verdict.getValue());
	}

	@Test
	public void rejectsInvalidBytes() {
		byte[] invalidBytes = "NONSENSE".getBytes();

		// when:
		var verdict = subject.preUpdate(throttleDefs, invalidBytes);

		// then:
		assertEquals(ThrottleDefsManager.UNPARSEABLE_VERDICT, verdict);
	}

	@Test
	void returnsMaximumPriorityForThrottleDefsUpdate() {
		// given:
		var priority = subject.priorityForCandidate(fileNums.toFid(123L));

		// expect:
		assertEquals(OptionalInt.of(ThrottleDefsManager.APPLICABLE_PRIORITY), priority);
	}

	@Test
	void returnsNoPriorityIfNoThrottleDefs() {
		// given:
		var priority = subject.priorityForCandidate(fileNums.toFid(124L));

		// expect:
		assertTrue(priority.isEmpty());
	}
}
