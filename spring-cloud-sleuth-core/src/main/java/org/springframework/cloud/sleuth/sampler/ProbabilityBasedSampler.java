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

package org.springframework.cloud.sleuth.sampler;

import java.util.List;

import io.opentelemetry.common.ReadableAttributes;
import io.opentelemetry.sdk.trace.Sampler;
import io.opentelemetry.sdk.trace.Samplers;
import io.opentelemetry.trace.Link;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.TraceId;

/**
 * Probability based sampler.
 *
 * @author Marcin Grzejszczak
 * @since 3.0.0
 */
class ProbabilityBasedSampler implements Sampler {

	private final SamplerProperties samplerProperties;

	public ProbabilityBasedSampler(SamplerProperties samplerProperties) {
		this.samplerProperties = samplerProperties;
	}

	@Override
	public SamplingResult shouldSample(SpanContext parentContext, TraceId traceId, String name, Span.Kind spanKind, ReadableAttributes attributes, List<Link> parentLinks) {
		return Samplers.probability(this.samplerProperties.getProbability()).shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
	}

	@Override
	public String getDescription() {
		return Samplers.probability(this.samplerProperties.getProbability()).getDescription();
	}
}
