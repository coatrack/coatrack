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
 * Locally stores, periodically refreshes and provides API keys belonging to the gateway.
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

    private GatewayMode modeOfPreviousUpdateInterval = GatewayMode.OFFLINE;
    private GatewayMode lastModeDisplayedInLog = GatewayMode.OFFLINE;

    public LocalApiKeyManager(
            ApiKeyFetcher apiKeyFetcher,
            @Value("${number-of-minutes-the-gateway-shall-work-without-connection-to-admin}") long minutes) {
        this.apiKeyFetcher = apiKeyFetcher;
        this.numberOfMinutesTheGatewayShallWorkWithoutConnectionToAdmin = minutes;
    }

    public ApiKey getApiKeyEntityByApiKeyValue(String apiKeyValue) {
        log.debug("Trying to get the API key with the value {} from the local list.", apiKeyValue);

        if (apiKeyValue == null)
            return null;

        Optional<ApiKey> optionalApiKey = localApiKeyList.stream().filter(
                apiKeyFromLocalList -> apiKeyFromLocalList.getKeyValue().equals(apiKeyValue)).findFirst();
        return optionalApiKey.orElse(null);
    }

    public boolean wasLatestUpdateOfLocalApiKeyListWithinDeadline() {
        return LocalDateTime.now().isBefore(deadline);
    }

    @Async
    @PostConstruct
    @Scheduled(fixedRateString = "${local-api-key-list-update-interval-in-millis}")
    public void updateLocalApiKeyList() {
        log.debug("Trying to update the local API key list by contacting CoatRack admin.");

        List<ApiKey> fetchedApiKeyList;
        try {
            fetchedApiKeyList = apiKeyFetcher.requestLatestApiKeyListFromAdmin();
        } catch (ApiKeyFetchingFailedException e) {
            displayGatewayModeSwitchesConsideringLatestUpdate(GatewayMode.OFFLINE);
            log.debug("Following error occurred: " + e);
            return;
        }
        updateLocalApiKeyListAndGatewayMode(fetchedApiKeyList);
    }

    private void displayGatewayModeSwitchesConsideringLatestUpdate(GatewayMode modeOfCurrentUpdateInterval) {
        if (isSwitchingToOnlineMode(modeOfCurrentUpdateInterval)){
            log.info("Connection to the CoatRack admin server could be established. Switching to online mode.");
            lastModeDisplayedInLog = GatewayMode.ONLINE;
        }
        else if (isSwitchingToOfflineMode(modeOfCurrentUpdateInterval)){
            log.info("There is a problem with CoatRack admin. Switching to offline mode.");
            lastModeDisplayedInLog = GatewayMode.OFFLINE;
        }
        modeOfPreviousUpdateInterval = modeOfCurrentUpdateInterval;
    }

    private boolean isSwitchingToOnlineMode(GatewayMode modeOfCurrentUpdateInterval) {
        return lastModeDisplayedInLog == GatewayMode.OFFLINE
                && modeOfCurrentUpdateInterval == GatewayMode.ONLINE;
    }

    private boolean isSwitchingToOfflineMode(GatewayMode modeOfCurrentUpdateInterval) {
        return modeOfPreviousUpdateInterval == GatewayMode.OFFLINE
                && lastModeDisplayedInLog == GatewayMode.ONLINE
                && modeOfCurrentUpdateInterval == GatewayMode.OFFLINE;
    }

    private void updateLocalApiKeyListAndGatewayMode(List<ApiKey> fetchedApiKeyList) {
        if(fetchedApiKeyList != null){
            localApiKeyList = fetchedApiKeyList;
            deadline = LocalDateTime.now().plusMinutes(numberOfMinutesTheGatewayShallWorkWithoutConnectionToAdmin);
            displayGatewayModeSwitchesConsideringLatestUpdate(GatewayMode.ONLINE);
        } else {
            log.debug("CoatRack admin delivered an invalid API key list.");
            displayGatewayModeSwitchesConsideringLatestUpdate(GatewayMode.OFFLINE);
        }
    }

    private enum GatewayMode{
        ONLINE, OFFLINE
    }
}
