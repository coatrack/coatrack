package eu.coatrack.admin.service;

import eu.coatrack.admin.model.repository.ApiKeyRepository;
import eu.coatrack.admin.model.repository.MetricsAggregationCustomRepository;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.api.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

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
    enum CallType {
        MATCHING,
        NOT_MATCHING,
        FREE,
        MONTHLY,
    }

    private static final String REPORT_VIEW = "admin/reports/report";

    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private ServiceApiRepository serviceApiRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MetricsAggregationCustomRepository metricsAggregationCustomRepository;

    private final AntPathMatcher parser = new AntPathMatcher();

    public ModelAndView report() {
        return report(null, null, -1L, -1L, false);
    }

    public ModelAndView report(String dateFrom, String dateUntil, Long selectedServiceId, Long selectedApiConsumerUserId, boolean isOnlyPaidCalls) {
        Date dateFromDate = new Date();
        Date dateUntilDate = new Date();

        if (dateFrom != null && dateUntil != null) {
            try {
                dateFromDate = df.parse(dateFrom);
                dateUntilDate = df.parse(dateUntil);
            } catch (ParseException ex) {
                ex.printStackTrace();
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User exportUser = userRepository.findByUsername(auth.getName());

        // Gets all the consumers of the services of the Authenticated User
        List<ServiceApi> servicesProvidedByLoggedInUser = serviceApiRepository.findByDeletedWhen(null);
        List<User> serviceConsumers = servicesProvidedByLoggedInUser.stream()
                .flatMap(api -> api.getApiKeys().stream())
                .map(ApiKey::getUser)
                .distinct()
                .collect(Collectors.toList());

        List<String> payPerCallServicesIds = getPayPerCallServicesIds(servicesProvidedByLoggedInUser);

        ModelAndView mav = new ModelAndView();

        mav.setViewName(REPORT_VIEW);

        mav.addObject("services", serviceApiRepository.findByDeletedWhen(null));
        mav.addObject("users", serviceConsumers);
        mav.getModel().put("dateFrom", df.format(dateFromDate));
        mav.getModel().put("dateUntil", df.format(dateUntilDate));
        mav.addObject("selectedServiceId", selectedServiceId);
        mav.addObject("selectedApiConsumerUserId", selectedApiConsumerUserId);
        mav.addObject("serviceApiSelectedForReport", (selectedServiceId == -1L) ? null : serviceApiRepository.findById(selectedServiceId).orElse(null));
        mav.addObject("consumerUserSelectedForReport", (selectedApiConsumerUserId == -1L) ? null : userRepository.findById(selectedApiConsumerUserId).orElse(null));
        mav.addObject("payPerCallServicesIds", payPerCallServicesIds);
        mav.addObject("exportUser", exportUser);
        mav.addObject("isReportForConsumer", false);
        mav.addObject("isOnlyPaidCalls", isOnlyPaidCalls);

        return mav;
    }

    public DataTableView<ApiUsageReport> reportApiUsage(String dateFrom, String dateUntil, Long selectedServiceId, Long apiConsumerId, boolean onlyPaidCalls) {
        ServiceApi serviceApi = serviceApiRepository.findById(selectedServiceId).orElse(null);
        List<ApiUsageReport> result;
        Date dateFromDate;
        Date dateUntilDate;
        try {
            if (serviceApi != null) {
                dateFromDate = df.parse(dateFrom);
                dateUntilDate = df.parse(dateUntil);
                result = calculateApiUsageReportForSpecificService(serviceApi, apiConsumerId, dateFromDate, dateUntilDate, onlyPaidCalls);
            } else {
                result = new ArrayList<>();
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        DataTableView<ApiUsageReport> table = new DataTableView<>();
        table.setData(result);
        return table;
    }

    private List<String> getPayPerCallServicesIds(List<ServiceApi> serviceApis) {
        List<String> payPerCallServicesIds = new ArrayList<>();
        if (!serviceApis.isEmpty()) {
            payPerCallServicesIds = serviceApis
                    .stream()
                    .filter(serviceApi -> serviceApi.getServiceAccessPaymentPolicy().equals(WELL_DEFINED_PRICE))
                    .map(ServiceApi::getId)
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        }
        return payPerCallServicesIds;
    }

    public double calculateTotalRevenueForApiProvider(String apiProviderUsername, LocalDate timePeriodStart, LocalDate timePeriodEnd) {
        List<ApiUsageReport> apiUsageReportsForAllOfferedServices = new ArrayList<>();
        List<ServiceApi> offeredServices = serviceApiRepository.findByOwnerUsername(apiProviderUsername);
        for (ServiceApi service : offeredServices) {
            List<ApiUsageReport> calculatedApiUsage = calculateApiUsageReportForSpecificService(
                    service,
                    -1L, // for all consumers
                    java.sql.Date.valueOf(timePeriodStart),
                    java.sql.Date.valueOf(timePeriodEnd),
                    true);
            apiUsageReportsForAllOfferedServices.addAll(calculatedApiUsage);
        }
        double total = apiUsageReportsForAllOfferedServices.stream().mapToDouble(ApiUsageReport::getTotal).sum();

        return total;
    }

    public List<ApiUsageReport> calculateApiUsageReportForSpecificService(ServiceApi serviceApi, Long apiConsumerId, Date dateFromDate, Date dateUntilDate, boolean onlyPaidCalls) {
        // TODO needs to be typed, raw use for implicit types are no fun
        List metricResult = metricsAggregationCustomRepository.getUsageApiConsumer(
                MetricType.RESPONSE,
                serviceApi.getId(),
                serviceApi.getOwner().getUsername(),
                apiConsumerId,
                dateFromDate,
                dateUntilDate,
                true);


        List<ApiUsageReport> result = new ArrayList<>();
        Map<CallType, AtomicLong> numberOfCalls = new HashMap<>();
        numberOfCalls.put(CallType.NOT_MATCHING, new AtomicLong(0));
        numberOfCalls.put(CallType.FREE, new AtomicLong(0));
        numberOfCalls.put(CallType.MONTHLY, new AtomicLong(0));

        Map<EntryPoint, AtomicLong> noOfCallsPerEntryPoint = new TreeMap<>();

        for (EntryPoint entryPoint : serviceApi.getEntryPoints()) {
            noOfCallsPerEntryPoint.put(entryPoint, new AtomicLong(0L));
        }

        if (metricResult != null && !metricResult.isEmpty()) {
            // For Each Metric
            metricResult.forEach((metricResultEntry) -> {

                Object[] item = (Object[]) metricResultEntry;

                String username = (String) item[0];
                Long serviceId = (Long) item[1];
                MetricType metricType = (MetricType) item[2];
                Long callsPerEntry = (Long) item[3];
                String path = (String) item[4];
                String requestMethod = (String) item[5];
                BigDecimal price = new BigDecimal(0);
                BigDecimal priceUnit = new BigDecimal(0);

                log.debug("Metric for report: user '{}' service '{}' type '{}' calls '{}' path '{}' method '{}'",
                        username, serviceId, metricType, callsPerEntry, path, requestMethod);

                if (metricType == MetricType.RESPONSE) {

                    if (serviceApi.getServiceAccessPaymentPolicy() == ServiceAccessPaymentPolicy.FOR_FREE) {
                        numberOfCalls.get(CallType.FREE).addAndGet(callsPerEntry);
                    } else if (serviceApi.getServiceAccessPaymentPolicy() == ServiceAccessPaymentPolicy.MONTHLY_FEE) {
                        numberOfCalls.get(CallType.MONTHLY).addAndGet(callsPerEntry);
                    } else if (serviceApi.getServiceAccessPaymentPolicy() == WELL_DEFINED_PRICE) {
                        boolean anEntryPointWasMatched = false;

                        for (EntryPoint entryPoint : serviceApi.getEntryPoints()) {
                            if (entryPoint.getPathPattern() != null && entryPoint.getHttpMethod() != null) {

                                boolean pathMatches = parser.match(entryPoint.getPathPattern(), path);

                                if (pathMatches) {
                                    if (entryPoint.getHttpMethod().equals(requestMethod) || entryPoint.getHttpMethod().equals("*")) {

                                        noOfCallsPerEntryPoint.get(entryPoint).addAndGet(callsPerEntry);
                                        anEntryPointWasMatched = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (!anEntryPointWasMatched && !onlyPaidCalls) {
                            numberOfCalls.get(CallType.NOT_MATCHING).addAndGet(callsPerEntry);
                        }
                    }
                }
            });

            if (!noOfCallsPerEntryPoint.isEmpty())
                sumUpCallsWithMatchingRules(serviceApi, result, noOfCallsPerEntryPoint);

            if (numberOfCalls.get(CallType.NOT_MATCHING).get() > 0)
                sumUpAllOtherCalls(result, numberOfCalls.get(CallType.NOT_MATCHING).get());

            if (numberOfCalls.get(CallType.FREE).get() > 0)
                sumUpAllFreeCalls(result, numberOfCalls.get(CallType.FREE).get());

            if (numberOfCalls.get(CallType.MONTHLY).get() > 0)
                sumUpCallsWithMonthlyFlatrate(serviceApi, result, numberOfCalls.get(CallType.MONTHLY).get(), dateFromDate, dateUntilDate);
        }
        result.forEach(reportRow -> log.debug("row for report: " + reportRow));
        return result;
    }

    private void sumUpCallsWithMatchingRules(ServiceApi service, List<ApiUsageReport> reports, Map<EntryPoint, AtomicLong> noOfCallsPerEntryPoint) {
        for (EntryPoint entryPoint : service.getEntryPoints()) {
            String name = String.format("%s (%s %s)", entryPoint.getName(), entryPoint.getHttpMethod(), entryPoint.getPathPattern());
            long numberOfCalls = noOfCallsPerEntryPoint.get(entryPoint).longValue();
            double price = BigDecimal.valueOf(entryPoint.getPricePerCall()).doubleValue();
            double total = BigDecimal.valueOf(entryPoint.getPricePerCall() * noOfCallsPerEntryPoint.get(entryPoint).longValue() / 1000).doubleValue();

            reports.add(new ApiUsageReport(name, numberOfCalls, price, total));
        }
    }

    private void sumUpAllOtherCalls(List<ApiUsageReport> reports, long numberOfCalls) {
        reports.add(new ApiUsageReport("Other Calls", numberOfCalls, 0, 0));
    }

    private void sumUpAllFreeCalls(List<ApiUsageReport> reports, long numberOfCalls) {
        reports.add(new ApiUsageReport("All Calls", numberOfCalls, 0, 0));
    }

    private void sumUpCallsWithMonthlyFlatrate(ServiceApi service, List<ApiUsageReport> reports, long numberOfCalls, Date fromDate, Date untilDate) {
        Calendar startCalendar = new GregorianCalendar();
        startCalendar.setTime(fromDate);
        Calendar endCalendar = new GregorianCalendar();
        endCalendar.setTime(untilDate);

        int diffYear = endCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR);
        int diffMonth = diffYear * 12 + endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH);

        ApiUsageReport apiUsageReportForMonthlyFlatrate = new ApiUsageReport("All Calls", numberOfCalls, service.getMonthlyFee(), service.getMonthlyFee() * diffMonth);
        reports.add(apiUsageReportForMonthlyFlatrate);
    }


    public ModelAndView showGenerateReportPageForServiceConsumer() {
        return searchReportsByServicesConsumed(null, null, -1L,  false);
    }

    // deleted parameter selectedApiConsumerUserId because it has no use inside method
    public ModelAndView searchReportsByServicesConsumed(String dateFrom, String dateUntil, Long selectedServiceId, boolean isOnlyPaidCalls) {

        Date dateFromDate = new Date();
        Date dateUntilDate = new Date();

        if (dateFrom != null && dateUntil != null) {
            try {
                dateFromDate = df.parse(dateFrom);
                dateUntilDate = df.parse(dateUntil);
            } catch (ParseException ex) {
                ex.printStackTrace();
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        List<ApiKey> apiKeysByLoggedInUser = apiKeyRepository.findByLoggedInAPIConsumer();

        List<ServiceApi> servicesThatLoggedInUserHasAKeyFor = new ArrayList<>();

        if (apiKeysByLoggedInUser != null && !apiKeysByLoggedInUser.isEmpty()) {
            servicesThatLoggedInUserHasAKeyFor = serviceApiRepository.findByApiKeyList(apiKeysByLoggedInUser);
        }

        List <String> payPerCallServicesIds = getPayPerCallServicesIds(servicesThatLoggedInUserHasAKeyFor);

        ModelAndView mav = new ModelAndView();
        mav.setViewName(REPORT_VIEW);
        mav.addObject("services", servicesThatLoggedInUserHasAKeyFor);
        mav.getModel().put("dateFrom", df.format(dateFromDate));
        mav.getModel().put("dateUntil", df.format(dateUntilDate));
        mav.addObject("selectedServiceId", selectedServiceId);
        mav.addObject("selectedApiConsumerUserId", user.getId());
        mav.addObject("consumerUserSelectedForReport", user);
        mav.addObject("serviceApiSelectedForReport", (selectedServiceId == -1L) ? null : serviceApiRepository.findById(selectedServiceId).orElse(null));
        mav.addObject("payPerCallServicesIds", payPerCallServicesIds);
        mav.addObject("isReportForConsumer", true);
        mav.addObject("isOnlyPaidCalls", isOnlyPaidCalls);

        return mav;
    }



}
