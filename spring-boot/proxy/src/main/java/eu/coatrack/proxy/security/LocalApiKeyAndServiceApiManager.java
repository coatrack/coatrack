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
import eu.coatrack.api.ServiceApi;
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
 * Locally stores and periodically refreshes API keys and their associated service APIs
 * belonging to the gateway. Provides API key verification services for incoming consumer
 * calls which work even if the CoatRack admin server is down.
 *
 * @author Christoph Baier
 */

@EnableAsync
@Service
public class LocalApiKeyAndServiceApiManager {

    private static final Logger log = LoggerFactory.getLogger(LocalApiKeyAndServiceApiManager.class);
    private final int fiveMinutesInMillis = 1000 * 60 * 5;

    protected List<ApiKey> localApiKeyList = new ArrayList<>();
    protected LocalDateTime latestLocalApiKeyListUpdate = LocalDateTime.now();

    private long numberOfMinutesTheGatewayShallWorkWithoutConnectionToAdmin;
    private AdminCommunicator adminCommunicator;

    public LocalApiKeyAndServiceApiManager(
            AdminCommunicator adminCommunicator,
            @Value("${minutes-the-gateway-works-without-connection-to-admin}") long minutes) {
        this.adminCommunicator = adminCommunicator;
        this.numberOfMinutesTheGatewayShallWorkWithoutConnectionToAdmin = minutes;
    }

    public boolean isApiKeyAuthorizedToAccessItsServiceApiConsideringTheLocalApiKeyList(String apiKeyValue) {
        log.debug("Begin checking if the API key with the value {} is valid using the local API key list.",
                apiKeyValue);
        if (apiKeyValue == null) {
            log.info("The passed API key value is null and can therefore not be checked for validity. " +
                    "It is therefore rejected.");
            return false;
        }

        ApiKey apiKey = findApiKeyFromLocalApiKeyList(apiKeyValue);
        if (apiKey == null) {
            return false;
        } else {
            log.debug("The API key with the value {} is within the local API key list.", apiKeyValue);
            return isApiKeyAuthorizedToAccessItsServiceApi(apiKey);
        }
    }

    private ApiKey findApiKeyFromLocalApiKeyList(String apiKeyValue) {
        log.debug("Trying to find the service API associated to the API key with the value {} from the local list.",
                apiKeyValue);

        Optional<ApiKey> optionalApiKey = localApiKeyList.stream().filter(apiKeyFromLocalList -> apiKeyFromLocalList.getKeyValue()
                    .equals(apiKeyValue)).findFirst();

        if (!optionalApiKey.isPresent()) {
            log.info("The API key with the value {} can not be found within the local API key list " +
                    "and is therefore rejected.", apiKeyValue);
            return null;
        } else
            return optionalApiKey.get();
    }

    private boolean isApiKeyAuthorizedToAccessItsServiceApi(ApiKey apiKey) {
        boolean isApiKeyAuthorizedToAccessItsServiceApi = isApiKeyValid(apiKey) &&
                        wasLatestUpdateOfLocalApiKeyListWithinTheGivenDeadline();
        if (isApiKeyAuthorizedToAccessItsServiceApi)
            log.info("The API key with the value {} is valid considering the local API key list and is " +
                    "therefore accepted.", apiKey.getKeyValue());
        return isApiKeyAuthorizedToAccessItsServiceApi;
    }

    private boolean isApiKeyNotDeleted(ApiKey apiKey) {
        boolean isNotDeleted = apiKey.getDeletedWhen() == null;
        if (isNotDeleted) {
            return true;
        } else {
            log.info("The API key with the value {} is deleted and therefore rejected.", apiKey.getKeyValue());
            return false;
        }
    }

    private boolean isApiKeyNotExpired(ApiKey apiKey) {
        boolean isNotExpired = apiKey.getValidUntil().getTime() > System.currentTimeMillis();
        if (isNotExpired) {
            return true;
        } else {
            log.info("The API key with the value {} is expired and therefore rejected.", apiKey.getKeyValue());
            return false;
        }
    }

    private boolean wasLatestUpdateOfLocalApiKeyListWithinTheGivenDeadline() {
        LocalDateTime deadline = latestLocalApiKeyListUpdate.plusMinutes(
                numberOfMinutesTheGatewayShallWorkWithoutConnectionToAdmin);
        boolean wasLatestUpdateWithinDeadline = LocalDateTime.now().isBefore(deadline);
        if (!wasLatestUpdateWithinDeadline)
            log.info("The CoatRack admin server was not reachable for longer than {} minutes. Since this " +
                    "predefined threshold was exceeded, this and all subsequent service API requests are " +
                    "rejected until a connection to CoatRack admin could be re-established.",
                    numberOfMinutesTheGatewayShallWorkWithoutConnectionToAdmin);
        return wasLatestUpdateWithinDeadline;
    }

    public boolean isApiKeyValid(ApiKey apiKey) {
        return isApiKeyNotDeleted(apiKey) && isApiKeyNotExpired(apiKey);
    }

    public ServiceApi getServiceByApiKeyValue(String apiKeyValue) {
        ApiKey apiKey = findApiKeyFromLocalApiKeyList(apiKeyValue);
        if (apiKey != null) {
            return apiKey.getServiceApi();
        } else
            return null;
    }

    @Async
    @PostConstruct
    @Scheduled(fixedRate = fiveMinutesInMillis)
    public void updateLocalApiKeyList() {
        List<ApiKey> apiKeys;
        try {
            apiKeys = adminCommunicator.requestLatestApiKeyListFromAdmin();
        } catch (Exception e) {
            log.info("Trying to update the local API key list, the connection to CoatRack admin failed. Probably " +
                    "the server is temporarily down.");
            log.debug("Following error occurred: " + e);
            return;
        }
        localApiKeyList = apiKeys;
        latestLocalApiKeyListUpdate = LocalDateTime.now();
    }
}
