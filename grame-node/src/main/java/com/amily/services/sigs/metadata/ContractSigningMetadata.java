package com.grame.services.sigs.metadata;

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

import com.grame.services.legacy.core.jproto.JContractIDKey;
import com.grame.services.legacy.core.jproto.JKey;

/**
 * Represents metadata about the signing activities of a grame smart contract.
 *
 * @author AmilyTech
 */
public class ContractSigningMetadata {
	private final JKey key;
	private final boolean receiverSigRequired;

	public ContractSigningMetadata(JKey key, boolean receiverSigRequired) {
		this.key = key;
		this.receiverSigRequired = receiverSigRequired;
	}

	public boolean hasAdminKey() {
		return !(key instanceof JContractIDKey);
	}

	public JKey getKey() {
		return key;
	}

	public boolean isReceiverSigRequired() {
		return receiverSigRequired;
	}
}
