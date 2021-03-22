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
 * Offers communication services to the Coatrack admin server to receive data
 * required by the gateway for key verification.
 *
 * @author Christoph Baier
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

    private String
            apiKeyListRequestUrl,
            apiKeyUrlWithoutApiKeyValue,
            serviceApiUrlWithoutApiKeyValue;

    @PostConstruct
    private void initUrls() {
        apiKeyListRequestUrl = securityUtil.attachGatewayApiKeyToUrl(adminBaseUrl + adminResourceToSearchForApiKeyList);
        apiKeyUrlWithoutApiKeyValue = securityUtil.attachGatewayApiKeyToUrl(adminBaseUrl + adminResourceToSearchForApiKeys);
        serviceApiUrlWithoutApiKeyValue = securityUtil.attachGatewayApiKeyToUrl(adminBaseUrl + adminResourceToGetServiceByApiKeyValue);
    }

    public ApiKey[] requestLatestApiKeyListFromAdmin() throws Exception {
        log.debug("Requesting latest API key list from CoatRack admin.");
        ResponseEntity<ApiKey[]> responseEntity = restTemplate.getForEntity(apiKeyListRequestUrl, ApiKey[].class, gatewayId);

        if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
            log.info("Successfully requested latest API key list from CoatRack admin.");
            return responseEntity.getBody();
        } else {
            log.warn("Request of latest API key list from CoatRack admin failed. Received http status {} from " +
                    "CoatRack admin.", responseEntity.getStatusCode());
            return null;
        }
    }

    public ApiKey requestApiKeyFromAdmin(String apiKeyValue) throws Exception {
        log.debug("Requesting API key with the value {} from CoatRack admin.", apiKeyValue);
        ResponseEntity<ApiKey> responseEntity = restTemplate.getForEntity(apiKeyUrlWithoutApiKeyValue
                + apiKeyValue, ApiKey.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
            log.info("The API key with the value {} was found by CoatRack admin.", apiKeyValue);
            return responseEntity.getBody();
        } else {
            log.info("The API key with the value {} was not found by CoatRack admin.", apiKeyValue);
            return null;
        }
    }

    public ServiceApi requestServiceApiFromAdmin(String apiKeyValue) throws Exception {
        log.debug("Requesting service from CoatRack admin using the API with the value {}.", apiKeyValue);
        ResponseEntity<ServiceApi> responseEntity = restTemplate.getForEntity(serviceApiUrlWithoutApiKeyValue
                + apiKeyValue, ServiceApi.class);
        return responseEntity.getBody();
    }
}