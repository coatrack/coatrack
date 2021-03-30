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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.*;

/**
 * Checks if the API key token value sent by the service consumer is valid.
 *
 * @author gr-hovest, Christoph Baier
 */

@Service("apiKeyAuthTokenVerifier")
public class ApiKeyAuthTokenVerifier implements AuthenticationManager {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthTokenVerifier.class);

    private final LocalApiKeyAndServiceApiManager localApiKeyAndServiceApiManager;
    private final AdminCommunicator adminCommunicator;

    public ApiKeyAuthTokenVerifier(LocalApiKeyAndServiceApiManager localApiKeyAndServiceApiManager, AdminCommunicator adminCommunicator) {
        this.localApiKeyAndServiceApiManager = localApiKeyAndServiceApiManager;
        this.adminCommunicator = adminCommunicator;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        log.debug("Verifying the authentication {}.", authentication.getName());
        try {
            String apiKeyValue = getApiKeyValue(authentication);
            //TODO this is just a workaround for now: check for fixed API key to allow CoatRack admin access
            if (isAdminsKey(apiKeyValue)) {
                return createAdminsAuthToken(apiKeyValue);
            }

            if (isApiKeyFromConsumerValid(apiKeyValue)) {
                return createConsumersAuthToken(apiKeyValue);
            }

        } catch (Exception e) {
            log.info("During the authentication process this exception occurred: ", e);
        }
        throw new SessionAuthenticationException("The authentication process failed.");
    }

    private String getApiKeyValue(Authentication authentication) {
        log.debug("Getting API key value from authentication {}.", authentication.getName());
        Assert.notNull(authentication.getCredentials());
        Assert.isInstanceOf(String.class, authentication.getCredentials());
        String apiKeyValue = (String) authentication.getCredentials();
        Assert.hasText(apiKeyValue);
        return apiKeyValue;
    }

    private boolean isAdminsKey(String apiKeyValue) {
        log.debug("Checking if the API with the value {} is admins API key.", apiKeyValue);
        return apiKeyValue.equals(ApiKey.API_KEY_FOR_YGG_ADMIN_TO_ACCESS_PROXIES);
    }

    private Authentication createAdminsAuthToken(String apiKeyValue) {
        log.debug("Creating admins authentication token using API key with the value {}.", apiKeyValue);
        Set<SimpleGrantedAuthority> authoritiesGrantedToYggAdmin = new HashSet<>();
        authoritiesGrantedToYggAdmin.add(new SimpleGrantedAuthority(
                ServiceApiAccessRightsVoter.ACCESS_SERVICE_AUTHORITY_PREFIX + "refresh"));
        ApiKeyAuthToken apiKeyAuthToken = new ApiKeyAuthToken(apiKeyValue, authoritiesGrantedToYggAdmin);
        apiKeyAuthToken.setAuthenticated(true);
        log.info("Admin is successfully authenticated using the API key with the value {}.", apiKeyValue);
        return apiKeyAuthToken;
    }

    private boolean isApiKeyFromConsumerValid(String apiKeyValue) {
        log.debug("Verifying the API with the value {} from consumer.", apiKeyValue);
        boolean isApiKeyValid;
        try {
            ApiKey apiKey = adminCommunicator.requestApiKeyFromAdmin(apiKeyValue);
            isApiKeyValid = localApiKeyAndServiceApiManager.isApiKeyReceivedFromAdminValid(apiKey);
        } catch (Exception e) {
            log.info("Trying to verify consumers API key with the value {}, the connection to admin " +
                    "failed. Probably the server is temporarily down.", apiKeyValue);
            isApiKeyValid = localApiKeyAndServiceApiManager.isApiKeyValidConsideringTheLocalApiKeyList(apiKeyValue);
        }
        return isApiKeyValid;
    }

    private ApiKeyAuthToken createConsumersAuthToken(String apiKeyValue) {
        log.debug("Create consumers authentication token using API key with the value {}.", apiKeyValue);
        ServiceApi serviceApi = findServiceApi(apiKeyValue);
        String uriIdentifier = serviceApi.getUriIdentifier();

        ApiKeyAuthToken apiKeyAuthToken = new ApiKeyAuthToken(apiKeyValue, Collections.singleton(
                new SimpleGrantedAuthority(
                        ServiceApiAccessRightsVoter.ACCESS_SERVICE_AUTHORITY_PREFIX + uriIdentifier)));
        apiKeyAuthToken.setAuthenticated(true);
        return apiKeyAuthToken;
    }

    private ServiceApi findServiceApi(String apiKeyValue) {
        log.debug("Finding the service API for the API with the value " + apiKeyValue);
        ServiceApi serviceApi;
        try {
            serviceApi = adminCommunicator.requestServiceApiFromAdmin(apiKeyValue);
        } catch (Exception e) {
            log.info("Trying to receive a service API from CoatRack admin, the connection process failed. Probably " +
                    "the server is temporarily down.");
            serviceApi = localApiKeyAndServiceApiManager.getServiceApiFromLocalList(apiKeyValue);
        }
        return serviceApi;
    }
}
