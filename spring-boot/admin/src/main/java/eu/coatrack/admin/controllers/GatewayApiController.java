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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller that handles HTTP calls by CoatRack gateways
 */
@Controller
public class GatewayApiController {

    private Long latestUpdateOfApiKeyList = new Date().getTime();

    public void updateApiKeyList(){
        latestUpdateOfApiKeyList = new Date().getTime();
    }

    public Long getLatestUpdateOfApiKeyList(){
        return latestUpdateOfApiKeyList;
    }

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

    @RequestMapping(value = "/api/services/search/receiveHashedApiKeysByGatewayId", method = RequestMethod.GET, produces = "application/json")
    public List<ApiKey> findServiceByGatewayId(@RequestParam("gatewayId") String gatewayId) {
        Proxy proxy = proxyRepository.findById(gatewayId);
        List<ApiKey> apiKeyList = proxy.getServiceApis().stream().flatMap(x -> x.getApiKeys()
                .stream()).collect(Collectors.toList());
        return apiKeyList;
    }
}
