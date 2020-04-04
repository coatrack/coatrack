package eu.iof2020.ygg.proxy.filters.pre;

/*-
 * #%L
 * ygg-proxy
 * %%
 * Copyright (C) 2013 - 2019 Corizon
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

import eu.iof2020.ygg.proxy.security.ApiKeyAuthToken;
import eu.iof2020.ygg.proxy.security.ApiKeyAuthTokenVerifier;
import eu.coatrack.api.ApiKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;

/**
 * Simple filter that extracts the API key token value from the incoming request and adds it to the security context
 *
 * @author gr-hovest
 */
public class ApiKeyAuthFilter extends GenericFilterBean {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);

    @Autowired
    private ApiKeyAuthTokenVerifier apiKeyAuthTokenVerifier;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        log.debug(String.format("Api key auth filter received request of type %s", servletRequest.getClass()));

        if (servletRequest instanceof HttpServletRequest) {
            final HttpServletRequest req = (HttpServletRequest) servletRequest;
            final String keyParam = req.getParameter(ApiKey.API_KEY_REQUEST_PARAMETER_NAME);
            log.debug(String.format("request parameter '%s' has value '%s'", ApiKey.API_KEY_REQUEST_PARAMETER_NAME, keyParam));

            Authentication authToken = new ApiKeyAuthToken(keyParam, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
