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
import eu.coatrack.api.MetricType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.coatrack.api.ApiKey;

@Service
public class MetricsCounterService {

    private static final Logger log = LoggerFactory.getLogger(MetricsCounterService.class);

    private static final String PREFIX = "CUSTOM-METRICS";
    private static final String SEPARATOR = "___";

    private static final Pattern PATTERN_TO_SPLIT_SERVLET_PATH = Pattern.compile("^/?([^/]+)(/.*)?");
    private static final int MATCHER_GROUP_INDEX_OF_SERVICE_API_ID = 1;
    private static final int MATCHER_GROUP_INDEX_OF_PATH = 2;

    // this ID should be unique for each start of the proxy application, so that CoatRack admin knows when counting was restarted
    private static final String counterSessionID = UUID.randomUUID().toString();
    private final MeterRegistry meterRegistry;

    @Autowired
    private MetricsTransmitter metricsTransmitter;

    public MetricsCounterService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void increment(HttpServletRequest request, String apiKeyValue, MetricType metricType, Integer httpResponseCode) {
        log.debug(String.format("incrementing metric '%s' for URI '%s' and api key %s",
                metricType,
                request.getRequestURI(),
                apiKeyValue
        ));

        String requestMethod = request.getMethod();

        String serviceApiName = null;
        String path = null;

        Matcher matcher = PATTERN_TO_SPLIT_SERVLET_PATH.matcher(request.getServletPath());

        if (matcher.find()) {
            // first element of the servlet path is the service api's name/id
            serviceApiName = matcher.group(MATCHER_GROUP_INDEX_OF_SERVICE_API_ID);
            // rest of the servlet path is the actual path that is called on the proxied service
            path = matcher.group(MATCHER_GROUP_INDEX_OF_PATH);
            log.debug("matched servlet path '{}' with service uri identifier '{}' and path '{}'",
                    matcher.group(0), serviceApiName, path);
            if (path == null) {
                // if api consumer did not specifc path, set as "/"
                path = "/";
            } else if (path.endsWith("/") && !path.equals("/")) {
                // remove trailing "/" as this is the same path from metrics point of view
                path = path.substring(0, path.length()-1);
            }
        } else {
            log.warn("matcher {} did not match servlet path {}", matcher, request.getServletPath());
        }

        LocalDate today = LocalDate.now();

        String counterId = new StringBuilder()
                .append(PREFIX) // [0]
                .append(SEPARATOR)
                .append(serviceApiName) // [1]
                .append(SEPARATOR)
                .append(requestMethod) // [2]
                .append(SEPARATOR)
                .append(apiKeyValue) // [3]
                .append(SEPARATOR)
                .append(metricType) // [4]
                .append(SEPARATOR)
                .append(httpResponseCode) // [5]
                .append(SEPARATOR)
                .append(today) // [6]
                .append(SEPARATOR)
                .append(path) // [7]
                .toString();

        Counter counter = meterRegistry.find(counterId).counter();
        if (counter == null){
            counter = meterRegistry.counter(counterId);
        }
        counter.increment();

        Metric metricToTransmit = new Metric();
        metricToTransmit.setCount((int) counter.count());
        metricToTransmit.setMetricsCounterSessionID(request.getRequestedSessionId());
        metricToTransmit.setHttpResponseCode(httpResponseCode);
        metricToTransmit.setType(metricType);
        metricToTransmit.setDateOfApiCall(new Date());
        metricToTransmit.setPath("/");

        ApiKey tempApiKey = new ApiKey();
        tempApiKey.setKeyValue(apiKeyValue);
        metricToTransmit.setApiKey(tempApiKey);

        metricToTransmit.setRequestMethod(requestMethod);
        //metricToTransmit.setProxy(apiKey);
        metricsTransmitter.transmitToYggAdmin(metricToTransmit);
    }
}
