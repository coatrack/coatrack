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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Checks if the API key token value sent by the client is valid. Right now this
 * is done by a request to CoatRack admin, asking if the token value is known.
 *
 * @author gr-hovest
 */
@Service
public class ApiKeyAuthTokenVerifier implements AuthenticationManager {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthTokenVerifier.class);

    @Autowired
    private ApiKeyValidityChecker apiKeyValidityChecker;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SecurityUtil securityUtil;

    @Value("${ygg.admin.api-base-url}")
    private String adminBaseUrl;

    @Value("${ygg.admin.resources.search-api-keys-by-token-value}")
    private String adminResourceToSearchForApiKeys;

    @Value("${ygg.admin.resources.search-service-by-api-key-value}")
    private String adminResourceToGetServiceByApiKeyValue;

    private List<ServiceApi> serviceApiList = new ArrayList<>();

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
                authoritiesGrantedToYggAdmin.add(new SimpleGrantedAuthority(ServiceApiAccessRightsVoter.ACCESS_SERVICE_AUTHORITY_PREFIX + "refresh"));

                ApiKeyAuthToken apiKeyAuthToken = new ApiKeyAuthToken(apiKey, authoritiesGrantedToYggAdmin);
                apiKeyAuthToken.setAuthenticated(true);
                return apiKeyAuthToken;
            }

            // regular api consumer's api key check
            if (isApiKeyValid(apiKey)) {
                // key is valid, now get service api URI identifier
                ServiceApi serviceApi = getServiceApiByApiKey(apiKey);
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

    private boolean isApiKeyValid(String apiKeyValue) throws AuthenticationException {
        log.debug(String.format("Checking API key value '%s' with CoatRack admin server at '%s'", apiKeyValue, adminBaseUrl));
        String url = securityUtil.attachGatewayApiKeyToUrl(
                adminBaseUrl + adminResourceToSearchForApiKeys + apiKeyValue);
        ResponseEntity<ApiKey> resultOfApiKeySearch = findApiKey(url, apiKeyValue);
        return apiKeyValidityChecker.doesResultValidateApiKey(resultOfApiKeySearch, apiKeyValue);
    }

    private ResponseEntity<ApiKey> findApiKey(String urlToSearchForApiKeys, String apiKeyValue) {
        ResponseEntity<ApiKey> resultOfApiKeySearch;
        try {
            resultOfApiKeySearch = restTemplate.getForEntity(urlToSearchForApiKeys, ApiKey.class);
        } catch (HttpClientErrorException e) {
            interpretAndLogHttpStatus(e, apiKeyValue);
            return null;
        } catch (Exception e) {
            log.info("Connection to admin failed, ResponseEntity is null. Probably the server is temporarily down.", e);
            return null;
        }
        return resultOfApiKeySearch;
    }

    private void interpretAndLogHttpStatus(HttpClientErrorException e, String apiKeyValue) {
        if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            log.debug("API key value is invalid: " + apiKeyValue);
        } else {
            log.error("Error when communicating with auth server", e, " API key value was: " + apiKeyValue,
                    " Maybe your Gateway is deprecated. Please, try downloading and running a new one.");
        }
    }

    //TODO extract this functionality to an extra class: ServiceApiProvider.findServiceApi(String apiKeyValue) -> after cleanup
    private ServiceApi getServiceApiByApiKey(String apiKeyValue) {
        log.debug("Trying to get service API entity by API key value {}", apiKeyValue);
        String urlToGetServiceApi = createUrlToGetServiceApi(apiKeyValue);
        ResponseEntity<ServiceApi> responseEntity;

        try {
            responseEntity = restTemplate.getForEntity(urlToGetServiceApi, ServiceApi.class);
        } catch (Exception e) {
            log.info("The Service API request to the admin server failed. Probably the admin server is " +
                    "temporarily down.", e);
            log.debug("Checking if service API belonging to the API key with the value " + apiKeyValue +
                    " is within the local service API list.");
            return getMatchingServiceApiFromLocalServiceApiList(apiKeyValue);
        }
        return extractServiceApi(responseEntity);
    }

    private String createUrlToGetServiceApi(String apiKeyValue) {
        return securityUtil.attachGatewayApiKeyToUrl(
                adminBaseUrl + adminResourceToGetServiceByApiKeyValue + apiKeyValue);
    }

    private ServiceApi getMatchingServiceApiFromLocalServiceApiList(String apiKeyValue) {
        Optional<ServiceApi> optionalServiceApi = serviceApiList.stream().filter(x -> x.getApiKeys()
                .stream().anyMatch(y -> y.getKeyValue().equals(apiKeyValue))).findFirst();
        if (optionalServiceApi.isPresent())
            return optionalServiceApi.get();
        else {
            log.warn("The API key with the value " + apiKeyValue + " does not belong to any service despite " +
                    "it is considered valid by the gateway. Probably the cause lies within a bad GatewayUpdate " +
                    "received from the admin server which does not include this API keys service.");
            return null;
        }
    }

    private ServiceApi extractServiceApi(ResponseEntity<ServiceApi> responseEntity) {
        if (responseEntity != null) {
            ServiceApi serviceApi = responseEntity.getBody();
            log.debug("Service API was found by CoatRack admin: " + serviceApi);
            return serviceApi;
        } else {
            log.warn("Communication with Admin server failed, result is: " + responseEntity);
            return null;
        }
    }

    public void setServiceApiList(List<ServiceApi> serviceApiList) {
        this.serviceApiList = serviceApiList;
    }
}
