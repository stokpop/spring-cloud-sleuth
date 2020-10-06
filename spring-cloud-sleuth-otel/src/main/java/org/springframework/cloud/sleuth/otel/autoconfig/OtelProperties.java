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

package org.springframework.cloud.sleuth.otel.autoconfig;

import io.opentelemetry.sdk.trace.config.TraceConfig;
import io.opentelemetry.trace.Event;
import io.opentelemetry.trace.Link;
import io.opentelemetry.trace.Span;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sleuth settings for OpenTelemetry.
 *
 * @author Marcin Grzejszczak
 * @since 3.0.0
 */
@ConfigurationProperties("spring.sleuth.otel.config")
class OtelProperties {

	/**
	 * Instrumentation name to be used to find a Tracer.
	 */
	private String instrumentationName = "org.springframework.cloud.sleuth";

	/**
	 * Instrumentation version to be used to find a Tracer.
	 */
	private String instrumentationVersion;

	private int samplerProbability;

	/**
	 * Returns the global default max number of attributes per {@link Span}.
	 */
	private int maxAttrs = TraceConfig.getDefault().getMaxNumberOfAttributes();

	/**
	 * Returns the global default max number of {@link Event}s per {@link Span}.
	 */
	private int maxEvents = TraceConfig.getDefault().getMaxNumberOfEvents();

	/**
	 * Returns the global default max number of {@link Link} entries per {@link Span}.
	 */
	private int maxLinks = TraceConfig.getDefault().getMaxNumberOfLinks();

	/**
	 * Returns the global default max number of attributes per {@link Event}.
	 */
	private int maxEventAttrs = TraceConfig.getDefault().getMaxNumberOfAttributesPerEvent();

	/**
	 * Returns the global default max number of attributes per {@link Link}.
	 */
	private int maxLinkAttrs = TraceConfig.getDefault().getMaxNumberOfAttributesPerLink();

	/**
	 * Returns the global default max length of string attribute value in characters.
	 */
	private int maxAttrLength = TraceConfig.getDefault().getMaxLengthOfAttributeValues();

	public String getInstrumentationName() {
		return this.instrumentationName;
	}

	public void setInstrumentationName(String instrumentationName) {
		this.instrumentationName = instrumentationName;
	}

	String getInstrumentationVersion() {
		return instrumentationVersion;
	}

	void setInstrumentationVersion(String instrumentationVersion) {
		this.instrumentationVersion = instrumentationVersion;
	}

	public int getSamplerProbability() {
		return this.samplerProbability;
	}

	public void setSamplerProbability(int samplerProbability) {
		this.samplerProbability = samplerProbability;
	}

	public int getMaxAttrs() {
		return this.maxAttrs;
	}

	public void setMaxAttrs(int maxAttrs) {
		this.maxAttrs = maxAttrs;
	}

	public int getMaxEvents() {
		return this.maxEvents;
	}

	public void setMaxEvents(int maxEvents) {
		this.maxEvents = maxEvents;
	}

	public int getMaxLinks() {
		return this.maxLinks;
	}

	public void setMaxLinks(int maxLinks) {
		this.maxLinks = maxLinks;
	}

	public int getMaxEventAttrs() {
		return this.maxEventAttrs;
	}

	public void setMaxEventAttrs(int maxEventAttrs) {
		this.maxEventAttrs = maxEventAttrs;
	}

	public int getMaxLinkAttrs() {
		return this.maxLinkAttrs;
	}

	public void setMaxLinkAttrs(int maxLinkAttrs) {
		this.maxLinkAttrs = maxLinkAttrs;
	}

	public int getMaxAttrLength() {
		return this.maxAttrLength;
	}

	public void setMaxAttrLength(int maxAttrLength) {
		this.maxAttrLength = maxAttrLength;
	}

}
