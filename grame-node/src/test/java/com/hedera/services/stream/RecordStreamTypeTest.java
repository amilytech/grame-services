package com.grame.services.stream;

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

import java.io.File;

import static com.grame.services.stream.RecordStreamType.RECORD;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecordStreamTypeTest {
	public static final File NULL_FILE = null;

	public static final String RECORD_FILE_NAME = "test.rcd";
	public static final File RECORD_FILE = new File(RECORD_FILE_NAME);
	public static final String RECORD_SIG_FILE_NAME = "test.rcd_sig";
	public static final File RECORD_SIG_FILE = new File(RECORD_SIG_FILE_NAME);

	public static final String EVENT_FILE_NAME = "test.evts";
	public static final File EVENT_FILE = new File(EVENT_FILE_NAME);
	public static final String EVENT_SIG_FILE_NAME = "test.evts_sig";
	public static final File EVENT_SIG_FILE = new File(EVENT_SIG_FILE_NAME);

	public static final String NON_STREAM_FILE_NAME = "test.soc";
	public static final File NON_STREAM_FILE = new File(NON_STREAM_FILE_NAME);

	private static final String IS_STREAM_FILE_ERROR_MSG = "isStreamFile() returns unexpected result";
	private static final String IS_STREAM_SIG_FILE_ERROR_MSG = "isStreamSigFile() returns unexpected result";

	@Test
	public void isStreamFileTest() {
		assertFalse(RECORD.isStreamFile(NULL_FILE), IS_STREAM_FILE_ERROR_MSG);

		assertTrue(RECORD.isStreamFile(RECORD_FILE_NAME), IS_STREAM_FILE_ERROR_MSG);
		assertTrue(RECORD.isStreamFile(RECORD_FILE), IS_STREAM_FILE_ERROR_MSG);

		assertFalse(RECORD.isStreamFile(RECORD_SIG_FILE_NAME), IS_STREAM_FILE_ERROR_MSG);
		assertFalse(RECORD.isStreamFile(RECORD_SIG_FILE), IS_STREAM_FILE_ERROR_MSG);

		assertFalse(RECORD.isStreamFile(NON_STREAM_FILE_NAME), IS_STREAM_FILE_ERROR_MSG);
		assertFalse(RECORD.isStreamFile(NON_STREAM_FILE), IS_STREAM_FILE_ERROR_MSG);

		assertFalse(RECORD.isStreamFile(EVENT_FILE_NAME), IS_STREAM_FILE_ERROR_MSG);
		assertFalse(RECORD.isStreamFile(EVENT_FILE), IS_STREAM_FILE_ERROR_MSG);

		assertFalse(RECORD.isStreamFile(EVENT_SIG_FILE_NAME), IS_STREAM_FILE_ERROR_MSG);
		assertFalse(RECORD.isStreamFile(EVENT_SIG_FILE), IS_STREAM_FILE_ERROR_MSG);
	}

	@Test
	public void isStreamSigFileTest() {
		assertFalse(RECORD.isStreamFile(NULL_FILE), IS_STREAM_SIG_FILE_ERROR_MSG);

		assertTrue(RECORD.isStreamSigFile(RECORD_SIG_FILE_NAME), IS_STREAM_SIG_FILE_ERROR_MSG);
		assertTrue(RECORD.isStreamSigFile(RECORD_SIG_FILE), IS_STREAM_SIG_FILE_ERROR_MSG);

		assertFalse(RECORD.isStreamSigFile(RECORD_FILE_NAME), IS_STREAM_SIG_FILE_ERROR_MSG);
		assertFalse(RECORD.isStreamSigFile(RECORD_FILE), IS_STREAM_SIG_FILE_ERROR_MSG);

		assertFalse(RECORD.isStreamSigFile(EVENT_FILE_NAME), IS_STREAM_SIG_FILE_ERROR_MSG);
		assertFalse(RECORD.isStreamSigFile(EVENT_FILE), IS_STREAM_SIG_FILE_ERROR_MSG);

		assertFalse(RECORD.isStreamSigFile(EVENT_SIG_FILE_NAME), IS_STREAM_SIG_FILE_ERROR_MSG);
		assertFalse(RECORD.isStreamSigFile(EVENT_SIG_FILE), IS_STREAM_SIG_FILE_ERROR_MSG);

		assertFalse(RECORD.isStreamSigFile(NON_STREAM_FILE_NAME), IS_STREAM_SIG_FILE_ERROR_MSG);
		assertFalse(RECORD.isStreamSigFile(NON_STREAM_FILE), IS_STREAM_SIG_FILE_ERROR_MSG);
	}

	@Test
	public void getTest() {
		assertEquals(RecordStreamType.RECORD_DESCRIPTION, RECORD.getDescription());
		assertEquals(RecordStreamType.RECORD_EXTENSION, RECORD.getExtension());
		assertEquals(RecordStreamType.RECORD_SIG_EXTENSION, RECORD.getSigExtension());
	}
}

