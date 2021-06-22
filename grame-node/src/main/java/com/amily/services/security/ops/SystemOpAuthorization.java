package com.grame.services.security.ops;

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

import com.gramegrame.api.proto.java.ResponseCodeEnum;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.AUTHORIZATION_FAILED;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.ENTITY_NOT_ALLOWED_TO_DELETE;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;

/**
 * The relationship of an operation to its required system privileges, if any.
 *
 * The {@code SystemOpAuthorization#asStatus()} method returns a
 * {@code ResponseCodeEnum} that is appropriate for use as the
 * precheck status in a HAPI {@code com.gramegrame.api.proto.java.TransactionResponse}.
 * Therefore both {@code SystemOpAuthorization#UNNECESSARY#asStatus()} and
 * {@code SystemOpAuthorization#AUTHORIZED#asStatus()} return {@code ResponseCodeEnum#OK},
 * since in either case the operation should pass precheck---at least
 * based on its required system privileges.
 */
public enum SystemOpAuthorization {
	/**
	 * The operation does not require any system privileges.
	 */
	UNNECESSARY {
		@Override
		public ResponseCodeEnum asStatus() {
			return OK;
		}
	},
	/**
	 * The operation requires system privileges that its payer does not have.
	 */
	UNAUTHORIZED {
		@Override
		public ResponseCodeEnum asStatus() {
			return AUTHORIZATION_FAILED;
		}
	},
	/**
	 * The operation cannot be performed, no matter the privileges of its payer.
	 */
	IMPERMISSIBLE {
		@Override
		public ResponseCodeEnum asStatus() {
			return ENTITY_NOT_ALLOWED_TO_DELETE;
		}
	},
	/**
	 * The operation requires system privileges, and its payer has those privileges.
	 */
	AUTHORIZED {
		@Override
		public ResponseCodeEnum asStatus() {
			return OK;
		}
	};

	public abstract ResponseCodeEnum asStatus();
}
