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

package org.springframework.cloud.sleuth.api.noop;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.springframework.cloud.sleuth.api.CurrentTraceContext;
import org.springframework.cloud.sleuth.api.TraceContext;

/**
 * A noop implementation. Does nothing.
 *
 * @author Marcin Grzejszczak
 * @since 3.0.0
 */
public class NoOpCurrentTraceContext implements CurrentTraceContext {

	@Override
	public TraceContext context() {
		return new NoOpTraceContext();
	}

	@Override
	public Scope newScope(TraceContext context) {
		return () -> {
		};
	}

	@Override
	public Scope maybeScope(TraceContext context) {
		return () -> {
		};
	}

	@Override
	public <C> Callable<C> wrap(Callable<C> task) {
		return task;
	}

	@Override
	public Runnable wrap(Runnable task) {
		return task;
	}

	@Override
	public Executor wrap(Executor delegate) {
		return delegate;
	}

	@Override
	public ExecutorService wrap(ExecutorService delegate) {
		return delegate;
	}

}
