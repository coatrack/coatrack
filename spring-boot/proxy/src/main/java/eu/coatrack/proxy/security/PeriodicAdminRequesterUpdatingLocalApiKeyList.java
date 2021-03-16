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
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@EnableAsync
@EnableScheduling
@RestController
public class PeriodicAdminRequesterUpdatingLocalApiKeyList {

    private static final Logger log = LoggerFactory.getLogger(eu.coatrack.proxy.security.PeriodicAdminRequesterUpdatingLocalApiKeyList.class);

    @Autowired
    private AdminCommunicator adminCommunicator;

    @Autowired
    private LocalApiKeyAndServiceApiManager localApiKeyAndServiceApiManager;

    @Async
    @PostConstruct
    @Scheduled(fixedRate = 5000)
    public void requestApiKeyListFromAdmin() {
        ResponseEntity<ApiKey[]> responseEntity = adminCommunicator.requestLatestApiKeyListFromAdmin();
        if (responseEntity == null)
            return;
        checkAndLogHttpStatus(responseEntity.getStatusCode());

        List<ApiKey> apiKeyList = Arrays.asList(responseEntity.getBody());
        if (apiKeyList != null)
            localApiKeyAndServiceApiManager.updateLocalApiKeyList(apiKeyList, LocalDateTime.now());
    }

    private void checkAndLogHttpStatus(HttpStatus statusCode) {
        if (statusCode == HttpStatus.OK)
            log.debug("GatewayUpdate was successfully requested and delivered.");
        else
            log.warn("Gateway is probably not recognized by admin, maybe it is deprecated. " +
                    "Please download and run a new one from coatrack.eu");
    }
}
