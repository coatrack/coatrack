package eu.coatrack.admin.controllers;

/*-
 * #%L
 * coatrack-admin
 * %%
 * Copyright (C) 2013 - 2020 Corizon | Institut für angewandte Systemtechnik Bremen GmbH (ATB)
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

import eu.coatrack.admin.model.repository.*;
import eu.coatrack.admin.service.MetricsService;
import eu.coatrack.api.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(path = "/api/metricsTransmission")
public class MetricsController {

    @Autowired
    private MetricsService metricsService;

    @RequestMapping(method = RequestMethod.POST)
    public Long storeMetricAndProperlySetRelationships(
            @RequestParam("proxyId") String proxyId,
            @RequestParam("apiKeyValue") String apiKeyValue,
            @RequestBody Metric metricSubmitted) {

        return metricsService.storeMetricAndProperlySetRelationships(proxyId, apiKeyValue, metricSubmitted);
    }



}
