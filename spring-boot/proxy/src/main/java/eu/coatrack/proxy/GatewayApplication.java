package eu.coatrack.proxy;

/*-
 * #%L
 * coatrack-proxy
 * %%
 * Copyright (C) 2013 - 2020 Corizon | Institut für angewandte Systemtechnik Bremen GmbH (ATB)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import eu.coatrack.proxy.filters.post.ErrorLoggingFilter;
import eu.coatrack.proxy.filters.post.ResponseLoggingFilter;
import eu.coatrack.proxy.filters.pre.ApiKeyAuthFilter;
import eu.coatrack.proxy.filters.pre.RequestLoggingFilter;
import eu.coatrack.proxy.metrics.MetricsCounterService;
import eu.coatrack.proxy.metrics.MetricsTransmitter;
import eu.coatrack.proxy.security.SecurityConfigurer;
import eu.coatrack.proxy.security.ServiceApiAccessRightsVoter;
import org.apache.catalina.authenticator.jaspic.AuthConfigFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.metrics.buffer.BufferMetricReader;
import org.springframework.boot.actuate.metrics.export.MetricCopyExporter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.access.vote.UnanimousBased;
import org.springframework.security.web.access.expression.WebExpressionVoter;
import org.springframework.web.client.RestTemplate;

import javax.security.auth.message.config.AuthConfigFactory;
import java.util.Arrays;
import java.util.List;

/**
 * https://spring.io/guides/gs/routing-and-filtering/
 *
 * @author Timon Veenstra <timon@corizon.nl>
 */
@EnableZuulProxy
@SpringBootApplication
public class GatewayApplication {

    @Value("${spring.cloud.config.username}")
    String config_server_username;

    @Value("${spring.cloud.config.password}")
    String config_server_password;

    public static void main(String[] args) {
        if (AuthConfigFactory.getFactory() == null) {
            AuthConfigFactory.setFactory(new AuthConfigFactoryImpl());
        }
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public RequestLoggingFilter requestLoggingFilter() {
        return new RequestLoggingFilter();
    }

    @Bean
    public ResponseLoggingFilter responseLoggingFilter() {
        return new ResponseLoggingFilter();
    }

    @Bean
    public ErrorLoggingFilter errorLoggingFilter() {
        return new ErrorLoggingFilter();
    }

    @Bean
    public MetricsCounterService metricsCounterService() {
        return new MetricsCounterService();
    }

    @Bean
    public MetricsTransmitter metricsTransmitter() {
        return new MetricsTransmitter();
    }

    /**
     * Basic Spring copy exporter that regularly exports metrics, but just in case
     * they have changed
     */
    @Bean
    @Autowired
    public MetricCopyExporter metricCopyExporter(BufferMetricReader metricReader,
            MetricsTransmitter metricsTransmitter) {
        return new MetricCopyExporter(metricReader, metricsTransmitter);
    }

    @Bean
    public ApiKeyAuthFilter apiKeyAuthFilter() {
        return new ApiKeyAuthFilter();
    }

    @Bean
    public SecurityConfigurer securityConfigurer() {
        return new SecurityConfigurer();
    }

    /**
     * Rest template to be used for communication with CoatRack admin, configured
     * with auth credentials.
     */
    @Bean("restTemplate")
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(
                new BasicAuthorizationInterceptor(config_server_username, config_server_password));
        return restTemplate;
    }

    @Bean(name = "zuul-org.springframework.cloud.netflix.zuul.filters.ZuulProperties")
    @RefreshScope
    @ConfigurationProperties("zuul")
    public ZuulProperties zuulProperties() {
        return new ZuulProperties();
    }

    @Bean
    public AccessDecisionManager accessDecisionManager() {
        List<AccessDecisionVoter<? extends Object>> accessDecisionVoters = Arrays.asList(
                new AuthenticatedVoter(),
                new WebExpressionVoter(),
                new ServiceApiAccessRightsVoter());
        return new UnanimousBased(accessDecisionVoters);
    }
}
