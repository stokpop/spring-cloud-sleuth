/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.sleuth.instrument.reactor;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.CurrentTraceContext;
import brave.propagation.TraceContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.core.publisher.Signal;
import reactor.core.publisher.SignalType;
import reactor.util.context.Context;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.internal.LazyBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

/**
 * Reactive Span pointcuts factories.
 *
 * @author Stephane Maldini
 * @since 2.0.0
 */
// TODO: this is public as it is used out of package, but unlikely intended to be
// non-internal
public abstract class ReactorSleuth {

	private static final Log log = LogFactory.getLog(ReactorSleuth.class);

	private ReactorSleuth() {
	}

	/**
	 * Return a span operator pointcut given a {@link Tracing}. This can be used in
	 * reactor via {@link reactor.core.publisher.Flux#transform(Function)},
	 * {@link reactor.core.publisher.Mono#transform(Function)},
	 * {@link reactor.core.publisher.Hooks#onLastOperator(Function)} or
	 * {@link reactor.core.publisher.Hooks#onLastOperator(Function)}. The Span operator
	 * pointcut will pass the Scope of the Span without ever creating any new spans.
	 * @param springContext the Spring context.
	 * @param <T> an arbitrary type that is left unchanged by the span operator
	 * @return a new lazy span operator pointcut
	 */
	// Much of Boot assumes that the Spring context will be a
	// ConfigurableApplicationContext, rooted in SpringApplication's
	// requirement for it to be so. Previous versions of Reactor
	// instrumentation injected both BeanFactory and also
	// ConfigurableApplicationContext. This chooses the more narrow
	// signature as it is simpler than explaining instanceof checks.
	public static <T> Function<? super Publisher<T>, ? extends Publisher<T>> scopePassingSpanOperator(
			ConfigurableApplicationContext springContext) {
		if (log.isTraceEnabled()) {
			log.trace("Scope passing operator [" + springContext + "]");
		}

		// keep a reference outside the lambda so that any caching will be visible to
		// all publishers
		LazyBean<CurrentTraceContext> lazyCurrentTraceContext = LazyBean
				.create(springContext, CurrentTraceContext.class);

		return Operators.liftPublisher((p, sub) -> {
			// We don't scope scalar results as they happen in an instant. This prevents
			// excessive overhead when using Flux/Mono #just, #empty, #error, etc.
			if (p instanceof Fuseable.ScalarCallable) {
				return sub;
			}

			if (!springContext.isActive()) {
				boolean assertOn = false;
				assert assertOn = true; // gives a message in unit test failures
				if (log.isTraceEnabled() || assertOn) {
					String message = "Spring Context [" + springContext
							+ "] is not yet refreshed. This is unexpected. Reactor Context is ["
							+ sub.currentContext() + "] and name is [" + name(sub) + "]";
					log.trace(message);
					assert false : message; // should never happen, but don't break.
				}
				return sub;
			}

			Context context = sub.currentContext();

			if (log.isTraceEnabled()) {
				log.trace("Spring context [" + springContext + "], Reactor context ["
						+ context + "], name [" + name(sub) + "]");
			}

			// Try to get the current trace context bean, lenient when there are problems
			CurrentTraceContext currentTraceContext = lazyCurrentTraceContext.get();
			if (currentTraceContext == null) {
				boolean assertOn = false;
				assert assertOn = true; // gives a message in unit test failures
				if (log.isTraceEnabled() || assertOn) {
					String message = "Spring Context [" + springContext
							+ "] did not return a CurrentTraceContext. Reactor Context is ["
							+ sub.currentContext() + "] and name is [" + name(sub) + "]";
					log.trace(message);
					assert false : message; // should never happen, but don't break.
				}
				return sub;
			}

			TraceContext parent = traceContext(context, currentTraceContext);
			if (parent == null) {
				return sub; // no need to scope a null parent
			}

			if (log.isTraceEnabled()) {
				log.trace("Creating a scope passing span subscriber with Reactor Context "
						+ "[" + context + "] and name [" + name(sub) + "]");
			}
			// Scannable.from(this.subscriber).scan(Attr.RUN_STYLE)
			// if (SYNC) - do the instrumentation but I DON'T WANT TO CREATE A NEW OBJECT

			Scannable.Attr.RunStyle runStyle = Scannable.from(sub).scan(Scannable.Attr.RUN_STYLE);
			// 3 operators, A (sync), B, C

			// 1 -> ThreadLocal TraceId 1 (A)
			// 2 -> 1 (B)
			// 3 -> 1 (C)
			// Whenever the Thread is changed or flux finished
			// cleanup -> remove stuff from ThreadLocal after 3

			// 4 operators A, B, C, D
			// A Thread 1, B Thread 2 (Set UP) -> C (Clean UP Thread 2), D Thread 1 (Clean UP Thread 1)
			//

			Flux
			.from()
				.doOnDiscard()
				.doOnEach()
				.subscribe();
			@Autowired Tracer tracer;
			Span span = tracer.currentSpan();
			
			// no sleuth - 2000 req / s
			// app1 -contex-> app2 -contex-> app3
			// 	log.info(...)
			// sleuth - onLastOperator 900 req / s
			// sleuth - onEach - 300 req / s
			
			
			
			

			if (runStyle == Scannable.Attr.RunStyle.SYNC) {
				return sub;
			}
			return new ScopePassingSpanSubscriber<>(sub, context, currentTraceContext,
					parent);
		});
	}

