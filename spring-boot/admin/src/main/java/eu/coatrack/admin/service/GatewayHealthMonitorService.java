package eu.coatrack.admin.service;

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

import eu.coatrack.admin.model.repository.ProxyRepository;
import eu.coatrack.api.Proxy;
import eu.coatrack.api.ProxyHealthStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/*
 * This class calculates and provides all data required for the Gateway Health Monitor.
 */
@Service
public class GatewayHealthMonitorService {

    public GatewayHealthMonitorService(@Value("${ygg.gateway-health-monitor.warning.threshold.minutes}") int gatewayHealthWarningThresholdInMinutes,
                                       @Value("${ygg.gateway-health-monitor.critical.threshold.minutes}") int gatewayHealthCriticalThresholdInMinutes,
                                       ProxyRepository proxyRepository) {
        this.gatewayHealthWarningThresholdInMinutes = gatewayHealthWarningThresholdInMinutes;
        this.gatewayHealthCriticalThresholdInMinutes = gatewayHealthCriticalThresholdInMinutes;
        this.proxyRepository = proxyRepository;
    }

    private int gatewayHealthWarningThresholdInMinutes;
    private int gatewayHealthCriticalThresholdInMinutes;
    private ProxyRepository proxyRepository;

    public class HealthDataForOneGateway {
        public String gatewayId;
        public String name;
        public ProxyHealthStatus status;
        public Long minutesPassedSinceLastContact;
        public boolean isMonitoringEnabled;

        public HealthDataForOneGateway(Proxy proxy) {
            this.gatewayId = proxy.getId();
            this.name = proxy.getName();
            this.isMonitoringEnabled = proxy.isHealthMonitoringEnabled();
            this.minutesPassedSinceLastContact = (proxy.getTimeOfLastSuccessfulCallToAdmin() == null ?
                    null :
                    Duration.between(proxy.getTimeOfLastSuccessfulCallToAdmin(), LocalDateTime.now()).toMinutes());
            this.status = calculateProxyStateForHealthMonitor(proxy);
        }
    }

    public ProxyHealthStatus calculateGatewayHealthStatusSummary(List<HealthDataForOneGateway> dataForAllGateways) {
        List<HealthDataForOneGateway> dataJustForMonitoredGateways = dataForAllGateways
                .stream()
                .filter(gateway -> gateway.isMonitoringEnabled == true)
                .collect(Collectors.toList());

        if (dataJustForMonitoredGateways
                .stream()
                .anyMatch(gateway -> gateway.status == ProxyHealthStatus.CRITICAL)) {
            return ProxyHealthStatus.CRITICAL;
        } else if (dataJustForMonitoredGateways
                .stream()
                .anyMatch(gateway -> gateway.status == ProxyHealthStatus.WARNING)) {
            return ProxyHealthStatus.WARNING;
        } else if (dataJustForMonitoredGateways
                .stream()
                .allMatch(gateway -> gateway.status == ProxyHealthStatus.OK)) {
            return ProxyHealthStatus.OK;
        }
        return ProxyHealthStatus.NEVER_CONNECTED;
    }

    public List<HealthDataForOneGateway> getGatewayHealthMonitorData() {
        List<HealthDataForOneGateway> gatewayDataListForGatewayHealthMonitor = proxyRepository.findAvailable()
                .stream()
                .map(proxy -> new HealthDataForOneGateway(proxy))
                .collect(Collectors.toList());

        Collections.sort(gatewayDataListForGatewayHealthMonitor, new GatewayHealthDataComparator());
        return gatewayDataListForGatewayHealthMonitor;
    }

    private ProxyHealthStatus calculateProxyStateForHealthMonitor(Proxy proxy) {
        if (!proxy.isHealthMonitoringEnabled()) {
            return ProxyHealthStatus.IGNORE;
        } else if (proxy.getTimeOfLastSuccessfulCallToAdmin() == null) {
            return ProxyHealthStatus.NEVER_CONNECTED;
        } else if (proxy.getTimeOfLastSuccessfulCallToAdmin()
                        .plusMinutes(gatewayHealthCriticalThresholdInMinutes)
                        .isBefore(LocalDateTime.now())) {
            return ProxyHealthStatus.CRITICAL;
        } else if (proxy.getTimeOfLastSuccessfulCallToAdmin()
                        .plusMinutes(gatewayHealthWarningThresholdInMinutes)
                        .isBefore(LocalDateTime.now())) {
            return ProxyHealthStatus.WARNING;
        } else {
            return ProxyHealthStatus.OK;
        }
    }

    /*
     * Sorts the list of gateways for the Gateway Health Monitor GUI:
     * - gateways with "monitoring enabled" should be displayed first
     * - second sorting criterion is "alphabetical by name"
     */
    private class GatewayHealthDataComparator implements Comparator<HealthDataForOneGateway> {
        @Override
        public int compare(HealthDataForOneGateway a, HealthDataForOneGateway b) {
            if (a.isMonitoringEnabled && !b.isMonitoringEnabled) {
                return -1;
            } else if (!a.isMonitoringEnabled && b.isMonitoringEnabled) {
                return 1;
            } else {
                return a.name.compareTo(b.name);
            }
        }
    }
}
