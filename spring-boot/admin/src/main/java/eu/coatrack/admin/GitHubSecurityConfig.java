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

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GitHubSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final String[] PERMITALL_RESOURCE_LIST = new String[]{"/bower_components/**", "/catchPaymentResponse", "/json/**", "/login", "/", "/403", "/registerUser", "/callback", "/fonts/**", "/webjars/**", "/robots.txt", "/assets/**", "/images/**"};

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http
                .antMatcher("/**").authorizeRequests()
                .antMatchers(PERMITALL_RESOURCE_LIST).permitAll()
                .anyRequest().authenticated()
                .and()
                .logout().logoutUrl("/logout").logoutSuccessUrl("/").deleteCookies("XSRF-TOKEN", "auth_code", "JSESSIONID").invalidateHttpSession(true).clearAuthentication(true).permitAll()
                .and()
                .oauth2Login()
                .and()
                //.csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
                .csrf().disable();
    }
}
