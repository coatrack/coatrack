package eu.coatrack.proxy.security;

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

import eu.coatrack.api.ApiKey;
import eu.coatrack.api.ServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;

/**
 *  Checks if the API key token value sent by the client is valid. If so, the client
 *  is forwarded to the requested service API.
 *
 *  @author gr-hovest
 */
@Service
public class ApiKeyAuthTokenVerifier implements AuthenticationManager {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthTokenVerifier.class);

    @Autowired
    private ApiKeyRequester apiKeyRequester;

    @Autowired
    ServiceApiProvider serviceApiProvider;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        try {
            Assert.notNull(authentication.getCredentials());
            Assert.isInstanceOf(String.class, authentication.getCredentials());
            String apiKey = (String) authentication.getCredentials();
            Assert.hasText(apiKey);

            //TODO this is just a workaround for now: check for fixed API key to allow CoatRack admin access
            if (apiKey.equals(ApiKey.API_KEY_FOR_YGG_ADMIN_TO_ACCESS_PROXIES)) {

                Set<SimpleGrantedAuthority> authoritiesGrantedToYggAdmin = new HashSet<>();
                authoritiesGrantedToYggAdmin.add(new SimpleGrantedAuthority(
                        ServiceApiAccessRightsVoter.ACCESS_SERVICE_AUTHORITY_PREFIX + "refresh"));

                ApiKeyAuthToken apiKeyAuthToken = new ApiKeyAuthToken(apiKey, authoritiesGrantedToYggAdmin);
                apiKeyAuthToken.setAuthenticated(true);
                return apiKeyAuthToken;
            }

            // regular api consumer's api key check
            if (apiKeyRequester.isApiKeyValid(apiKey)) {
                // key is valid, now get service api URI identifier
                ServiceApi serviceApi = serviceApiProvider.getServiceApiByApiKey(apiKey);
                String uriIdentifier = serviceApi.getUriIdentifier();

                // add uri identifier as granted authority
                ApiKeyAuthToken apiKeyAuthToken = new ApiKeyAuthToken(apiKey, Collections.singleton(
                        new SimpleGrantedAuthority(
                                ServiceApiAccessRightsVoter.ACCESS_SERVICE_AUTHORITY_PREFIX + uriIdentifier)));

                apiKeyAuthToken.setAuthenticated(true);
                return apiKeyAuthToken;
            } else {
                // TODO log this via MetricsCounterService?
                throw new BadCredentialsException("API key value is not valid");
            }
        } catch (IllegalArgumentException e) {
            log.debug("API key parameter missing or invalid: " + e.getMessage());
            throw new BadCredentialsException("API key parameter missing or invalid: " + e.getMessage());
        }
    }
}
