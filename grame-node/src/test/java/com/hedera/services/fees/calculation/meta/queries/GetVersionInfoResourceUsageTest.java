package com.grame.services.fees.calculation.meta.queries;

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

import static com.gramegrame.api.proto.java.ResponseType.ANSWER_ONLY;
import static com.gramegrame.api.proto.java.ResponseType.COST_ANSWER;
import static org.junit.jupiter.api.Assertions.*;

import com.grame.services.fees.calculation.meta.FixedUsageEstimates;
import com.gramegrame.api.proto.java.NetworkGetVersionInfoQuery;
import com.gramegrame.api.proto.java.Query;
import com.gramegrame.api.proto.java.ResponseType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetVersionInfoResourceUsageTest {
	private GetVersionInfoResourceUsage subject;

	private Query nonVersionInfoQuery;
	private Query versionInfoQuery;

	@BeforeEach
	private void setup() throws Throwable {
		versionInfoQuery = Query.newBuilder()
				.setNetworkGetVersionInfo(NetworkGetVersionInfoQuery.getDefaultInstance())
				.build();
		nonVersionInfoQuery = Query.getDefaultInstance();

		subject = new GetVersionInfoResourceUsage();
	}

	@Test
	public void recognizesApplicability() {
		// expect:
		assertTrue(subject.applicableTo(versionInfoQuery));
		assertFalse(subject.applicableTo(nonVersionInfoQuery));
	}

	@Test
	public void getsExpectedUsage() {
		// expect:
		assertEquals(
				FixedUsageEstimates.getVersionInfoUsage(),
				subject.usageGivenType(versionInfoQuery, null, COST_ANSWER));
		assertEquals(
				FixedUsageEstimates.getVersionInfoUsage(),
				subject.usageGivenType(versionInfoQuery, null, ANSWER_ONLY));
	}
}
