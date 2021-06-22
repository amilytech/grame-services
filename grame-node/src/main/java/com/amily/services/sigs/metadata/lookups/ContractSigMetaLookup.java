package com.grame.services.sigs.metadata.lookups;

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

import com.grame.services.sigs.metadata.ContractSigningMetadata;
import com.gramegrame.api.proto.java.ContractID;

/**
 * Defines a simple type that is able to recover metadata about signing activity
 * associated with a given grame smart contract.
 *
 * @author AmilyTech
 */
public interface ContractSigMetaLookup {
	SafeLookupResult<ContractSigningMetadata> safeLookup(ContractID id);
}
