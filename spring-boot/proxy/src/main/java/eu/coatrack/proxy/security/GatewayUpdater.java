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
import eu.coatrack.api.GatewayUpdate;
import eu.coatrack.api.ServiceApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *  This bean performs the updates provided by GatewayUpdateRequester.
 *
 *  @author Christoph Baier
 */

@Service
public class GatewayUpdater {

    @Autowired
    private ServiceApiProvider serviceApiProvider;

    @Autowired
    private ApiKeyValidityVerifier apiKeyValidityVerifier;

    public void extractDataAndPerformUpdates(GatewayUpdate gatewayUpdate) {
        if(gatewayUpdate.apiKeys != null && gatewayUpdate.adminsLocalTime != null){
            Timestamp adminsLocalTime = gatewayUpdate.adminsLocalTime;
            List<ApiKey> apiKeyList = Arrays.asList(gatewayUpdate.apiKeys);
            List<String> apiKeyValueList = apiKeyList.stream().map(ApiKey::getKeyValue).collect(Collectors.toList());
            List<ServiceApi> serviceApiList = Arrays.asList(gatewayUpdate.serviceApis);

            updateBeansUsingApiKeyValueListAndLastUpdateTimestamp(apiKeyValueList);
            updateBeansUsingApiAdminsLocalTime(adminsLocalTime);
            updateBeansUsingServiceApiList(serviceApiList);
        }
    }

    private void updateBeansUsingApiKeyValueListAndLastUpdateTimestamp(List<String> apiKeyValueList) {
        apiKeyValidityVerifier.setLastApiKeyValueListUpdate(new Timestamp(System.currentTimeMillis()));

        if(apiKeyValueList != null)
            apiKeyValidityVerifier.setApiKeyList(apiKeyValueList);
        else
            apiKeyValidityVerifier.setApiKeyList(new ArrayList<>());
    }

    private void updateBeansUsingApiAdminsLocalTime(Timestamp adminsLocalTime) {
        if(adminsLocalTime != null)
            apiKeyValidityVerifier.setAdminsLocalTime(adminsLocalTime);
        else
            apiKeyValidityVerifier.setAdminsLocalTime(new Timestamp(0));
    }

    private void updateBeansUsingServiceApiList(List<ServiceApi> serviceApiList) {
        serviceApiProvider.setServiceApiList(serviceApiList);
    }
}
