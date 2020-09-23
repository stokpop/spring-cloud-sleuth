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

package org.springframework.cloud.sleuth.instrument.messaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import io.grpc.Context;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.ContextUtils;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.Tracer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.sleuth.internal.SpanNameUtil;
import org.springframework.cloud.sleuth.internal.TraceContextUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.StringUtils;

// TODO: Duplicates a lot from TraceChannelInterceptor, need to figure out how to merge the two
class TraceMessageHandler {

	private static final Log log = LogFactory.getLog(TraceMessageHandler.class);

	/**
	 * Using the literal "broker" until we come up with a better solution.
	 *
	 * <p>
	 * If the message originated from a binder (consumer binding), there will be different
	 * headers present (e.g. "KafkaHeaders.RECEIVED_TOPIC" Vs.
	 * "AmqpHeaders.CONSUMER_QUEUE" (unless the application removes them before sending).
	 * These don't represent the broker, rather a queue, and in any case the heuristics
	 * are not great. At least we might be able to tell if this is rabbit or not (ex how
	 * spring-rabbit works). We need to think this through before making an api, possibly
	 * experimenting.
	 *
	 * <p>
	 * If the app is outbound only (producer), there's no indication of what type the
	 * destination broker is. This may hint at a non-manual solution being overwriting the
	 * remoteServiceName later, similar to how servlet instrumentation lazy set
	 * "http.route".
	 */
	private static final String REMOTE_SERVICE_NAME = "broker";

	private static final String TRACE_HANDLER_PARENT_SPAN = "traceHandlerParentSpan";

	private final Tracer tracer;

	private final TextMapPropagator.Setter<MessageHeaderAccessor> injector;

	private final TextMapPropagator.Getter<MessageHeaderAccessor> extractor;

	private final Function<SpanContext, Span> preSendFunction;

	private final TriConsumer<MessageHeaderAccessor, Span, Span> preSendMessageManipulator;

	private final Function<SpanContext, Span> outputMessageSpanFunction;

	TraceMessageHandler(Tracer tracer, Function<SpanContext, Span> preSendFunction,
			TriConsumer<MessageHeaderAccessor, Span, Span> preSendMessageManipulator,
			Function<SpanContext, Span> outputMessageSpanFunction) {
		this.tracer = tracer;
		this.injector = MessageHeaderPropagation.INSTANCE;
		this.extractor = MessageHeaderPropagation.INSTANCE;
		// TODO: Abstractions to reuse in TraceChannelInterceptors?
		this.preSendFunction = preSendFunction;
		this.preSendMessageManipulator = preSendMessageManipulator;
		this.outputMessageSpanFunction = outputMessageSpanFunction;
	}

	static TraceMessageHandler forNonSpringIntegration(Tracer tracer) {
		// TODO: How to continue a span?
		Function<SpanContext, Span> preSendFunction = ctx -> tracer.spanBuilder("handle").setParent(ctx).startSpan();
		TriConsumer<MessageHeaderAccessor, Span, Span> preSendMessageManipulator = (headers, parentSpan, childSpan) -> {
			headers.setHeader("traceHandlerParentSpan", parentSpan);
			headers.setHeader(Span.class.getName(), childSpan);
		};
		// TODO: How to continue a span?
		Function<SpanContext, Span> postReceiveFunction = ctx -> tracer.spanBuilder("").setParent(ctx).startSpan();
		return new TraceMessageHandler(tracer, preSendFunction, preSendMessageManipulator, postReceiveFunction);
	}

