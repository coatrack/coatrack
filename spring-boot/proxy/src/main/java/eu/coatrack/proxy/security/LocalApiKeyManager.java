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
import eu.coatrack.proxy.security.exceptions.LocalApiKeyListWasNotInitializedException;
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
import java.util.ArrayList;
import java.util.List;

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

    private static final Logger log = LoggerFactory.getLogger(LocalApiKeyManager.class);

    public final static String switchingToOfflineModeMessage = "Gateway is switching to offline mode.";
    public final static String switchingToOnlineModeMessage = "Gateway is switching to online mode.";

    private List<ApiKey> localApiKeyList = new ArrayList<>();
    private LocalDateTime deadlineWhenOfflineModeShallStopWorking = LocalDateTime.MIN;

    private final ApiKeyFetcher apiKeyFetcher;
    private final long numberOfMinutesTheGatewayShallWorkInOfflineMode;

    private GatewayMode lastModeDisplayedInLog = GatewayMode.OFFLINE;

    public LocalApiKeyManager(
            ApiKeyFetcher apiKeyFetcher,
            @Value("${number-of-minutes-the-gateway-shall-work-in-offline-mode}") long minutesInOfflineMode) {
        this.apiKeyFetcher = apiKeyFetcher;
        this.numberOfMinutesTheGatewayShallWorkInOfflineMode = minutesInOfflineMode;
    }

    public ApiKey getApiKeyEntityByApiKeyValue(String apiKeyValue) {
        log.debug("Trying to get the API key with the value {} from the local list.", apiKeyValue);
        updateGatewayMode(GatewayMode.OFFLINE);

        return localApiKeyList.stream().filter(
                apiKeyFromLocalList -> apiKeyFromLocalList.getKeyValue().equals(apiKeyValue)
        ).findFirst().orElse(null);
    }

    public boolean isOfflineWorkingTimeExceeded() {
        if (deadlineWhenOfflineModeShallStopWorking == LocalDateTime.MIN)
            throw new LocalApiKeyListWasNotInitializedException("The offline mode does not work without a local " +
                    "copy of the API key list.");
        return LocalDateTime.now().isAfter(deadlineWhenOfflineModeShallStopWorking);
    }

    @Async
    @PostConstruct
    @Scheduled(fixedRateString = "${local-api-key-list-update-interval-in-millis}")
    public void updateLocalApiKeyList() {
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
    }

    private void updateGatewayMode(GatewayMode currentGatewayMode) {
        if (lastModeDisplayedInLog != currentGatewayMode) {
            logGatewayMode(currentGatewayMode);
            lastModeDisplayedInLog = currentGatewayMode;
        }
    }

    private void logGatewayMode(GatewayMode currentGatewayMode) {
        if (currentGatewayMode == GatewayMode.ONLINE)
            log.info(switchingToOnlineModeMessage);
        else
            log.info(switchingToOfflineModeMessage);
    }

    /*
        If the gateway successfully receives the latest list of API keys from CoatRack admin, it goes to online mode.
        If that connection attempt failed, it goes to the time-limited functioning offline mode.
     */

    private enum GatewayMode {
        ONLINE, OFFLINE;
    }
}
