/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.instrument.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.sleuth.api.Tracer;
import org.springframework.cloud.sleuth.test.TestSpanHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.BDDAssertions.then;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = SkipEndPointsIntegrationTestsWithoutContextPathWithoutBasePath.TestConfig.class)
@TestPropertySource(
		properties = { "management.endpoints.web.exposure.include:*", "management.endpoints.web.base-path:/" })
public abstract class SkipEndPointsIntegrationTestsWithoutContextPathWithoutBasePath {

	@LocalServerPort
	int port;

	@Autowired
	private TestSpanHandler spans;

	@Autowired
	private Tracer tracer;

	@BeforeEach
	@AfterEach
	public void clearSpans() {
		this.spans.clear();
	}

	@Test
	public void should_sample_non_actuator_endpoint() {
		new RestTemplate().getForObject("http://localhost:" + this.port + "/something", String.class);

		then(this.tracer.currentSpan()).isNull();
		then(this.spans).hasSize(1);
	}

	@Test
	public void should_sample_non_actuator_endpoint_with_healthcare_in_path() {
		new RestTemplate().getForObject("http://localhost:" + this.port + "/healthcare", String.class);

		then(this.tracer.currentSpan()).isNull();
		then(this.spans).hasSize(1);
	}

	@Test
	public void should_not_sample_actuator_endpoint() {
		new RestTemplate().getForObject("http://localhost:" + this.port + "/health", String.class);

		then(this.tracer.currentSpan()).isNull();
		then(this.spans).hasSize(0);
	}

	@Test
	public void should_not_sample_actuator_endpoint_with_parameter() {
		new RestTemplate().getForObject("http://localhost:" + this.port + "/metrics?xyz", String.class);

		then(this.tracer.currentSpan()).isNull();
		then(this.spans).hasSize(0);
	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	@RestController
	public static class TestConfig {

		@GetMapping("something")
		void doNothing() {
		}

		@GetMapping("healthcare")
		void healthCare() {
		}

		@GetMapping("metrics")
		void metrics() {
		}

	}

}
