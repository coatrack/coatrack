package eu.coatrack.admin.server.service.filter;

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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import eu.coatrack.admin.server.repository.ConfigServerCredentialRepository;
import eu.coatrack.config.ConfigServerCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

/**
 *
 * @author perezdf // TODO Consider the idea to split up the filter in case, it
 * keep growing up
 */
@Component
public class CheckIfUrlAndCredentialsFitWithEachOtherFilter extends GenericFilterBean {

    private static final Logger log = LoggerFactory.getLogger(CheckIfUrlAndCredentialsFitWithEachOtherFilter.class);

    @Autowired
    ConfigServerCredentialRepository configServerCredentialRepository;

    @Value("${ygg.admin.config.access.user.name}")
    String name;

    @Value("${ygg.admin.config.access.user.password}")
    String password;

    private final String ADMIN_URI = "/ygg-admin";
    private final String PROXY_URI = "/ygg-proxy";
    private final String CREDENTIAL_URI = "/credentials";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest
                // added the following check to avoid "IllegalStateException: resource already commited"
                && !response.isCommitted()) {
            /**
             * Cast request/response
             */
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // Retrieve URI
            String url = httpRequest.getRequestURI();

            // Accepted URI - only check in case it is a proxy URI, other cases are covered by WebSecurityConfig
            if (url.startsWith(PROXY_URI)) { // || url.startsWith(ADMIN_URI) || url.startsWith(CREDENTIAL_URI)) {

                // Retrieve the user/password
                final String authorization = httpRequest.getHeader("Authorization");
                if (authorization != null && authorization.toLowerCase().startsWith("basic")) {
                    // Authorization: Basic base64credentials
                    String base64Credentials = authorization.substring("Basic".length()).trim();
                    byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
                    String credentials = new String(credDecoded, StandardCharsets.UTF_8);

                    // Credentials = username:password
                    final String[] values = credentials.split(":", 2);
                    if (values != null && values.length > 0) {
                        String user = values[0];

                        // check if the proxy is accessing only URLs that this proxy is allowed to access
                        ConfigServerCredential credentialForUserAndResource = configServerCredentialRepository.findOneByNameAndResource(user, url);
                        if (credentialForUserAndResource != null) {
                            log.debug("Credential was found: {} -> this user is authorized to access this resource", credentialForUserAndResource);
                        } else {
                            log.debug("No Credential was found for user {} and resource {}", user, url);
                            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to access: " + url);
                        }

                        // Retrieve the Id in Long format TODO Consider to separate the business key to the technical key which generates such kind of dirty logic
                        /**
                         * For the Proxy user case
                         */
                        // It is a proxy credential key, then retrieve the credential
                        /*
                        ConfigServerCredential credential = configServerCredentialRepository.findOneByName(user);
                        if (url.startsWith(PROXY_URI) && credential == null) {
                            // There is no credential, then returns an error
                            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Credenctial is missing:" + url);
                        } // Check Resource, not required yet a complete Resource Server because one credential has one resource
                        else if (credential != null && credential.getResource() != null && !url.equals(credential.getResource())) {
                            // There is no credential, then returns an error
                            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid resource :" + url);
                        }

                        /**
                         * For the admin case
                         */
                        /*
                        if ((url.startsWith(ADMIN_URI) || url.startsWith(CREDENTIAL_URI)) && !user.equals(name)) // CKECK
                        {

                            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Provider user doesnt match with administrator user" + url);
                        }
                        */

                    }
                }
            }

        }
        chain.doFilter(request, response);
    }

}
