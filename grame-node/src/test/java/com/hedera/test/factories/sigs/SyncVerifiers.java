package com.grame.test.factories.sigs;

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

import com.grame.services.sigs.verification.SyncVerifier;
import com.swirlds.common.crypto.Signature;
import com.swirlds.common.crypto.TransactionSignature;

import java.util.List;

import static com.grame.test.factories.sigs.SigWrappers.asInvalid;
import static com.grame.test.factories.sigs.SigWrappers.asValid;

public class SyncVerifiers {
	public static final SyncVerifier NEVER_VALID = l -> { List<TransactionSignature> lv = asInvalid(l); l.clear(); l.addAll(lv); };
	public static final SyncVerifier ALWAYS_VALID = l -> { List<TransactionSignature> lv = asValid(l); l.clear(); l.addAll(lv); };
}
