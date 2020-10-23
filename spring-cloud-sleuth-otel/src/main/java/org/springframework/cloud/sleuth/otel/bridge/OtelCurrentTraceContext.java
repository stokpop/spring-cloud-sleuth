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

package org.springframework.cloud.sleuth.otel.bridge;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.ContextStorageProvider;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.sleuth.api.CurrentTraceContext;
import org.springframework.cloud.sleuth.api.TraceContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;

/**
 * OpenTelemetry implementation of a {@link CurrentTraceContext}.
 *
 * @author Marcin Grzejszczak
 * @since 3.0.0
 */
public class OtelCurrentTraceContext implements CurrentTraceContext, ContextStorageProvider {

	private static final Log log = LogFactory.getLog(OtelCurrentTraceContext.class);

	private final ApplicationEventPublisher publisher;

	private final ContextStorageProvider delegate = ContextStorage::get;

	public OtelCurrentTraceContext(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public TraceContext context() {
		Span currentSpan = Span.current();
		if (Span.getInvalid().equals(currentSpan)) {
			return null;
		}
		return new OtelTraceContext(currentSpan);
	}

	@Override
	public Scope newScope(TraceContext context) {
		OtelTraceContext otelTraceContext = (OtelTraceContext) context;
		if (otelTraceContext == null) {
			return io.opentelemetry.context.Scope::noop;
		}
		SpanContext spanContext = otelTraceContext.delegate;
		Span fromContext = new SpanFromSpanContext(((OtelTraceContext) context).span, spanContext, otelTraceContext);
		io.opentelemetry.context.Scope attach = get().attach(Context.current().with(fromContext));
		return attach::close;
	}

	@Override
	public Scope maybeScope(TraceContext context) {
		return newScope(context);
	}

	@Override
	public <C> Callable<C> wrap(Callable<C> task) {
		return get().current().wrap(task);
	}

	@Override
	public Runnable wrap(Runnable task) {
		return get().current().wrap(task);
	}

	@Override
	public Executor wrap(Executor delegate) {
		return get().current().wrap(delegate);
	}

	@Override
	public ExecutorService wrap(ExecutorService delegate) {
		return get().current().wrap(delegate);
	}

	private boolean traceAndSpanIdsAreEqual(Span fromContext, Span currentSpan) {
		return fromContext.getSpanContext().getTraceIdAsHexString()
				.equals(currentSpan.getSpanContext().getTraceIdAsHexString())
				&& fromContext.getSpanContext().getSpanIdAsHexString()
						.equals(currentSpan.getSpanContext().getSpanIdAsHexString());
	}

	@Override
	public ContextStorage get() {
		ContextStorage threadLocalStorage = this.delegate.get();
		return new ContextStorage() {
			@Override
			public io.opentelemetry.context.Scope attach(Context toAttach) {
				Context current = current();
				if (log.isTraceEnabled()) {
					log.trace("Will check if new scope should be created for context [" + current + "]");
				}
				Span fromContext = toAttach != null ? Span.fromContext(toAttach) : Span.getInvalid();
				Span currentSpan = current != null ? Span.fromContext(current) : Span.getInvalid();
				// if the context is the same noop scope will be returned
				io.opentelemetry.context.Scope scope = threadLocalStorage.attach(toAttach);
				if (traceAndSpanIdsAreEqual(fromContext, currentSpan)) {
					if (log.isTraceEnabled()) {
						log.trace("Same context as the current one - will return noop");
					}
				}
				else {
					publisher.publishEvent(new ScopeChanged(this, fromContext));
				}
				return () -> {
					publisher.publishEvent(new OtelCurrentTraceContext.ScopeClosed(this));
					publisher.publishEvent(new ScopeChanged(this, currentSpan));
					scope.close();
				};
			}

			@Override
			public Context current() {
				return threadLocalStorage.current();
			}
		};
	}

	public static class ScopeChanged extends ApplicationEvent {

		/**
		 * Span corresponding to the changed scope. Might be {@code null}.
		 */
		public final Span span;

		/**
		 * Create a new {@code ApplicationEvent}.
		 * @param source the object on which the event initially occurred or with which
		 * the event is associated (never {@code null})
		 * @param span corresponding trace context
		 */
		public ScopeChanged(Object source, @Nullable Span span) {
			super(source);
			this.span = span;
		}

	}

	public static class ScopeClosed extends ApplicationEvent {

		/**
		 * Create a new {@code ApplicationEvent}.
		 * @param source the object on which the event initially occurred or with which
		 * the event is associated (never {@code null})
		 */
		public ScopeClosed(Object source) {
			super(source);
		}

	}

}
