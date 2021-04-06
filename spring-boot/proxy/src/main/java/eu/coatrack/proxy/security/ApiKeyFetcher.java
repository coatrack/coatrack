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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

/**
 * Offers request services to the Coatrack admin server to receive single
 * API keys or a list of API keys belonging to this gateway.
 *
 * @author Christoph Baier
 */

@Service
public class ApiKeyFetcher {

    private static final Logger log = LoggerFactory.getLogger(eu.coatrack.proxy.security.ApiKeyFetcher.class);

    private final UrlResourcesProvider urlResourcesProvider;
    private final RestTemplate restTemplate;

    public ApiKeyFetcher(RestTemplate restTemplate, UrlResourcesProvider urlResourcesProvider) {
        this.restTemplate = restTemplate;
        this.urlResourcesProvider = urlResourcesProvider;
    }

    public List<ApiKey> requestLatestApiKeyListFromAdmin() throws RestClientException {
        log.debug("Requesting latest API key list from CoatRack admin.");

        ResponseEntity<ApiKey[]> responseEntity = restTemplate.getForEntity(
                urlResourcesProvider.getApiKeyListRequestUrl(), ApiKey[].class, urlResourcesProvider.getGatewayId());

        if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
            log.info("Successfully requested latest API key list from CoatRack admin.");
            return Arrays.asList(responseEntity.getBody());
        } else {
            log.warn("Request of latest API key list from CoatRack admin failed. Received http status {} from " +
                    "CoatRack admin.", responseEntity.getStatusCode());
            return null;
        }
    }

    public ApiKey requestApiKeyFromAdmin(String apiKeyValue) throws RestClientException {
        log.debug("Requesting API key with the value {} from CoatRack admin.", apiKeyValue);

        ResponseEntity<ApiKey> responseEntity = restTemplate.getForEntity(
                urlResourcesProvider.getApiKeyRequestUrl(apiKeyValue), ApiKey.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
            log.info("The API key with the value {} was found by CoatRack admin.", apiKeyValue);
            return responseEntity.getBody();
        } else {
            log.info("Fetching the API key with value {} from CoatRack admin failed.", apiKeyValue);
            return null;
        }
    }
}
