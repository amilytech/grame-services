package com.grame.services.keys;

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

import com.grame.services.legacy.core.AccountKeyListObj;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class LegacyEd25519KeyReader {
	public static final Logger log = LogManager.getLogger(LegacyEd25519KeyReader.class);

	public String hexedABytesFrom(String b64EncodedKeyPairLoc, String keyPairId) {
		try {
			var b64Encoded = Files.readString(Paths.get(b64EncodedKeyPairLoc));

			byte[] bytes = Base64.getDecoder().decode(b64Encoded);
			Map<String, List<AccountKeyListObj>> keyPairMap = (Map<String, List<AccountKeyListObj>>)readObjFrom(bytes);

			var keyPairs = keyPairMap.get(keyPairId);
			return keyPairs.get(0).getKeyPairList().get(0).getPublicKeyAbyteStr();
		} catch (Exception e) {
			throw new IllegalArgumentException(
					String.format(
							"Argument 'b64EncodedKeyPairLoc=%s' not a Base64-encoded key pair!",
							b64EncodedKeyPairLoc), e);
		}
	}

	private Object readObjFrom(byte[] bytes) throws Exception {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
			try (ObjectInput oi = new ObjectInputStream(bais)) {
				return oi.readObject();
			}
		}
	}
}
