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
import java.time.*;
import java.util.StringJoiner;
import java.util.TimeZone;
import java.util.UUID;

import eu.coatrack.api.ApiKey;

@Service
public class MetricsCounterService {

    // Quick fix of #237 by setting default time zones of Admin and Gateway to UCT timezone.
    // In the future, this system should be overhauled by the introduction of ZonedDateTime's.
    static {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/London")));
    }
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

    public void increment(TemporaryMetricsAggregation tma) {
        logBeginningOfIncrementation(tma);

        LocalDate now = LocalDate.now();
        int currentCount = incrementCounterAndReturnCurrentValue(tma, now);
        Metric metricToTransmit = createMetricToTransmit(tma, now);

        metricToTransmit.setCount(currentCount);
        metricsTransmitter.transmitToCoatRackAdmin(metricToTransmit);
    }

    private void logBeginningOfIncrementation(TemporaryMetricsAggregation tma) {
        log.debug(String.format("incrementing metric '%s' for URI '%s' and api key %s",
                tma.getMetricType(),
                tma.getRequestURI(),
                tma.getApiKeyValue()
        ));
    }

    private int incrementCounterAndReturnCurrentValue(TemporaryMetricsAggregation tma, LocalDate now) {
        String counterId = createCounterIdFromMetric(tma, now);
        log.debug("Incrementing counter with ID {}.", counterId);

        Counter counter = meterRegistry.find(counterId).counter();
        if (counter == null){
            counter = meterRegistry.counter(counterId);
        }
        counter.increment();
        return (int) counter.count();
    }

    private String createCounterIdFromMetric(TemporaryMetricsAggregation tma, LocalDate now) {
        StringJoiner stringJoiner = new StringJoiner(SEPARATOR);
        stringJoiner.add(PREFIX)
                .add(tma.getServiceApiName())
                .add(tma.getRequestMethod())
                .add(tma.getApiKeyValue())
                .add(tma.getMetricType().toString())
                .add(String.valueOf(tma.getHttpResponseCode()))
                .add(now.toString())
                .add(tma.getPath());
        return stringJoiner.toString();
    }

    private Metric createMetricToTransmit(TemporaryMetricsAggregation tma, LocalDate now) {
        Metric metricToTransmit = new Metric();

        ApiKey apiKey = new ApiKey();
        apiKey.setKeyValue(tma.getApiKeyValue());
        metricToTransmit.setApiKey(apiKey);

        metricToTransmit.setRequestMethod(tma.getRequestMethod());
        metricToTransmit.setType(tma.getMetricType());
        metricToTransmit.setHttpResponseCode(tma.getHttpResponseCode());
        metricToTransmit.setDateOfApiCall(java.util.Date.from(now.atStartOfDay().toInstant(ZoneOffset.UTC)));
        metricToTransmit.setPath(tma.getPath());
        metricToTransmit.setMetricsCounterSessionID(counterSessionID);

        return metricToTransmit;
    }
}