	/**
	 * Wraps the given input message with tracing headers and returns a corresponding
	 * span.
	 * @param message - message to wrap
	 * @param destinationName - destination from which the message was received
	 * @return a tuple with the wrapped message and a corresponding span
	 */
	MessageAndSpans wrapInputMessage(Message<?> message, String destinationName) {
		MessageHeaderAccessor headers = mutableHeaderAccessor(message);
		Context extractedContext = OpenTelemetry.getPropagators().getTextMapPropagator()
				.extract(Context.current(), headers, extractor);
		// Start and finish a consumer span as we will immediately process it.
		Span consumerSpan = TraceContextUtils.fromContext(this.tracer, extractedContext);
		if (!consumerSpan.isRecording()) {
			// TODO: How to do this on a continued span?
			//			consumerSpan.kind(Span.Kind.CONSUMER).start();
			//			consumerSpan.remoteServiceName(REMOTE_SERVICE_NAME);
			addTags(consumerSpan, destinationName);
			consumerSpan.end();
		}
		// create and scope a span for the message processor
		Span span = this.preSendFunction.apply(consumerSpan.getContext());
		// remove any trace headers, but don't re-inject as we are synchronously
		// processing the
		// message and can rely on scoping to access this span later.
		clearTracingHeaders(headers);
		this.preSendMessageManipulator.accept(headers, consumerSpan, span);
		if (log.isDebugEnabled()) {
			log.debug("Created a handle span after retrieving the message " + consumerSpan);
		}
		if (message instanceof ErrorMessage) {
			return new MessageAndSpans(new ErrorMessage((Throwable) message.getPayload(), headers.getMessageHeaders()),
					consumerSpan, span);
		}
		headers.setImmutable();
		return new MessageAndSpans(new GenericMessage<>(message.getPayload(), headers.getMessageHeaders()),
				consumerSpan, span);
	}

	Span spanFromMessage(Message<?> message) {
		MessageHeaderAccessor headers = mutableHeaderAccessor(message);
		Span span = span(headers, Span.class.getName());
		if (span != null) {
			return span;
		}
		span = span(headers, TRACE_HANDLER_PARENT_SPAN);
		if (span != null) {
			return span;
		}
		Context current = Context.current();
		Context extract = OpenTelemetry.getPropagators().getTextMapPropagator()
				.extract(current, headers, extractor);
		if (extract == current) {
			return null;
		}
		try (Scope scope = ContextUtils.withScopedContext(extract)) {
			return this.tracer.getCurrentSpan();
		}
	}

	private void addTags(Span result, String destinationName) {
		if (StringUtils.hasText(destinationName)) {
			result.setAttribute("channel", SpanNameUtil.shorten(destinationName));
		}
	}

	/**
	 * Called either when message got received and processed or message got sent.
	 * @param span - span that corresponds to the given operation
	 * @param ex - an optional exception that occurred while processing / sending.
	 */
	void afterMessageHandled(Span span, Throwable ex) {
		if (log.isDebugEnabled()) {
			log.debug("Will finish the current span after message handled " + span);
		}
		finishSpan(span, ex);
	}

	Span parentSpan(Message message) {
		return span(mutableHeaderAccessor(message), "traceHandlerParentSpan");
	}

	Span consumerSpan(Message message) {
		return span(mutableHeaderAccessor(message), Span.class.getName());
	}

	private Span span(MessageHeaderAccessor headerAccessor, String key) {
		return headerAccessor.getMessageHeaders().get(key, Span.class);
	}

