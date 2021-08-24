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

import be.ceau.chart.DoughnutChart;
import be.ceau.chart.color.Color;
import be.ceau.chart.data.DoughnutData;
import be.ceau.chart.dataset.DoughnutDataset;
import eu.coatrack.admin.UserSessionSettings;
import eu.coatrack.admin.logic.CreateApiKeyAction;
import eu.coatrack.admin.logic.CreateServiceAction;
import eu.coatrack.admin.logic.UpdateServiceAction;
import eu.coatrack.admin.model.repository.*;
import eu.coatrack.admin.model.vo.MetricsAggregation;
import eu.coatrack.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * @author gr-hovest(at)atb-bremen.de
 */
@Controller
@RequestMapping(value = "/admin/services")
public class AdminServicesController {

    private static final Logger log = LoggerFactory.getLogger(AdminServicesController.class);

    private static final String ADMIN_SERVICES_LIST_VIEW = "admin/services/list";
    private static final String ADMIN_SERVICES_VIEW = "admin/services/service";
    private static final String ADMIN_SERVICE_EDITOR = "admin/services/edit";
    private static final String ADMIN_SERVICE_CREATE = "admin/services/create";
    private static final String ADMIN_SERVICES_LIST_VIEW_FOR_CONSUMER = "admin/services/consumer/list";
    private static final String ADMIN_SERVICE_COVER_EDITOR = "admin/services/serviceCover";

    @Autowired
    private ServiceApiRepository serviceApiRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ProxyRepository proxyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminProxiesController proxyController;

    @Autowired
    private MetricsAggregationCustomRepository metricsAggregationRepo;

    @Autowired
    private UserSessionSettings session;

    @Autowired
    private CreateServiceAction createServiceAction;

    @Autowired
    private UpdateServiceAction updateServiceAction;

    @Autowired
    private CreateApiKeyAction createApiKeyAction;

    private int ADMIN_SERVICE_ADD_MODE = 0;

    private int ADMIN_SERVICE_UPDATE_MODE = 1;

    @RequestMapping(value = "", method = GET)
    public ModelAndView serviceListPage() {
        ModelAndView mav = new ModelAndView();

        mav.addObject("services", serviceApiRepository.findByDeletedWhen(null));
        mav.setViewName(ADMIN_SERVICES_LIST_VIEW);
        return mav;
    }

    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Iterable<ServiceApi> serviceListPageRest() throws IOException {
        log.info("serviceListPageRest:" + serviceApiRepository.findByDeletedWhen(null));
        return serviceApiRepository.findAll();
    }

    @GetMapping(value = "/formAdd")
    public ModelAndView newServiceForm() {
        log.debug("New Service");

        ModelAndView mav = new ModelAndView();

        ServiceApi service = new ServiceApi();

        mav.addObject("service", service);
        mav.addObject("mode", ADMIN_SERVICE_ADD_MODE);

        mav.addObject("serviceAccessPermissionPolicies", ServiceAccessPermissionPolicy.values());
        mav.addObject("serviceAccessPaymentPolicies", ServiceAccessPaymentPolicy.values());

        mav.setViewName(ADMIN_SERVICE_CREATE);
        return mav;
    }

    @GetMapping(value = "{id}/servicecover")
    public ModelAndView updateServiceCoverForm(@PathVariable("id") long id) {
        log.debug("Update Service");

        ModelAndView mav = new ModelAndView();
        mav.addObject("service", serviceApiRepository.findOne(id));

        mav.setViewName(ADMIN_SERVICE_COVER_EDITOR);
        return mav;
    }

    @GetMapping(value = "{id}/formUpdate")
    public ModelAndView updateServiceForm(@PathVariable("id") long id) {
        log.debug("Update Service");

        ModelAndView mav = new ModelAndView();
        mav.addObject("service", serviceApiRepository.findOne(id));
        mav.addObject("mode", ADMIN_SERVICE_UPDATE_MODE);

        mav.addObject("serviceAccessPermissionPolicies", ServiceAccessPermissionPolicy.values());
        mav.addObject("serviceAccessPaymentPolicies", ServiceAccessPaymentPolicy.values());

        mav.setViewName(ADMIN_SERVICE_EDITOR);
        return mav;
    }

