package eu.coatrack.admin.service;

import eu.coatrack.admin.model.repository.MetricsAggregationCustomRepository;
import eu.coatrack.api.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.AntPathMatcher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static eu.coatrack.api.ServiceAccessPaymentPolicy.WELL_DEFINED_PRICE;

@Slf4j
@AllArgsConstructor
public class ApiUsageCalculator {
    enum CallType {
        MATCHING, NOT_MATCHING, FREE, MONTHLY,
    }

    private final Map<CallType, AtomicLong> generalNumberOfCalls = new HashMap<>();
    private final Map<EntryPoint, AtomicLong> noOfCallsPerEntryPoint = new TreeMap<>();
    private final ServiceApi service;
    private final AntPathMatcher parser = new AntPathMatcher();
    private final List<ApiUsageReport> results = new ArrayList<>();
    private final Date from;
    private final Date until;

    @Autowired
    private MetricsAggregationCustomRepository metricsAggregationCustomRepository;

    public ApiUsageCalculator(ServiceApi serviceApi, Date from, Date until) {
        this.service = serviceApi;
        this.from = from;
        this.until = until;
        generalNumberOfCalls.put(CallType.NOT_MATCHING, new AtomicLong(0));
        generalNumberOfCalls.put(CallType.FREE, new AtomicLong(0));
        generalNumberOfCalls.put(CallType.MONTHLY, new AtomicLong(0));

        for (EntryPoint entryPoint : serviceApi.getEntryPoints()) {
            noOfCallsPerEntryPoint.put(entryPoint, new AtomicLong(0L));
        }
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

    public void countCalls(List metricResults, boolean onlyPaidCalls) {
        metricResults.forEach(metricResult -> {
            MetricResult metric = metricResultFromObjArray((Object[]) metricResult);
            log.debug("Metric for report: user '{}' service '{}' type '{}' calls '{}' path '{}' method '{}'", metric.getUsername(), metric.getServiceId(), metric.getType(), metric.getCallsPerEntry(), metric.getPath(), metric.getRequestMethod());
            if (metric.getType() == MetricType.RESPONSE) {
                if (service.getServiceAccessPaymentPolicy() == ServiceAccessPaymentPolicy.FOR_FREE) {
                    generalNumberOfCalls.get(CallType.FREE).addAndGet(metric.getCallsPerEntry());
                } else if (service.getServiceAccessPaymentPolicy() == ServiceAccessPaymentPolicy.MONTHLY_FEE) {
                    generalNumberOfCalls.get(CallType.MONTHLY).addAndGet(metric.getCallsPerEntry());
                } else if (service.getServiceAccessPaymentPolicy() == WELL_DEFINED_PRICE) {
                    boolean entryPointMatching = matchesEntryPoint(metric);
                    if (!entryPointMatching && !onlyPaidCalls) {
                        generalNumberOfCalls.get(CallType.NOT_MATCHING).addAndGet(metric.getCallsPerEntry());
                    }
                }
            }
        });
    }

    private boolean matchesEntryPoint(MetricResult metric) {
        for (EntryPoint entryPoint : service.getEntryPoints()) {
            if (entryPoint.getPathPattern() != null && entryPoint.getHttpMethod() != null) {
                boolean pathMatches = parser.match(entryPoint.getPathPattern(), metric.getPath());
                if (pathMatches) {
                    if (entryPoint.getHttpMethod().equals(metric.getRequestMethod()) || entryPoint.getHttpMethod().equals("*")) {
                        noOfCallsPerEntryPoint.get(entryPoint).addAndGet(metric.getCallsPerEntry());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public List<ApiUsageReport> getReports() {
        if (!noOfCallsPerEntryPoint.isEmpty())
            sumUpCallsWithMatchingRules();

        if (generalNumberOfCalls.get(CallType.NOT_MATCHING).get() > 0)
            sumUpAllOtherCalls();

        if (generalNumberOfCalls.get(CallType.FREE).get() > 0)
            sumUpAllFreeCalls();

        if (generalNumberOfCalls.get(CallType.MONTHLY).get() > 0)
            sumUpCallsWithMonthlyFlatrate();
        return results;
    }

    private void sumUpCallsWithMatchingRules() {
        for (EntryPoint entryPoint : service.getEntryPoints()) {
            String name = String.format("%s (%s %s)", entryPoint.getName(), entryPoint.getHttpMethod(), entryPoint.getPathPattern());
            long numberOfCalls = noOfCallsPerEntryPoint.get(entryPoint).longValue();
            double price = BigDecimal.valueOf(entryPoint.getPricePerCall()).doubleValue();
            double total = BigDecimal.valueOf(entryPoint.getPricePerCall() * noOfCallsPerEntryPoint.get(entryPoint).longValue() / 1000).doubleValue();

            results.add(new ApiUsageReport(name, numberOfCalls, price, total));
        }
    }

    private void sumUpAllOtherCalls() {
        long numberOfCalls = generalNumberOfCalls.get(CallType.NOT_MATCHING).get();
        results.add(new ApiUsageReport("Other Calls", numberOfCalls, 0, 0));
    }

    private void sumUpAllFreeCalls() {
        long numberOfCalls = generalNumberOfCalls.get(CallType.FREE).get();
        results.add(new ApiUsageReport("All Calls", numberOfCalls, 0, 0));
    }

    private void sumUpCallsWithMonthlyFlatrate() {
        Calendar startCalendar = new GregorianCalendar();
        startCalendar.setTime(from);
        Calendar endCalendar = new GregorianCalendar();
        endCalendar.setTime(until);

        int diffYear = endCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR);
        int diffMonth = diffYear * 12 + endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH);
        long numberOfCalls = generalNumberOfCalls.get(CallType.MONTHLY).get();

        ApiUsageReport apiUsageReportForMonthlyFlatrate = new ApiUsageReport("All Calls", numberOfCalls, service.getMonthlyFee(), service.getMonthlyFee() * diffMonth);
        results.add(apiUsageReportForMonthlyFlatrate);
    }

    public List<ApiUsageReport> calculateForSpecificService(Long apiConsumerId, boolean onlyPaidCalls) {
        // TODO needs to be typed, raw use for implicit types are no fun
        List metricResult = metricsAggregationCustomRepository.getUsageApiConsumer(MetricType.RESPONSE, service.getId(), service.getOwner().getUsername(), apiConsumerId, from, until, true);
        List<ApiUsageReport> apiUsageReports = new ArrayList<>();
        if (metricResult != null && !metricResult.isEmpty()) {
            countCalls(metricResult, onlyPaidCalls);
            apiUsageReports = getReports();
            apiUsageReports.forEach(reportRow -> log.debug("row for report: " + reportRow));
        }
        return apiUsageReports;

    }
}
