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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@EnableOAuth2Sso
@RestController
@RequestMapping
public class GitHubSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final String[] PERMITALL_RESOURCE_LIST = new String[]{"/bower_components/**", "/catchPaymentResponse", "/json/**", "/login", "/", "/403", "/registerUser", "/callback", "/fonts/**", "/webjars/**", "/robots.txt", "/assets/**", "/images/**"};

    @Qualifier("userInfoTokenServices")
    @Autowired
    private ResourceServerTokenServices tokenServices;

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http
                .antMatcher("/**").authorizeRequests().expressionHandler(null).
                .antMatchers(PERMITALL_RESOURCE_LIST).permitAll()
                .anyRequest().authenticated()
                .and()
                //.addFilterBefore(new PublicApiTokenAccessFilter(tokenServices), AbstractPreAuthenticatedProcessingFilter.class)
                .logout().logoutUrl("/logout")
                /*.addLogoutHandler((requestArg, responseArg, authenticationArg)
                        -> {

                    HttpServletRequest request = (HttpServletRequest) requestArg;
                    HttpServletResponse response = (HttpServletResponse) responseArg;
                    Authentication authentication = (Authentication) authenticationArg;

                    String authorization = request.getHeader("Authorization");
                    if (authorization != null && authorization.contains("Bearer")) {
                        String tokenValue = authorization.replace("Bearer", "").trim();

                        /* OAuth2AccessToken accessToken = tokenStore.readAccessToken(tokenValue);
                tokenStore.removeAccessToken(accessToken);

                //OAuth2RefreshToken refreshToken = tokenStore.readRefreshToken(tokenValue);
                OAuth2RefreshToken refreshToken = accessToken.getRefreshToken();
                tokenStore.removeRefreshToken(refreshToken);* /
                    }

                    SecurityContextHolder.clearContext();
                    request.getSession().invalidate();
                    authentication.setAuthenticated(false);
                    SecurityContextHolder.clearContext();
                    SecurityContextHolder.getContext().setAuthentication(null);
                    HttpSession session = request.getSession(false);
                    SecurityContextHolder.clearContext();
                    session = request.getSession(false);
                    if (session != null) {
                        session.invalidate();
                    }
                    for (Cookie cookie : request.getCookies()) {
                        cookie.setMaxAge(0);
                    }
                    new SecurityContextLogoutHandler().logout(request, response, authentication);
                    try {
                        request.logout();
                    } catch (ServletException ex) {
                        Logger.getLogger(GitHubSecurityConfig.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    request.getSession().invalidate();

                }
                )*/
                .logoutSuccessUrl("/")
                .oauth2Login();
        //.deleteCookies("XSRF-TOKEN", "auth_code", "JSESSIONID")
        //.invalidateHttpSession(true).clearAuthentication(true).permitAll()
        //.and()
        //.csrf().disable();
    }

    @Bean
    @Primary
    public ResourceServerProperties resource() {
        return new ResourceServerProperties();
    }

}
