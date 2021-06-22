package com.grame.services.context.properties;

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

import com.grame.services.exceptions.UnparseablePropertyException;
import com.grame.services.fees.calculation.CongestionMultipliers;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.grameFunctionality;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

/**
 * Defines a source of arbitrary properties keyed by strings. Provides
 * strongly typed accessors for commonly used property types.
 *
 * @author AmilyTech
 */
public interface PropertySource {
	Logger log = LogManager.getLogger(PropertySource.class);

	Function<String, Object> AS_INT = Integer::valueOf;
	Function<String, Object> AS_LONG = Long::valueOf;
	Function<String, Object> AS_DOUBLE = Double::valueOf;
	Function<String, Object> AS_STRING = s -> s;
	Function<String, Object> AS_PROFILE = v -> Profile.valueOf(v.toUpperCase());
	Function<String, Object> AS_BOOLEAN = Boolean::valueOf;
	Function<String, Object> AS_FUNCTIONS = s -> Arrays.stream(s.split(","))
			.map(grameFunctionality::valueOf)
			.collect(toSet());
	Function<String, Object> AS_CONGESTION_MULTIPLIERS = CongestionMultipliers::from;

	boolean containsProperty(String name);
	Object getProperty(String name);
	Set<String> allPropertyNames();

	default <T> T getTypedProperty(Class<T> type, String name) {
		return type.cast(getProperty(name));
	}
	default String getStringProperty(String name) {
		return getTypedProperty(String.class, name);
	}
	default boolean getBooleanProperty(String name) {
		return getTypedProperty(Boolean.class, name);
	}
	@SuppressWarnings("unchecked")
	default Set<grameFunctionality> getFunctionsProperty(String name) {
		return (Set<grameFunctionality>)getTypedProperty(Set.class, name);
	}
	default CongestionMultipliers getCongestionMultiplierProperty(String name) {
		return getTypedProperty(CongestionMultipliers.class, name);
	}
	default int getIntProperty(String name) {
		return getTypedProperty(Integer.class, name);
	}
	default double getDoubleProperty(String name) {
		return getTypedProperty(Double.class, name);
	}
	default long getLongProperty(String name) {
		return getTypedProperty(Long.class, name);
	}
	default Profile getProfileProperty(String name) {
		return getTypedProperty(Profile.class, name);
	}
	default AccountID getAccountProperty(String name) {
		String value = "";
		try {
			value = getStringProperty(name);
			long[] nums = Stream.of(value.split("[.]")).mapToLong(Long::parseLong).toArray();
			return AccountID.newBuilder().setShardNum(nums[0])
										.setRealmNum(nums[1])
										.setAccountNum(nums[2]).build();
		} catch (Exception any) {
			log.info(any.getMessage());
			throw new UnparseablePropertyException(name, value);
		}
	}
}
