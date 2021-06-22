package com.grame.services.state.migrations;

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
import com.grame.services.state.migration.StdStateMigrations;
import com.grame.services.utils.Pause;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.mock;

public class StdStateMigrationsTest {
	Pause pause;
	ServicesContext ctx;
	StdStateMigrations subject;

	@BeforeEach
	private void setup() {
		ctx = mock(ServicesContext.class);
		pause = mock(Pause.class);
		subject = new StdStateMigrations(pause);
	}

	@Test
	public void isNoop() {
		// expect:
		assertDoesNotThrow(() -> subject.runAllFor(ctx));
	}
}