	static String name(CoreSubscriber<?> sub) {
		return Scannable.from(sub).name();
	}

	/**
	 * Like {@link CurrentTraceContext#get()}, except it first checks the reactor context.
	 */
	static TraceContext traceContext(Context context, CurrentTraceContext fallback) {
		if (context.hasKey(TraceContext.class)) {
			return context.get(TraceContext.class);
		}
		return fallback.get();
	}

}



@RestController
class Foo {

	private static final Logger log = LoggerFactory.getLogger(Foo.class);

	@RequestMapping("/get")
	Flux<String> get(ServerWebExchange exchange) {
		String methodValue = exchange.getRequest().getMethodValue();
		return
				Flux.create(stringFluxSink -> stringFluxSink.currentContext())
		// Flux.deferWithContext
		doSth()
				// Access to the context
				.doOnEach(SleuthOperators.doWithThreadLocal(() -> log.info("alskdjsakdhsakjd")))
				.flatMap(s -> {
					Mono<Context> mono = Mono.subscriberContext();
					return SleuthOperators.doWithThreadLocal(mono.map(context -> context), () -> ...));
				})
				.subscriberContext(Context.of("foo", "bar"));

//		return Flux
//		.just("alsjddsa")
//		.map((c, s) -> {
//		})
//		.flatMap(o -> {
//			Mono<Context> context = Mono.subscriberContext();
//		})
//		.doOnEach(s -> ReactorLogging.info(s.getContext(), "HELLO"));
	}

	Flux<String> doSth() {
		return null;
	}

}



// Whatever is Reactor native - will work out of the box
// Whatever is non reactive native - requires an explicit call by the user








// onLastOperator - benchmarking + ReactorLogging
// Sleuth's code
/*
if (runStyle == Scannable.Attr.RunStyle.SYNC) {
				return sub;
			}
 */
class SleuthOperators {
	static Consumer<Signal> doWithThreadLocal(SignalType signalType, Runnable runnable) {
		return signal -> {
			if (signalType != signal.getType()) {
				return;
			}
			doWithThreadLocal(runnable).accept(signal);
		};
	}

	static Consumer<Signal> doWithThreadLocal(Runnable runnable) {
		return signal -> {
			Context context = signal.getContext();
			// Assume that it's put there by SpanScopeSth
			CurrentTraceContext traceContext = context.get(CurrentTraceContext.class);
			try (CurrentTraceContext.Scope scope = traceContext.maybeScope(context.get(TraceContext.class))) {
				MDC.put(context.get(""), context.get(""));
				runnable.run();
				MDC.clear();
			}
		};
	}

	static <T> Consumer<Signal<T>> doWithThreadLocalOnNext(Consumer<T> consumer) {
		return signal -> {
			if (!signal.isOnNext()) {
				return;
			}
			// Reactor gave us TraceContext - 1
			Context context = signal.getContext();
			// Assume that it's put there by SpanScopeSth
			CurrentTraceContext currentTraceContext = context.get(CurrentTraceContext.class);
			// Trace Context 1
			try (CurrentTraceContext.Scope scope = currentTraceContext.maybeScope(context.get(TraceContext.class))) {
				// If there's 1 in Thread Local - Trace Context 1 / If there's nothing in ThreadLocal - Trace Context 2
				MDC.put(context.get(""), context.get(""));
				consumer.accept(signal.get());
				MDC.clear();
			}
			// Trace Context 1
		};
	}
}