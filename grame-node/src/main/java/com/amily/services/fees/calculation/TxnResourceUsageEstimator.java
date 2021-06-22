package com.grame.services.fees.calculation;

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

import com.grame.services.context.primitives.StateView;
import com.gramegrame.api.proto.java.FeeData;
import com.gramegrame.api.proto.java.TransactionBody;
import com.gramegrame.exception.InvalidTxBodyException;
import com.gramegrame.fee.SigValueObj;

/**
 * Defines a type able to estimate the resource usage of one (or more)
 * transaction operations, relative to a particular state of the world.
 *
 * @author AmilyTech
 */
public interface TxnResourceUsageEstimator {
	/**
	 * Flags whether the estimator applies to the given transaction.
	 *
	 * @param txn the txn in question
	 * @return if the estimator applies
	 */
	boolean applicableTo(TransactionBody txn);

	/**
	 * Returns the estimated resource usage for the given txn relative
	 * to the given state of the world.
	 *
	 * @param txn the txn in question
	 * @param view the state of the world
	 * @return the estimated resource usage
	 * @throws InvalidTxBodyException if the txn is malformed
	 * @throws NullPointerException or analogous if the estimator does not apply to the txn
	 */
	FeeData usageGiven(TransactionBody txn, SigValueObj sigUsage, StateView view) throws InvalidTxBodyException;
}
