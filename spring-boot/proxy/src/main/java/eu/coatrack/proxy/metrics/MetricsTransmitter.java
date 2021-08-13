package eu.coatrack.proxy.metrics;

/*-
 * #%L
 * coatrack-proxy
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

import eu.coatrack.api.Metric;
import eu.coatrack.proxy.security.UrlResourcesProvider;
import eu.coatrack.api.ApiKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * Transmitter receives metric value updates from the buffer and transmits them to CoatRack admin
 *
 * @author gr-hovest@atb-bremen.de
 */

@Service
public class MetricsTransmitter{

    private static Logger log = LoggerFactory.getLogger(MetricsTransmitter.class);

    @Autowired
    private RestTemplate restTemplate;

    private final HttpHeaders httpHeadersForPuttingRelationships;

    @Autowired
    private MetricsCounterService metricsCounterService;

    @Autowired
    private UrlResourcesProvider urlResourcesProvider;

    @Value("${custom-metrics.prefix.counter}")
    private String prefixForCustomCounterMetrics;

    @Value("${proxy-id}")
    private String myProxyID;

    @Value("${ygg.admin.api-base-url}")
    private String adminBaseUrl;

    @Value("${ygg.admin.resources.metricsTransmission}")
    private String adminEndpointForMetricsTransmission;

    @Value("${ygg.admin.resources.proxies}")
    private String adminEndpointToGetProxies;

    @Value("${ygg.admin.resources.api-keys}")
    private String adminEndpointToGetApiKeys;

    @Value("${ygg.admin.resources.search-api-keys-by-token-value}")
    private String adminResourceToSearchForApiKeys;

    public MetricsTransmitter() {
        super();

        // init headers for relationship put operations
        httpHeadersForPuttingRelationships = new HttpHeaders();
        httpHeadersForPuttingRelationships.add("Content-Type", "text/uri-list");
    }

    public void transmitToYggAdmin(Metric metricToTransmit) {
        try {
            URI uriToTransmitMetric = new URI(
                    urlResourcesProvider.attachGatewayIdToUrl(
                            adminBaseUrl +
                                    adminEndpointForMetricsTransmission +
                                    "?proxyId=" + myProxyID +
                                    "&apiKeyValue=" + metricToTransmit.getApiKey().getKeyValue()));

            log.debug("uri to transmit metric: {}", uriToTransmitMetric.toString());
            Object idOfTransmittedMetric = restTemplate.postForObject(uriToTransmitMetric, metricToTransmit, Long.class);
            log.info("transmitted Metrics to admin: {} - response was metric ID {}", metricToTransmit.toString(), idOfTransmittedMetric);

        } catch (Exception e) {
            log.error("Exception when communicating with CoatRack admin server", e);
        }
    }
}