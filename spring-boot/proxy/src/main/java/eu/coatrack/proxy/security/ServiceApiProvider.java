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

import eu.coatrack.api.ServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 *  Performs a service API request to admin and handles possible side effects.
 *  If the admin server is not reachable the local API key list is used.
 *
 *  @author Christoph Baier
 */

@Service
public class ServiceApiProvider {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthTokenVerifier.class);

    private List<ServiceApi> serviceApiList = new ArrayList<>();

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    SecurityUtil securityUtil;

    @Value("${ygg.admin.api-base-url}")
    private String adminBaseUrl;

    @Value("${ygg.admin.resources.search-service-by-api-key-value}")
    private String adminResourceToGetServiceByApiKeyValue;

    public ServiceApi getServiceApiByApiKey(String apiKeyValue) {
        log.debug("Trying to get service API entity by API key value {}", apiKeyValue);
        String urlToGetServiceApi = createUrlToGetServiceApi(apiKeyValue);
        ResponseEntity<ServiceApi> responseEntity;

        try {
            responseEntity = restTemplate.getForEntity(urlToGetServiceApi, ServiceApi.class);
        } catch (Exception e) {
            log.info("The Service API request to the admin server failed. Probably the admin server is " +
                    "temporarily down.");
            log.debug("Checking if service API belonging to the API key with the value " + apiKeyValue +
                    " is within the local service API list.");
            return getMatchingServiceApiFromLocalServiceApiList(apiKeyValue);
        }
        return extractServiceApi(responseEntity);
    }

    private String createUrlToGetServiceApi(String apiKeyValue) {
        return securityUtil.attachGatewayApiKeyToUrl(
                adminBaseUrl + adminResourceToGetServiceByApiKeyValue + apiKeyValue);
    }

    private ServiceApi getMatchingServiceApiFromLocalServiceApiList(String apiKeyValue) {
        Optional<ServiceApi> serviceApiToBeFound = serviceApiList.stream().filter(x -> x.getApiKeys()
                .stream().anyMatch(y -> y.getKeyValue().equals(apiKeyValue))).findFirst();
        if (serviceApiToBeFound.isPresent())
            return serviceApiToBeFound.get();
        else {
            log.warn("The API key with the value " + apiKeyValue + " does not belong to any service " +
                    "despite it is considered valid by the gateway. Probably the cause lies within a " +
                    "bad GatewayUpdate received from the admin server which did not include this API " +
                    "keys service.");
            return null;
        }
    }

    private ServiceApi extractServiceApi(ResponseEntity<ServiceApi> responseEntity) {
        if (responseEntity != null) {
            ServiceApi serviceApi = responseEntity.getBody();
            log.debug("The service API with the name " + serviceApi.getName() + " was found by admin.");
            return serviceApi;
        } else {
            log.warn("The communication with the admin server was successful but the ResponseEntity " +
                    "is still null. This should not happen and therefore be debugged.");
            return null;
        }
    }

    public void setServiceApiList(List<ServiceApi> serviceApiList) {
        this.serviceApiList = serviceApiList;
    }
}
