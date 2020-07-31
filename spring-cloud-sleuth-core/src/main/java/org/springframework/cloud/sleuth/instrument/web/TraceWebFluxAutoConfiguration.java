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
import brave.netty.http.NettyHttpTracing;
import io.netty.channel.ChannelDuplexHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} enables tracing to HTTP requests with Spring WebFlux.
 *
 * @author Marcin Grzejszczak
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "spring.sleuth.web.enabled", matchIfMissing = true)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnBean(Tracing.class)
@AutoConfigureAfter(SkipPatternConfiguration.class)
class TraceWebFluxAutoConfiguration {

	@Bean
	public TraceWebFilter traceFilter(BeanFactory beanFactory) {
		return new TraceWebFilter(beanFactory);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(NettyReactiveWebServerFactory.class)
	@ConditionalOnProperty(value = "spring.sleuth.web.netty.enabled",
			matchIfMissing = true)
	static class NettyConfiguration {

		@Bean
		@ConditionalOnBean(HttpTracing.class)
		TraceNettyWebServerFactoryCustomizer traceNettyWebServerFactoryCustomizer(
				HttpTracing httpTracing) {
			return new TraceNettyWebServerFactoryCustomizer(httpTracing);
		}

	}

}

class TraceNettyWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<NettyReactiveWebServerFactory> {

	private static final Log log = LogFactory
			.getLog(TraceNettyWebServerFactoryCustomizer.class);

	private final ChannelDuplexHandler handler;

	TraceNettyWebServerFactoryCustomizer(HttpTracing httpTracing) {
		NettyHttpTracing nettyHttpTracing = NettyHttpTracing.create(httpTracing);
		this.handler = nettyHttpTracing.serverHandler();
	}

	@Override
	public void customize(NettyReactiveWebServerFactory factory) {
		factory.addServerCustomizers(
				httpServer -> httpServer.doOnConnection(connection -> {
					if (log.isDebugEnabled()) {
						log.debug("Added tracing handler");
					}
					if (connection.channel().pipeline().get("tracing") == null) {
						connection.addHandlerFirst("tracing", handler);
					}
				}));
	}

}
