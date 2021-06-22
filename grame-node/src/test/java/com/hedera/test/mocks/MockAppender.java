package com.grame.test.mocks;

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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import java.util.ArrayList;
import java.util.List;

public class MockAppender extends AbstractAppender {
	private final List<String> messages = new ArrayList<>();

	public MockAppender() {
		super("MockAppender", null, null, true, null);
	}

	@Override
	public void append(LogEvent event) {
		messages.add(String.format("%s - %s", event.getLevel(), event.getMessage().getFormattedMessage()));
	}

	public int size() {
		return messages.size();
	}

	public void clear() {
		messages.clear();
	}

	public String get(int index) {
		return messages.get(index);
	}
}
