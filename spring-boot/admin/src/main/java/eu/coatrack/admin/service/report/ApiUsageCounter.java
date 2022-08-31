package eu.coatrack.admin.service.report;

import eu.coatrack.admin.model.repository.MetricsAggregationCustomRepository;
import eu.coatrack.api.EntryPoint;
import eu.coatrack.api.MetricResult;
import eu.coatrack.api.MetricType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.Date;
import java.util.List;

import static eu.coatrack.api.MetricType.RESPONSE;
import static eu.coatrack.api.ServiceAccessPaymentPolicy.*;

@Getter
@Slf4j
@Service
@AllArgsConstructor
public class ApiUsageCounter {
    private static final AntPathMatcher parser = new AntPathMatcher();

    @Autowired
    private MetricsAggregationCustomRepository metricsAggregationCustomRepository;

    public CallCount count(ApiUsageDTO apiUsageDTO) {
        CallCount callCount = new CallCount();
        // TODO needs to be typed, raw use of implicit types are no fun

        Long serviceId = apiUsageDTO.getService().getId();
        String ownerName = apiUsageDTO.getService().getOwner().getUsername();
        Long consumerId = apiUsageDTO.getConsumer() != null ? apiUsageDTO.getConsumer().getId() : -1L;
        Date from = apiUsageDTO.getFrom();
        Date until = apiUsageDTO.getUntil();


        List metricResults = metricsAggregationCustomRepository.getUsageApiConsumer(
                RESPONSE, serviceId, ownerName, consumerId, from, until,true);

        if (metricResults != null && !metricResults.isEmpty()) {
            metricResults.forEach(metricResult -> evaluateMetric(metricResult, apiUsageDTO, callCount));
        }
        return callCount;
    }

    // Will disappear after typing the Queries
    @Deprecated
    private void evaluateMetric(Object metricResult, ApiUsageDTO apiUsageDTO, CallCount callCount) {
        MetricResult metric = metricResultFromObjArray((Object[]) metricResult);
        evaluateMetric(metric, apiUsageDTO, callCount);
    }

    private void evaluateMetric(MetricResult metric, ApiUsageDTO apiUsageDTO, CallCount callCount) {
        log.debug("Metric for report: user '{}' service '{}' type '{}' calls '{}' path '{}' method '{}'", metric.getUsername(), metric.getServiceId(), metric.getType(), metric.getCallsPerEntry(), metric.getPath(), metric.getRequestMethod());
        if (metric.getType() == RESPONSE) {
            if (apiUsageDTO.getService().getServiceAccessPaymentPolicy() == FOR_FREE) {
                callCount.addFree(metric.getCallsPerEntry());
            } else if (apiUsageDTO.getService().getServiceAccessPaymentPolicy() == MONTHLY_FEE) {
                callCount.addMonthlyBilled(metric.getCallsPerEntry());
            } else if (apiUsageDTO.getService().getServiceAccessPaymentPolicy() == WELL_DEFINED_PRICE) {
                boolean entryPointMatching = false;

                for (EntryPoint entryPoint : apiUsageDTO.getService().getEntryPoints()) {
                    entryPointMatching = matchesForEntryPoint(entryPoint, metric);
                    if (entryPointMatching) {
                        callCount.addForEntryPoint(entryPoint, metric.getCallsPerEntry());
                    }
                }

                if (!entryPointMatching && !apiUsageDTO.isConsiderOnlyPaidCalls()) {
                    callCount.addNotMatching(metric.getCallsPerEntry());
                }
            }
        }
    }

    private boolean matchesForEntryPoint(EntryPoint entryPoint, MetricResult metric) {
        boolean isMatchForEntryPoint = false;
        if (entryPoint.getPathPattern() != null && entryPoint.getHttpMethod() != null) {
            boolean pathMatches = parser.match(entryPoint.getPathPattern(), metric.getPath());
            isMatchForEntryPoint = pathMatches && entryPoint.getHttpMethod().equals(metric.getRequestMethod()) || entryPoint.getHttpMethod().equals("*");
        }
        return isMatchForEntryPoint;
    }

    //This method is going to disappear with implementation of typesafe queries
    @Deprecated
    private MetricResult metricResultFromObjArray(Object[] item) {
        String username = (String) item[0];
        Long serviceId = (Long) item[1];
        MetricType metricType = (MetricType) item[2];
        Long callsPerEntry = (Long) item[3];
        String path = (String) item[4];
        String requestMethod = (String) item[5];
        return new MetricResult(username, serviceId, metricType, callsPerEntry, path, requestMethod);
    }

}
