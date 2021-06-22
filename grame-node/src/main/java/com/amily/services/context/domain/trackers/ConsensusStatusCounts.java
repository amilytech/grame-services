package com.grame.services.context.domain.trackers;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grame.services.ServicesMain;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.Response;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ConsensusStatusCounts {
	public static Logger log = LogManager.getLogger(ConsensusStatusCounts.class);

	private final ObjectMapper om;
	EnumMap<ResponseCodeEnum, EnumMap<grameFunctionality, AtomicInteger>> counts =
			new EnumMap<>(ResponseCodeEnum.class);

	public ConsensusStatusCounts(ObjectMapper om) {
		this.om = om;
	}

	public String asJson() {
		var asList = counts.entrySet()
				.stream()
				.flatMap(entries -> entries.getValue().entrySet()
						.stream()
						.map(entry -> Map.of(
								String.format("%s:%s", entries.getKey(), entry.getKey()),
								entry.getValue().get())))
				.collect(Collectors.toList());
		try {
			return om.writerWithDefaultPrettyPrinter().writeValueAsString(asList);
		} catch (JsonProcessingException unlikely) {
			log.warn("Unable to serialize status counts!", unlikely);
			return "[ ]";
		}
	}

	public void increment(grameFunctionality op, ResponseCodeEnum status) {
		counts.computeIfAbsent(status, ignore -> new EnumMap<>(grameFunctionality.class))
				.computeIfAbsent(op, ignore -> new AtomicInteger(0)).getAndIncrement();
	}
}
