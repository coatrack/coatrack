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
import eu.coatrack.proxy.security.exceptions.ApiKeyNotFoundInLocalApiKeyListException;
import eu.coatrack.proxy.security.exceptions.LocalApiKeyListWasNotInitializedException;
import eu.coatrack.proxy.security.exceptions.OfflineWorkingTimeExceedingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Provides a local cache for API keys, allowing the gateway to validate API keys without connection to CoatRack
 * admin application. Caches (and periodically refreshes) a list of all API keys assigned to all services offered
 * by the gateway.
 *
 * @author Christoph Baier
 */

@EnableAsync
@Service
public class LocalApiKeyManager {

    private final static Logger log = LoggerFactory.getLogger(LocalApiKeyManager.class);

    public final static String switchingToOfflineModeMessage = "Gateway is switching to offline mode.";
    public final static String switchingToOnlineModeMessage = "Gateway is switching to online mode.";

    private List<ApiKey> localApiKeyList;
    private LocalDateTime deadlineWhenOfflineModeShallStopWorking;
    private boolean isLocalApiKeyListInitialized = false;

    private final ApiKeyFetcher apiKeyFetcher;
    private final long numberOfMinutesTheGatewayShallWorkInOfflineMode;

    private GatewayMode lastModeDisplayedInLog = GatewayMode.OFFLINE;

    public LocalApiKeyManager(
            ApiKeyFetcher apiKeyFetcher,
            @Value("${number-of-minutes-the-gateway-shall-work-in-offline-mode}") long minutesInOfflineMode) {
        this.apiKeyFetcher = apiKeyFetcher;
        this.numberOfMinutesTheGatewayShallWorkInOfflineMode = minutesInOfflineMode;
    }

    public ApiKey getApiKeyEntityFromLocalCache(String apiKeyValue) {
        if (!isLocalApiKeyListInitialized) {
            throw new LocalApiKeyListWasNotInitializedException("The gateway is currently not able to validate " +
                    "API keys in offline mode, as the local API cache was not yet initialized. This probably " +
                    "means that a network connection to CoatRack admin could not yet be established.");
        } else if (isOfflineWorkingTimeExceeded()) {
            throw new OfflineWorkingTimeExceedingException("The predefined time for working in offline mode is exceeded. The " +
                    "gateway will reject every request until a connection to CoatRack admin could be re-established.");
        } else {
            return extractApiKeyFromLocalApiKeyList(apiKeyValue);
        }
    }

    private boolean isOfflineWorkingTimeExceeded() {
        return LocalDateTime.now().isAfter(deadlineWhenOfflineModeShallStopWorking);
    }

    private ApiKey extractApiKeyFromLocalApiKeyList(String apiKeyValue) {
        log.debug("Trying to extract the API key with the value {} from the local list.", apiKeyValue);

        //This is only a fallback solution if connection to admin does not work. Therefore offline mode is triggered.
        updateGatewayMode(GatewayMode.OFFLINE);

        Optional<ApiKey> optionalApiKey = localApiKeyList.stream().filter(
                apiKeyFromLocalList -> apiKeyFromLocalList.getKeyValue().equals(apiKeyValue)
        ).findFirst();

        if (optionalApiKey.isPresent()) {
            return optionalApiKey.get();
        } else {
            throw new ApiKeyNotFoundInLocalApiKeyListException("The API key with the value " + apiKeyValue +
                    " could not be found in the local API key list.");
        }
    }

    @Async
    @PostConstruct
    @Scheduled(fixedRateString = "${local-api-key-list-update-interval-in-millis}")
    public void refreshLocalApiKeyCacheWithApiKeysFromAdmin() {
        log.debug("Trying to update the local API key list by contacting CoatRack admin.");

        try {
            List<ApiKey> fetchedApiKeyList = apiKeyFetcher.requestLatestApiKeyListFromAdmin();
            updateApiKeyList(fetchedApiKeyList);
            updateGatewayMode(GatewayMode.ONLINE);
        } catch (Exception e) {
            log.error("API key list fetching process failed." + e);
            updateGatewayMode(GatewayMode.OFFLINE);
        }
    }

    private void updateApiKeyList(List<ApiKey> fetchedApiKeyList) {
        Assert.notNull(fetchedApiKeyList, "Fetched API key list was null.");
        localApiKeyList = fetchedApiKeyList;
        deadlineWhenOfflineModeShallStopWorking = LocalDateTime.now().plusMinutes(numberOfMinutesTheGatewayShallWorkInOfflineMode);

        if(!isLocalApiKeyListInitialized)
            isLocalApiKeyListInitialized = true;
    }

    private void updateGatewayMode(GatewayMode currentGatewayMode) {
        if (lastModeDisplayedInLog != currentGatewayMode) {
            logGatewayMode(currentGatewayMode);
            lastModeDisplayedInLog = currentGatewayMode;
        }
    }

    private void logGatewayMode(GatewayMode currentGatewayMode) {
        log.info(currentGatewayMode == GatewayMode.ONLINE ? switchingToOnlineModeMessage : switchingToOfflineModeMessage);
    }

    /*
        If the gateway successfully receives the latest list of API keys from CoatRack admin, it goes to online mode.
        If a connection attempt to CoatRack admin server failed, it goes to the time-limited functioning offline mode.
     */

    private enum GatewayMode {
        ONLINE, OFFLINE;
    }
}
