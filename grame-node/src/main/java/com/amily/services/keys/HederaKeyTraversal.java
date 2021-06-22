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

import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.legacy.core.jproto.JKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Provides static helpers for interrogating the simple keys in a complex grame key.
 *
 * @author AmilyTech
 * @see JKey
 */
public class grameKeyTraversal {

	private static final Logger log = LogManager.getLogger(grameKeyTraversal.class);

	private grameKeyTraversal(){
		throw new IllegalStateException("Utility Class");
	}

	/**
	 * Performs a left-to-right DFS of the grame key structure, offering each simple key to
	 * the provided {@link Consumer}.
	 *
	 * @param key the top-level grame key to traverse.
	 * @param actionOnSimpleKey the logic to apply to each visited simple key.
	 */
	public static void visitSimpleKeys(JKey key, Consumer<JKey> actionOnSimpleKey) {
		if (key.hasThresholdKey()) {
			key.getThresholdKey().getKeys().getKeysList().forEach(k -> visitSimpleKeys(k, actionOnSimpleKey));
		} else if (key.hasKeyList()) {
			key.getKeyList().getKeysList().forEach(k -> visitSimpleKeys(k, actionOnSimpleKey));
		} else {
			actionOnSimpleKey.accept(key);
		}
	}

	/**
	 * Counts the simple keys present in a complex grame key.
	 *
	 * @param key the top-level grame key.
	 * @return the number of simple keys in the leaves of the grame key.
	 */
	public static int numSimpleKeys(JKey key) {
		AtomicInteger count = new AtomicInteger(0);
		visitSimpleKeys(key, ignore -> count.incrementAndGet());
		return count.get();
	}

	/** Counts the simple keys present in an account's grame key.
	 *
	 * @param account the account with the grame key of interest.
	 * @return the number of simple keys.
	 */
	public static int numSimpleKeys(MerkleAccount account) {
		try {
			return numSimpleKeys(account.getKey());
		} catch (Exception ignore) {
			log.warn(ignore.getMessage());
			return 0;
		}
	}
 }
