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

import io.opentelemetry.sdk.trace.export.SpanExporter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * {@link SpanExporter} that ignores spans via names.
 *
 * @author Marcin Grzejszczak
 * @since 3.0.0
 */
class SpanIgnoringSpanHandlerBeanPostProcessor implements BeanPostProcessor {

	private final BeanFactory beanFactory;

	SpanIgnoringSpanHandlerBeanPostProcessor(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof SpanExporter) {
			return new SpanIgnoringSpanExporter(this.beanFactory, (SpanExporter) bean);
		}
		return bean;
	}
}
