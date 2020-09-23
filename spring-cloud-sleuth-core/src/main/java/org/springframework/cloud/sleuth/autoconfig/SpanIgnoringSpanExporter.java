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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.StringUtils;

/**
 * {@link SpanExporter} that ignores spans via names.
 *
 * @author Marcin Grzejszczak
 * @since 3.0.0
 */
class SpanIgnoringSpanExporter implements SpanExporter {

	private static final Log log = LogFactory.getLog(SpanIgnoringSpanExporter.class);

	private final BeanFactory beanFactory;

	private final SpanExporter delegate;

	private SleuthProperties sleuthProperties;

	static final Map<String, Pattern> cache = new ConcurrentHashMap<>();

	SpanIgnoringSpanExporter(BeanFactory beanFactory, SpanExporter delegate) {
		this.beanFactory = beanFactory;
		this.delegate = delegate;
	}

	private SleuthProperties sleuthProperties() {
		if (this.sleuthProperties == null) {
			this.sleuthProperties = this.beanFactory.getBean(SleuthProperties.class);
		}
		return this.sleuthProperties;
	}

	private List<Pattern> spanNamesToIgnore() {
		return spanNames().stream().map(regex -> cache.computeIfAbsent(regex, Pattern::compile))
				.collect(Collectors.toList());
	}

	private List<String> spanNames() {
		List<String> spanNamesToIgnore = new ArrayList<>(
				sleuthProperties().getSpanHandler().getSpanNamePatternsToSkip());
		spanNamesToIgnore.addAll(sleuthProperties().getSpanHandler().getAdditionalSpanNamePatternsToIgnore());
		return spanNamesToIgnore;
	}

	@Override
	public CompletableResultCode export(Collection<SpanData> spans) {
		List<Pattern> spanNamesToIgnore = spanNamesToIgnore();
		List<SpanData> filteredSpans = spans.stream().filter(s -> {
			String name = s.getName();
			if (StringUtils.hasText(name) && spanNamesToIgnore.stream().anyMatch(p -> p.matcher(name).matches())) {
				if (log.isDebugEnabled()) {
					log.debug("Will ignore a span with name [" + name + "]");
				}
				return false;
			}
			return true;
		}).collect(Collectors.toList());
		return this.delegate.export(filteredSpans);
	}

	@Override
	public CompletableResultCode flush() {
		return this.delegate.flush();
	}

	@Override
	public CompletableResultCode shutdown() {
		return this.delegate.flush();
	}

}
