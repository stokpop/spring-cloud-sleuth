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

package org.springframework.cloud.sleuth.instrument.web;

import brave.Tracing;
import brave.http.HttpTracing;
import brave.propagation.StrictCurrentTraceContext;
import brave.sampler.Sampler;
import brave.test.TestSpanHandler;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.HttpStatus;
import org.springframework.util.SocketUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.BDDAssertions.then;

class TraceNettyWebServerFactoryCustomizerTests {

	TestSpanHandler spans = new TestSpanHandler();

	Tracing tracing = Tracing.newBuilder()
			.currentTraceContext(StrictCurrentTraceContext.create())
			.addSpanHandler(this.spans).sampler(Sampler.ALWAYS_SAMPLE).build();

	HttpTracing httpTracing = HttpTracing.create(this.tracing);

	@Test
	void should_add_tracing_via_the_netty_codec() {
		WebServer webServer = webServer(factory());

		try {
			webServer.start();
			ClientResponse response = WebClient.builder().build().post()
					.uri("http://localhost:" + webServer.getPort() + "/")
					.header("X-B3-TraceId", "4883117762eb9420")
					.header("X-B3-SpanId", "4883117762eb9420").exchange().block();
			then(response.statusCode()).isEqualTo(HttpStatus.OK);
			then(this.spans).hasSize(1);
			then(this.spans.get(0).traceId()).isEqualTo("4883117762eb9420");
		}
		finally {
			webServer.stop();
		}
	}

	private NettyReactiveWebServerFactory factory() {
		int port = SocketUtils.findAvailableTcpPort();
		NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory(port);
		TraceNettyWebServerFactoryCustomizer customizer = new TraceNettyWebServerFactoryCustomizer(
				this.httpTracing);
		customizer.customize(factory);
		return factory;
	}

	private WebServer webServer(NettyReactiveWebServerFactory factory) {
		return factory.getWebServer((request, response) -> {
			response.setStatusCode(HttpStatus.OK);
			return Mono.empty();
		});
	}

}
