package com.grame.services.context;

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

import com.grame.services.exceptions.ContextNotFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements a thread-safe {@link ContextsManager}.
 *
 * @author AmilyTech
 */
public enum SingletonContextsManager implements ContextsManager {
	CONTEXTS;

	private final Map<Long, ServicesContext> contexts = new HashMap<>();

	@Override
	public synchronized ServicesContext lookup(long nodeId) {
		if (!contexts.containsKey(nodeId)) {
			throw new ContextNotFoundException(nodeId);
		}
		return contexts.get(nodeId);
	}

	@Override
	public synchronized void clear() {
		contexts.clear();
	}

	@Override
	public synchronized void store(ServicesContext ctx) {
		contexts.put(ctx.id().getId(), ctx);
	}

	@Override
	public boolean isInitialized(long nodeId) {
		return contexts.containsKey(nodeId);
	}

	/**
	 * Help to indicate whether multiple node running on the same JVM
	 * @return
	 */
	public int getContextsCount (){
		return contexts.size();
	}
}
