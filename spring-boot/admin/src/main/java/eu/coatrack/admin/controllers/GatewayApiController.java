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
import eu.coatrack.api.Proxy;
import eu.coatrack.api.ServiceApi;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *  Controller that handles HTTP calls by CoatRack gateways.
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

    @GetMapping( "/api/gateways/{gatewayId}/api-keys")
    public ResponseEntity<List<ApiKey>> findApiKeyListByGatewayId(@PathVariable("gatewayId") String gatewayId) {
        log.debug("The gateway with the ID {} requests its latest API key list.", gatewayId);
        try {
            Proxy proxy = proxyRepository.findById(gatewayId);
            if(proxy != null)
                return createApiKeyListResponseEntity(proxy);
            else
                throw new NotFoundException("The gateway with the ID " + gatewayId + " was not found.");
        } catch (Exception e) {
            log.debug(e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    private ResponseEntity<List<ApiKey>> createApiKeyListResponseEntity(Proxy proxy) {
        if(proxy.getServiceApis() == null || proxy.getServiceApis().isEmpty()){
            log.debug("The gateway with the ID {} does not provide any services.", proxy.getId());
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
        } else{
            List<ApiKey> apiKeyList = proxy.getServiceApis().stream().flatMap(serviceApi -> serviceApi.getApiKeys()
                    .stream()).collect(Collectors.toList());
            return new ResponseEntity<>(apiKeyList, HttpStatus.OK);
        }
    }
}
