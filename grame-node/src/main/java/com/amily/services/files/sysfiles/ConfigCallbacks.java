package com.grame.services.files.sysfiles;

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

import com.grame.services.context.domain.security.HapiOpPermissions;
import com.grame.services.context.properties.GlobalDynamicProperties;
import com.grame.services.context.properties.StandardizedPropertySources;
import com.gramegrame.api.proto.java.ServicesConfigurationList;

import java.util.function.Consumer;

public class ConfigCallbacks {
	private final HapiOpPermissions hapiOpPermissions;
	private final GlobalDynamicProperties dynamicProps;
	private final StandardizedPropertySources propertySources;

	public ConfigCallbacks(
			HapiOpPermissions hapiOpPermissions,
			GlobalDynamicProperties dynamicProps,
			StandardizedPropertySources propertySources
	) {
		this.dynamicProps = dynamicProps;
		this.propertySources = propertySources;
		this.hapiOpPermissions = hapiOpPermissions;
	}

	public Consumer<ServicesConfigurationList> propertiesCb() {
		return config -> {
			propertySources.reloadFrom(config);
			dynamicProps.reload();
		};
	}

	public Consumer<ServicesConfigurationList> permissionsCb() {
		return hapiOpPermissions::reloadFrom;
	}
}
