package com.grame.services.files.sysfiles;

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

import com.grame.services.fees.FeeMultiplierSource;
import com.grame.services.throttling.FunctionalityThrottling;
import com.grame.test.utils.SerdeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.BDDMockito.*;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
class ThrottlesCallbackTest {
	@Mock
	FeeMultiplierSource multiplierSource;
	@Mock
	FunctionalityThrottling hapiThrottling;
	@Mock
	FunctionalityThrottling handleThrottling;

	ThrottlesCallback subject;

	@BeforeEach
	void setUp() {
		subject = new ThrottlesCallback(multiplierSource, hapiThrottling, handleThrottling);
	}

	@Test
	void throttlesCbAsExpected() throws IOException {
		var throttles = SerdeUtils.protoDefs("bootstrap/throttles.json");

		// when:
		subject.throttlesCb().accept(throttles);

		// then:
		verify(hapiThrottling).rebuildFor(argThat(pojo -> pojo.toProto().equals(throttles)));
		verify(handleThrottling).rebuildFor(argThat(pojo -> pojo.toProto().equals(throttles)));
		verify(multiplierSource).resetExpectations();
	}
}
