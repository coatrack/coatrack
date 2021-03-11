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
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.sql.Date;
import java.time.LocalDate;

public class ApiKeyValidityChecker {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyValidityChecker.class);

    @Autowired
    private RestTemplate restTemplate;

    private String apiKeyValue;

    public boolean doesResultValidateApiKey(ResponseEntity<ApiKey> resultOfApiKeySearch, String apiKeyValue) throws AuthenticationException {
        if (resultOfApiKeySearch != null) {
            return isApiKeyPresentAndValid(resultOfApiKeySearch);
        } else {
            log.error("Communication with Admin server failed checking API key with the value: " + apiKeyValue);
            return isKeyValidInLocalDatabase();
        }
    }

    private boolean isKeyValidInLocalDatabase() {
        //TODO checkLocallySavedApiKeyList(); otherwise return false
        return false;
    }

    private boolean isApiKeyPresentAndValid(ResponseEntity<ApiKey> resultOfApiKeySearch) {
        ApiKey apiKey = resultOfApiKeySearch.getBody();
        if (apiKey != null) {
            apiKeyValue = apiKey.getKeyValue();
            log.debug("CoatRack admin found a valid key with the value: " + apiKeyValue);
            return isKeyNeitherDeletedNorExpired(apiKey);
        } else {
            log.debug("API key value could not be found by CoatRack Admin: " + apiKey.getKeyValue());
            return false;
        }
    }

    private boolean isKeyNeitherDeletedNorExpired(ApiKey apiKey) {
        boolean isExpired = Date.valueOf(LocalDate.now()).after(apiKey.getValidUntil());
        boolean isDeleted = apiKey.getDeletedWhen() != null;
        logIfApiKeyisExpiredOrDeleted(isExpired, isDeleted);
        return !(isDeleted || isExpired);
    }

    private void logIfApiKeyisExpiredOrDeleted(boolean isExpired, boolean isDeleted) {
        String preamble = "Access to services denied. The api key with the value ";
        String helpInstruction = "Please create a new one.";
        if(isExpired)
            log.info(preamble + apiKeyValue + " is expired. " + helpInstruction);
        if (isDeleted)
            log.info(preamble + apiKeyValue + " is deleted. " + helpInstruction);
    }
}
