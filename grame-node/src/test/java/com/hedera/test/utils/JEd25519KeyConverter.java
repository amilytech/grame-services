package com.grame.test.utils;

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

import com.grame.services.legacy.core.jproto.JEd25519Key;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ArgumentConverter;
import org.spongycastle.util.encoders.Hex;

public final class JEd25519KeyConverter implements ArgumentConverter {
	@Override
	public Object convert(Object input, ParameterContext parameterContext)
			throws ArgumentConversionException {
		if (null == input) {
			return null;
		}
		if (!(input instanceof String)) {
			throw new ArgumentConversionException(input + " is not a string");
		}
		var inputString = (String) input;
		return new JEd25519Key(Hex.decode(inputString));
	}
}
