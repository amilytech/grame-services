package com.grame.services.grpc;

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

import com.grame.services.context.properties.NodeLocalProperties;
import com.grame.services.context.properties.Profile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.SSLException;
import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@ExtendWith(MockitoExtension.class)
class ConfigDrivenNettyFactoryTest {

	int port = 50123;
	long keepAliveTime = 10;
	long keepAliveTimeout = 3;
	long maxConnectionAge = 15;
	long maxConnectionAgeGrace = 5;
	long maxConnectionIdle = 10;
	int maxConcurrentCalls = 10;
	int flowControlWindow = 10240;

	@Mock
	NodeLocalProperties nodeLocalProperties;

	ConfigDrivenNettyFactory subject;

	@BeforeEach
	void setup() {
		subject = new ConfigDrivenNettyFactory(nodeLocalProperties);
	}

	@Test
	void usesProdPropertiesWhenAppropros() {
		given(nodeLocalProperties.nettyMode()).willReturn(Profile.PROD);
		given(nodeLocalProperties.nettyProdKeepAliveTime()).willReturn(keepAliveTime);
		given(nodeLocalProperties.nettyProdKeepAliveTimeout()).willReturn(keepAliveTimeout);
		given(nodeLocalProperties.nettyMaxConnectionAge()).willReturn(maxConnectionAge);
		given(nodeLocalProperties.nettyMaxConnectionAgeGrace()).willReturn(maxConnectionAgeGrace);
		given(nodeLocalProperties.nettyMaxConnectionIdle()).willReturn(maxConnectionIdle);
		given(nodeLocalProperties.nettyMaxConcurrentCalls()).willReturn(maxConcurrentCalls);
		given(nodeLocalProperties.nettyFlowControlWindow()).willReturn(flowControlWindow);

		// when:
		try {
			subject.builderFor(port, false);
		} catch (Throwable ignore) {
			/* If run on OS X, will throw java.lang.UnsatisfiedLinkError from Epoll.ensureAvailability */
		}

		// then:
		verify(nodeLocalProperties, times(2)).nettyProdKeepAliveTime();
		verify(nodeLocalProperties).nettyProdKeepAliveTimeout();
		verify(nodeLocalProperties).nettyMaxConnectionAge();
		verify(nodeLocalProperties).nettyMaxConnectionAgeGrace();
		verify(nodeLocalProperties).nettyMaxConnectionIdle();
		verify(nodeLocalProperties).nettyMaxConcurrentCalls();
		verify(nodeLocalProperties).nettyFlowControlWindow();
	}

	@Test
	void failsFastWhenCrtIsMissing() {
		given(nodeLocalProperties.nettyMode()).willReturn(Profile.TEST);
		given(nodeLocalProperties.nettyTlsCrtPath()).willReturn("not-a-real-crt");

		// when:
		assertThrows(FileNotFoundException.class, () -> subject.builderFor(port, true));
	}

	@Test
	void failsFastWhenKeyIsMissing() {
		given(nodeLocalProperties.nettyMode()).willReturn(Profile.TEST);
		given(nodeLocalProperties.nettyTlsCrtPath()).willReturn("src/test/resources/test-grame.crt");
		given(nodeLocalProperties.nettyTlsKeyPath()).willReturn("not-a-real-key");

		// when:
		assertThrows(FileNotFoundException.class, () -> subject.builderFor(port, true));
	}

	@Test
	void usesSslPropertiesWhenAppropros() throws FileNotFoundException, SSLException {
		given(nodeLocalProperties.nettyMode()).willReturn(Profile.TEST);
		given(nodeLocalProperties.nettyTlsCrtPath()).willReturn("src/test/resources/test-grame.crt");
		given(nodeLocalProperties.nettyTlsKeyPath()).willReturn("src/test/resources/test-grame.key");

		// when:
		subject.builderFor(port, true).build();

		// then:
		verify(nodeLocalProperties).nettyTlsCrtPath();
		verify(nodeLocalProperties).nettyTlsKeyPath();
	}
}
