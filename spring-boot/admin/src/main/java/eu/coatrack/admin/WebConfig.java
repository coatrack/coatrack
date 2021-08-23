package eu.coatrack.admin;

/*-
 * #%L
 * coatrack-admin
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

import eu.coatrack.admin.interceptor.AdminInterceptor;
import eu.coatrack.admin.interceptor.GatewayRequestLoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@ComponentScan(basePackages = { "eu.coatrack.admin.controllers" })
public class WebConfig extends WebMvcConfigurerAdapter {

    @Autowired
    AdminInterceptor interceptor;

    @Autowired
    GatewayRequestLoggingInterceptor gatewayRequestLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // Register admin interceptor with multiple path patterns
        registry.addInterceptor(interceptor)
                .addPathPatterns(new String[] { "/admin", "/admin/*", "/admin/**/*", "/admin/**/**/*" });
        registry.addInterceptor(gatewayRequestLoggingInterceptor).addPathPatterns(new String[] { "/api/**/*" });
    }

    /**
     *
     *
     * Based on
     * solution:https://blog.georgovassilis.com/2015/10/29/spring-mvc-rest-controller-says-406-when-emails-are-part-url-path/
     *
     * @param configurer
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // Simple strategy: only path extension is taken into account
        configurer.favorPathExtension(true).favorParameter(false).parameterName("mediaType").ignoreAcceptHeader(false)
                .useJaf(false).defaultContentType(MediaType.APPLICATION_JSON)
                .mediaType("xml", MediaType.APPLICATION_XML).mediaType("json", MediaType.APPLICATION_JSON);
    }
}
