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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private Matcher matcher;
    private Counter counter;

    @Autowired
    private MetricsTransmitter metricsTransmitter;

    private MetricsHolder mh;

    public MetricsCounterService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void increment(MetricsHolder mh) {
        this.mh = mh;
        initializeFields();
        adaptFields();
        incrementCounter();
        Metric metricToTransmit = createMetricToTransmit();
        metricsTransmitter.transmitToCoatRackAdmin(metricToTransmit);
    }

    private void initializeFields() {
        log.debug(String.format("incrementing metric '%s' for URI '%s' and api key %s",
                mh.getMetricType(),
                mh.getRequest().getRequestURI(),
                mh.getApiKeyValue()
        ));
        requestMethod = mh.getRequest().getMethod();
        serviceApiName = null;
        path = null;
        matcher = PATTERN_TO_SPLIT_SERVLET_PATH.matcher(mh.getRequest().getServletPath());
    }

    private void adaptFields() {
        if (matcher.find()) {
            // first element of the servlet path is the service api's name/id
            serviceApiName = matcher.group(MATCHER_GROUP_INDEX_OF_SERVICE_API_ID);
            adaptPath();
        } else {
            log.warn("matcher {} did not match servlet path {}", matcher, mh.getRequest().getServletPath());
        }
    }

    private void adaptPath() {
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
    }

    private void incrementCounter() {
        String counterId = createCounterId();

        counter = meterRegistry.find(counterId).counter();
        if (counter == null){
            counter = meterRegistry.counter(counterId);
        }
        counter.increment();
    }

    private String createCounterId() {
        long now = new Date().getTime();
        //Set the time of day to a constant value. The counterId should be day specific but not more precise.
        Date todayAtConstantTime = new Date(now - now % (24 * 60 * 60 * 1000));

        StringJoiner stringJoiner = new StringJoiner(SEPARATOR);
        stringJoiner.add(PREFIX)
                .add(serviceApiName)
                .add(requestMethod)
                .add(mh.getApiKeyValue())
                .add(mh.getMetricType().toString())
                .add(String.valueOf(mh.getHttpResponseCode()))
                .add(todayAtConstantTime.toString())
                .add(path);
        return stringJoiner.toString();
    }

    private Metric createMetricToTransmit() {
        Metric metricToTransmit = new Metric();
        metricToTransmit.setCount((int) counter.count());
        metricToTransmit.setMetricsCounterSessionID(counterSessionID);
        metricToTransmit.setHttpResponseCode(mh.getHttpResponseCode());
        metricToTransmit.setType(mh.getMetricType());
        metricToTransmit.setDateOfApiCall(new Date());
        metricToTransmit.setPath(path);

        ApiKey tempApiKey = new ApiKey();
        tempApiKey.setKeyValue(mh.getApiKeyValue());
        metricToTransmit.setApiKey(tempApiKey);

        metricToTransmit.setRequestMethod(requestMethod);
        return metricToTransmit;
    }
}
