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
import eu.coatrack.proxy.security.exceptions.ApiKeyFetchingFailedException;
import eu.coatrack.proxy.security.exceptions.AuthenticationProcessFailedException;
import eu.coatrack.proxy.security.exceptions.OfflineWorkingTimeExceedingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
    private final Set<SimpleGrantedAuthority> authoritiesGrantedToCoatRackAdminApp = new HashSet<>();

    public ApiKeyAuthTokenVerifier(LocalApiKeyManager localApiKeyManager,
                                   ApiKeyFetcher apiKeyFetcher, ApiKeyVerifier apiKeyVerifier) {
        this.localApiKeyManager = localApiKeyManager;
        this.apiKeyFetcher = apiKeyFetcher;
        this.apiKeyVerifier = apiKeyVerifier;

        authoritiesGrantedToCoatRackAdminApp.add(new SimpleGrantedAuthority(
                ServiceApiAccessRightsVoter.ACCESS_SERVICE_AUTHORITY_PREFIX + "refresh"));
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        try {
            log.debug("Verifying the authentication {}.", authentication.getName());
            String apiKeyValue = extractApiKeyValueFromAuthentication(authentication);
            return doesApiKeyBelongToAdminApp(apiKeyValue) ? createAdminAuthTokenFromApiKey(apiKeyValue)
                    : createConsumerAuthTokenIfApiKeyIsAuthorized(apiKeyValue);
        } catch (Exception e) {
            if (e instanceof AuthenticationException)
                throw e;
            else
                log.error("During the authentication process this unexpected exception occurred: ", e);

            throw new AuthenticationProcessFailedException("The authentication process failed due to an unknown error.");
        }
    }

    private String extractApiKeyValueFromAuthentication(Authentication authentication) {
        log.debug("Getting API key value from authentication {}.", authentication.getName());

        Assert.notNull(authentication.getCredentials(), "The credentials of " + authentication.getName() + " were null.");
        Assert.isInstanceOf(String.class, authentication.getCredentials());
        String apiKeyValue = (String) authentication.getCredentials();
        Assert.hasText(apiKeyValue, "The credentials did not contain any letters.");

        return apiKeyValue;
    }

    private boolean doesApiKeyBelongToAdminApp(String apiKeyValue) {
        log.debug("Checking if '{}' is an API key of the admin application.", apiKeyValue);
        return apiKeyValue.equals(ApiKey.API_KEY_FOR_YGG_ADMIN_TO_ACCESS_PROXIES);
    }

    private Authentication createAdminAuthTokenFromApiKey(String apiKeyValue) {
        log.debug("Creating admins authentication token using API key with the value {}.", apiKeyValue);
        ApiKeyAuthToken apiKeyAuthTokenForValidApiKey = new ApiKeyAuthToken(apiKeyValue, authoritiesGrantedToCoatRackAdminApp);
        apiKeyAuthTokenForValidApiKey.setAuthenticated(true);
        return apiKeyAuthTokenForValidApiKey;
    }

    private Authentication createConsumerAuthTokenIfApiKeyIsAuthorized(String apiKeyValue) throws
            BadCredentialsException, OfflineWorkingTimeExceedingException {
        log.debug("Verifying the API with the value {} from consumer.", apiKeyValue);

        ApiKey apiKey = getApiKeyEntityByApiKeyValue(apiKeyValue);
        if (apiKeyVerifier.isApiKeyValid(apiKey))
            return createAuthTokenGrantingAccessToServiceApi(apiKey);
        else
            throw new BadCredentialsException("The API key " + apiKeyValue + " is not valid.");
    }

    private ApiKey getApiKeyEntityByApiKeyValue(String apiKeyValue) throws OfflineWorkingTimeExceedingException {
        ApiKey apiKey;
        try {
            apiKey = apiKeyFetcher.requestApiKeyFromAdmin(apiKeyValue);
        } catch (ApiKeyFetchingFailedException e) {
            log.debug("Trying to verify consumers API key with the value {}, the connection to admin failed. " +
                    "Therefore checking the local API key list as fallback solution.", apiKeyValue);
            apiKey = getApiKeyEntityFromLocalCache(apiKeyValue);
        }
        return apiKey;
    }

    private ApiKey getApiKeyEntityFromLocalCache(String apiKeyValue) throws OfflineWorkingTimeExceedingException {
        if (localApiKeyManager.isOfflineWorkingTimeExceeded())
            throw new OfflineWorkingTimeExceedingException("The predefined time for working in offline mode is exceeded. The " +
                    "gateway will reject every request until a connection to CoatRack admin could be re-established.");
        else
            return localApiKeyManager.getApiKeyEntityByApiKeyValue(apiKeyValue);
    }

    private ApiKeyAuthToken createAuthTokenGrantingAccessToServiceApi(ApiKey apiKey) {
        log.debug("Create consumers authentication token using API key with the value {}.", apiKey.getKeyValue());

        String serviceUriIdentifier = apiKey.getServiceApi().getUriIdentifier();
        ApiKeyAuthToken apiKeyAuthToken = new ApiKeyAuthToken(apiKey.getKeyValue(), Collections.singleton(
                new SimpleGrantedAuthority(ServiceApiAccessRightsVoter.ACCESS_SERVICE_AUTHORITY_PREFIX
                        + serviceUriIdentifier)));
        apiKeyAuthToken.setAuthenticated(true);

        return apiKeyAuthToken;
    }
}
