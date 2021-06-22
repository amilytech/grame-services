package com.grame.services.queries.contract;

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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.*;

class ContractAnswersTest {
	GetBytecodeAnswer getBytecodeAnswer = mock(GetBytecodeAnswer.class);
	GetContractInfoAnswer getContractInfoAnswer = mock(GetContractInfoAnswer.class);
	GetContractRecordsAnswer getContractRecordsAnswer = mock(GetContractRecordsAnswer.class);
	ContractCallLocalAnswer contractCallLocalAnswer = mock(ContractCallLocalAnswer.class);
	GetBySolidityIdAnswer getBySolidityIdAnswer = mock(GetBySolidityIdAnswer.class);

	ContractAnswers subject;

	@Test
	public void hasExpectedAnswers() {
		// given:
		subject = new ContractAnswers(
				getBytecodeAnswer,
				getContractInfoAnswer,
				getBySolidityIdAnswer,
				getContractRecordsAnswer,
				contractCallLocalAnswer);

		// then:
		assertSame(getBytecodeAnswer, subject.getBytecode());
		assertSame(getContractInfoAnswer, subject.getContractInfo());
		assertSame(getContractRecordsAnswer, subject.getContractRecords());
		assertSame(contractCallLocalAnswer, subject.contractCallLocal());
		assertSame(getBySolidityIdAnswer, subject.getBySolidityId());
	}
}
