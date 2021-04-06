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

import eu.coatrack.proxy.security.UrlResourcesProvider;
import eu.coatrack.api.ApiKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.GaugeWriter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * Transmitter receives metric value updates from the buffer and transmits them to CoatRack admin
 *
 * @author gr-hovest@atb-bremen.de
 */
public class MetricsTransmitter implements GaugeWriter {

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

    @Override
    public void set(Metric<?> metric) {

        // filter relevant metrics and transform them into YGG format
        eu.coatrack.api.Metric metricToTransmit = metricsCounterService.filterAndTransformMetric(metric);

        // if metric is null, it is irrelevant for transmission
        if (metricToTransmit != null) transmitToYggAdmin(metricToTransmit);
    }

    private void transmitToYggAdmin(eu.coatrack.api.Metric metricToTransmit) {
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

            /*
            // create relationship from transmitted metric to this proxy
            addRelationshipFromMetricToOtherEntity(
                    uriOfTransmittedMetric,
                    adminBaseUrl + adminEndpointToGetProxies + "/" + myProxyID,
                    "proxy");

            // get Api key entity from CoatRack admin for given key value
            ApiKey apiKey = getApiKeyEntityForApiKeyStringValue(metricToTransmit.getApiKey().getKeyValue());

            // create relationship from transmitted metric to the api key that was used
            addRelationshipFromMetricToOtherEntity(
                    uriOfTransmittedMetric,
                    adminBaseUrl + adminEndpointToGetApiKeys + "/"
                            // TODO replace hardcoded ID
                            + 1,
                    "apiKey");
            */

        } catch (Exception e) {
            log.debug("Exception when communicating with CoatRack admin server", e);
        }
    }

    private void addRelationshipFromMetricToOtherEntity(URI uriOfTransmittedMetric, String urlOfOtherEntity, String relationshipAttributeName) {
        log.debug("call to create relationship from {} to {} {}", uriOfTransmittedMetric, relationshipAttributeName, urlOfOtherEntity);
        // create Http Entity object for the related Entity
        HttpEntity<String> relatedEntity
                = new HttpEntity<>(urlOfOtherEntity, httpHeadersForPuttingRelationships);

        // pass relationship to CoatRack admin api
        ResponseEntity<String> relationshipPutResponse = restTemplate.exchange(uriOfTransmittedMetric + "/" + relationshipAttributeName,
                HttpMethod.PUT, relatedEntity, String.class);
        log.info(String.format("created relationship from transmitted metric %s to %s entity (url=%s), result was: %s",
                uriOfTransmittedMetric, relationshipAttributeName, urlOfOtherEntity, relationshipPutResponse.getStatusCode()));
    }

    private ApiKey getApiKeyEntityForApiKeyStringValue(String apiKeyValue) {
        ApiKey apiKey = null;
        try {
            String apiKeySearchUrl = adminBaseUrl + adminResourceToSearchForApiKeys + apiKeyValue;
            log.debug("searchingForApiKey via {}", apiKeySearchUrl);
            ResponseEntity<ApiKey> resultOfApiKeySearch = restTemplate.getForEntity(apiKeySearchUrl, ApiKey.class);

            if (resultOfApiKeySearch != null && resultOfApiKeySearch.getBody() != null) {
                apiKey = resultOfApiKeySearch.getBody();
                log.debug("API key was found by CoatRack admin: " + apiKey.toString());
            } else {
                log.error("Communication with CoatRack admin server failed, result is: " + resultOfApiKeySearch);
            }
        } catch (Exception e) {
            log.error("Exception when trying to get API consumer name from CoatRack admin", e);
        }
        return apiKey;
    }
}
