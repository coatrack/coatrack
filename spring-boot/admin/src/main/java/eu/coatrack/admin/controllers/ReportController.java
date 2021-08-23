package eu.coatrack.admin.controllers;

/*-
 * #%L
 * coatrack-admin
 * %%
 * Copyright (C) 2013 - 2020 Corizon | Institut für angewandte Systemtechnik Bremen GmbH (ATB)
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

import eu.coatrack.admin.model.repository.ApiKeyRepository;
import eu.coatrack.admin.model.repository.MetricsAggregationCustomRepository;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static eu.coatrack.api.ServiceAccessPaymentPolicy.WELL_DEFINED_PRICE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping(path = "/admin/reports")
public class ReportController {

    private static final String REPORT_VIEW = "admin/reports/report";

    Logger log = LoggerFactory.getLogger(ReportController.class);

    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    private List<String> getPayPerCallServicesIds(List<ServiceApi> serviceApis) {

        List<String> payPerCallServicesIds = new ArrayList<>();
        if (!serviceApis.isEmpty()) {
            payPerCallServicesIds = serviceApis.stream()
                    .filter(serviceApi -> serviceApi.getServiceAccessPaymentPolicy().equals(WELL_DEFINED_PRICE))
                    .map(ServiceApi::getId).map(idAsLong -> String.valueOf(idAsLong)).collect(Collectors.toList());
        }
        return payPerCallServicesIds;
    }

    @Autowired
    private ServiceApiRepository serviceApiRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MetricsAggregationCustomRepository metricsAggregationCustomRepository;

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ModelAndView report() {
        ModelAndView mav = report(null, null, -1L, -1L, false);

        return mav;
    }

    @RequestMapping(value = "/{dateFrom}/{dateUntil}/{selectedServiceId}/{selectedApiConsumerUserId}/{isOnlyPaidCalls}", method = RequestMethod.GET)
    public ModelAndView report(@PathVariable("dateFrom") String dateFrom, @PathVariable("dateUntil") String dateUntil,
            @PathVariable("selectedServiceId") Long selectedServiceId,
            @PathVariable("selectedApiConsumerUserId") Long selectedApiConsumerUserId,
            @PathVariable("isOnlyPaidCalls") boolean isOnlyPaidCalls) {
        Date dateFromDate = new Date();
        if (dateFrom != null) {
            try {
                dateFromDate = df.parse(dateFrom);
            } catch (ParseException ex) {
                dateFromDate = new Date();
            }
        }

        Date dateUntilDate = new Date();
        if (dateFrom != null) {
            try {
                dateUntilDate = df.parse(dateUntil);
            } catch (ParseException ex) {
                dateUntilDate = new Date();
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User exportUser = userRepository.findByUsername(auth.getName());

        // Gets all the consumers of the services of the Authenticated User
        List<ServiceApi> servicesProvidedByLoggedInUser = serviceApiRepository.findByDeletedWhen(null);
        List<User> serviceConsumers = servicesProvidedByLoggedInUser.stream().flatMap(api -> api.getApiKeys().stream())
                .map(ApiKey::getUser).distinct().collect(Collectors.toList());

        List<String> payPerCallServicesIds = getPayPerCallServicesIds(servicesProvidedByLoggedInUser);

        ModelAndView mav = new ModelAndView();

        mav.setViewName(REPORT_VIEW);

        mav.addObject("services", serviceApiRepository.findByDeletedWhen(null));
        mav.addObject("users", serviceConsumers);
        mav.getModel().put("dateFrom", df.format(dateFromDate));
        mav.getModel().put("dateUntil", df.format(dateUntilDate));
        mav.addObject("selectedServiceId", selectedServiceId);
        mav.addObject("selectedApiConsumerUserId", selectedApiConsumerUserId);
        mav.addObject("serviceApiSelectedForReport",
                (selectedServiceId == -1L) ? null : serviceApiRepository.findOne(selectedServiceId));
        mav.addObject("consumerUserSelectedForReport",
                (selectedApiConsumerUserId == -1L) ? null : userRepository.findOne(selectedApiConsumerUserId));
        mav.addObject("payPerCallServicesIds", payPerCallServicesIds);
        mav.addObject("exportUser", exportUser);
        mav.addObject("isReportForConsumer", false);
        mav.addObject("isOnlyPaidCalls", isOnlyPaidCalls);

        return mav;
    }

    @RequestMapping(value = "/apiUsage/{dateFrom}/{dateUntil}/{selectedServiceId}/{apiConsumerId}/{onlyPaidCalls}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public DataTableView reportApiUsage(@PathVariable("dateFrom") String dateFrom,
            @PathVariable("dateUntil") String dateUntil, @PathVariable("selectedServiceId") Long selectedServiceId,
            @PathVariable("apiConsumerId") Long apiConsumerId, @PathVariable("onlyPaidCalls") boolean onlyPaidCalls)
            throws IOException, ParseException {

        ServiceApi serviceApi = serviceApiRepository.findOne(selectedServiceId);
        List<ApiUsageReport> result;

        if (serviceApi != null) {

            Date dateFromDate = df.parse(dateFrom);
            Date dateUntilDate = df.parse(dateUntil);

            result = calculateApiUsageReportForSpecificService(serviceApi, apiConsumerId, dateFromDate, dateUntilDate,
                    onlyPaidCalls);

        } else {
            // in case there is no valid API selected, the result is an empty list
            result = new ArrayList<>();
        }

        DataTableView table = new DataTableView();
        table.setData(result);

        return table;
    }

    public double calculateTotalRevenueForApiProvider(String apiProviderUsername, LocalDate timePeriodStart,
            LocalDate timePeriodEnd) {

        List<ApiUsageReport> apiUsageReportsForAllOfferedServices = new ArrayList<>();
        for (ServiceApi offeredService : serviceApiRepository.findByOwnerUsername(apiProviderUsername)) {
            apiUsageReportsForAllOfferedServices.addAll(calculateApiUsageReportForSpecificService(offeredService, -1L, // for
                                                                                                                       // all
                                                                                                                       // consumers
                    java.sql.Date.valueOf(timePeriodStart), java.sql.Date.valueOf(timePeriodEnd), true));
        }
        return apiUsageReportsForAllOfferedServices.stream().mapToDouble(apiUsageReport -> apiUsageReport.getTotal())
                .sum();
    }

    AntPathMatcher parser = new AntPathMatcher();

    public List<ApiUsageReport> calculateApiUsageReportForSpecificService(ServiceApi serviceApi, Long apiConsumerId,
            Date dateFromDate, Date dateUntilDate, boolean onlyPaidCalls) {
        /**
         * ******** Retrieve the usage
         */
        List metricResult = metricsAggregationCustomRepository.getUsageApiConsumer(MetricType.RESPONSE,
                serviceApi.getId(), serviceApi.getOwner().getUsername(), apiConsumerId, dateFromDate, dateUntilDate,
                true);

        /**
         * ******** Calculate the cost
         */
        List<ApiUsageReport> result = new ArrayList<>();
        AtomicLong noOfCallsThatDoNotMatchEntryPoints = new AtomicLong(0L);
        AtomicLong noOfCallsToFreeService = new AtomicLong(0L);
        AtomicLong noOfCallsToMonthlyFlatrateService = new AtomicLong(0L);
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
                Long noCalls = (Long) item[3];
                String path = (String) item[4];
                String requestMethod = (String) item[5];
                BigDecimal price = new BigDecimal(0);
                BigDecimal priceUnit = new BigDecimal(0);

                log.debug("Metric for report: user '{}' service '{}' type '{}' calls '{}' path '{}' method '{}'",
                        username, serviceId, metricType, noCalls, path, requestMethod);

                if (metricType == MetricType.RESPONSE) {

                    if (serviceApi.getServiceAccessPaymentPolicy() == ServiceAccessPaymentPolicy.FOR_FREE) {
                        noOfCallsToFreeService.addAndGet(noCalls);
                    } else if (serviceApi.getServiceAccessPaymentPolicy() == ServiceAccessPaymentPolicy.MONTHLY_FEE) {
                        noOfCallsToMonthlyFlatrateService.addAndGet(noCalls);
                    } else if (serviceApi.getServiceAccessPaymentPolicy() == WELL_DEFINED_PRICE) {
                        boolean anEntryPointWasMatched = false;

                        for (EntryPoint entryPoint : serviceApi.getEntryPoints()) {

                            if (entryPoint.getPathPattern() != null && entryPoint.getHttpMethod() != null) {

                                boolean pathMatches = parser.match(entryPoint.getPathPattern(), path);

                                if (pathMatches) {
                                    if (entryPoint.getHttpMethod().equals(requestMethod)
                                            && !entryPoint.getHttpMethod().equals("*")
                                            || entryPoint.getHttpMethod().equals("*")) {

                                        noOfCallsPerEntryPoint.get(entryPoint).addAndGet(noCalls);
                                        anEntryPointWasMatched = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (!anEntryPointWasMatched && !onlyPaidCalls) {
                            noOfCallsThatDoNotMatchEntryPoints.addAndGet(noCalls);
                        }
                    }
                }
            });
            // Sums all the calls whom have matching rules
            if (!noOfCallsPerEntryPoint.isEmpty()) {
                for (EntryPoint entryPoint : serviceApi.getEntryPoints()) {
                    ApiUsageReport apiUsageReport = new ApiUsageReport(
                            entryPoint.getName() + " (" + entryPoint.getHttpMethod() + " " + entryPoint.getPathPattern()
                                    + ")",
                            noOfCallsPerEntryPoint.get(entryPoint).longValue(),
                            new BigDecimal(entryPoint.getPricePerCall()).doubleValue(),
                            new BigDecimal(entryPoint.getPricePerCall()
                                    * noOfCallsPerEntryPoint.get(entryPoint).longValue() / 1000).doubleValue());
                    result.add(apiUsageReport);
                }
            }
            // Sums all calls for the Other calls if they exist
            if (noOfCallsThatDoNotMatchEntryPoints.get() > 0L) {
                ApiUsageReport apiUsageReport = new ApiUsageReport("Other Calls",
                        noOfCallsThatDoNotMatchEntryPoints.longValue(), new BigDecimal(0).doubleValue(),
                        new BigDecimal(0).doubleValue());
                result.add(apiUsageReport);
            }
            // Sums all calls for free services, if any
            if (noOfCallsToFreeService.get() > 0L) {
                ApiUsageReport apiUsageReportForFreeServices = new ApiUsageReport("All Calls",
                        noOfCallsToFreeService.longValue(), new BigDecimal(0).doubleValue(),
                        new BigDecimal(0).doubleValue());
                result.add(apiUsageReportForFreeServices);
            }
            // Sums all calls for montly flatrate services, if any
            if (noOfCallsToMonthlyFlatrateService.get() > 0L) {

                Calendar startCalendar = new GregorianCalendar();
                startCalendar.setTime(dateFromDate);
                Calendar endCalendar = new GregorianCalendar();
                endCalendar.setTime(dateUntilDate);

                int diffYear = endCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR);
                int diffMonth = diffYear * 12 + endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH);

                ApiUsageReport apiUsageReportForMonthlyFlatrate = new ApiUsageReport("All Calls",
                        noOfCallsToMonthlyFlatrateService.longValue(), serviceApi.getMonthlyFee(),
                        serviceApi.getMonthlyFee() * diffMonth);
                result.add(apiUsageReportForMonthlyFlatrate);
            }
        }
        result.forEach(reportRow -> log.debug("row for report: " + reportRow));
        return result;
    }

    @RequestMapping(value = "/consumer", method = GET)
    public ModelAndView showGenerateReportPageForServiceConsumer() {
        ModelAndView mav = searchReportsByServicesConsumed(null, null, -1L, -1L, false);
        return mav;
    }

    @RequestMapping(value = "/consumer/{dateFrom}/{dateUntil}/{selectedServiceId}/{selectedApiConsumerUserId}/{isOnlyPaidCalls}", method = RequestMethod.GET)
    public ModelAndView searchReportsByServicesConsumed(@PathVariable("dateFrom") String dateFrom,
            @PathVariable("dateUntil") String dateUntil, @PathVariable("selectedServiceId") Long selectedServiceId,
            @PathVariable("selectedApiConsumerUserId") Long selectedApiConsumerUserId,
            @PathVariable("isOnlyPaidCalls") boolean isOnlyPaidCalls) {

        Date dateFromDate = new Date();
        if (dateFrom != null) {
            try {
                dateFromDate = df.parse(dateFrom);
            } catch (ParseException ex) {
                dateFromDate = new Date();
            }
        }

        Date dateUntilDate = new Date();
        if (dateFrom != null) {
            try {
                dateUntilDate = df.parse(dateUntil);
            } catch (ParseException ex) {
                dateUntilDate = new Date();
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        List<ApiKey> apiKeysByLoggedInUser = apiKeyRepository.findByLoggedInAPIConsumer();

        List<ServiceApi> servicesThatLoggedInUserHasAKeyFor = new ArrayList<>();

        if (apiKeysByLoggedInUser != null && !apiKeysByLoggedInUser.isEmpty()) {
            servicesThatLoggedInUserHasAKeyFor = serviceApiRepository.findByApiKeyList(apiKeysByLoggedInUser);
        }

        List<String> payPerCallServicesIds = getPayPerCallServicesIds(servicesThatLoggedInUserHasAKeyFor);

        ModelAndView mav = new ModelAndView();
        mav.setViewName(REPORT_VIEW);
        mav.addObject("services", servicesThatLoggedInUserHasAKeyFor);
        mav.getModel().put("dateFrom", df.format(dateFromDate));
        mav.getModel().put("dateUntil", df.format(dateUntilDate));
        mav.addObject("selectedServiceId", selectedServiceId);
        mav.addObject("selectedApiConsumerUserId", user.getId());
        mav.addObject("consumerUserSelectedForReport", user);
        mav.addObject("serviceApiSelectedForReport",
                (selectedServiceId == -1L) ? null : serviceApiRepository.findOne(selectedServiceId));
        mav.addObject("payPerCallServicesIds", payPerCallServicesIds);
        mav.addObject("isReportForConsumer", true);
        mav.addObject("isOnlyPaidCalls", isOnlyPaidCalls);

        return mav;
    }
}
