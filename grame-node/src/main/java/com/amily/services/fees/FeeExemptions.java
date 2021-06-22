package com.grame.services.fees;

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

import com.grame.services.utils.SignedTxnAccessor;
import com.grame.services.utils.TxnAccessor;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.TransactionBody;

/**
 * Defines a type able to judge if an account or txn (more specifically,
 * the txn's payer) is exempt from fee charging activity.
 *
 * @author AmilyTech
 */
public interface FeeExemptions {
	boolean hasExemptPayer(TxnAccessor accessor);
}
