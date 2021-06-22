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

import com.grame.services.sigs.metadata.lookups.SafeLookupResult;
import com.grame.services.store.schedule.ScheduleStore;
import com.grame.services.store.tokens.TokenStore;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.ContractID;
import com.gramegrame.api.proto.java.FileID;
import com.gramegrame.api.proto.java.ScheduleID;
import com.gramegrame.api.proto.java.TokenID;
import com.gramegrame.api.proto.java.TopicID;

import java.util.function.Function;

import static com.grame.services.sigs.metadata.ScheduleSigningMetadata.from;
import static com.grame.services.sigs.metadata.TokenSigningMetadata.from;
import static com.grame.services.sigs.metadata.lookups.SafeLookupResult.failure;
import static com.grame.services.sigs.order.KeyOrderingFailure.MISSING_SCHEDULE;
import static com.grame.services.sigs.order.KeyOrderingFailure.MISSING_TOKEN;

/**
 * Defines a type able to look up metadata associated to the signing activities
 * of any grame entity (account, smart contract, file, topic, or token).
 *
 * @author AmilyTech
 */
public interface SigMetadataLookup {
	Function<
			TokenStore,
			Function<TokenID, SafeLookupResult<TokenSigningMetadata>>> REF_LOOKUP_FACTORY = tokenStore -> ref -> {
		TokenID id;
		return TokenStore.MISSING_TOKEN.equals(id = tokenStore.resolve(ref))
				? failure(MISSING_TOKEN)
				: new SafeLookupResult<>(from(tokenStore.get(id)));
	};
	Function<
			ScheduleStore,
			Function<ScheduleID, SafeLookupResult<ScheduleSigningMetadata>>> SCHEDULE_REF_LOOKUP_FACTORY = scheduleStore -> ref -> {
		ScheduleID id;
		return ScheduleStore.MISSING_SCHEDULE.equals(id = scheduleStore.resolve(ref))
				? failure(MISSING_SCHEDULE)
				: new SafeLookupResult<>(from(scheduleStore.get(id)));
	};

	SafeLookupResult<FileSigningMetadata> fileSigningMetaFor(FileID id);
	SafeLookupResult<TopicSigningMetadata> topicSigningMetaFor(TopicID id);
	SafeLookupResult<TokenSigningMetadata> tokenSigningMetaFor(TokenID id);
	SafeLookupResult<AccountSigningMetadata> accountSigningMetaFor(AccountID id);
	SafeLookupResult<ScheduleSigningMetadata> scheduleSigningMetaFor(ScheduleID id);
	SafeLookupResult<ContractSigningMetadata> contractSigningMetaFor(ContractID id);
}
