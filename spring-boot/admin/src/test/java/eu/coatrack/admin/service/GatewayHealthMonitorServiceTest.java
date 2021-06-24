package eu.coatrack.admin.service;

import eu.coatrack.admin.model.repository.ProxyRepository;
import eu.coatrack.api.Proxy;
import eu.coatrack.api.ProxyHealthStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.when;

public class GatewayHealthMonitorServiceTest {

    @Mock
    ProxyRepository proxyRepository;

    @Mock
    Proxy proxySample;

    private List<Proxy> sampleProxies = new ArrayList<>();

    @InjectMocks
    private GatewayHealthMonitorService gatewayHealthMonitorService;

    public Proxy createSampleProxy(boolean monitoredEnabled, String proxyName) {
        Proxy proxy = new Proxy();
        proxy.setName(proxyName);
        proxy.setId("testIdFor-" + proxyName);
        proxy.setHealthMonitoringEnabled(monitoredEnabled);
        proxy.updateTimeOfLastSuccessfulCallToAdmin_setToNow();
        return proxy;
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        sampleProxies = Arrays.asList(createSampleProxy(true, "testProxyMonitoringEnabledA"),
                createSampleProxy(false, "testProxyMonitoringDisabledA"),
                proxySample);
        when(proxyRepository.findAvailable()).thenReturn(sampleProxies);
        when(proxySample.isHealthMonitoringEnabled()).thenReturn(true);
        when(proxySample.getName()).thenReturn("testProxyMonitoringEnabledB");
        ReflectionTestUtils.setField(gatewayHealthMonitorService, "gatewayHealthWarningThresholdInMinutes", 5);
        ReflectionTestUtils.setField(gatewayHealthMonitorService, "gatewayHealthCriticalThresholdInMinutes", 60);
    }

    @Test
    public void ifAllGatewaysAreOkOrIgnore_ThenStatusSummaryShouldReturnOkState() {
        when(proxySample.getTimeOfLastSuccessfulCallToAdmin()).thenReturn(LocalDateTime.now());
        ProxyHealthStatus healthStatusSummary = gatewayHealthMonitorService
                .calculateGatewayHealthStatusSummary(gatewayHealthMonitorService
                        .getGatewayHealthMonitorData());
        Assert.assertEquals(ProxyHealthStatus.OK, healthStatusSummary);
    }

    @Test
    public void ifOneGatewayIsWarningAndOthersOKOrIgnore_ThenStatusSummaryShouldReturnWarningState() {
        when(proxySample.getTimeOfLastSuccessfulCallToAdmin()).thenReturn(LocalDateTime.now().minusMinutes(6));
        ProxyHealthStatus healthStatusSummary = gatewayHealthMonitorService
                .calculateGatewayHealthStatusSummary(gatewayHealthMonitorService
                        .getGatewayHealthMonitorData());
        Assert.assertEquals(ProxyHealthStatus.WARNING, healthStatusSummary);
    }

    @Test
    public void ifOneGatewayIsCritical_ThenStatusSummaryShouldReturnCriticalState() {
        when(proxySample.getTimeOfLastSuccessfulCallToAdmin()).thenReturn(LocalDateTime.now().minusMinutes(70));
        ProxyHealthStatus healthStatusSummary = gatewayHealthMonitorService
                .calculateGatewayHealthStatusSummary(gatewayHealthMonitorService
                        .getGatewayHealthMonitorData());
        Assert.assertEquals(ProxyHealthStatus.CRITICAL, healthStatusSummary);
    }

    @Test
    public void ifAllGatewaysNotConnected_ThenStatusSummaryShouldReturnNotConnectedState() {
        when(proxySample.getTimeOfLastSuccessfulCallToAdmin()).thenReturn(null);
        ProxyHealthStatus healthStatusSummary = gatewayHealthMonitorService
                .calculateGatewayHealthStatusSummary(gatewayHealthMonitorService
                        .getGatewayHealthMonitorData());
        Assert.assertEquals(ProxyHealthStatus.NEVER_CONNECTED, healthStatusSummary);
    }

    @Test
    public void listShouldBeOrganizedFirstByMonitoringEnabledAndThenByGatewayName() {
        List<String> expectedOrderOfGatewayNames = Arrays.asList("testProxyMonitoringEnabledA", "testProxyMonitoringEnabledB", "testProxyMonitoringDisabledA");
        Assert.assertEquals(expectedOrderOfGatewayNames,
                gatewayHealthMonitorService.getGatewayHealthMonitorData()
                        .stream()
                        .map(gateway -> gateway.name)
                        .collect(Collectors.toList()));
    }
}
