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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Checks if the API key token value sent by the client is valid. If so, the client
 * is forwarded to the requested service API.
 *
 * @author gr-hovest, Christoph Baier
 */

@Service
public class ApiKeyAuthTokenVerifier implements AuthenticationManager {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthTokenVerifier.class);

    private final LocalApiKeyManager localApiKeyManager;
    private final ApiKeyFetcher apiKeyFetcher;
    private final ApiKeyVerifier apiKeyVerifier;
    private final Set<SimpleGrantedAuthority> authoritiesGrantedToYggAdmin = new HashSet<>();

    public ApiKeyAuthTokenVerifier(LocalApiKeyManager localApiKeyManager,
                                   ApiKeyFetcher apiKeyFetcher, ApiKeyVerifier apiKeyVerifier) {
        this.localApiKeyManager = localApiKeyManager;
        this.apiKeyFetcher = apiKeyFetcher;
        this.apiKeyVerifier = apiKeyVerifier;

        authoritiesGrantedToYggAdmin.add(new SimpleGrantedAuthority(
                ServiceApiAccessRightsVoter.ACCESS_SERVICE_AUTHORITY_PREFIX + "refresh"));
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        try {
            log.debug("Verifying the authentication {}.", authentication.getName());
            return createApiKeyAuthToken(authentication);
        } catch (Exception e) {
            log.info("During the authentication process this exception occurred: ", e);
        }
        throw new SessionAuthenticationException("The authentication process failed.");
    }

    private Authentication createApiKeyAuthToken(Authentication authentication) {
        String apiKeyValue = extractApiKeyValueFromAuthentication(authentication);
        //TODO this is just a workaround for now: check for fixed API key to allow CoatRack admin access
        return isApiKeyOfAdminApp(apiKeyValue) ? createAdminAuthTokenFromApiKey(apiKeyValue)
                : createConsumerAuthTokenIfApiKeyIsAuthorized(apiKeyValue);
    }

    private String extractApiKeyValueFromAuthentication(Authentication authentication) {
        log.debug("Getting API key value from authentication {}.", authentication.getName());

        Assert.notNull(authentication.getCredentials(), "The credentials were null.");
        Assert.isInstanceOf(String.class, authentication.getCredentials());
        String apiKeyValue = (String) authentication.getCredentials();
        Assert.hasText(apiKeyValue, "The credentials did not contain any letters.");

        return apiKeyValue;
    }

    private boolean isApiKeyOfAdminApp(String apiKeyValue) {
        log.debug("Checking if '{}' is an API key of the admin application.", apiKeyValue);
        return apiKeyValue.equals(ApiKey.API_KEY_FOR_YGG_ADMIN_TO_ACCESS_PROXIES);
    }

    private Authentication createAdminAuthTokenFromApiKey(String apiKeyValue) {
        log.debug("Creating admins authentication token using API key with the value {}.", apiKeyValue);
        ApiKeyAuthToken apiKeyAuthToken = new ApiKeyAuthToken(apiKeyValue, authoritiesGrantedToYggAdmin);
        apiKeyAuthToken.setAuthenticated(true);
        return apiKeyAuthToken;
    }

    private Authentication createConsumerAuthTokenIfApiKeyIsAuthorized(String apiKeyValue) {
        log.debug("Verifying the API with the value {} from consumer.", apiKeyValue);
        ApiKey apiKey = getApiKey(apiKeyValue);
        return apiKeyVerifier.isApiKeyValid(apiKey) ? createConsumersAuthToken(apiKey) : null;
    }

    private ApiKey getApiKey(String apiKeyValue) {
        ApiKey apiKey;
        try {
            apiKey = apiKeyFetcher.requestApiKeyFromAdmin(apiKeyValue);
        } catch (ApiKeyFetchingException e) {
            log.debug("Trying to verify consumers API key with the value {}, the connection to admin failed.",
                    apiKeyValue);
            apiKey = createApiKeyLocally(apiKeyValue);
        }
        return apiKey;
    }

    private ApiKey createApiKeyLocally(String apiKeyValue) {
        if (!localApiKeyManager.wasLatestUpdateOfLocalApiKeyListWithinDeadline()){
            log.warn("The predefined time for working in offline mode is exceeded. The gateway will reject " +
                    "every request until a connection to CoatRack admin could be re-established.");
            return null;
        }
        return localApiKeyManager.findApiKeyFromLocalApiKeyList(apiKeyValue);
    }

    private ApiKeyAuthToken createConsumersAuthToken(ApiKey apiKey) {
        log.debug("Create consumers authentication token using API key with the value {}.", apiKey.getKeyValue());

        String serviceUriIdentifier = apiKey.getServiceApi().getUriIdentifier();
        ApiKeyAuthToken apiKeyAuthToken = new ApiKeyAuthToken(apiKey.getKeyValue(), Collections.singleton(
                new SimpleGrantedAuthority(ServiceApiAccessRightsVoter.ACCESS_SERVICE_AUTHORITY_PREFIX
                        + serviceUriIdentifier)));
        apiKeyAuthToken.setAuthenticated(true);

        return apiKeyAuthToken;
    }
}
