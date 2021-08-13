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
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Date;
import java.util.StringJoiner;
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

    private String requestMethod;
    private String serviceApiName;
    private String path;
    private String apiKeyValue;
    private MetricType metricType;
    private Integer httpResponseCode;
    private Matcher matcher;
    private HttpServletRequest request;

    @Autowired
    private MetricsTransmitter metricsTransmitter;

    public MetricsCounterService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void increment(HttpServletRequest request, String apiKeyValue, MetricType metricType, Integer httpResponseCode) {
        init(request, apiKeyValue, metricType, httpResponseCode);

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

        String counterId = createCounterId();

        Counter counter = meterRegistry.find(counterId).counter();
        if (counter == null){
            counter = meterRegistry.counter(counterId);
        }
        counter.increment();

        Metric metricToTransmit = createMetricToTransmit(counter);
        metricsTransmitter.transmitToCoatRackAdmin(metricToTransmit);
    }

    private Metric createMetricToTransmit(Counter counter) {
        Metric metricToTransmit = new Metric();
        metricToTransmit.setCount((int) counter.count());
        metricToTransmit.setMetricsCounterSessionID(request.getRequestedSessionId());
        metricToTransmit.setHttpResponseCode(httpResponseCode);
        metricToTransmit.setType(metricType);
        metricToTransmit.setDateOfApiCall(new Date());
        metricToTransmit.setPath(path);

        ApiKey tempApiKey = new ApiKey();
        tempApiKey.setKeyValue(apiKeyValue);
        metricToTransmit.setApiKey(tempApiKey);

        metricToTransmit.setRequestMethod(requestMethod);
        return metricToTransmit;
    }

    private void init(HttpServletRequest request, String apiKeyValue, MetricType metricType, Integer httpResponseCode) {
        log.debug(String.format("incrementing metric '%s' for URI '%s' and api key %s",
                metricType,
                request.getRequestURI(),
                apiKeyValue
        ));
        this.apiKeyValue = apiKeyValue;
        this.metricType = metricType;
        this.httpResponseCode = httpResponseCode;
        this.request = request;
        requestMethod = request.getMethod();
        serviceApiName = null;
        path = null;
        matcher = PATTERN_TO_SPLIT_SERVLET_PATH.matcher(request.getServletPath());
    }

    private String createCounterId() {
        StringJoiner stringJoiner = new StringJoiner(SEPARATOR);
        stringJoiner.add(PREFIX)
                .add(serviceApiName)
                .add(requestMethod)
                .add(apiKeyValue)
                .add(metricType.toString())
                .add(String.valueOf(httpResponseCode))
                .add(LocalDate.now().toString())
                .add(path);
        return stringJoiner.toString();
    }
}
