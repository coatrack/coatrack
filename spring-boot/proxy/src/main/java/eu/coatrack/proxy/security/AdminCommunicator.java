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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

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

    String apiKeyListRequestUrl;

    @PostConstruct
    private void initUrls(){
        apiKeyListRequestUrl = securityUtil.attachGatewayApiKeyToUrl(adminBaseUrl + adminResourceToSearchForApiKeyList);
    }

    //TODO: Provide apiKeyList, apiKey and serviceApi from admin

    public ResponseEntity<ApiKey[]> requestLatestApiKeyListFromAdmin(){
        ResponseEntity<ApiKey[]> responseEntity;
        try {
            responseEntity = restTemplate.getForEntity(apiKeyListRequestUrl, ApiKey[].class, gatewayId);
        } catch (Exception e) {
            log.info("Connection to admin server failed. Probably the server is temporarily down.");
            return null;
        }
        return responseEntity;
    }

    public ApiKey requestApiKeyFromAdmin(){
        return null;
    }

    public ServiceApi requestServiceApiFromAdmin(){
        return null;
    }

}