	/**
	 * Wraps the given output message with tracing headers and returns a corresponding
	 * span.
	 * @param message - message to wrap
	 * @param destinationName - destination to which the message should be sent
	 * @return a tuple with the wrapped message and a corresponding span
	 */
	MessageAndSpan wrapOutputMessage(Message<?> message, SpanContext parentSpan,
			String destinationName) {
		Message<?> retrievedMessage = getMessage(message);
		MessageHeaderAccessor headers = mutableHeaderAccessor(retrievedMessage);
		Span span = this.outputMessageSpanFunction.apply(parentSpan);
		clearTracingHeaders(headers);
		OpenTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), headers, this.injector);
		markProducerSpan(headers, span, destinationName);
		if (log.isDebugEnabled()) {
			log.debug("Created a new span output message " + span);
		}
		return new MessageAndSpan(outputMessage(message, retrievedMessage, headers), span);
	}

	private void markProducerSpan(MessageHeaderAccessor headers, Span span, String destinationName) {
		if (!span.isRecording()) {
			// TODO: How to do this on a continued span?
//			span.kind(Span.Kind.PRODUCER).name("send").start();
//			span.remoteServiceName(toRemoteServiceName(headers));
			addTags(span, destinationName);
		}
	}

	private String toRemoteServiceName(MessageHeaderAccessor headers) {
		for (String key : headers.getMessageHeaders().keySet()) {
			if (key.startsWith("kafka_")) {
				return "kafka";
			}
			else if (key.startsWith("amqp_")) {
				return "rabbitmq";
			}
		}
		return REMOTE_SERVICE_NAME;
	}

	private Message<?> outputMessage(Message<?> originalMessage, Message<?> retrievedMessage,
			MessageHeaderAccessor additionalHeaders) {
		MessageHeaderAccessor headers = MessageHeaderAccessor.getMutableAccessor(originalMessage);
		clearTechnicalTracingHeaders(headers);
		if (originalMessage instanceof ErrorMessage) {
			ErrorMessage errorMessage = (ErrorMessage) originalMessage;
			// TODO: How to get keys?
//			headers.copyHeaders(MessageHeaderPropagation.propagationHeaders(additionalHeaders.getMessageHeaders(),
//					this.tracing.propagation().keys()));
			return new ErrorMessage(errorMessage.getPayload(), isWebSockets(headers) ? headers.getMessageHeaders()
					: new MessageHeaders(headers.getMessageHeaders()), errorMessage.getOriginalMessage());
		}
		headers.copyHeaders(additionalHeaders.getMessageHeaders());
		return new GenericMessage<>(retrievedMessage.getPayload(),
				isWebSockets(headers) ? headers.getMessageHeaders() : new MessageHeaders(headers.getMessageHeaders()));
	}

	private boolean isWebSockets(MessageHeaderAccessor headerAccessor) {
		return headerAccessor.getMessageHeaders().containsKey("stompCommand")
				|| headerAccessor.getMessageHeaders().containsKey("simpMessageType");
	}

	private Message<?> getMessage(Message<?> message) {
		Object payload = message.getPayload();
		if (payload instanceof MessagingException) {
			MessagingException e = (MessagingException) payload;
			Message<?> failedMessage = e.getFailedMessage();
			return failedMessage != null ? failedMessage : message;
		}
		return message;
	}

	private MessageHeaderAccessor mutableHeaderAccessor(Message<?> message) {
		MessageHeaderAccessor headers = MessageHeaderAccessor.getMutableAccessor(message);
		headers.setLeaveMutable(true);
		return headers;
	}

	private void clearTracingHeaders(MessageHeaderAccessor headers) {
		// TODO: How to get keys?
		// List<String> keysToRemove = new ArrayList<>(this.tracing.propagation().keys());
		List<String> keysToRemove = new ArrayList<>();
		keysToRemove.add(Span.class.getName());
		keysToRemove.add("traceHandlerParentSpan");
		MessageHeaderPropagation.removeAnyTraceHeaders(headers, keysToRemove);
	}

	private void clearTechnicalTracingHeaders(MessageHeaderAccessor headers) {
		MessageHeaderPropagation.removeAnyTraceHeaders(headers,
				Arrays.asList(Span.class.getName(), "traceHandlerParentSpan"));
	}

	private void finishSpan(Span span, Throwable error) {
		if (span == null || span.isRecording()) {
			return;
		}
		if (error != null) { // an error occurred, adding error to span
			String message = error.getMessage();
			if (message == null) {
				message = error.getClass().getSimpleName();
			}
			span.setAttribute("error", message);
		}
		span.end();
	}

}

class MessageAndSpan {

	final Message msg;

	final Span span;

	MessageAndSpan(Message msg, Span span) {
		this.msg = msg;
		this.span = span;
	}

	@Override
	public String toString() {
		return "MessageAndSpan{" + "msg=" + this.msg + ", span=" + this.span + '}';
	}

}

class MessageAndSpans {

	final Message msg;

	final Span parentSpan;

	final Span childSpan;

	MessageAndSpans(Message msg, Span parentSpan, Span childSpan) {
		this.msg = msg;
		this.parentSpan = parentSpan;
		this.childSpan = childSpan;
	}

	@Override
	public String toString() {
		return "MessageAndSpans{" + "msg=" + msg + ", parentSpan=" + parentSpan + ", childSpan=" + childSpan + '}';
	}

}

interface TriConsumer<K, V, S> {

	/**
	 * Performs the operation given the specified arguments.
	 * @param k the first input argument
	 * @param v the second input argument
	 * @param s the third input argument
	 */
	void accept(K k, V v, S s);

}
