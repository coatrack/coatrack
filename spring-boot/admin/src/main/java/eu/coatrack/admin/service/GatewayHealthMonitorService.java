package eu.coatrack.admin.service;

import eu.coatrack.admin.model.repository.ProxyRepository;
import eu.coatrack.api.Proxy;
import eu.coatrack.api.ProxyStates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GatewayHealthMonitorService {

    @Value("${ygg.gateway-health-monitor.warning.threshold.minutes}")
    private int gatewayHealthWarningThresholdInMinutes;

    @Value("${ygg.gateway-health-monitor.critical.threshold.minutes}")
    private int gatewayHealthCriticalThresholdInMinutes;

    @Autowired
    ProxyRepository proxyRepository;

    public class GatewayDataForTheGatewayHealthMonitor {
        public String gatewayId;
        public String name;
        public ProxyStates status;
        public Long minutesPastSinceLastContact;
        public boolean isMonitoringEnabled;
    }

    public ProxyStates gatewayHealthStatusSummary(List<GatewayDataForTheGatewayHealthMonitor> gatewayDataForTheGatewayHealthMonitorList){
        List <GatewayDataForTheGatewayHealthMonitor> gatewaysWithMonitoringActivatedList = gatewayDataForTheGatewayHealthMonitorList
                .stream()
                .filter(gateway -> gateway.isMonitoringEnabled == true)
                .collect(Collectors.toList());
        if (gatewaysWithMonitoringActivatedList
                .stream()
                .anyMatch(gateway -> gateway.status == ProxyStates.CRITICAL))
            return ProxyStates.CRITICAL;
        else if(gatewaysWithMonitoringActivatedList
                .stream()
                .anyMatch(gateway -> gateway.status == ProxyStates.WARNING))
            return ProxyStates.WARNING;
        return ProxyStates.OK;
    }

    public List<GatewayDataForTheGatewayHealthMonitor> updateProxyInfoForGatewayHealthMonitor() {
        List <GatewayDataForTheGatewayHealthMonitor> proxyDataForGatewayHealthMonitorList = new ArrayList<>();
        List<Proxy> AllProxiesOwnedByTheLoggedInUser = proxyRepository.findAvailable();
        AllProxiesOwnedByTheLoggedInUser.forEach((proxy) -> {
            GatewayDataForTheGatewayHealthMonitor gatewayDataForGatewayHealthMonitor = new GatewayDataForTheGatewayHealthMonitor();
            gatewayDataForGatewayHealthMonitor.gatewayId = proxy.getId();
            gatewayDataForGatewayHealthMonitor.name = proxy.getName();
            gatewayDataForGatewayHealthMonitor.isMonitoringEnabled = proxy.isMonitoringEnabled();
            if (proxy.getTimeOfLastSuccessfulCallToAdmin() != null && proxy.isMonitoringEnabled()) {
                gatewayDataForGatewayHealthMonitor.minutesPastSinceLastContact = Duration.between(proxy.getTimeOfLastSuccessfulCallToAdmin(), LocalDateTime.now()).toMinutes();
                if (proxy.getTimeOfLastSuccessfulCallToAdmin()
                        .plusMinutes(gatewayHealthCriticalThresholdInMinutes)
                        .isBefore(LocalDateTime.now())) {
                    gatewayDataForGatewayHealthMonitor.status = ProxyStates.CRITICAL;
                } else if (proxy.getTimeOfLastSuccessfulCallToAdmin()
                        .plusMinutes(gatewayHealthWarningThresholdInMinutes)
                        .isBefore(LocalDateTime.now())) {
                    gatewayDataForGatewayHealthMonitor.status = ProxyStates.WARNING;
                } else gatewayDataForGatewayHealthMonitor.status = ProxyStates.OK;
            } else gatewayDataForGatewayHealthMonitor.status = ProxyStates.IGNORE;
            proxyDataForGatewayHealthMonitorList.add(gatewayDataForGatewayHealthMonitor);
        });
        return proxyDataForGatewayHealthMonitorList;
    }
}
