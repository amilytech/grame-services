package com.grame.services.stats;

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

import com.grame.services.context.properties.NodeLocalProperties;
import com.swirlds.common.Platform;
import com.swirlds.platform.StatsRunningAverage;

public class MiscRunningAvgs {
	private final RunningAvgFactory runningAvg;

	StatsRunningAverage accountRetryWaitMs;
	StatsRunningAverage accountLookupRetries;
	StatsRunningAverage handledSubmitMessageSize;

	StatsRunningAverage writeQueueSizeRecordStream;
	StatsRunningAverage hashQueueSizeRecordStream;

	public MiscRunningAvgs(RunningAvgFactory runningAvg, NodeLocalProperties properties) {
		this.runningAvg = runningAvg;

		double halfLife = properties.statsRunningAvgHalfLifeSecs();

		accountRetryWaitMs = new StatsRunningAverage(halfLife);
		accountLookupRetries = new StatsRunningAverage(halfLife);
		handledSubmitMessageSize = new StatsRunningAverage(halfLife);

		writeQueueSizeRecordStream = new StatsRunningAverage(halfLife);
		hashQueueSizeRecordStream = new StatsRunningAverage(halfLife);
	}

	public void registerWith(Platform platform) {
		platform.addAppStatEntry(
				runningAvg.from(
						Names.ACCOUNT_LOOKUP_RETRIES,
						Descriptions.ACCOUNT_LOOKUP_RETRIES,
						accountLookupRetries));
		platform.addAppStatEntry(
				runningAvg.from(
						Names.ACCOUNT_RETRY_WAIT_MS,
						Descriptions.ACCOUNT_RETRY_WAIT_MS,
						accountRetryWaitMs));
		platform.addAppStatEntry(
				runningAvg.from(
						Names.HANDLED_SUBMIT_MESSAGE_SIZE,
						Descriptions.HANDLED_SUBMIT_MESSAGE_SIZE,
						handledSubmitMessageSize));
		platform.addAppStatEntry(
				runningAvg.from(
						Names.WRITE_QUEUE_SIZE_RECORD_STREAM,
						Descriptions.WRITE_QUEUE_SIZE_RECORD_STREAM,
						writeQueueSizeRecordStream));
		platform.addAppStatEntry(
				runningAvg.from(
						Names.HASH_QUEUE_SIZE_RECORD_STREAM,
						Descriptions.HASH_QUEUE_SIZE_RECORD_STREAM,
						hashQueueSizeRecordStream
				)
		);
	}

	public void recordAccountLookupRetries(int num) {
		accountLookupRetries.recordValue(num);
	}

	public void recordAccountRetryWaitMs(double time) {
		accountRetryWaitMs.recordValue(time);
	}

	public void recordHandledSubmitMessageSize(int bytes) {
		handledSubmitMessageSize.recordValue(bytes);
	}

	public void writeQueueSizeRecordStream(int num) {
		writeQueueSizeRecordStream.recordValue(num);
	}

	public void hashQueueSizeRecordStream(int num) {
		hashQueueSizeRecordStream.recordValue(num);
	}

	static class Names {
		public static final String ACCOUNT_RETRY_WAIT_MS = "avgAcctRetryWaitMs";
		public static final String ACCOUNT_LOOKUP_RETRIES = "avgAcctLookupRetryAttempts";
		public static final String HANDLED_SUBMIT_MESSAGE_SIZE = "avgHdlSubMsgSize";

		public static final String WRITE_QUEUE_SIZE_RECORD_STREAM = "writeQueueSizeRecordStream";
		public static final String HASH_QUEUE_SIZE_RECORD_STREAM = "hashQueueSizeRecordStream";
	}

	static class Descriptions {
		public static final String ACCOUNT_RETRY_WAIT_MS =
				"average time is millis spent waiting to lookup the account number";
		public static final String ACCOUNT_LOOKUP_RETRIES =
				"average number of retry attempts made to lookup the account number";
		public static final String HANDLED_SUBMIT_MESSAGE_SIZE =
				"average size of the handled HCS submit message transaction";

		public static final String WRITE_QUEUE_SIZE_RECORD_STREAM =
				"size of the queue from which we take records and write to RecordStream file";
		public static final String HASH_QUEUE_SIZE_RECORD_STREAM = "size of working queue for calculating hash and runningHash";
	}
}
