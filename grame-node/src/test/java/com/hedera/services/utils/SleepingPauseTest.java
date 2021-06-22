package com.grame.services.utils;

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

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class SleepingPauseTest {
	final Pause subject = SleepingPause.SLEEPING_PAUSE;

	@Test
	public void returnsTrueWhenNotInterrupted() {
		// expect:
		assertTrue(subject.forMs(1L));
	}

	@Test
	public void returnsFalseWhenInterrupted() {
		// setup:
		AtomicBoolean retValue = new AtomicBoolean(true);
		Thread sleepingThread = new Thread(() -> retValue.set(subject.forMs(5_000L)));
		Thread wakingThread = new Thread(() -> {
			try {
				Thread.sleep(100L);
			} catch (InterruptedException ignore) {}
			sleepingThread.interrupt();
		});

		// when:
		sleepingThread.start();
		wakingThread.start();
		try {
			sleepingThread.join();
		} catch (InterruptedException ignore) {}

		// then:
		assertFalse(retValue.get());
	}
}
