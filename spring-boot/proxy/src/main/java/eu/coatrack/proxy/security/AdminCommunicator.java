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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class AdminCommunicator {

    private static final Logger log = LoggerFactory.getLogger(eu.coatrack.proxy.security.AdminCommunicator.class);

    @Autowired
    private LocalApiKeyAndServiceApiManager localApiKeyAndServiceApiManager;

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

    @Value("${ygg.admin.resources.search-api-keys-by-token-value}")
    private String adminResourceToSearchForApiKeys;

    @Value("${ygg.admin.resources.search-service-by-api-key-value}")
    private String adminResourceToGetServiceByApiKeyValue;

    private String apiKeyListRequestUrl;
    private String apiKeyUrlWithoutApiKeyValue;
    private String serviceApiUrlWithoutApiKeyValue;

    @PostConstruct
    private void initUrls(){
        apiKeyListRequestUrl = securityUtil.attachGatewayApiKeyToUrl(adminBaseUrl + adminResourceToSearchForApiKeyList);
        apiKeyUrlWithoutApiKeyValue = securityUtil.attachGatewayApiKeyToUrl(adminBaseUrl + adminResourceToSearchForApiKeys);
        serviceApiUrlWithoutApiKeyValue = securityUtil.attachGatewayApiKeyToUrl(adminBaseUrl + adminResourceToGetServiceByApiKeyValue);
    }

    @Async
    @PostConstruct
    @Scheduled(fixedRate = 5000)
    public void requestLatestApiKeyListFromAdmin(){
        log.debug("Trying to receive an update of local API key list by requesting admin.");
        ResponseEntity<ApiKey[]> responseEntity = null;
        try {
            responseEntity = restTemplate.getForEntity(apiKeyListRequestUrl, ApiKey[].class, gatewayId);
        } catch (Exception e) {
            log.info("Connection to admin server failed. Probably the server is temporarily down.");
            return;
        }
        if (responseEntity.getStatusCode() == HttpStatus.OK){
            log.debug("Successfully requested API key list from admin.");
            updateLocalApiKeyListManagerUsing(responseEntity);
        }
        else {
            log.warn("Received http status " + responseEntity.getStatusCode() + " from admin.");
            return;
        }
    }

    private void updateLocalApiKeyListManagerUsing(ResponseEntity<ApiKey[]> responseEntity) {
        ApiKey[] apiKeys = responseEntity.getBody();
        List<ApiKey> apiKeyList;
        if (apiKeys != null){
            apiKeyList = Arrays.asList(responseEntity.getBody());
            localApiKeyAndServiceApiManager.updateLocalApiKeyList(apiKeyList, LocalDateTime.now());
        }
    }

    public boolean isApiKeyVerifiedByAdmin(String apiKeyValue) throws Exception {
        log.debug("Requesting API key with the value " + apiKeyValue + " from admin and checking its validity.");
        ResponseEntity<ApiKey> responseEntity = restTemplate.getForEntity(apiKeyUrlWithoutApiKeyValue + apiKeyValue, ApiKey.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK){
            log.debug("The API key with the value " + apiKeyValue + " is found to be valid by admin.");
            return true;
        }
        else {
            log.debug("The API key with the value " + apiKeyValue + " is found to be invalid by admin.");
            return false;
        }
    }

    public ServiceApi requestServiceApiFromAdmin(String apiKeyValue) throws Exception {
        log.debug("Trying to get service API entity by using API key value {}.", apiKeyValue);
        ResponseEntity<ServiceApi> responseEntity = restTemplate.getForEntity(serviceApiUrlWithoutApiKeyValue + apiKeyValue, ServiceApi.class);
        return responseEntity.getBody();
    }
}
