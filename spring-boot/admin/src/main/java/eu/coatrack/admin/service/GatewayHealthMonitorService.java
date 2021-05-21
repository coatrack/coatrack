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

    public static class GatewayDataForTheGatewayHealthMonitor {
        public String proxyId;
        public String name;
        public ProxyStates status;
        public Long minutesPastSinceLastContact;
        public boolean isMonitoringEnabled;
    }

    public ProxyStates gatewayHealthStatusSummary(List<GatewayDataForTheGatewayHealthMonitor> gatewayDataForTheGatewayHealthMonitorList){
        List <GatewayDataForTheGatewayHealthMonitor> proxiesWithMonitoringActivatedList = gatewayDataForTheGatewayHealthMonitorList.stream().filter(proxy -> proxy.isMonitoringEnabled == true).collect(Collectors.toList());
        if (proxiesWithMonitoringActivatedList
                .stream()
                .anyMatch(proxy -> proxy.status == ProxyStates.CRITICAL))
            return ProxyStates.CRITICAL;
        else if(proxiesWithMonitoringActivatedList
                .stream()
                .anyMatch(proxy -> proxy.status == ProxyStates.WARNING))
            return ProxyStates.WARNING;
        return ProxyStates.OK;
    }

    public List<GatewayDataForTheGatewayHealthMonitor> updateProxyInfoForGatewayHealthMonitor() {
        List <GatewayDataForTheGatewayHealthMonitor> proxyDataForGatewayHealthMonitorList = new ArrayList<>();
        List<Proxy> proxiesToBeChanged = proxyRepository.findAvailable();
        proxiesToBeChanged.forEach((proxy) -> {
            GatewayDataForTheGatewayHealthMonitor proxyDataForGatewayHealthMonitor = new GatewayDataForTheGatewayHealthMonitor();
            proxyDataForGatewayHealthMonitor.proxyId = proxy.getId();
            proxyDataForGatewayHealthMonitor.name = proxy.getName();
            proxyDataForGatewayHealthMonitor.isMonitoringEnabled = proxy.isMonitoringEnabled();
            if (proxy.getTimeOfLastSuccessfulCallToAdmin() != null && proxy.isMonitoringEnabled()) {
                Long minutesPastSinceLastContact = Duration.between(proxy.getTimeOfLastSuccessfulCallToAdmin(), LocalDateTime.now()).toMinutes();
                proxyDataForGatewayHealthMonitor.minutesPastSinceLastContact = minutesPastSinceLastContact;
                if (minutesPastSinceLastContact > gatewayHealthCriticalThresholdInMinutes) {
                    proxyDataForGatewayHealthMonitor.status = ProxyStates.CRITICAL;
                } else if (minutesPastSinceLastContact > gatewayHealthWarningThresholdInMinutes) {
                    proxyDataForGatewayHealthMonitor.status = ProxyStates.WARNING;
                } else proxyDataForGatewayHealthMonitor.status = ProxyStates.OK;
            } else proxyDataForGatewayHealthMonitor.status = ProxyStates.IGNORE;
            proxyDataForGatewayHealthMonitorList.add(proxyDataForGatewayHealthMonitor);
        });
        return proxyDataForGatewayHealthMonitorList;
    }
}
