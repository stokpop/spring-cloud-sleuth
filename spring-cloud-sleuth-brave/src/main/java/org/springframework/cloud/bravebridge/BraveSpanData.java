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

import java.util.List;

import io.opentelemetry.common.ReadableAttributes;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.TraceFlags;
import io.opentelemetry.trace.TraceId;
import io.opentelemetry.trace.TraceState;

public class BraveSpanData implements SpanData {

	private final BraveSpan span;

	private final BraveSpanContext spanContext;

	public BraveSpanData(BraveSpan span) {
		this.span = span;
		this.spanContext = new BraveSpanContext(span.span.context());
	}

	@Override
	public TraceId getTraceId() {
		return this.spanContext.getTraceId();
	}

	@Override
	public SpanId getSpanId() {
		return this.spanContext.getSpanId();
	}

	@Override
	public TraceFlags getTraceFlags() {
		return this.spanContext.getTraceFlags();
	}

	@Override
	public TraceState getTraceState() {
		return this.spanContext.getTraceState();
	}

	@Override
	public SpanId getParentSpanId() {
		// TODO: ?
		return null;
	}

	@Override
	public Resource getResource() {
		// TODO: ?
		return null;
	}

	@Override
	public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
		return BraveInstrumentationLibraryInfo.INSTANCE;
	}

	@Override
	public String getName() {
		return span.name;
	}

	@Override
	public Span.Kind getKind() {
		// TODO: ?
		return null;
	}

	@Override
	public long getStartEpochNanos() {
		return 0;
	}

	@Override
	public ReadableAttributes getAttributes() {
		// TODO: ?
		return null;
	}

	@Override
	public List<Event> getEvents() {
		// TODO: ?
		return null;
	}

	@Override
	public List<Link> getLinks() {
		// TODO: ?
		return null;
	}

	@Override
	public Status getStatus() {
		// TODO: ?
		return null;
	}

	@Override
	public long getEndEpochNanos() {
		// TODO: ?
		return 0;
	}

	@Override
	public boolean getHasRemoteParent() {
		// TODO: ?
		return false;
	}

	@Override
	public boolean getHasEnded() {
		// TODO: ?
		return false;
	}

	@Override
	public int getTotalRecordedEvents() {
		// TODO: ?
		return 0;
	}

	@Override
	public int getTotalRecordedLinks() {
		// TODO: ?
		return 0;
	}

	@Override
	public int getTotalAttributeCount() {
		// TODO: ?
		return 0;
	}
}
