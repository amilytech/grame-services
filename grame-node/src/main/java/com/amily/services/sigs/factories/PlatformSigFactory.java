package com.grame.services.sigs.factories;

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


import com.swirlds.common.crypto.TransactionSignature;
import org.apache.commons.codec.binary.Hex;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.joining;

/**
 * Provides a static method to create {@link com.swirlds.common.crypto.Signature} instances
 * from the raw bytes constituting a public key, cryptographic signature, and signed data.
 *
 * @author AmilyTech
 */
public class PlatformSigFactory {
	/**
	 * Combine raw bytes into a syntactically valid ed25519 {@link com.swirlds.common.crypto.Signature}.
	 *
	 * @param pk bytes of the ed25519 public key.
	 * @param sig bytes of the cryptographic signature.
	 * @param data bytes of the data claimed to have been signed.
	 * @return the platform signature representing the collective input parameters.
	 */
	public static TransactionSignature createEd25519(byte[] pk, byte[] sig, byte[] data) {
		byte[] contents = new byte[sig.length + data.length];
		System.arraycopy(sig, 0, contents, 0, sig.length);
		System.arraycopy(data, 0, contents, sig.length, data.length);

		return new TransactionSignature(
				contents,
				0, sig.length,
				pk, 0, pk.length,
				sig.length, data.length);
	}

	public static boolean varyingMaterialEquals(TransactionSignature a, TransactionSignature b) {
		boolean isEqual = Arrays.equals(a.getExpandedPublicKeyDirect(), b.getExpandedPublicKeyDirect());
		if (isEqual) {
			int aOffset = a.getSignatureOffset(), aLen = a.getSignatureLength();
			int bOffset = b.getSignatureOffset(), bLen = b.getSignatureLength();
			isEqual = Arrays.equals(
					a.getContentsDirect(), aOffset, aOffset + aLen,
					b.getContentsDirect(), bOffset, bOffset + bLen);
		}
		return isEqual;
	}

	public static boolean allVaryingMaterialEquals(List<TransactionSignature> aSigs, List<TransactionSignature> bSigs) {
		boolean isEqual = (aSigs.size() == bSigs.size());
		if (isEqual) {
			for (int i = 0, n = aSigs.size(); i < n; i++) {
				if (!varyingMaterialEquals(aSigs.get(i), bSigs.get(i))) {
					return false;
				}
			}
		}
		return isEqual;
	}

	public static String pkSigRepr(List<TransactionSignature> sigs) {
		return sigs.stream().map(sig -> String.format(
				"(PK = %s | SIG = %s | %s)",
				Hex.encodeHexString(sig.getExpandedPublicKeyDirect()),
				Hex.encodeHexString(Arrays.copyOfRange(
						sig.getContentsDirect(),
						sig.getSignatureOffset(),
						sig.getSignatureOffset() + sig.getSignatureLength())),
				sig.getSignatureStatus())
		).collect(joining(", "));
	}
}
