package eu.coatrack.admin.controllers;

/*-
 * #%L
 * coatrack-admin
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

import eu.coatrack.admin.model.repository.ApiKeyRepository;
import eu.coatrack.admin.model.repository.ProxyRepository;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.api.ApiKey;
import eu.coatrack.api.GatewayUpdate;
import eu.coatrack.api.Proxy;
import eu.coatrack.api.ServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.sql.Time;
import java.sql.Timestamp;

/**
 * Controller that handles HTTP calls by CoatRack gateways
 */
@Controller
public class GatewayApiController {

    private static final Logger log = LoggerFactory.getLogger(GatewayApiController.class);

    @Autowired
    ApiKeyRepository apiKeyRepository;

    @Autowired
    ServiceApiRepository serviceApiRepository;

    @Autowired
    ProxyRepository proxyRepository;

    @RequestMapping(value = "/api/api-keys/search/findByKeyValue", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ApiKey findApiKeyEntityByApiKeyValue(@RequestParam("keyValue") String apiKeyValue) {
        return apiKeyRepository.findByKeyValue(apiKeyValue);
    }

    @RequestMapping(value = "/api/services/search/findByApiKeyValue", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ServiceApi findServiceByApiKeyValue(@RequestParam("apiKeyValue") String apiKeyValue) {
        return serviceApiRepository.findByApiKeyValue(apiKeyValue);
    }

    @GetMapping( "/api/gateways/{gatewayId}/receiveApiKeyList")
    public ResponseEntity<GatewayUpdate> findServiceByGatewayId(@PathVariable("gatewayId") String gatewayId) {
        Proxy proxy = proxyRepository.findById(gatewayId);
        GatewayUpdate gatewayUpdate;
        Timestamp now = new Timestamp(System.currentTimeMillis());

        if(proxy != null){
            ServiceApi[] serviceApis = proxy.getServiceApis().stream().toArray(ServiceApi[]::new);
            ApiKey[] apiKeys = proxy.getServiceApis().stream().flatMap(x -> x.getApiKeys()
                .stream()).toArray(ApiKey[]::new);
            log.info("Successfully created API key and service API list for the gateway with the ID: " + gatewayId);
            gatewayUpdate = new GatewayUpdate(apiKeys, serviceApis, now);
            return new ResponseEntity<>(gatewayUpdate, HttpStatus.OK);
        }
        else {
            log.info("GatewayUpdate request failed. No legal object of gateway with the ID " + gatewayId +
                    " could be found. Sending an empty Gateway update.");
            gatewayUpdate = new GatewayUpdate(new ApiKey[0], new ServiceApi[0], now);
            return new ResponseEntity<>(gatewayUpdate, HttpStatus.NOT_ACCEPTABLE);
        }
    }
}
