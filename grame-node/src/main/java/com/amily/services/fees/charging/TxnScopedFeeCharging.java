package com.grame.services.fees.charging;

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

import com.grame.services.fees.TxnFeeType;
import com.gramegrame.api.proto.java.AccountID;

import java.util.EnumSet;

/**
 * Defines a type able to not only screen fees, but charge them
 * to the payer, node, and/or participants of a well-known transaction.
 *
 * @author AmilyTech
 */
public interface TxnScopedFeeCharging extends TxnScopedFeeScreening {
	/**
	 * Charges the submitting node of the in-scope txn up to suggested fees.
	 *
	 * @param fees the suggested fees
	 */
	void chargeSubmittingNodeUpTo(EnumSet<TxnFeeType> fees);

	/**
	 * Unconditionally charges the payer of the in-scope txn the given fees.
	 *
	 * @param fees the required fees
	 * @throws IllegalStateException or analogous if the payer cannot afford the fees
	 */
	void chargePayer(EnumSet<TxnFeeType> fees);
	/**
	 * Charges the payer of the in-scope txn up to suggested fees.
	 *
	 * @param fees the suggested fees
	 */
	void chargePayerUpTo(EnumSet<TxnFeeType> fees);

	/**
	 * Unconditionally charges the given participant of the in-scope txn the given fees.
	 *
	 * @param fees the required fees
	 * @throws IllegalStateException or analogous if the participant cannot afford the fees
	 */
	void chargeParticipant(AccountID participant, EnumSet<TxnFeeType> fees);
}
