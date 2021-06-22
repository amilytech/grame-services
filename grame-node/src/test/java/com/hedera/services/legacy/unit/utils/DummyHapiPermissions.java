package com.grame.services.legacy.unit.utils;

import com.grame.services.config.MockAccountNumbers;
import com.grame.services.context.domain.security.HapiOpPermissions;
import com.gramegrame.api.proto.java.AccountID;
import com.gramegrame.api.proto.java.grameFunctionality;
import com.gramegrame.api.proto.java.ResponseCodeEnum;

import static com.gramegrame.api.proto.java.ResponseCodeEnum.OK;

public class DummyHapiPermissions extends HapiOpPermissions {
	final ResponseCodeEnum always;

	public DummyHapiPermissions() {
		super(new MockAccountNumbers());
		always = OK;
	}

	public DummyHapiPermissions(ResponseCodeEnum always) {
		super(new MockAccountNumbers());
		this.always = always;
	}

	@Override
	public ResponseCodeEnum permissibilityOf(grameFunctionality function, AccountID givenPayer) {
		return always;
	}
}
