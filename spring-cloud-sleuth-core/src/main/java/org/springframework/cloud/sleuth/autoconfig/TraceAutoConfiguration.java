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

package org.springframework.cloud.sleuth.autoconfig;

import io.opentelemetry.trace.DefaultTracer;
import io.opentelemetry.trace.Tracer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.sleuth.SpanNamer;
import org.springframework.cloud.sleuth.internal.DefaultSpanNamer;
import org.springframework.cloud.sleuth.sampler.SamplerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} to enable tracing via Spring Cloud Sleuth.
 *
 * @author Spencer Gibb
 * @author Marcin Grzejszczak
 * @author Tim Ysewyn
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "spring.sleuth.enabled", matchIfMissing = true)
@EnableConfigurationProperties(SleuthProperties.class)
@Import(SamplerAutoConfiguration.class)
// public allows @AutoConfigureAfter(TraceAutoConfiguration)
// for components needing Tracing
public class TraceAutoConfiguration {

	/**
	 * Tracer bean name. Name of the bean matters for some instrumentations.
	 */
	public static final String TRACER_BEAN_NAME = "tracer";

	/**
	 * Default value used for service name if none provided.
	 */
	public static final String DEFAULT_SERVICE_NAME = "default";

	@Bean(name = TRACER_BEAN_NAME)
	@ConditionalOnMissingBean
	Tracer tracer() {
		return DefaultTracer.getInstance();
	}

	@Bean
	@ConditionalOnMissingBean
	SpanNamer sleuthSpanNamer() {
		return new DefaultSpanNamer();
	}
	@Bean
	@ConditionalOnProperty(value = "spring.sleuth.span-handler.enabled", matchIfMissing = true)
	SpanIgnoringSpanHandlerBeanPostProcessor spanIgnoringSpanHandlerBeanPostProcessor(BeanFactory beanFactory) {
		return new SpanIgnoringSpanHandlerBeanPostProcessor(beanFactory);
	}

}
