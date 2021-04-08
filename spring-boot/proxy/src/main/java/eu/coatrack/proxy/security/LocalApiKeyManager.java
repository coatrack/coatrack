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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Locally stores, periodically refreshes and provides API keys and their
 * associated service APIs belonging to the gateway.
 *
 * @author Christoph Baier
 */

@EnableAsync
@Service
public class LocalApiKeyManager {

    private static final Logger log = LoggerFactory.getLogger(LocalApiKeyManager.class);

    private List<ApiKey> localApiKeyList = new ArrayList<>();
    private LocalDateTime deadline = LocalDateTime.now();

    private final ApiKeyFetcher apiKeyFetcher;
    private final long numberOfMinutesTheGatewayShallWorkWithoutConnectionToAdmin;

    public LocalApiKeyManager(
            ApiKeyFetcher apiKeyFetcher,
            @Value("${number-of-minutes-the-gateway-shall-work-without-connection-to-admin}") long minutes) {
        this.apiKeyFetcher = apiKeyFetcher;
        this.numberOfMinutesTheGatewayShallWorkWithoutConnectionToAdmin = minutes;
    }

    public ApiKey findApiKeyFromLocalApiKeyList(String apiKeyValue) {
        log.debug("Trying to find the service API associated to the API key with the value {} from the local list.",
                apiKeyValue);

        if (apiKeyValue == null) {
            log.info("The passed API key value is null and can therefore not be checked for validity. " +
                    "It is therefore rejected.");
            return null;
        }
        Optional<ApiKey> optionalApiKey = localApiKeyList.stream().filter(
                apiKeyFromLocalList -> apiKeyFromLocalList.getKeyValue().equals(apiKeyValue)).findFirst();
        return extractOptionalApiKey(optionalApiKey);
    }

    private ApiKey extractOptionalApiKey(Optional<ApiKey> optionalApiKey) {
        if (optionalApiKey.isPresent()) {
            log.debug("The API key is found in the local API key list.");
            return optionalApiKey.get();
        } else {
            log.debug("The API key can not be found within the local API key list.");
            return null;
        }
    }

    public boolean wasLatestUpdateOfLocalApiKeyListWithinDeadline() {
        boolean wasLatestUpdateWithinDeadline = LocalDateTime.now().isBefore(deadline);

        if (!wasLatestUpdateWithinDeadline)
            log.info("The CoatRack admin server was not reachable for longer than {} minutes. Since this " +
                    "predefined threshold was exceeded, this and all subsequent service API requests are " +
                    "rejected until a connection to CoatRack admin could be re-established.",
                    numberOfMinutesTheGatewayShallWorkWithoutConnectionToAdmin);

        return wasLatestUpdateWithinDeadline;
    }

    @Async
    @PostConstruct
    @Scheduled(fixedRateString = "${local-api-key-list-update-interval-in-millis}")
    public void updateLocalApiKeyList() {
        List<ApiKey> apiKeys;
        try {
            apiKeys = apiKeyFetcher.requestLatestApiKeyListFromAdmin();
        } catch (ApiKeyFetchingException e) {
            log.info("Trying to update the local API key list, the connection to CoatRack admin failed.");
            log.debug("Following error occurred: " + e);
            return;
        }
        localApiKeyList = apiKeys;
        deadline = LocalDateTime.now().plusMinutes(numberOfMinutesTheGatewayShallWorkWithoutConnectionToAdmin);
    }
}
