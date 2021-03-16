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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

/**
 *  Offers communication services to the Coatrack admin server to receive data
 *  needed by the gateway.
 *
 *  @author Christoph Baier
 */

@Service
public class AdminCommunicator {

    private static final Logger log = LoggerFactory.getLogger(eu.coatrack.proxy.security.AdminCommunicator.class);

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

    public ApiKey[] requestLatestApiKeyListFromAdmin() throws Exception {
        log.debug("Trying to receive an update of local API key list by requesting admin.");
        ResponseEntity<ApiKey[]> responseEntity = restTemplate.getForEntity(apiKeyListRequestUrl, ApiKey[].class, gatewayId);

        if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null){
            log.debug("Successfully requested API key list from admin.");
            return responseEntity.getBody();
        }
        else {
            log.warn("Request of latest API key list from admin failed. Received http status " +
                    responseEntity.getStatusCode() + " from admin.");
            return null;
        }
    }

    public ApiKey requestApiKeyFromAdmin(String apiKeyValue) throws Exception {
        log.debug("Requesting API key with the value " + apiKeyValue + " from admin and checking its validity.");
        ResponseEntity<ApiKey> responseEntity = restTemplate.getForEntity(apiKeyUrlWithoutApiKeyValue + apiKeyValue, ApiKey.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null){
            log.debug("The API key with the value " + apiKeyValue + " was found by admin.");
            return responseEntity.getBody();
        }
        else {
            log.debug("The API key with the value " + apiKeyValue + " was not found by admin.");
            return null;
        }
    }

    public ServiceApi requestServiceApiFromAdmin(String apiKeyValue) throws Exception {
        log.debug("Trying to get service API entity by using API key value {}.", apiKeyValue);
        ResponseEntity<ServiceApi> responseEntity = restTemplate.getForEntity(serviceApiUrlWithoutApiKeyValue + apiKeyValue, ServiceApi.class);
        return responseEntity.getBody();
    }
}
