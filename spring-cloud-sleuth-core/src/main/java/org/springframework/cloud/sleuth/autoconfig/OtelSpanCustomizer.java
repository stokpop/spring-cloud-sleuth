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

import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.common.Attributes;
import io.opentelemetry.trace.EndSpanOptions;
import io.opentelemetry.trace.Event;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.Tracer;

import org.springframework.lang.Nullable;

public class OtelSpanCustomizer implements Span {

	private final Tracer tracer;

	public OtelSpanCustomizer(Tracer tracer) {
		this.tracer = tracer;
	}

	@Override
	public void setAttribute(String key, @Nullable String value) {
		this.tracer.getCurrentSpan().setAttribute(key, value);
	}

	@Override
	public void setAttribute(String key, long value) {
		this.tracer.getCurrentSpan().setAttribute(key, value);
	}

	@Override
	public void setAttribute(String key, double value) {
		this.tracer.getCurrentSpan().setAttribute(key, value);
	}

	@Override
	public void setAttribute(String key, boolean value) {
		this.tracer.getCurrentSpan().setAttribute(key, value);
	}

	@Override
	public void setAttribute(String key, AttributeValue value) {
		this.tracer.getCurrentSpan().setAttribute(key, value);
	}

	@Override
	public void addEvent(String name) {
		this.tracer.getCurrentSpan().addEvent(name);
	}

	@Override
	public void addEvent(String name, long timestamp) {
		this.tracer.getCurrentSpan().addEvent(name, timestamp);
	}

	@Override
	public void addEvent(String name, Attributes attributes) {
		this.tracer.getCurrentSpan().addEvent(name, attributes);
	}

	@Override
	public void addEvent(String name, Attributes attributes, long timestamp) {
		this.tracer.getCurrentSpan().addEvent(name, attributes, timestamp);
	}

	@Override
	public void addEvent(Event event) {
		this.tracer.getCurrentSpan().addEvent(event);
	}

	@Override
	public void addEvent(Event event, long timestamp) {
		this.tracer.getCurrentSpan().addEvent(event, timestamp);
	}

	@Override
	public void setStatus(Status status) {
		this.tracer.getCurrentSpan().setStatus(status);
	}

	@Override
	public void recordException(Throwable exception) {
		this.tracer.getCurrentSpan().recordException(exception);
	}

	@Override
	public void recordException(Throwable exception, Attributes additionalAttributes) {
		this.tracer.getCurrentSpan().recordException(exception, additionalAttributes);
	}

	@Override
	public void updateName(String name) {
		this.tracer.getCurrentSpan().updateName(name);
	}

	@Override
	public void end() {
		this.tracer.getCurrentSpan().end();
	}

	@Override
	public void end(EndSpanOptions endOptions) {
		this.tracer.getCurrentSpan().end(endOptions);
	}

	@Override
	public SpanContext getContext() {
		return this.tracer.getCurrentSpan().getContext();
	}

	@Override
	public boolean isRecording() {
		return this.tracer.getCurrentSpan().isRecording();
	}

}
