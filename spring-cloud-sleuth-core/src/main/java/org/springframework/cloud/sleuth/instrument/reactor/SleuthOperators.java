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

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import brave.propagation.CurrentTraceContext;
import brave.propagation.TraceContext;
import reactor.core.publisher.Signal;
import reactor.core.publisher.SignalType;
import reactor.util.context.Context;

// onLastOperator - benchmarking + ReactorLogging
// Sleuth's code
/*
if (runStyle == Scannable.Attr.RunStyle.SYNC) {
				return sub;
			}
 */
public final class SleuthOperators {

	private SleuthOperators() {
		throw new IllegalStateException("You can't instantiate a utility class");
	}

	public static Consumer<Signal> doWithThreadLocal(SignalType signalType,
			Runnable runnable) {
		return signal -> {
			if (signalType != signal.getType()) {
				return;
			}
			doWithThreadLocal(runnable).accept(signal);
		};
	}

	public static Consumer<Signal> doWithThreadLocal(Runnable runnable) {
		return signal -> {
			Context context = signal.getContext();
			doWithThreadLocal(context, runnable);
		};
	}

	public static void doWithThreadLocal(Context context, Runnable runnable) {
		CurrentTraceContext traceContext = context.get(CurrentTraceContext.class);
		try (CurrentTraceContext.Scope scope = traceContext
				.maybeScope(context.get(TraceContext.class))) {
			runnable.run();
		}
	}

	public static <T> T doWithThreadLocal(Context context, Callable<T> callable) {
		CurrentTraceContext traceContext = context.get(CurrentTraceContext.class);
		try (CurrentTraceContext.Scope scope = traceContext
				.maybeScope(context.get(TraceContext.class))) {
			try {
				return callable.call();
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}

	public static <T> Consumer<Signal<T>> doWithThreadLocalOnNext(Consumer<T> consumer) {
		return signal -> {
			if (!signal.isOnNext()) {
				return;
			}
			// Reactor gave us TraceContext - 1
			Context context = signal.getContext();
			// Assume that it's put there by SpanScopeSth
			CurrentTraceContext currentTraceContext = context
					.get(CurrentTraceContext.class);
			// Trace Context 1
			try (CurrentTraceContext.Scope scope = currentTraceContext
					.maybeScope(context.get(TraceContext.class))) {
				// If there's 1 in Thread Local - Trace Context 1 / If there's nothing in
				// ThreadLocal - Trace Context 2
				consumer.accept(signal.get());
			}
			// Trace Context 1
		};
	}

}

/*
 *
 *
 * @RestController class Foo {
 *
 * private static final Logger log = LoggerFactory.getLogger(Foo.class);
 *
 * @RequestMapping("/get") Flux<String> get(ServerWebExchange exchange) { String
 * methodValue = exchange.getRequest().getMethodValue(); return Flux.create(stringFluxSink
 * -> stringFluxSink.currentContext()) // Flux.deferWithContext doSth() // Access to the
 * context .doOnEach(SleuthOperators.doWithThreadLocal(() ->
 * log.info("alskdjsakdhsakjd"))) .flatMap(s -> { Mono<Context> mono =
 * Mono.subscriberContext(); return SleuthOperators.doWithThreadLocal(mono.map(context ->
 * context), () -> ...)); }) .subscriberContext(Context.of("foo", "bar"));
 *
 * // return Flux // .just("alsjddsa") // .map((c, s) -> { // }) // .flatMap(o -> { //
 * Mono<Context> context = Mono.subscriberContext(); // }) // .doOnEach(s ->
 * ReactorLogging.info(s.getContext(), "HELLO")); }
 *
 * Flux<String> doSth() { return null; }
 *
 * }
 *
 *
 *
 * // Whatever is Reactor native - will work out of the box // Whatever is non reactive
 * native - requires an explicit call by the user
 *
 *
 *
 *
 */
