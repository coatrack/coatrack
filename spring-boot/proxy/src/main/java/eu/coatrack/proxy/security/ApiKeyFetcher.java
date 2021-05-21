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

import java.util.*;

/**
 * Sends requests to the Coatrack admin server to receive single
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

    public List<ApiKey> requestLatestApiKeyListFromAdmin() {
        log.debug("Requesting latest API key list from CoatRack admin.");

        try {
            ResponseEntity<ApiKey[]> responseEntity = restTemplate.getForEntity(
                    urlResourcesProvider.getApiKeyListRequestUrl(), ApiKey[].class);
            ApiKey[] apiKeys = (ApiKey[]) extractBodyFromResponseEntity(responseEntity);
            return new ArrayList<>(Arrays.asList(apiKeys));
        } catch (RestClientException e) {
            throw new ApiKeyFetchingFailedException("Trying to request the latest API key list from Admin, the " +
                    "connection failed.", e);
        }
    }

    private Object extractBodyFromResponseEntity(ResponseEntity<?> responseEntity) {
        log.debug("Extracting ResponseEntity: " + responseEntity);

        Optional<String> errorMessage = validateResponseEntityAndCreateErrorMessageInCaseOfProblems(responseEntity);

        if (errorMessage.isPresent())
            throw new ApiKeyFetchingFailedException("A problem occurred referring to the ResponseEntity. "
                    + errorMessage.get());
        else
            return responseEntity.getBody();
    }

    private Optional<String> validateResponseEntityAndCreateErrorMessageInCaseOfProblems(ResponseEntity<?> responseEntity) {
        Optional<String> errorMessage = Optional.empty();

        if (responseEntity == null)
            errorMessage = Optional.of("The ResponseEntity was null.");
        else if (responseEntity.getBody() == null)
            errorMessage = Optional.of("The body was null");
        else if (responseEntity.getStatusCode() != HttpStatus.OK)
            errorMessage = Optional.of("The HTTP status was not OK.");

        return errorMessage;
    }

    public ApiKey requestApiKeyFromAdmin(String apiKeyValue) {
        log.debug("Requesting API key with the value {} from CoatRack admin.", apiKeyValue);

        try {
            ResponseEntity<ApiKey> responseEntity = restTemplate.getForEntity(
                    urlResourcesProvider.getApiKeyRequestUrl(apiKeyValue), ApiKey.class);
            return (ApiKey) extractBodyFromResponseEntity(responseEntity);
        } catch (RestClientException e) {
            throw new ApiKeyFetchingFailedException("Trying to request the API key with the value " + apiKeyValue +
                    " from CoatRack admin, the connection failed.", e);
        }
    }
}
