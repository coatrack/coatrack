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
import eu.coatrack.api.ProxyStates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/*
 * In this class it is contained all the business logic regarding the Gateway Health Monitor.
 * Each time the user updates a switch or adds a new proxy, this service class is responsible
 * to process this data and send it to the Front End on the Gateway Health Monitor
 */
@Service
public class GatewayHealthMonitorService {

    @Value("${ygg.gateway-health-monitor.warning.threshold.minutes}")
    private int gatewayHealthWarningThresholdInMinutes;

    @Value("${ygg.gateway-health-monitor.critical.threshold.minutes}")
    private int gatewayHealthCriticalThresholdInMinutes;

    @Autowired
    ProxyRepository proxyRepository;

    public class HealthDataForOneGateway {
        public String gatewayId;
        public String name;
        public ProxyStates status;
        public Long minutesPastSinceLastContact;
        public boolean isMonitoringEnabled;

        public HealthDataForOneGateway(Proxy proxy) {
            this.gatewayId = proxy.getId();
            this.name = proxy.getName();
            this.isMonitoringEnabled = proxy.isMonitoringEnabled();
            this.minutesPastSinceLastContact = (proxy.getTimeOfLastSuccessfulCallToAdmin() == null ? null : Duration.between(proxy.getTimeOfLastSuccessfulCallToAdmin(), LocalDateTime.now()).toMinutes());
        }
    }

    public ProxyStates calculateGatewayHealthStatusSummary(List<HealthDataForOneGateway> gatewayHealthMonitorDataList) {
        List<HealthDataForOneGateway> dataForMonitoredGateways = gatewayHealthMonitorDataList
                .stream()
                .filter(gateway -> gateway.isMonitoringEnabled == true)
                .collect(Collectors.toList());
        if (dataForMonitoredGateways
                .stream()
                .anyMatch(gateway -> gateway.status == ProxyStates.CRITICAL)) {
            return ProxyStates.CRITICAL;
        } else if (dataForMonitoredGateways
                .stream()
                .anyMatch(gateway -> gateway.status == ProxyStates.WARNING)) {
            return ProxyStates.WARNING;
        } else if (dataForMonitoredGateways.stream().allMatch(gateway -> gateway.status == ProxyStates.OK)) {
            return ProxyStates.OK;
        }
        return ProxyStates.NEVER_CONNECTED;
    }

    public List<HealthDataForOneGateway> getGatewayHealthMonitorData() {
        List<HealthDataForOneGateway> gatewayDataForGatewayHealthMonitorList = new ArrayList<>();
        List<Proxy> allProxiesOwnedByTheLoggedInUser = proxyRepository.findAvailable();
        allProxiesOwnedByTheLoggedInUser.forEach((proxy) -> {
            HealthDataForOneGateway gatewayDataForGatewayHealthMonitor = new HealthDataForOneGateway(proxy);
            gatewayDataForGatewayHealthMonitor.status = calculateProxyStateForHealthMonitor(proxy);
            gatewayDataForGatewayHealthMonitorList.add(gatewayDataForGatewayHealthMonitor);
        });
        Collections.sort(gatewayDataForGatewayHealthMonitorList, new GatewayHealthDataComparator());
        return gatewayDataForGatewayHealthMonitorList;
    }

    private ProxyStates calculateProxyStateForHealthMonitor(Proxy proxy) {
        if (proxy.isMonitoringEnabled()) {
            if (proxy.getTimeOfLastSuccessfulCallToAdmin() != null) {
                if (proxy.getTimeOfLastSuccessfulCallToAdmin()
                        .plusMinutes(gatewayHealthCriticalThresholdInMinutes)
                        .isBefore(LocalDateTime.now())) {
                    return ProxyStates.CRITICAL;
                } else if (proxy.getTimeOfLastSuccessfulCallToAdmin()
                        .plusMinutes(gatewayHealthWarningThresholdInMinutes)
                        .isBefore(LocalDateTime.now())) {
                    return ProxyStates.WARNING;
                } else {
                    return ProxyStates.OK;
                }
            } else {
                return ProxyStates.NEVER_CONNECTED;
            }
        } else {
            return ProxyStates.IGNORE;
        }
    }

    /*
    * This method will sort the list of gateways to be
    * sent for the Gateway Health Monitor first by the
    * monitoring enable status and then by name
    */
    class GatewayHealthDataComparator implements Comparator<HealthDataForOneGateway> {
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
