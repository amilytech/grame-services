package com.grame.services.files;

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

import com.grame.services.fees.calculation.FeeCalcUtilsTest;
import com.grame.test.utils.IdUtils;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.grame.services.files.DataMapFactory.*;

class DataMapFactoryTest {
	@Test
	public void toFidConversionWorks() {
		// given:
		var key = "/666/f888";
		// and:
		var expected = IdUtils.asFile("0.666.888");

		// when:
		var actual = toFid(key);

		// then:
		assertEquals(expected, actual);
	}

	@Test
	public void toKeyConversionWorks() {
		// given:
		var fid = IdUtils.asFile("0.2.3");
		// and:
		var expected = FeeCalcUtilsTest.pathOf(fid);

		// when:
		var actual = toKeyString(fid);

		// then:
		assertEquals(expected, actual);
	}

	private String asLegacyPath(String fid) {
		return FeeCalcUtilsTest.pathOf(IdUtils.asFile(fid));
	}

	@Test
	public void productHasMapSemantics() {
		// setup:
		Map<String, byte[]> delegate = new HashMap<>();
		delegate.put(asLegacyPath("0.2.7"), "APRIORI".getBytes());
		// and:
		var fid1 = IdUtils.asFile("0.2.3");
		var fid2 = IdUtils.asFile("0.3333.4");
		var fid3 = IdUtils.asFile("0.4.555555");
		// and:
		var theData = "THE".getBytes();
		var someData = "SOME".getBytes();
		var moreData = "MORE".getBytes();

		// given:
		var dataMap = dataMapFrom(delegate);

		// when:
		dataMap.put(fid1, someData);
		dataMap.put(fid2, moreData);
		dataMap.put(fid3, theData);

		assertFalse(dataMap.isEmpty());
		assertEquals(4, dataMap.size());
		dataMap.remove(fid2);
		assertEquals(3, dataMap.size());
		assertEquals(
				"/2/f3->SOME, /4/f555555->THE, /2/f7->APRIORI",
				delegate.entrySet()
						.stream()
						.sorted(Comparator.comparingLong(entry ->
								Long.parseLong(entry.getKey().substring(
									entry.getKey().indexOf('f') + 1, entry.getKey().indexOf('f') + 2
								))))
						.map(entry -> String.format("%s->%s", entry.getKey(), new String(entry.getValue())))
						.collect(Collectors.joining(", ")));

		assertTrue(dataMap.containsKey(fid1));
		assertFalse(dataMap.containsKey(fid2));

		dataMap.clear();
		assertTrue(dataMap.isEmpty());
	}

	@Test
	public void cannotBeConstructed() {
		// expect:
		assertThrows(IllegalStateException.class, DataMapFactory::new);
	}
}
