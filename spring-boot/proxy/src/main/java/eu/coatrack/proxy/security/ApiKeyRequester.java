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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 *  This bean provides a validity check of an API key. Its purpose
 *  is to perform a request to the admin server and redirect the
 *  results to another bean for evaluation.
 *
 *  @author Christoph Baier
 */

@Service
public class ApiKeyRequester {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthTokenVerifier.class);

    @Autowired
    private ApiKeyValidityVerifier apiKeyValidityVerifier;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SecurityUtil securityUtil;

    @Value("${ygg.admin.api-base-url}")
    private String adminBaseUrl;

    @Value("${ygg.admin.resources.search-api-keys-by-token-value}")
    private String adminResourceToSearchForApiKeys;

    public boolean isApiKeyValid(String apiKeyValue) {
        log.debug(String.format("Checking API key value '%s' with CoatRack admin server at '%s'",
                apiKeyValue, adminBaseUrl));
        String url = securityUtil.attachGatewayApiKeyToUrl(
                adminBaseUrl + adminResourceToSearchForApiKeys + apiKeyValue);
        ResponseEntity<ApiKey> resultOfApiKeySearch = findApiKey(url, apiKeyValue);
        return apiKeyValidityVerifier.doesResultValidateApiKey(resultOfApiKeySearch, apiKeyValue);
    }

    private ResponseEntity<ApiKey> findApiKey(String urlToSearchForApiKeys, String apiKeyValue) {
        ResponseEntity<ApiKey> resultOfApiKeySearch;
        try {
            resultOfApiKeySearch = restTemplate.getForEntity(urlToSearchForApiKeys, ApiKey.class);
        } catch (HttpClientErrorException e) {
            interpretAndLogHttpStatus(e, apiKeyValue);
            return null;
        } catch (Exception e) {
            log.info("Connection to admin failed, ResponseEntity is null. Probably the server is " +
                    "temporarily down.");
            return null;
        }
        return resultOfApiKeySearch;
    }

    private void interpretAndLogHttpStatus(HttpClientErrorException e, String apiKeyValue) {
        if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            log.info("API key value is invalid: " + apiKeyValue);
        } else {
            log.warn("Error when communicating with the admin server", e, " The API keys " +
                    "value was: " + apiKeyValue, "Maybe your Gateway is deprecated. " +
                    "Please, try downloading and running a new one.");
        }
    }
}
