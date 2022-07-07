package eu.coatrack.admin.service.report;

import eu.coatrack.admin.model.repository.MetricsAggregationCustomRepository;
import eu.coatrack.api.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.math.BigDecimal;
import java.util.*;

import static eu.coatrack.api.ServiceAccessPaymentPolicy.*;

@Slf4j
@Service
@AllArgsConstructor
public class ApiUsageCalculator {
    private static final AntPathMatcher parser = new AntPathMatcher();

    @Autowired
    private final MetricsAggregationCustomRepository metricsAggregationCustomRepository;

    public List<ApiUsageReport> calculateForSpecificService(ApiUsageDTO apiUsageDTO) {
        // TODO needs to be typed, raw use of implicit types are no fun
        List metricResults = metricsAggregationCustomRepository.getUsageApiConsumer(MetricType.RESPONSE, apiUsageDTO.service.getId(), apiUsageDTO.service.getOwner().getUsername(), apiUsageDTO.consumer.getId(), apiUsageDTO.from, apiUsageDTO.until, true);
        List<ApiUsageReport> apiUsageReports = new ArrayList<>();
        if (metricResults != null && !metricResults.isEmpty()) {
            ApiUsageCounter countedCalls = countCalls(metricResults, apiUsageDTO);
            apiUsageReports = getReports(apiUsageDTO, countedCalls);
            apiUsageReports.forEach(reportRow -> log.debug("row for report: " + reportRow));
        }
        return apiUsageReports;
    }

    /**
     * @deprecated This method is going to disappear with implementation of typesafe queries
     */
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

    private ApiUsageCounter countCalls(List metricResults, ApiUsageDTO apiUsageDTO) {
        ApiUsageCounter counter = new ApiUsageCounter(apiUsageDTO.service);
        metricResults.forEach(metricResult -> evaluateMetric(metricResult, apiUsageDTO, counter));
        return counter;
    }

    // Will disappear after typing the Queries
    @Deprecated
    private void evaluateMetric(Object metricResult, ApiUsageDTO apiUsageDTO, ApiUsageCounter counter) {
        MetricResult metric = metricResultFromObjArray((Object[]) metricResult);
        evaluateMetric(metric, apiUsageDTO, counter);
    }

    private void evaluateMetric(MetricResult metric, ApiUsageDTO apiUsageDTO, ApiUsageCounter counter) {
        log.debug("Metric for report: user '{}' service '{}' type '{}' calls '{}' path '{}' method '{}'", metric.getUsername(), metric.getServiceId(), metric.getType(), metric.getCallsPerEntry(), metric.getPath(), metric.getRequestMethod());
        if (metric.getType() == MetricType.RESPONSE) {
            if (apiUsageDTO.service.getServiceAccessPaymentPolicy() == FOR_FREE) {
                counter.addFree(metric.getCallsPerEntry());
            } else if (apiUsageDTO.service.getServiceAccessPaymentPolicy() == MONTHLY_FEE) {
                counter.addMonthlyBilled(metric.getCallsPerEntry());
            } else if (apiUsageDTO.service.getServiceAccessPaymentPolicy() == WELL_DEFINED_PRICE) {
                boolean entryPointMatching = matchesEntryPoint(metric, apiUsageDTO.service, counter);
                if (!entryPointMatching && !apiUsageDTO.considerOnlyPaidCalls) {
                    counter.addNotMatching(metric.getCallsPerEntry());
                }
            }
        }
    }

    private boolean matchesEntryPoint(MetricResult metric, ServiceApi service, ApiUsageCounter counter) {
        for (EntryPoint entryPoint : service.getEntryPoints()) {
            if (entryPoint.getPathPattern() != null && entryPoint.getHttpMethod() != null) {
                boolean pathMatches = parser.match(entryPoint.getPathPattern(), metric.getPath());
                if (pathMatches) {
                    if (entryPoint.getHttpMethod().equals(metric.getRequestMethod()) || entryPoint.getHttpMethod().equals("*")) {
                        counter.addForEntryPoint(entryPoint, metric.getCallsPerEntry());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<ApiUsageReport> getReports(ApiUsageDTO apiUsageDTO, ApiUsageCounter counter) {
        List<ApiUsageReport> results = new ArrayList<>();

        if (counter.hasCallsPerEntryPoint()) {
            for (EntryPoint entryPoint : apiUsageDTO.service.getEntryPoints()) {
                String name = String.format("%s (%s %s)", entryPoint.getName(), entryPoint.getHttpMethod(), entryPoint.getPathPattern());
                long numberOfCalls = counter.getNoOfCallsPerEntryPoint().get(entryPoint).longValue();
                double price = BigDecimal.valueOf(entryPoint.getPricePerCall()).doubleValue();
                double total = BigDecimal.valueOf(entryPoint.getPricePerCall() * counter.getNoOfCallsPerEntryPoint().get(entryPoint).longValue() / 1000).doubleValue();
                results.add(new ApiUsageReport(name, numberOfCalls, price, total));
            }
        }

        if (counter.getNotMatchingCalls() > 0) {
            results.add(new ApiUsageReport("Other Calls", counter.getNotMatchingCalls(), 0, 0));
        }

        if (counter.getFreeCalls() > 0) {
            results.add(new ApiUsageReport("All Calls", counter.getFreeCalls(), 0, 0));
        }

        if(counter.getMonthlyBilledCalls() > 0) {
            int diffMonth = getMonthDifference(apiUsageDTO.from, apiUsageDTO.until);
            ApiUsageReport apiUsageReportForMonthlyFlatrate = new ApiUsageReport(
                    "All Calls",
                    counter.getMonthlyBilledCalls(),
                    apiUsageDTO.service.getMonthlyFee(),
                    apiUsageDTO.service.getMonthlyFee() * diffMonth
            );
            results.add(apiUsageReportForMonthlyFlatrate);
        }
        return results;
    }

    private int getMonthDifference(Date from, Date until) {
        Calendar startCalendar = new GregorianCalendar();
        startCalendar.setTime(from);
        Calendar endCalendar = new GregorianCalendar();
        endCalendar.setTime(until);
        int diffYear = endCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR);
        int diffMonth = diffYear * 12 + endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH);
        return diffMonth;
    }
}
