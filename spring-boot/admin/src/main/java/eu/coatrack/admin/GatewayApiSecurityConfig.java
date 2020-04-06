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

import eu.coatrack.admin.security.GatewayAuthFilter;
import eu.coatrack.admin.security.GatewayAuthProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * WebSecurityConfigurerAdapter for the API that is accessed by the gateways.
 * <p>
 * This needs to be separated from the WebSecurityConfig because the authentication
 * is not done via form login but via an API key.
 * <p>
 * The @Order annotation is used to assure that this config is considered first.
 *
 * @author gr-hovest(at)atb-bremen.de
 */
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(1)
public class GatewayApiSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final String GATEWAY_API_RESOURCE_MATCHER = "/api/**";
    private static final String GATEWAY_ROLE_NAME = "YGG_GATEWAY";

    @Bean
    GatewayAuthProvider gatewayAuthProvider() {
        return new GatewayAuthProvider();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(
                gatewayAuthProvider());
    }

    protected void configure(HttpSecurity http) throws Exception {

        http.antMatcher(GATEWAY_API_RESOURCE_MATCHER)
                .csrf().disable()
                .addFilterAfter(new GatewayAuthFilter(), BasicAuthenticationFilter.class)
                .authorizeRequests()
                .anyRequest().hasRole(GATEWAY_ROLE_NAME);

        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }
}

