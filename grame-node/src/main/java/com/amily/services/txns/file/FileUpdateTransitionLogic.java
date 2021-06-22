package com.grame.services.txns.file;

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

import com.grame.services.config.EntityNumbers;
import com.grame.services.context.TransactionContext;
import com.grame.services.files.grameFs;
import com.grame.services.files.TieredgrameFs;
import com.grame.services.txns.TransitionLogic;
import com.grame.services.txns.validation.OptionValidator;
import com.gramegrame.api.proto.java.Duration;
import com.gramegrame.api.proto.java.FileUpdateTransactionBody;
import com.gramegrame.api.proto.java.Key;
import com.gramegrame.api.proto.java.KeyList;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.TransactionBody;
import org.apache.commons.codec.DecoderException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.grame.services.files.TieredgrameFs.firstUnsuccessful;
import static com.grame.services.utils.MiscUtils.asFcKeyUnchecked;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.AUTORENEW_DURATION_NOT_IN_RANGE;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.BAD_ENCODING;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.FAIL_INVALID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.FILE_DELETED;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.INVALID_FILE_ID;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.MEMO_TOO_LONG;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.SUCCESS;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.UNAUTHORIZED;
import static java.lang.Boolean.TRUE;
import static java.lang.Math.max;

public class FileUpdateTransitionLogic implements TransitionLogic {
	private static final Logger log = LogManager.getLogger(FileUpdateTransitionLogic.class);

	private final Function<TransactionBody, ResponseCodeEnum> SYNTAX_CHECK = this::validate;

	private final grameFs hfs;
	private final EntityNumbers entityNums;
	private final OptionValidator validator;
	private final TransactionContext txnCtx;

	public FileUpdateTransitionLogic(
			grameFs hfs,
			EntityNumbers entityNums,
			OptionValidator validator,
			TransactionContext txnCtx
	) {
		this.hfs = hfs;
		this.entityNums = entityNums;
		this.txnCtx = txnCtx;
		this.validator = validator;
	}

	@Override
	public void doStateTransition() {
		var op = txnCtx.accessor().getTxn().getFileUpdate();

		try {
			var validity = assessedValidity(op);
			if (validity != OK) {
				txnCtx.setStatus(validity);
				return;
			}

			var target = op.getFileID();
			var attr = hfs.getattr(target);
			if (attr.isDeleted()) {
				txnCtx.setStatus(FILE_DELETED);
				return;
			}
			if (attr.getWacl().isEmpty() && (op.hasKeys() || !op.getContents().isEmpty())) {
				/* The transaction is trying to update an immutable file; in general, not a legal operation,
				but the semantics change for a superuser (i.e., sysadmin or treasury) updating a system file. */
				var isSysFile = entityNums.isSystemFile(target);
				var isSysAdmin = entityNums.accounts().isSuperuser(txnCtx.activePayer().getAccountNum());
				if (!(isSysAdmin && isSysFile)) {
					txnCtx.setStatus(UNAUTHORIZED);
					return;
				}
			}

			Optional<grameFs.UpdateResult> replaceResult = Optional.empty();
			if (!op.getContents().isEmpty()) {
				replaceResult = Optional.of(hfs.overwrite(target, op.getContents().toByteArray()));
			}
			attr.setExpiry(max(op.getExpirationTime().getSeconds(), attr.getExpiry()));

			Optional<grameFs.UpdateResult> changeResult = Optional.empty();
			if (replaceResult.map(grameFs.UpdateResult::fileReplaced).orElse(TRUE)) {
				if (op.hasKeys()) {
					attr.setWacl(asFcKeyUnchecked(wrapped(op.getKeys())));
				}
				if (op.hasMemo()) {
					attr.setMemo(op.getMemo().getValue());
				}
				changeResult = Optional.of(hfs.setattr(target, attr));
			}

			txnCtx.setStatus(firstUnsuccessful(
					replaceResult.map(grameFs.UpdateResult::outcome)
							.orElse(SUCCESS),
					changeResult.map(grameFs.UpdateResult::outcome)
							.orElse(SUCCESS)));
		} catch (IllegalArgumentException iae) {
			mapToStatus(iae, txnCtx);
		} catch (Exception unknown) {
			log.warn("Unrecognized failure handling {}!", txnCtx.accessor().getSignedTxn4Log(), unknown);
			txnCtx.setStatus(FAIL_INVALID);
		}
	}

	static void mapToStatus(IllegalArgumentException iae, TransactionContext txnCtx) {
		if (iae.getCause() instanceof DecoderException) {
			txnCtx.setStatus(BAD_ENCODING);
			return;
		}
		try {
			var type = TieredgrameFs.IllegalArgumentType.valueOf(iae.getMessage());
			txnCtx.setStatus(type.suggestedStatus());
		} catch (IllegalArgumentException untyped) {
			log.warn(
					"Unrecognized detail message '{}' handling {}!",
					iae.getMessage(),
					txnCtx.accessor().getSignedTxn4Log(),
					untyped);
			txnCtx.setStatus(FAIL_INVALID);
		}
	}

	private ResponseCodeEnum assessedValidity(FileUpdateTransactionBody op) {
		if (!op.hasFileID() || !hfs.exists(op.getFileID())) {
			return INVALID_FILE_ID;
		}

		if (op.hasKeys() && !validator.hasGoodEncoding(wrapped(op.getKeys()))) {
			return BAD_ENCODING;
		}

		return OK;
	}

	@Override
	public Predicate<TransactionBody> applicability() {
		return TransactionBody::hasFileUpdate;
	}

	@Override
	public Function<TransactionBody, ResponseCodeEnum> syntaxCheck() {
		return SYNTAX_CHECK;
	}

	private ResponseCodeEnum validate(TransactionBody fileUpdateTxn) {
		var op = fileUpdateTxn.getFileUpdate();

		var memoValidity = !op.hasMemo() ? OK : validator.memoCheck(op.getMemo().getValue());
		if (memoValidity != OK) {
			return memoValidity;
		}

		if (op.hasExpirationTime()) {
			var effectiveDuration = Duration.newBuilder()
					.setSeconds(
							op.getExpirationTime().getSeconds() -
									fileUpdateTxn.getTransactionID().getTransactionValidStart().getSeconds())
					.build();
			if (!validator.isValidAutoRenewPeriod(effectiveDuration)) {
				return AUTORENEW_DURATION_NOT_IN_RANGE;
			}
		}

		return OK;
	}

	static Key wrapped(KeyList wacl) {
		return Key.newBuilder().setKeyList(wacl).build();
	}
}
