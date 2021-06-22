package com.grame.services.context.properties;

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

import com.gramegrame.api.proto.java.ServicesConfigurationList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static com.grame.services.context.properties.BootstrapProperties.BOOTSTRAP_PROP_NAMES;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;

public class StandardizedPropertySourcesTest {
	Predicate fileSourceExists;
	PropertySource bootstrapProps;

	StandardizedPropertySources subject;

	@BeforeEach
	private void setup() {
		bootstrapProps = mock(PropertySource.class);
		fileSourceExists = mock(Predicate.class);
	}

	@Test
	void usesDynamicGlobalAsPriority() {
		// setup:
		ScreenedSysFileProps source = mock(ScreenedSysFileProps.class);
		given(source.containsProperty("testProp")).willReturn(true);
		given(source.getProperty("testProp")).willReturn("perfectAnswer");
		StandardizedPropertySources.dynamicGlobalPropsSupplier = () -> source;

		givenImpliedSubject();

		// when:
		subject.reloadFrom(ServicesConfigurationList.getDefaultInstance());

		// expect:
		assertEquals("perfectAnswer", subject.asResolvingSource().getStringProperty("testProp"));

		// cleanup:
		StandardizedPropertySources.dynamicGlobalPropsSupplier = ScreenedSysFileProps::new;
	}

	@Test
	void usesNodeAsSecondPriority() {
		// setup:
		var localSource = mock(ScreenedNodeFileProps.class);
		given(localSource.containsProperty("testProp")).willReturn(true);
		given(localSource.getProperty("testProp")).willReturn("imperfectAnswer");
		given(localSource.containsProperty("testProp2")).willReturn(true);
		given(localSource.getProperty("testProp2")).willReturn("goodEnoughForMe");
		var source = mock(ScreenedSysFileProps.class);
		given(source.containsProperty("testProp")).willReturn(true);
		given(source.getProperty("testProp")).willReturn("perfectAnswer");
		StandardizedPropertySources.dynamicGlobalPropsSupplier = () -> source;
		StandardizedPropertySources.nodePropertiesSupplier = () -> localSource;

		givenImpliedSubject();

		// when:
		subject.reloadFrom(ServicesConfigurationList.getDefaultInstance());

		// expect:
		assertEquals("perfectAnswer", subject.asResolvingSource().getStringProperty("testProp"));
		assertEquals("goodEnoughForMe", subject.asResolvingSource().getStringProperty("testProp2"));

		// cleanup:
		StandardizedPropertySources.dynamicGlobalPropsSupplier = ScreenedSysFileProps::new;
		StandardizedPropertySources.nodePropertiesSupplier = ScreenedNodeFileProps::new;
	}

	@Test
	void propagatesReloadToDynamicGlobalProps() {
		// setup:
		ScreenedSysFileProps source = mock(ScreenedSysFileProps.class);
		StandardizedPropertySources.dynamicGlobalPropsSupplier = () -> source;

		givenImpliedSubject();

		// when:
		subject.reloadFrom(ServicesConfigurationList.getDefaultInstance());

		// expect:
		verify(source).screenNew(ServicesConfigurationList.getDefaultInstance());

		// cleanup:
		StandardizedPropertySources.dynamicGlobalPropsSupplier = ScreenedSysFileProps::new;
	}

	@Test
	public void usesBootstrapSourceAsApropos() {
		givenImpliedSubject();
		// and:
		subject.nodeProps.fromFile.clear();

		// when:
		PropertySource properties = subject.asResolvingSource();
		// and:
		BOOTSTRAP_PROP_NAMES.forEach(properties::getProperty);

		// then:
		for (String bootstrapProp : BOOTSTRAP_PROP_NAMES) {
			verify(bootstrapProps).getProperty(bootstrapProp);
		}
	}

	@Test
	public void failsOnMissingApiPermissionProps() {
		given(bootstrapProps.getStringProperty("bootstrap.networkProperties.path"))
				.willReturn("application.properties");
		given(bootstrapProps.getStringProperty("bootstrap.hapiPermissions.path"))
				.willReturn("api-permission.properties");
		// and:
		given(fileSourceExists.test("application.properties")).willReturn(true);
		given(fileSourceExists.test("api-permission.properties")).willReturn(false);
		givenImpliedSubject();

		// expect:
		assertThrows(IllegalStateException.class, () -> subject.assertSourcesArePresent());
		verify(fileSourceExists).test("application.properties");
		verify(fileSourceExists).test("api-permission.properties");
	}

	@Test
	public void failsOnMissingAppProps() {
		given(bootstrapProps.getStringProperty("bootstrap.networkProperties.path"))
				.willReturn("application.properties");
		given(bootstrapProps.getStringProperty("bootstrap.hapiPermissions.path"))
				.willReturn("api-permission.properties");
		// and:
		given(fileSourceExists.test("application.properties")).willReturn(false);
		givenImpliedSubject();

		// expect:
		assertThrows(IllegalStateException.class, () -> subject.assertSourcesArePresent());
		verify(fileSourceExists).test("application.properties");
		verify(fileSourceExists,never()).test("api-permission.properties");
	}

	@Test
	public void uneventfulInitIfSourcesAvailable() {
		given(fileSourceExists.test(any())).willReturn(true);
		givenImpliedSubject();

		// expect:
		assertDoesNotThrow(() -> subject.assertSourcesArePresent());
	}

	private void givenImpliedSubject() {
		subject = new StandardizedPropertySources(bootstrapProps, fileSourceExists);
	}
}
