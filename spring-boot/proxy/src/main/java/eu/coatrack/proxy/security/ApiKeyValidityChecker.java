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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.sql.Date;
import java.time.LocalDate;

public class ApiKeyValidityChecker {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyValidityChecker.class);

    @Autowired
    private RestTemplate restTemplate;

    private boolean isKeyValid;
    private String apiKeyValue;

    public boolean isApiKeyValid(String apiKeyValue, String uri) throws AuthenticationException {
        this.apiKeyValue = apiKeyValue;
        isKeyValid = true;
        ResponseEntity<ApiKey> resultOfApiKeySearch = findApiKey(uri, apiKeyValue);
        if(!isKeyValid)
            return false;
        return checkResult(resultOfApiKeySearch);
    }

    private boolean checkResult(ResponseEntity<ApiKey> resultOfApiKeySearch) {
        if (resultOfApiKeySearch != null) {
            return isApiKeyPresentAndValid(resultOfApiKeySearch);
        } else {
            log.error("Communication with Admin server failed, result is: " + resultOfApiKeySearch);
            return isKeyValidInLocalDatabase();
        }
    }

    private boolean isKeyValidInLocalDatabase() {
        //checkLocallySavedApiKeyList(); otherwise return false
        return false;
    }

    private boolean isApiKeyPresentAndValid(ResponseEntity<ApiKey> resultOfApiKeySearch) {
        ApiKey apiKey = resultOfApiKeySearch.getBody();
        if (apiKey != null) {
            log.debug("API key was found by CoatRack admin: " + apiKey);
            return isKeyNotDeletedAndNotExpired(apiKey);
        } else {
            log.debug("API key value could not be found by CoatRack Admin: " + apiKey.getKeyValue());
            return false;
        }
    }

    private boolean isKeyNotDeletedAndNotExpired(ApiKey apiKey) {
        boolean isNotExpired = Date.valueOf(LocalDate.now()).after(apiKey.getValidUntil());
        boolean isNotDeleted = apiKey.getDeletedWhen() != null;
        logIfApiKeyisExpiredOrDeleted(isNotExpired, isNotDeleted);
        return isNotDeleted && isNotExpired;
    }

    private void logIfApiKeyisExpiredOrDeleted(boolean isNotExpired, boolean isNotDeleted) {
        if(!isNotExpired)
            log.info("Access to services denied. The api key with the value " + apiKeyValue + " is expired.");
        if (!isNotDeleted)
            log.info("Access to services denied. The api key with the value " + apiKeyValue + " is deleted.");
    }

    private ResponseEntity<ApiKey> findApiKey(String urlToSearchForApiKeys, String apiKeyValue) {
        ResponseEntity<ApiKey> resultOfApiKeySearch;
        try {
            resultOfApiKeySearch = restTemplate.getForEntity(urlToSearchForApiKeys, ApiKey.class);
        } catch (HttpClientErrorException e) {
            logHttpStatusAndCheckValidity(e, apiKeyValue);
            return null;
        }
        return resultOfApiKeySearch;
    }

    private void logHttpStatusAndCheckValidity(HttpClientErrorException e, String apiKeyValue) {
        if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            log.debug("API key value is invalid: " + apiKeyValue);
            isKeyValid = false;
        } else {
            log.error("Error when communicating with auth server", e);
        }
    }
}
