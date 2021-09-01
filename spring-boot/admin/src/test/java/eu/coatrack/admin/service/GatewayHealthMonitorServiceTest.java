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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class GatewayHealthMonitorServiceTest {

    @Mock
    ProxyRepository proxyRepository;

    @Mock
    Proxy proxySample;

    private List<Proxy> sampleProxies = new ArrayList<>();

    /*
     * 5 and 60 minutes represent the threshold for the gateway health status.
     * Warning state is triggered when the last call from a gateway was done more than 5 minutes ago, while Critical
     * state will be triggered if was done more than 60 minutes ago
     */
    @InjectMocks
    private GatewayHealthMonitorService gatewayHealthMonitorService = new GatewayHealthMonitorService(5, 60, proxyRepository);

    public Proxy createSampleProxy(boolean monitoredEnabled, String proxyName) {
        Proxy proxy = new Proxy();
        proxy.setName(proxyName);
        proxy.setId("testIdFor-" + proxyName);
        proxy.setHealthMonitoringEnabled(monitoredEnabled);
        proxy.updateTimeOfLastSuccessfulCallToAdmin_setToNow();
        return proxy;
    }

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        sampleProxies = Arrays.asList(createSampleProxy(true, "testProxyMonitoringEnabledA"),
                createSampleProxy(false, "testProxyMonitoringDisabledA"),
                proxySample);
        when(proxyRepository.findAvailable()).thenReturn(sampleProxies);
        when(proxySample.isHealthMonitoringEnabled()).thenReturn(true);
        when(proxySample.getName()).thenReturn("testProxyMonitoringEnabledB");
    }

    @Test
    public void ifAllGatewaysAreOkOrIgnore_ThenStatusSummaryShouldReturnOkState() {
        when(proxySample.getTimeOfLastSuccessfulCallToAdmin()).thenReturn(LocalDateTime.now());
        ProxyHealthStatus healthStatusSummary = gatewayHealthMonitorService
                .calculateGatewayHealthStatusSummary(gatewayHealthMonitorService
                        .getGatewayHealthMonitorData()
                        .healthDataForAllGateways);
        assertEquals(ProxyHealthStatus.OK, healthStatusSummary);
    }

    @Test
    public void ifOneGatewayIsWarningAndOthersOKOrIgnore_ThenStatusSummaryShouldReturnWarningState() {
        // The 6 means that the last gateway call was made 6 minutes ago, which should trigger a Warning State
        when(proxySample.getTimeOfLastSuccessfulCallToAdmin()).thenReturn(LocalDateTime.now().minusMinutes(6));
        ProxyHealthStatus healthStatusSummary = gatewayHealthMonitorService
                .calculateGatewayHealthStatusSummary(gatewayHealthMonitorService
                        .getGatewayHealthMonitorData()
                        .healthDataForAllGateways);
        assertEquals(ProxyHealthStatus.WARNING, healthStatusSummary);
    }

    @Test
    public void ifOneGatewayIsCritical_ThenStatusSummaryShouldReturnCriticalState() {
        // The 70 means that the last gateway call was made 70 minutes ago, which should trigger a Critical State
        when(proxySample.getTimeOfLastSuccessfulCallToAdmin()).thenReturn(LocalDateTime.now().minusMinutes(70));
        ProxyHealthStatus healthStatusSummary = gatewayHealthMonitorService
                .calculateGatewayHealthStatusSummary(gatewayHealthMonitorService
                        .getGatewayHealthMonitorData()
                        .healthDataForAllGateways);
        assertEquals(ProxyHealthStatus.CRITICAL, healthStatusSummary);
    }

    @Test
    public void ifAllGatewaysNotConnected_ThenStatusSummaryShouldReturnNotConnectedState() {
        when(proxySample.getTimeOfLastSuccessfulCallToAdmin()).thenReturn(null);
        ProxyHealthStatus healthStatusSummary = gatewayHealthMonitorService
                .calculateGatewayHealthStatusSummary(gatewayHealthMonitorService
                        .getGatewayHealthMonitorData()
                        .healthDataForAllGateways);
        assertEquals(ProxyHealthStatus.NEVER_CONNECTED, healthStatusSummary);
    }

    @Test
    public void listShouldBeOrganizedFirstByMonitoringEnabledAndThenByGatewayName() {
        List<String> expectedOrderOfGatewayNames = Arrays.asList("testProxyMonitoringEnabledA", "testProxyMonitoringEnabledB", "testProxyMonitoringDisabledA");
        assertEquals(expectedOrderOfGatewayNames,
                gatewayHealthMonitorService.getGatewayHealthMonitorData()
                        .healthDataForAllGateways
                        .stream()
                        .map(gateway -> gateway.name)
                        .collect(Collectors.toList()));
    }
}
