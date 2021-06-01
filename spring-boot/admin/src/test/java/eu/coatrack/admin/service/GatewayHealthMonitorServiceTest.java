package eu.coatrack.admin.service;

import eu.coatrack.admin.model.repository.ProxyRepository;
import eu.coatrack.api.Proxy;
import eu.coatrack.api.ProxyStates;
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

    private List<Proxy> sampleList = new ArrayList<>();

    @InjectMocks
    private GatewayHealthMonitorService gatewayHealthMonitorService;

    public Proxy createSampleProxy(boolean monitoredEnabled, String proxyName) {
        Proxy proxy = new Proxy();
        proxy.setName(proxyName);
        proxy.setId("GatewayIDTest");
        proxy.setMonitoringEnabled(monitoredEnabled);
        proxy.updateTimeOfLastSuccessfulCallToAdmin_setToNow();
        return proxy;
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        sampleList = Arrays.asList(createSampleProxy(true, "test1"), createSampleProxy(false, "test2"), proxySample);
        when(proxySample.isMonitoringEnabled()).thenReturn(true);
        when(proxySample.getName()).thenReturn("test3");
        ReflectionTestUtils.setField(gatewayHealthMonitorService, "gatewayHealthWarningThresholdInMinutes", 5);
        ReflectionTestUtils.setField(gatewayHealthMonitorService, "gatewayHealthCriticalThresholdInMinutes", 60);
    }

    @Test
    public void ifAllGatewaysAreOkOrIgnore_ThenStatusSummaryShouldReturnOkState() {
        when(proxyRepository.findAvailable()).thenReturn(sampleList);
        when(proxySample.getTimeOfLastSuccessfulCallToAdmin()).thenReturn(LocalDateTime.now());
        Assert.assertEquals(ProxyStates.OK, gatewayHealthMonitorService.calculateGatewayHealthStatusSummary(gatewayHealthMonitorService.getGatewayHealthMonitorData()));
    }

    @Test
    public void ifOneGatewayIsWarningAndOthersOKOrIgnore_ThenStatusSummaryShouldReturnWarningState() {
        when(proxyRepository.findAvailable()).thenReturn(sampleList);
        when(proxySample.getTimeOfLastSuccessfulCallToAdmin()).thenReturn(LocalDateTime.now().minusMinutes(6));
        Assert.assertEquals(ProxyStates.WARNING, gatewayHealthMonitorService.calculateGatewayHealthStatusSummary(gatewayHealthMonitorService.getGatewayHealthMonitorData()));
    }

    @Test
    public void ifOneGatewayIsCritical_ThenStatusSummaryShouldReturnCriticalState() {
        when(proxyRepository.findAvailable()).thenReturn(sampleList);
        when(proxySample.getTimeOfLastSuccessfulCallToAdmin()).thenReturn(LocalDateTime.now().minusMinutes(70));
        Assert.assertEquals(ProxyStates.CRITICAL, gatewayHealthMonitorService.calculateGatewayHealthStatusSummary(gatewayHealthMonitorService.getGatewayHealthMonitorData()));
    }

    @Test
    public void ifAllGatewaysNotConnected_ThenStatusSummaryShouldReturnNotConnectedState() {
        when(proxyRepository.findAvailable()).thenReturn(sampleList);
        when(proxySample.getTimeOfLastSuccessfulCallToAdmin()).thenReturn(null);
        Assert.assertEquals(ProxyStates.NEVER_CONNECTED, gatewayHealthMonitorService.calculateGatewayHealthStatusSummary(gatewayHealthMonitorService.getGatewayHealthMonitorData()));
    }

    @Test
    public void listShouldBeOrganizedFirstByMonitoringEnabledAndThenByName() {
        when(proxyRepository.findAvailable()).thenReturn(sampleList);
        List<String> gatewayNames = Arrays.asList("test1", "test3", "test2");
        Assert.assertEquals(gatewayNames,
                gatewayHealthMonitorService.getGatewayHealthMonitorData()
                        .stream()
                        .map(gateway -> gateway.name)
                        .collect(Collectors.toList()));
    }
}