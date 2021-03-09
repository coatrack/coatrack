package eu.coatrack.proxy;

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
import eu.coatrack.proxy.security.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@EnableAsync
@EnableScheduling
@RestController
public class ApiKeyListRequester {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyListRequester.class);

    private List<ApiKey> apiKeyList = new ArrayList<>();

    @Autowired
    private RestTemplate restTemplate;

    @Value("${proxy-id}")
    private String gatewayId = "";

    @Autowired
    private SecurityUtil securityUtil;

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
    @Scheduled(fixedRate = 5000) // TODO: After finishing #73, this value should be set to 30,000 (5 min).
    public void requestApiKeyList() {
        try {
            ResponseEntity<ApiKey[]> responseEntity = restTemplate.getForEntity(uri, ApiKey[].class, gatewayId);
            apiKeyList = Arrays.asList(responseEntity.getBody());
            checkAndLogHttpStatus(responseEntity.getStatusCode());
        } catch (RestClientException e){
            log.info("Connection to admin server failed. Probably the server is temporarily down.", e);
        }
    }

    private void checkAndLogHttpStatus(HttpStatus statusCode) {
        if (statusCode == HttpStatus.OK)
            log.debug("ApiKeyList was successfully requested and delivered.");
        else
            log.warn("Gateway is not recognized by admin, maybe it is deprecated. Please download and run a " +
                    "new one from cotrack.eu");
    }

    public List<ApiKey> getApiKeyList() {
        return apiKeyList;
    }
}
