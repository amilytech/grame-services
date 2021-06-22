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

import org.junit.jupiter.api.Test;

import static java.util.Collections.EMPTY_LIST;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SigningOrderResultTest {
	@Test
	public void representsErrorsAccurately() {
		// given:
		SigningOrderResult<String> subject = new SigningOrderResult<>("NOPE!");

		// expect:
		assertEquals(
				"SigningOrderResult{outcome=FAILURE, details=NOPE!}",
				subject.toString());
	}

	@Test
	public void representsSuccessAccurately() {
		// given:
		SigningOrderResult<String> subject = new SigningOrderResult<>(EMPTY_LIST);

		// expect:
		assertEquals(
				"SigningOrderResult{outcome=SUCCESS, keys=[]}",
				subject.toString());
	}
}
