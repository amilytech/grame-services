package com.grame.services.context.domain.security;

import com.grame.services.config.AccountNumbers;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.ResponseCodeEnum;
import com.gramegrame.api.proto.java.ServicesConfigurationList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;

import static com.grame.services.context.domain.security.PermissionFileUtils.legacyKeys;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.NOT_SUPPORTED;
import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;

public class HapiOpPermissions {
	static final String MISSING_OP_TPL = "Ignoring key '%s', which does not correspond to a known grame operation!";
	static final String UNPARSEABLE_RANGE_TPL = "Ignoring entry for supported op %s---cannot interpret range '%s'!";

	static Logger log = LogManager.getLogger(HapiOpPermissions.class);

	private final AccountNumbers accountNums;

	public HapiOpPermissions(AccountNumbers accountNums) {
		this.accountNums = accountNums;
	}

	private EnumMap<grameFunctionality, PermissionedAccountsRange> permissions = new EnumMap<>(grameFunctionality.class);

	public void reloadFrom(ServicesConfigurationList config) {
		EnumMap<grameFunctionality, PermissionedAccountsRange> newPerms = new EnumMap<>(grameFunctionality.class);
		for (var permission : config.getNameValueList()) {
			var opName = permission.getName();
			if (legacyKeys.containsKey(opName)) {
				var op = legacyKeys.get(opName);
				var range = PermissionedAccountsRange.from(permission.getValue());
				if (range == null) {
					log.warn(String.format(UNPARSEABLE_RANGE_TPL, op, permission.getValue()));
				} else {
					newPerms.put(op, range);
				}
			} else {
				log.warn(String.format(MISSING_OP_TPL, opName));
			}
		}
		permissions = newPerms;
	}

	public ResponseCodeEnum permissibilityOf(grameFunctionality function, AccountID givenPayer) {
		var num = givenPayer.getAccountNum();
		PermissionedAccountsRange range;
		return (range = permissions.get(function)) != null && (accountNums.isSuperuser(num) || range.contains(num))
				? OK : NOT_SUPPORTED;
	}

	EnumMap<grameFunctionality, PermissionedAccountsRange> getPermissions() {
		return permissions;
	}
}
