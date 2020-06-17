package eu.coatrack.admin.security;

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

import eu.coatrack.api.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

/**
 * Filter that checks if there is a request parameter 'gateway-api-key'. If this
 * parameter is included in the request, it is added to the security context in
 * the form of a @{@link GatewayAuthToken}.
 *
 * @author gr-hovest
 */
public class GatewayAuthFilter extends GenericFilterBean {

    private static final Logger log = LoggerFactory.getLogger(GatewayAuthFilter.class);

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        if (servletRequest instanceof HttpServletRequest) {
            final HttpServletRequest req = (HttpServletRequest) servletRequest;
            final String keyParam = req.getParameter(Proxy.GATEWAY_API_KEY_REQUEST_PARAMETER_NAME);
            log.debug(String.format("request parameter '%s' has value '%s'", Proxy.GATEWAY_API_KEY_REQUEST_PARAMETER_NAME, keyParam));

            if (keyParam != null && !keyParam.isEmpty()) {

                // Check credentials
                HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;

                final String authorization = httpRequest.getHeader("Authorization");
                if (authorization != null && authorization.toLowerCase().startsWith("basic")) {
                    log.debug("Authorization header is: '{}'", authorization);
                    // in case there is more than one auth token --> get the first one
                    String firstAuthTokenFromHeader = authorization.split(",")[0];
                    log.debug("First token from auth header is: '{}'", firstAuthTokenFromHeader);
                    // Authorization: Basic base64credentials
                    String base64Credentials = firstAuthTokenFromHeader.substring("Basic".length()).trim();
                    byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
                    String credentials = new String(credDecoded, StandardCharsets.UTF_8);

                    // Credentials = username:password
                    final String[] values = credentials.split(":", 2);
                    if (values != null && values.length > 0) {
                        String user = values[0];
                        String password = values[1];
                        Authentication authToken = new GatewayAuthToken(keyParam, user, password, Collections.emptyList());
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }

            }
        } else {
            log.debug("no HttpServletRequest: {}", servletRequest);
        }
        log.debug("filter chain is {}", filterChain);
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
