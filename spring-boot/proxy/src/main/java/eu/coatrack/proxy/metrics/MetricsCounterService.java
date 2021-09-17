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
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.StringJoiner;
import java.util.UUID;

import eu.coatrack.api.ApiKey;

@Service
public class MetricsCounterService {

    private static final Logger log = LoggerFactory.getLogger(MetricsCounterService.class);

    // this ID should be unique for each start of the proxy application, so that CoatRack admin knows when counting was restarted
    private static final String counterSessionID = UUID.randomUUID().toString();
    private static final String PREFIX = "CUSTOM-METRICS";
    private static final String SEPARATOR = "___";

    private final MeterRegistry meterRegistry;
    private final MetricsTransmitter metricsTransmitter;

    public MetricsCounterService(MeterRegistry meterRegistry, MetricsTransmitter metricsTransmitter) {
        this.meterRegistry = meterRegistry;
        this.metricsTransmitter = metricsTransmitter;
    }

    public void increment(MetricsHolder mh) {
        logBeginningOfIncrementation(mh);
        int currentCounter = incrementAndReturnCurrentCounter(mh);
        Metric metricToTransmit = createMetricToTransmit(mh);
        metricToTransmit.setCount(currentCounter);
        metricsTransmitter.transmitToCoatRackAdmin(metricToTransmit);
    }

    private void logBeginningOfIncrementation(MetricsHolder mh) {
        log.debug(String.format("incrementing metric '%s' for URI '%s' and api key %s",
                mh.getMetricType(),
                mh.getRequestURI(),
                mh.getApiKeyValue()
        ));
    }

    private int incrementAndReturnCurrentCounter(MetricsHolder mh) {
        String counterId = createCounterIdFromMetric(mh);

        Counter counter = meterRegistry.find(counterId).counter();
        if (counter == null){
            counter = meterRegistry.counter(counterId);
        }
        counter.increment();
        return (int) counter.count();
    }

    private String createCounterIdFromMetric(MetricsHolder mh) {
        StringJoiner stringJoiner = new StringJoiner(SEPARATOR);
        stringJoiner.add(PREFIX)
                .add(mh.getServiceApiName())
                .add(mh.getRequestMethod())
                .add(mh.getApiKeyValue())
                .add(mh.getMetricType().toString())
                .add(String.valueOf(mh.getHttpResponseCode()))
                .add(LocalDate.now().toString())
                .add(mh.getPath());
        return stringJoiner.toString();
    }

    private Metric createMetricToTransmit(MetricsHolder mh) {
        Metric metricToTransmit = new Metric();

        ApiKey apiKey = new ApiKey();
        apiKey.setKeyValue(mh.getApiKeyValue());
        metricToTransmit.setApiKey(apiKey);

        metricToTransmit.setRequestMethod(mh.getRequestMethod());
        metricToTransmit.setType(mh.getMetricType());
        metricToTransmit.setHttpResponseCode(mh.getHttpResponseCode());
        metricToTransmit.setDateOfApiCall(Date.valueOf(LocalDate.now()));
        metricToTransmit.setPath(mh.getPath());
        metricToTransmit.setMetricsCounterSessionID(counterSessionID);

        return metricToTransmit;
    }
}
