package com.grame.services.sigs.order;

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

import com.google.common.base.MoreObjects;
import com.grame.services.legacy.core.jproto.JKey;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.EMPTY_LIST;

/**
 * Summarize the outcome of trying to list, in canonical order, the grame keys
 * that must have active signatures for some gRCP transaction to be valid.
 *
 * The main purpose of this class is to let calls to methods of {@link grameSigningOrder}
 * be unchecked. This makes it easier to read and understand client code.
 *
 * @param <T> the type of error report that may be contained in this summary.
 * @author AmilyTech
 */
public class SigningOrderResult<T> {
	private final List<JKey> orderedKeys;
	private final Optional<T> errorReport;

	private static final SigningOrderResult<?> NO_KNOWN_KEYS = new SigningOrderResult<>(EMPTY_LIST);

	@SuppressWarnings("unchecked")
	public static <T> SigningOrderResult<T> noKnownKeys() {
		return (SigningOrderResult<T>)NO_KNOWN_KEYS;
	}

	public SigningOrderResult(List<JKey> orderedKeys) {
		this(orderedKeys, Optional.empty());
	}
	public SigningOrderResult(T errorReport) {
		this(EMPTY_LIST, Optional.of(errorReport));
	}
	public SigningOrderResult(List<JKey> orderedKeys, Optional<T> errorReport) {
		this.orderedKeys = orderedKeys;
		this.errorReport = errorReport;
	}

	public boolean hasKnownOrder() {
		return !errorReport.isPresent();
	}

	public boolean hasErrorReport() {
		return errorReport.isPresent();
	}

	public List<JKey> getOrderedKeys() {
		return orderedKeys;
	}

	public JKey getPayerKey() {
		return orderedKeys.get(0);
	}

	public T getErrorReport() {
		return errorReport.get();
	}

	@Override
	public String toString() {
		if (hasErrorReport()) {
			return MoreObjects.toStringHelper(SigningOrderResult.class)
					.add("outcome", "FAILURE")
					.add("details", errorReport.get())
					.toString();
		} else {
			return MoreObjects.toStringHelper(SigningOrderResult.class)
					.add("outcome", "SUCCESS")
					.add("keys", orderedKeys)
					.toString();
		}
	}
}