    @PostMapping(value = "/add")
    public ModelAndView newServiceSubmit(@RequestBody ServiceApi serviceApi) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());

        createServiceAction.setServiceApi(serviceApi);
        createServiceAction.setUser(user);
        createServiceAction.execute();

        return serviceListPage();
    }

    @PostMapping(value = "/update")
    public ModelAndView updateService(@RequestBody ServiceApi service) {
        log.debug("Update service: " + service.toString());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());

        ServiceApi serviceStored = serviceApiRepository.findOne(service.getId());

        // check if the attributes relevant to the proxy config have changed
        boolean theProxyConfigNeedsToBeUpdated = !((serviceStored.getLocalUrl() != null)
                && serviceStored.getLocalUrl().equals(service.getLocalUrl())
                && serviceStored.getUriIdentifier().equals(service.getUriIdentifier()));

        serviceStored.setDescription(service.getDescription());
        serviceStored.setName(service.getName());
        serviceStored.setLocalUrl(service.getLocalUrl());
        serviceStored.setUriIdentifier(service.getUriIdentifier());
        serviceStored.setServiceAccessPermissionPolicy(service.getServiceAccessPermissionPolicy());
        serviceStored.setServiceAccessPaymentPolicy(service.getServiceAccessPaymentPolicy());

        if (serviceStored.getServiceAccessPaymentPolicy().equals(ServiceAccessPaymentPolicy.WELL_DEFINED_PRICE)) {
            serviceStored.setEntryPoints(service.getEntryPoints());
        } else if (serviceStored.getServiceAccessPaymentPolicy().equals(ServiceAccessPaymentPolicy.MONTHLY_FEE)) {
            serviceStored.setMonthlyFee(service.getMonthlyFee());
        }

        updateServiceAction.setServiceApi(serviceStored);
        updateServiceAction.setUser(user);
        updateServiceAction.execute();

        if (theProxyConfigNeedsToBeUpdated) {
            Iterable<Proxy> proxyList = proxyRepository.customSearchByServiceApiId(service.getId());

            // transmit config changes to config server git repo
            try {
                for (Proxy proxy : proxyList) {
                    proxyController.transmitConfigChangesToGitConfigRepository(proxy);
                }
            } catch (IOException | URISyntaxException | GitAPIException e) {
                log.error("Error when trying to transmit config to git repository: ", e);
                return updateFormWithErrorMessage(serviceStored, "updateProxyGitError");
            }
            // call refresh on all proxies so that it will get the latest config from git
            // (by calling the config server)
            Map<Proxy, String> errorsOccuredWhenUpdatingProxy = new HashMap<>();
            for (Proxy proxy : proxyList) {
                try {
                    proxyController.informProxyAboutUpdatedConfiguration(proxy);
                } catch (Exception e) {
                    log.error("Error when trying to call 'refresh' on the proxy: ", e);
                    errorsOccuredWhenUpdatingProxy.put(proxy, e.getMessage());
                }
            }
            if (!errorsOccuredWhenUpdatingProxy.isEmpty()) {
                String errorMessageToDisplayInGUI = "";
                for (Proxy proxy : errorsOccuredWhenUpdatingProxy.keySet()) {
                    errorMessageToDisplayInGUI += String.format("Refresh on '%s' (public url: '%s') failed: %s; ",
                            proxy.getName(), proxy.getPublicUrl(), errorsOccuredWhenUpdatingProxy.get(proxy));
                }
                return updateFormWithErrorMessage(serviceStored, "updateProxyRefreshError", errorMessageToDisplayInGUI);
            }
        } else {
            log.debug("the service '{}' was updated, but the changes are not relevant for the related prox(y/ies)",
                    serviceStored);
        }
        return serviceListPage();
    }

    private ModelAndView updateFormWithErrorMessage(ServiceApi serviceStored, String errorMessageKey) {
        return updateFormWithErrorMessage(serviceStored, errorMessageKey, null);
    }

    // Someone reconigze this code, please contact to @perezdf
    // TODO the following method puts the error message into the form, however it is
    // never displayed to the user, as this code is called via AJAX
    // so we should either replace AJAX with normal form submit OR send these error
    // messages as AJAX response rather than via ModelAndView
    private ModelAndView updateFormWithErrorMessage(ServiceApi serviceStored, String errorMessageKey,
            String errorMessageText) {
        ModelAndView updateForm = updateServiceForm(serviceStored.getId());
        updateForm.addObject("errorMessageKey", errorMessageKey);
        if (errorMessageText != null && !errorMessageText.isEmpty()) {
            log.debug("errors during proxy update(s): " + errorMessageText);
            updateForm.addObject("errorMessageText", "Error Details: " + errorMessageText);
        }
        return updateForm;
    }

    @RequestMapping(value = "{id}", method = GET)
    public String get(@PathVariable("id") long id, Model model,
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodStart,
            @RequestParam(value = "dateUntil", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodEnd) {

        if (selectedTimePeriodStart != null && selectedTimePeriodEnd != null) {
            session.setDashboardDateRangeStart(selectedTimePeriodStart);
            session.setDashboardDateRangeEnd(selectedTimePeriodEnd);
        }

        model.addAttribute("service", serviceApiRepository.findOne(id));

        List<MetricsAggregation> metricsForThisService = metricsAggregationRepo
                .getSummarizedMetricsByServiceIdAndDateRange(
                        id, session.getDashboardDateRangeStart(), session.getDashboardDateRangeEnd());
        model.addAttribute("metrics", metricsForThisService);

        model.addAttribute("statisticsPerConsumerMap", buildMapOfStatisticsPerApiConsumer(metricsForThisService));
        model.addAttribute("statisticsPerResponseCodeMap", buildMapOfStatisticsPerResponseCode(metricsForThisService));

        model.addAttribute("totalNoOfCallsPerConsumerMap", buildMapOfTotalsPerApiConsumer(metricsForThisService));
        model.addAttribute("totalNoOfCallsPerResponseCodeMap", buildMapOfTotalsPerResponseCode(metricsForThisService));

        model.addAttribute("serviceId", id);
        model.addAttribute("statisticsDateFrom", session.getDashboardDateRangeStart());
        model.addAttribute("statisticsDateUntil", session.getDashboardDateRangeEnd());

        return ADMIN_SERVICES_VIEW;
    }

    @RequestMapping(value = "{id}/statisticsPerConsumer", method = GET, produces = "application/json")
    @ResponseBody
    public Map<String, Map<String, Map<String, Map<String, Long>>>> get(
            @PathVariable("id") long id,
            @RequestParam("dateFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodStart,
            @RequestParam("dateUntil") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodEnd) {

        List<MetricsAggregation> metricsForThisService = metricsAggregationRepo.getSummarizedMetricsByServiceId(id);

        return buildMapOfStatisticsPerApiConsumer(metricsForThisService);
    }

    private static final String TOTAL_MAP_KEY = "Total";

    private Map<String, Map<String, Map<String, Map<String, Long>>>> buildMapOfStatisticsPerApiConsumer(
            List<MetricsAggregation> metricsForThisService) {

        Map<String, Map<String, Map<String, Map<String, Long>>>> statsPerApiConsumer = new TreeMap<>();
        metricsForThisService.forEach(m -> {
            String userKey = m.getApiUser().replaceAll(" ", "_");
            statsPerApiConsumer.putIfAbsent(userKey, new TreeMap<>());
            Map<String, Map<String, Map<String, Long>>> statsPerRequestMethod = statsPerApiConsumer.get(userKey);
            statsPerRequestMethod.putIfAbsent(m.getRequestMethod(), new TreeMap<>());
            Map<String, Map<String, Long>> statsPerResponseCode = statsPerRequestMethod.get(m.getRequestMethod());

            if (m.getType().equals(MetricType.RESPONSE) || m.getType().equals(MetricType.EMPTY_RESPONSE)) {
                statsPerResponseCode.putIfAbsent(m.getHttpResponseCode().toString(), new TreeMap<>());
                Map<String, Long> statsPerPath = statsPerResponseCode.get(m.getHttpResponseCode().toString());
                statsPerPath.merge(m.getPath(), m.getCount(), (countOld, countNew) -> countOld + countNew);
                // Total per response key, summing up over all paths
                statsPerPath.merge(TOTAL_MAP_KEY, m.getCount(), (countOld, countNew) -> countOld + countNew);

            } else if (m.getType().equals(MetricType.AUTHORIZED_REQUEST)) {
                // This is the request count, so it is also "the total count of all responses"
                statsPerResponseCode.putIfAbsent(TOTAL_MAP_KEY, new TreeMap<>());
                Map<String, Long> statsPerPath = statsPerResponseCode.get(TOTAL_MAP_KEY);
                statsPerPath.merge(m.getPath(), m.getCount(), (countOld, countNew) -> countOld + countNew);
                // Total per Method, summing up over all paths and all response codes
                statsPerPath.merge(TOTAL_MAP_KEY, m.getCount(), (countOld, countNew) -> countOld + countNew);
            }
        });
        return statsPerApiConsumer;
    }

    private Map<String, Long> buildMapOfTotalsPerApiConsumer(List<MetricsAggregation> metricsForThisService) {

        Map<String, Long> totalsPerConsumer = new TreeMap<>();
        metricsForThisService.forEach(m -> {
            if (m.getType().equals(MetricType.AUTHORIZED_REQUEST)) {
                String userKey = m.getApiUser().replaceAll(" ", "_");
                totalsPerConsumer.merge(userKey, m.getCount(), (countOld, countNew) -> countOld + countNew);
            }
        });
        return totalsPerConsumer;
    }

    private Map<String, Long> buildMapOfTotalsPerResponseCode(List<MetricsAggregation> metricsForThisService) {

        Map<String, Long> totalsPerResponseCode = new TreeMap<>();
        metricsForThisService.forEach(m -> {
            if (m.getType().equals(MetricType.RESPONSE) || m.getType().equals(MetricType.EMPTY_RESPONSE)) {
                totalsPerResponseCode.merge(m.getHttpResponseCode().toString(), m.getCount(),
                        (countOld, countNew) -> countOld + countNew);
            }
        });
        return totalsPerResponseCode;
    }

    private Map<String, Map<String, Map<String, Long>>> buildMapOfStatisticsPerResponseCode(
            List<MetricsAggregation> metricsForThisService) {

        Map<String, Map<String, Map<String, Long>>> statsPerResponseCode = new TreeMap<>();

        metricsForThisService.stream()
                // consider only response metrics
                .filter(m -> m.getType().equals(MetricType.RESPONSE) || m.getType().equals(MetricType.EMPTY_RESPONSE))
                .forEach(m -> {
                    statsPerResponseCode.putIfAbsent(m.getHttpResponseCode().toString(), new TreeMap<>());
                    Map<String, Map<String, Long>> statsPerPath = statsPerResponseCode
                            .get(m.getHttpResponseCode().toString());
                    statsPerPath.putIfAbsent(m.getRequestMethod(), new TreeMap<>());
                    Map<String, Long> statsPerRequestMethod = statsPerPath.get(m.getRequestMethod());
                    statsPerRequestMethod.merge(m.getPath(), m.getCount(), (countOld, countNew) -> countOld + countNew);
                    // Total per response key, summing up over all paths
                    statsPerRequestMethod.merge(TOTAL_MAP_KEY, m.getCount(),
                            (countOld, countNew) -> countOld + countNew);
                });
        return statsPerResponseCode;
    }

    @RequestMapping(value = "{id}/usageStatisticsDoughnutCharts", method = GET, produces = "application/json")
    @ResponseBody
    public Map<String, DoughnutChart> generateUsageStatisticsDoughnutCharts(@PathVariable("id") long id) {

        Map<String, DoughnutChart> returnMap = new TreeMap<>();

        Color[] doughnutChartColors = {
                Color.LIGHT_BLUE, Color.LIGHT_GRAY, Color.LIGHT_SALMON,
                Color.RED, Color.AZURE, Color.BLACK,
                Color.GREEN, Color.GREEN_YELLOW, Color.DARK_OLIVE_GREEN };

        List<MetricsAggregation> metricsForThisService = metricsAggregationRepo
                .getSummarizedMetricsByServiceIdAndDateRange(
                        id, session.getDashboardDateRangeStart(), session.getDashboardDateRangeEnd());

        // stats per user: build doughnut charts for each combination of user-name +
        // method
        buildMapOfStatisticsPerApiConsumer(metricsForThisService).forEach((consumer, statsPerMethodMap) -> {
            statsPerMethodMap.forEach((method, statsPerResponseCodeMap) -> {
                DoughnutDataset dataset = new DoughnutDataset()
                        .setLabel("HTTP response codes")
                        .addBackgroundColors(doughnutChartColors)
                        .setBorderWidth(2);
                DoughnutData data = new DoughnutData().addDataset(dataset);

                statsPerResponseCodeMap.forEach((responseCode, noOfCallsPerPathMap) -> {
                    if (!responseCode.equals(TOTAL_MAP_KEY)) {
                        data.addLabel(method + " -> " + responseCode);
                        dataset.addData(
                                noOfCallsPerPathMap.entrySet().stream()
                                        .filter(callsPerPath -> !TOTAL_MAP_KEY.equals(callsPerPath.getKey()))
                                        .collect(Collectors.summingLong(Map.Entry::getValue)));
                    }
                });
                returnMap.put("CONSUMER_" + consumer + "_" + method, new DoughnutChart(data));
            });
        });

        // stats per response code: build doughnut charts for each combination of
        // responseCode + method
        buildMapOfStatisticsPerResponseCode(metricsForThisService).forEach((responseCode, statsPerMethodMap) -> {
            statsPerMethodMap.forEach((method, callsPerPathMap) -> {
                DoughnutDataset dataset = new DoughnutDataset()
                        .setLabel("Path")
                        .addBackgroundColors(doughnutChartColors)
                        .setBorderWidth(2);
                DoughnutData data = new DoughnutData().addDataset(dataset);

                callsPerPathMap.forEach((path, noOfCalls) -> {
                    if (!path.equals(TOTAL_MAP_KEY)) {
                        data.addLabel(method + " " + path);
                        dataset.addData(noOfCalls);
                    }
                });
                returnMap.put("STATUS_" + responseCode + "_" + method, new DoughnutChart(data));
            });
        });

        return returnMap;
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ServiceApi getByIdRest(@PathVariable("id") long id) throws IOException {
        log.info("getById " + id + " proxy:" + serviceApiRepository.findOne(id));
        return serviceApiRepository.findOne(id);
    }

    @RequestMapping(value = "{id}/proxies", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Iterable<Proxy> getProxiesByServiceRest(@PathVariable("id") long id) throws IOException {
        log.info("getProxiesByServiceRest " + id);
        return proxyRepository.customSearchByServiceApiId(id);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseBody
    public Iterable<ServiceApi> deleteRest(@PathVariable("id") long id) throws IOException {

        // TODO: Consider the idea to change by a Custom Query or Delete on Cascade
        ServiceApi serviceApi = serviceApiRepository.findOne(id);
        serviceApi.setDeletedWhen(new Date());
        serviceApiRepository.save(serviceApi);

        Iterable<Proxy> proxies = proxyRepository.customSearchByServiceApiId(id);
        for (Proxy proxy : proxies) {
            proxy.getServiceApis().remove(serviceApi);
            proxyRepository.save(proxy);
        }

        return serviceListPageRest();
    }

    @RequestMapping(value = "/consumer/list", method = GET)
    public ModelAndView showToLoggedInConsumerTheListOfPublicServicesByOtherProviders() {
        ModelAndView mav = new ModelAndView();

        List<ApiKey> userApiKeys = apiKeyRepository.findByLoggedInAPIConsumer();
        List<Long> idListOfServicesUserCanAccess = userApiKeys.stream().map(ApiKey::getServiceApi)
                .map(ServiceApi::getId).collect(Collectors.toList());

        mav.addObject("loggedInUser", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        mav.addObject("idsOfServicesUserCanAccess", idListOfServicesUserCanAccess);
        mav.addObject("services", serviceApiRepository
                .findByServiceAccessPermissionPolicyAndDeletedWhenIsNull(ServiceAccessPermissionPolicy.PUBLIC));
        mav.setViewName(ADMIN_SERVICES_LIST_VIEW_FOR_CONSUMER);
        return mav;
    }

    @RequestMapping(value = "consumer/subscribe", method = POST)
    @ResponseBody
    public ModelAndView createOwnApiKey(@RequestParam Long selectedServiceId) {

        // get logged in user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());

        // get selected service
        ServiceApi selectedService = serviceApiRepository.findOne(selectedServiceId);

        // create API key for logged in user and selected service
        log.debug("creating new api key for user {} and service {}", user, selectedService);
        createApiKeyAction.setServiceApi(selectedService);
        createApiKeyAction.setUser(user);
        createApiKeyAction.execute();

        // show the updated list of public services to the logged in consumer user
        return showToLoggedInConsumerTheListOfPublicServicesByOtherProviders();
    }

}
