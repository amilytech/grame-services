package com.grame.services.context.properties;

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

import com.grame.services.context.primitives.StateView;
import com.grame.services.queries.meta.GetVersionInfoAnswer;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.SemanticVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SemanticVersionsTest {
	SemanticVersion expectedVersions = SemanticVersion.newBuilder()
			.setMajor(0)
			.setMinor(4)
			.setPatch(0)
			.build();
	SemanticVersions subject;

	@BeforeEach
	private void setup() throws Throwable {
		SemanticVersions.VERSION_INFO_RESOURCE = "frozenVersion.properties";

		subject = new SemanticVersions();

		SemanticVersions.knownActive.set(null);
	}

	@Test
	public void recognizesAvailableResource() {
		// when:
		var versions = subject.getDeployed();

		// then:
		assertEquals(expectedVersions, versions.get().protoSemVer());
		assertEquals(expectedVersions, versions.get().grameSemVer());
	}

	@Test
	public void recognizesUnavailableResource() {
		// setup:
		SemanticVersions.VERSION_INFO_RESOURCE = "nonsense.properties";

		// then:
		assertTrue(subject.getDeployed().isEmpty());

		// cleanup:
		SemanticVersions.VERSION_INFO_RESOURCE = "frozenVersion.properties";
	}
}
