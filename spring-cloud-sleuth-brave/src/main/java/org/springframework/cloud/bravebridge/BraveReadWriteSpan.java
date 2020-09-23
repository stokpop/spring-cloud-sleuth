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

import brave.Span;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.trace.SpanContext;

public class BraveReadWriteSpan extends BraveSpan implements ReadWriteSpan {

	public BraveReadWriteSpan(Span span) {
		super(span);
	}

	public BraveReadWriteSpan(Span span, String name) {
		super(span, name);
	}

	@Override
	public SpanContext getSpanContext() {
		return new BraveSpanContext(span.context());
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public SpanData toSpanData() {
		return new BraveSpanData(this);
	}

	@Override
	public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
		return BraveInstrumentationLibraryInfo.INSTANCE;
	}

	@Override
	public boolean hasEnded() {
		// TODO: ?
		return false;
	}

	@Override
	public long getLatencyNanos() {
		// TODO: ?
		return 0;
	}
}
