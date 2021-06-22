package com.grame.services.sigs.order;

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

public enum KeyOrderingFailure {
	NONE,
	MISSING_FILE,
	MISSING_ACCOUNT,
	INVALID_CONTRACT,
	IMMUTABLE_CONTRACT,
	MISSING_AUTORENEW_ACCOUNT,
	MISSING_TOKEN,
	INVALID_TOPIC,
	MISSING_TOKEN_TREASURY,
	MISSING_SCHEDULE,
	INVALID_ACCOUNT
}
