package eu.coatrack.admin.server.config;

/*-
 * #%L
 * coatrack-config-server
 * %%
 * Copyright (C) 2013 - 2021 Corizon | Institut für angewandte Systemtechnik Bremen GmbH (ATB)
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

import eu.coatrack.admin.server.repository.ConfigServerCredentialRepository;
import eu.coatrack.admin.server.principal.entrypoint.YggBasicAuthenticationEntryPoint;
import eu.coatrack.admin.server.service.YggUserDetailsService;
import eu.coatrack.admin.server.service.filter.CheckIfUrlAndCredentialsFitWithEachOtherFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private YggUserDetailsService userDetailsService;

    @Autowired
    YggBasicAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    ConfigServerCredentialRepository configServerCredentialRepository;

    @Autowired
    CheckIfUrlAndCredentialsFitWithEachOtherFilter filter;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider
                = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);

        return authProvider;
    }

    private final String PROXY_CONFIG_URI_PATTERN = "/ygg-proxy*/default";
    private final String ADMIN_CONFIG_URI_PATTERN = "/ygg-admin/*";
    private final String CREDENTIALS_URI_PATTERN = "/credentials*/**";

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.authorizeRequests()
                // proxies are allowed to access proxy config - detailed mapping is done in separate filter
                .antMatchers(
                        PROXY_CONFIG_URI_PATTERN
                )
                .hasRole("USER")
                // admin is allowed to access admin config and credentials
                .antMatchers(
                        ADMIN_CONFIG_URI_PATTERN,
                        CREDENTIALS_URI_PATTERN
                )
                .hasRole("ADMIN")
                // add a "catchall" rule to assure that no URLs are accessible without authentication (e.g. "/" not covered by above patterns)
                .anyRequest()
                .hasRole("ADMIN")
                .and()
                .httpBasic()
                .authenticationEntryPoint(authenticationEntryPoint)
                .and()
                .sessionManagement()
                // do not create sessions, because proxies/admin should be authenticated on each request
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.addFilterAfter(filter,
                BasicAuthenticationFilter.class).csrf().disable();

    }

}
