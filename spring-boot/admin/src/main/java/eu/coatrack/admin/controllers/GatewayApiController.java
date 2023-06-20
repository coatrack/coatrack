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
import eu.coatrack.admin.service.GatewayApiService;
import eu.coatrack.api.ApiKey;
import eu.coatrack.api.Proxy;
import eu.coatrack.api.ServiceApi;
import javassist.NotFoundException;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller that handles HTTP calls by CoatRack gateways.
 */

@Slf4j
@Controller
public class GatewayApiController {
    @Autowired
    private GatewayApiService gatewayApiService;

    @RequestMapping(value = "/api/api-keys/search/findByKeyValue", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ApiKey findApiKeyEntityByApiKeyValue(@RequestParam("keyValue") String apiKeyValue) {
        return gatewayApiService.findApiKeyEntityByApiKeyValue(apiKeyValue);
    }

    @RequestMapping(value = "/api/services/search/findByApiKeyValue", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ServiceApi findServiceByApiKeyValue(@RequestParam("apiKeyValue") String apiKeyValue) {
        return gatewayApiService.findServiceByApiKeyValue(apiKeyValue);
    }

    //Due to legacy code reasons the gateway API key and the gateway id are exactly the same.
    @GetMapping("/api/gateways/api-keys")
    public ResponseEntity<List<ApiKey>> findApiKeyListByGatewayApiKey(@RequestParam("gateway-api-key") String gatewayIdAndApiKey) {
        return gatewayApiService.findApiKeyListByGatewayApiKey(gatewayIdAndApiKey);
    }
}
