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

package org.springframework.cloud.sleuth.otel.instrument.web.client;

import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.TraceFlags;
import io.opentelemetry.trace.TraceId;
import io.opentelemetry.trace.TraceState;

import org.springframework.cloud.sleuth.api.TraceContext;
import org.springframework.cloud.sleuth.otel.bridge.OtelTraceContext;

public class HttpClientBeanPostProcessorTest
		extends org.springframework.cloud.sleuth.instrument.web.client.HttpClientBeanPostProcessorTest {

	@Override
	public TraceContext traceContext() {
		return OtelTraceContext.fromOtel(SpanContext.create(TraceId.fromLongs(1L, 0L), SpanId.fromLong(2L),
				TraceFlags.getSampled(), TraceState.builder().build()));
	}

}
