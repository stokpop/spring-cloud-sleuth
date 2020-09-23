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

package org.springframework.cloud.bravebridge;

import java.util.Collection;

import brave.Span;
import brave.Tracer;
import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class BraveSpanExporter implements SpanExporter, SpanProcessor {

	private final SpanHandler spanHandler;

	private final Tracer tracer;

	public BraveSpanExporter(SpanHandler spanHandler, Tracer tracer) {
		this.spanHandler = spanHandler;
		this.tracer = tracer;
	}

	@Override
	public CompletableResultCode export(Collection<SpanData> spans) {
		return null;
	}

	@Override
	public CompletableResultCode flush() {
		// TODO: Is this a reporter?
		return null;
	}

	@Override
	public void onStart(ReadWriteSpan span) {
		Span fromOTel = BraveSpan.fromOTel((ReadableSpan) span, this.tracer);
		this.spanHandler.begin(fromOTel.context(), new MutableSpan(fromOTel.context(), null), null);
	}

	@Override
	public boolean isStartRequired() {
		// TODO: ?
		return false;
	}

	@Override
	public void onEnd(ReadableSpan span) {
		Span fromOTel = BraveSpan.fromOTel(span, this.tracer);
		this.spanHandler.end(fromOTel.context(), new MutableSpan(fromOTel.context(), null), null);
	}

	@Override
	public boolean isEndRequired() {
		// TODO: ?
		return false;
	}

	@Override
	public CompletableResultCode shutdown() {
		// TODO: ?
		return CompletableResultCode.ofSuccess();
	}

	@Override
	public CompletableResultCode forceFlush() {
		// TODO: ?
		return CompletableResultCode.ofSuccess();
	}
}
