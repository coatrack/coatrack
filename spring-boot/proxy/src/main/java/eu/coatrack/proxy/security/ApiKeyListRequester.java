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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This bean regularly requests admin to provide the latest API key list affecting this
 * gateway and updates the ApiKeyValidityChecker if new update content was received.
 *
 * @author ChristophBaierATB
 */

@EnableAsync
@EnableScheduling
@RestController
public class ApiKeyListRequester {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyListRequester.class);

    private final int fiveMinutesInMillis = 1000 * 60 * 5;

    @Autowired
    private ApiKeyValidityChecker apiKeyValidityChecker;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SecurityUtil securityUtil;

    @Value("${proxy-id}")
    private String gatewayId = "";

    @Value("${ygg.admin.api-base-url}")
    private String adminBaseUrl;

    @Value("${ygg.admin.resources.search-api-key-list}")
    private String adminResourceToSearchForApiKeyList;

    private String uri = "";

    @PostConstruct
    private void initUriAndRequestApiKeyList(){
        String raw_uri = adminBaseUrl + adminResourceToSearchForApiKeyList;
        uri = securityUtil.attachGatewayApiKeyToUrl(raw_uri);
        requestApiKeyList();
    }

    @Async
    @Scheduled(fixedRate = fiveMinutesInMillis)
    public void requestApiKeyList() {
        ResponseEntity<GatewayUpdate> responseEntity;
        try {
            responseEntity = restTemplate.getForEntity(uri, GatewayUpdate.class, gatewayId);
        } catch (RestClientException e){
            log.info("Connection to admin server failed. Probably the server is temporarily down.", e);
            return;
        }
        checkAndLogHttpStatus(responseEntity.getStatusCode());

        GatewayUpdate gatewayUpdate = responseEntity.getBody();
        ifGatewayUpdateIsPresentExtractDataAndUpdateApiKeyValidityChecker(gatewayUpdate);
    }

    private void checkAndLogHttpStatus(HttpStatus statusCode) {
        if (statusCode == HttpStatus.OK)
            log.debug("GatewayUpdate was successfully requested and delivered.");
        else
            log.warn("Gateway is not recognized by admin, maybe it is deprecated. Please download and run a " +
                    "new one from cotrack.eu");
    }

    private void ifGatewayUpdateIsPresentExtractDataAndUpdateApiKeyValidityChecker(GatewayUpdate gatewayUpdate) {
        if(gatewayUpdate != null){
            ifGatewayUpdateDataIsPresentExtractThemAndUpdateApiKeyValidityChecker(gatewayUpdate);
        }
    }

    private void ifGatewayUpdateDataIsPresentExtractThemAndUpdateApiKeyValidityChecker(GatewayUpdate gatewayUpdate) {
        if(gatewayUpdate.apiKeys != null && gatewayUpdate.adminsLocalTime != null){
            Timestamp localAdminTime = gatewayUpdate.adminsLocalTime;
            List<ApiKey> apiKeyList = Arrays.asList(gatewayUpdate.apiKeys);
            List<String> apiKeyValueList = apiKeyList.stream().map(ApiKey::getKeyValue).collect(Collectors.toList());
            updateApiKeyValidityChecker(apiKeyValueList, localAdminTime);
            System.out.println("This is the received api key value list: " + apiKeyValueList); //TODO to be removed after testing this feature
        }
    }

    private void updateApiKeyValidityChecker(List<String> apiKeyValueList, Timestamp adminsLocalTime) {
        apiKeyValidityChecker.setApiKeyList(apiKeyValueList);
        apiKeyValidityChecker.setLastApiKeyValueListUpdate(new Timestamp(System.currentTimeMillis()));
        apiKeyValidityChecker.setAdminsLocalTime(adminsLocalTime);
    }
}
