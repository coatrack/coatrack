package eu.coatrack.proxy.security;

/*-
 * #%L
 * coatrack-proxy
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

import eu.coatrack.api.ApiKey;
import eu.coatrack.proxy.security.exceptions.ApiKeyFetchingFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Offers request services to the Coatrack admin server to receive single
 * API keys or a list of API keys for all services offered by this gateway.
 *
 * @author Christoph Baier
 */

@Service
public class ApiKeyFetcher {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFetcher.class);

    private final UrlResourcesProvider urlResourcesProvider;
    private final RestTemplate restTemplate;

    public ApiKeyFetcher(RestTemplate restTemplate, UrlResourcesProvider urlResourcesProvider) {
        this.restTemplate = restTemplate;
        this.urlResourcesProvider = urlResourcesProvider;
    }

    public List<ApiKey> requestLatestApiKeyListFromAdmin() throws ApiKeyFetchingFailedException {
        log.debug("Requesting latest API key list from CoatRack admin.");

        ResponseEntity<ApiKey[]> responseEntity;
        try {
            responseEntity = restTemplate.getForEntity(
                    urlResourcesProvider.getApiKeyListRequestUrl(), ApiKey[].class);
        } catch (RestClientException e) {
            throw new ApiKeyFetchingFailedException("Trying to request the latest API key list from Admin, the " +
                    "connection failed." + e.getMessage() + e);
        }

        ApiKey[] apiKeys = (ApiKey[]) extractBodyFromResponseEntity(responseEntity);
        if (apiKeys == null)
            throw new ApiKeyFetchingFailedException("Received null instead of an API key list.");
        else
            return new ArrayList<>(Arrays.asList(apiKeys));
    }

    private Object extractBodyFromResponseEntity(ResponseEntity<?> responseEntity) {
        log.debug("Extracting ResponseEntity.");
        if (responseEntity == null)
            return null;

        boolean isResponseEntityOk = responseEntity.getStatusCode() == HttpStatus.OK
                && responseEntity.getBody() != null;
        return isResponseEntityOk ? responseEntity.getBody() : null;
    }

    public ApiKey requestApiKeyFromAdmin(String apiKeyValue) throws ApiKeyFetchingFailedException {
        log.debug("Requesting API key with the value {} from CoatRack admin.", apiKeyValue);

        if (apiKeyValue == null)
            throw new ApiKeyFetchingFailedException("Provided API key was null.");

        ResponseEntity<ApiKey> responseEntity;
        try {
            responseEntity = restTemplate.getForEntity(
                    urlResourcesProvider.getApiKeyRequestUrl(apiKeyValue), ApiKey.class);
        } catch (RestClientException e) {
            throw new ApiKeyFetchingFailedException("Trying to request the API key with the value " + apiKeyValue +
                    " from CoatRack admin, the connection failed." + e.getMessage() + e);
        }
        return (ApiKey) extractBodyFromResponseEntity(responseEntity);
    }
}
