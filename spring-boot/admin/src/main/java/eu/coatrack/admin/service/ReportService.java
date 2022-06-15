package eu.coatrack.admin.service;

/*-
 * #%L
 * coatrack-admin
 * %%
 * Copyright (C) 2013 - 2022 Corizon | Institut für angewandte Systemtechnik Bremen GmbH (ATB)
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

import eu.coatrack.admin.model.repository.MetricsAggregationCustomRepository;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.api.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static eu.coatrack.api.ServiceAccessPaymentPolicy.WELL_DEFINED_PRICE;


@Slf4j
@Service
public class ReportService {
    static class ApiUsageReporter {
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

        public ApiUsageReporter(ServiceApi serviceApi, Date from, Date until) {
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
         * @deprecated
         * This method is going to disappear with implementation of typesafe queries
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

    }

    private final static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceApiRepository serviceApiRepository;

    @Autowired
    private MetricsAggregationCustomRepository metricsAggregationCustomRepository;


    public Report getReport(String dateFrom, String dateUntil, Long selectedServiceId, Long selectedApiConsumerUserId, boolean isOnlyPaidCalls) {
        Date from = tryParseDateString(dateFrom);
        Date until = tryParseDateString(dateUntil);
        ServiceApi selectedService = (selectedServiceId == -1L) ? null : serviceApiRepository.findById(selectedServiceId).orElse(null);
        User selectedConsumer = (selectedApiConsumerUserId == -1L) ? null : userRepository.findById(selectedApiConsumerUserId).orElse(null);
        return new Report(isOnlyPaidCalls, false, from, until, selectedService, selectedConsumer);
    }

    public Report getReport() {
        return getReport(null, null, -1L, -1L, false);
    }

    public List<User> getServiceConsumers(List<ServiceApi> servicesProvidedByUser) {
        return servicesProvidedByUser.stream()
                .flatMap(api -> api.getApiKeys().stream())
                .map(ApiKey::getUser)
                .distinct()
                .collect(Collectors.toList());
    }

    public DataTableView<ApiUsageReport> reportApiUsage(String dateFrom, String dateUntil, Long selectedServiceId, Long apiConsumerId, boolean onlyPaidCalls) {
        ServiceApi serviceApi = serviceApiRepository.findById(selectedServiceId).orElse(null);
        Date from = tryParseDateString(dateFrom);
        Date until = tryParseDateString(dateUntil);
        List<ApiUsageReport> result;

        if (serviceApi != null) {
            result = calculateApiUsageReportForSpecificService(serviceApi, apiConsumerId, from, until, onlyPaidCalls);
        } else {
            result = new ArrayList<>();
        }

        DataTableView<ApiUsageReport> table = new DataTableView<>();
        table.setData(result);
        return table;
    }

    public List<String> getPayPerCallServicesIds(List<ServiceApi> serviceApis) {
        List<String> payPerCallServicesIds = new ArrayList<>();
        if (!serviceApis.isEmpty()) {
            payPerCallServicesIds = serviceApis.stream().filter(serviceApi -> serviceApi.getServiceAccessPaymentPolicy().equals(WELL_DEFINED_PRICE)).map(ServiceApi::getId).map(String::valueOf).collect(Collectors.toList());
        }
        return payPerCallServicesIds;
    }

    public double calculateTotalRevenueForApiProvider(String apiProviderUsername, LocalDate timePeriodStart, LocalDate timePeriodEnd) {
        List<ApiUsageReport> apiUsageReportsForAllOfferedServices = new ArrayList<>();
        List<ServiceApi> offeredServices = serviceApiRepository.findByOwnerUsername(apiProviderUsername);
        for (ServiceApi service : offeredServices) {
            List<ApiUsageReport> calculatedApiUsage = calculateApiUsageReportForSpecificService(service, -1L, // for all consumers
                    java.sql.Date.valueOf(timePeriodStart), java.sql.Date.valueOf(timePeriodEnd), true);
            apiUsageReportsForAllOfferedServices.addAll(calculatedApiUsage);
        }
        double total = apiUsageReportsForAllOfferedServices.stream().mapToDouble(ApiUsageReport::getTotal).sum();

        return total;
    }

    public List<ApiUsageReport> calculateApiUsageReportForSpecificService(ServiceApi serviceApi, Long apiConsumerId, Date from, Date until, boolean onlyPaidCalls) {
        // TODO needs to be typed, raw use for implicit types are no fun
        List metricResult = metricsAggregationCustomRepository.getUsageApiConsumer(MetricType.RESPONSE, serviceApi.getId(), serviceApi.getOwner().getUsername(), apiConsumerId, from, until, true);
        List<ApiUsageReport> result = new ArrayList<>();
        if (metricResult != null && !metricResult.isEmpty()) {
            ApiUsageReporter apiUsageReporter = new ApiUsageReporter(serviceApi, from, until);
            apiUsageReporter.countCalls(metricResult, onlyPaidCalls);
            result = apiUsageReporter.getReports();
            result.forEach(reportRow -> log.debug("row for report: " + reportRow));
        }
        return result;
    }

    //TODO this should not be here, put it somewhere senseful
    public static Date tryParseDateString(String dateString) {
        Date date = null;
        if (dateString != null) {
            try {
                date = df.parse(dateString);
            } catch (ParseException ex) {
                ex.printStackTrace();
            }
        }
        return date;
    }
}
