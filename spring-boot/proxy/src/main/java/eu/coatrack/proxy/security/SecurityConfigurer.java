package eu.iof2020.ygg.proxy.security;

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

import eu.iof2020.ygg.proxy.filters.pre.ApiKeyAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Basic web security config, adding custom auth filter.
 *
 * @author gr-hovest
 */
@Configuration
@EnableWebSecurity
public class SecurityConfigurer extends WebSecurityConfigurerAdapter {

    @Autowired
    private ApiKeyAuthFilter apiKeyAuthFilter;

    @Autowired
    private AccessDecisionManager accessDecisionManager;

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {

        httpSecurity.addFilterBefore(apiKeyAuthFilter, BasicAuthenticationFilter.class);

        httpSecurity.csrf().disable()
                .authorizeRequests()
                .anyRequest().authenticated()
                .accessDecisionManager(accessDecisionManager);
    }

}
