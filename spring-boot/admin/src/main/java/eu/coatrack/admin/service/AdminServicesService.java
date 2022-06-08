package eu.coatrack.admin.service;

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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminServicesService {

    private static final String ADMIN_SERVICES_LIST_VIEW = "admin/services/list";
    private static final String ADMIN_SERVICES_VIEW = "admin/services/service";
    private static final String ADMIN_SERVICE_EDITOR = "admin/services/edit";
    private static final String ADMIN_SERVICE_CREATE = "admin/services/create";
    private static final String ADMIN_SERVICES_LIST_VIEW_FOR_CONSUMER = "admin/services/consumer/list";
    private static final String ADMIN_SERVICE_COVER_EDITOR = "admin/services/serviceCover";

    private static final String TOTAL_MAP_KEY = "Total";

    private static final int ADMIN_SERVICE_ADD_MODE = 0;

    private static final int ADMIN_SERVICE_UPDATE_MODE = 1;

    @Autowired
    private ServiceApiRepository serviceApiRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ProxyRepository proxyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminProxiesService proxyService;

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


    public ModelAndView serviceListPage() {
        ModelAndView mav = new ModelAndView();

        mav.addObject("services", serviceApiRepository.findByDeletedWhen(null));
        mav.setViewName(ADMIN_SERVICES_LIST_VIEW);
        return mav;
    }

    public Iterable<ServiceApi> serviceListPageRest() {
        log.info("serviceListPageRest:" + serviceApiRepository.findByDeletedWhen(null));
        return serviceApiRepository.findAll();
    }

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

    public ModelAndView updateServiceCoverForm(long id) {
        log.debug("Update Service");

        ModelAndView mav = new ModelAndView();
        mav.addObject("service", serviceApiRepository.findById(id).orElse(null));

        mav.setViewName(ADMIN_SERVICE_COVER_EDITOR);
        return mav;
    }

    public ModelAndView updateServiceForm(long id) {
        log.debug("Update Service");

        ModelAndView mav = new ModelAndView();
        mav.addObject("service", serviceApiRepository.findById(id).orElse(null));
        mav.addObject("mode", ADMIN_SERVICE_UPDATE_MODE);
        mav.addObject("serviceAccessPermissionPolicies", ServiceAccessPermissionPolicy.values());
        mav.addObject("serviceAccessPaymentPolicies", ServiceAccessPaymentPolicy.values());

        mav.setViewName(ADMIN_SERVICE_EDITOR);
        return mav;
    }

    public ModelAndView newServiceSubmit(ServiceApi serviceApi) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());

        createServiceAction.setServiceApi(serviceApi);
        createServiceAction.setUser(user);
        createServiceAction.execute();

        return serviceListPage();
    }

    public ModelAndView tryUpdateService(ServiceApi service) {
        log.debug("Update service: " + service.toString());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        ServiceApi serviceStored = serviceApiRepository.findById(service.getId()).orElse(null);
        ModelAndView mav = null;

        if (serviceStored != null) {
            boolean theProxyConfigNeedsToBeUpdated = updateService(service, serviceStored, user);
            try {
                if (theProxyConfigNeedsToBeUpdated) {
                    updateProxyForService(service, serviceStored);
                }
            } catch (IOException | URISyntaxException | GitAPIException e) {
                log.error("Error when trying to transmit config to git repository: ", e);
                mav = updateFormWithErrorMessage(serviceStored, "updateProxyGitError");
            }
        } else
            log.debug("the service '{}' was updated, but the changes are not relevant for the related prox(y/ies)", serviceStored);

        return mav == null ? serviceListPage() : null;
    }

    private boolean updateService(ServiceApi updatedService, ServiceApi serviceStored, User user) {
        serviceStored.setDescription(updatedService.getDescription());
        serviceStored.setName(updatedService.getName());
        serviceStored.setLocalUrl(updatedService.getLocalUrl());
        serviceStored.setUriIdentifier(updatedService.getUriIdentifier());
        serviceStored.setServiceAccessPermissionPolicy(updatedService.getServiceAccessPermissionPolicy());
        serviceStored.setServiceAccessPaymentPolicy(updatedService.getServiceAccessPaymentPolicy());

        if (serviceStored.getServiceAccessPaymentPolicy().equals(ServiceAccessPaymentPolicy.WELL_DEFINED_PRICE)) {
            serviceStored.setEntryPoints(updatedService.getEntryPoints());
        } else if (serviceStored.getServiceAccessPaymentPolicy().equals(ServiceAccessPaymentPolicy.MONTHLY_FEE)) {
            serviceStored.setMonthlyFee(updatedService.getMonthlyFee());
        }

        updateServiceAction.setServiceApi(serviceStored);
        updateServiceAction.setUser(user);
        updateServiceAction.execute();

        return !((serviceStored.getLocalUrl() != null) && serviceStored.getLocalUrl().equals(updatedService.getLocalUrl())
                && serviceStored.getUriIdentifier().equals(updatedService.getUriIdentifier()));
    }

    private ModelAndView updateProxyForService(ServiceApi updatedService, ServiceApi serviceStored) throws GitAPIException, IOException, URISyntaxException {
        Iterable<Proxy> proxyList = proxyRepository.customSearchByServiceApiId(updatedService.getId());
        ModelAndView mav = null;
        // transmit config changes to config server git repo
        for (Proxy proxy : proxyList) {
            proxyService.transmitConfigChangesToGitConfigRepository(proxy);
        }

        Map<Proxy, String> errorsOccuredWhenUpdatingProxy = new HashMap<>();
        for (Proxy proxy : proxyList) {
            try {
                proxyService.informProxyAboutUpdatedConfiguration(proxy);
            } catch (Exception e) {
                log.error("Error when trying to call 'refresh' on the proxy: ", e);
                errorsOccuredWhenUpdatingProxy.put(proxy, e.getMessage());
            }
        }

        if (!errorsOccuredWhenUpdatingProxy.isEmpty()) {
            StringBuilder errorMessageUI = new StringBuilder();
            for (Proxy proxy : errorsOccuredWhenUpdatingProxy.keySet()) {
                String errorMessage = String.format("Refresh on '%s' (public url: '%s') failed: %s; ", proxy.getName(), proxy.getPublicUrl(), errorsOccuredWhenUpdatingProxy.get(proxy));
                errorMessageUI.append(errorMessage);
            }
            mav = updateFormWithErrorMessage(serviceStored, "updateProxyRefreshError", errorMessageUI.toString());
        }
        return mav;
    }

    private ModelAndView updateFormWithErrorMessage(ServiceApi serviceStored, String errorMessageKey) {
        return updateFormWithErrorMessage(serviceStored, errorMessageKey, null);
    }

    // Someone reconigze this code, please contact to @perezdf
    // TODO the following method puts the error message into the form, however it is never displayed to the user, as this code is called via AJAX
    //          so we should either replace AJAX with normal form submit OR send these error messages as AJAX response rather than via ModelAndView
    private ModelAndView updateFormWithErrorMessage(ServiceApi serviceStored, String errorMessageKey, String errorMessageText) {
        ModelAndView updateForm = updateServiceForm(serviceStored.getId());
        updateForm.addObject("errorMessageKey", errorMessageKey);
        if (errorMessageText != null && !errorMessageText.isEmpty()) {
            log.debug("errors during proxy update(s): " + errorMessageText);
            updateForm.addObject("errorMessageText", "Error Details: " + errorMessageText);
        }
        return updateForm;
    }

    public String get(long id, Model model, LocalDate selectedTimePeriodStart, LocalDate selectedTimePeriodEnd) {
        if (selectedTimePeriodStart != null && selectedTimePeriodEnd != null) {
            session.setDashboardDateRangeStart(selectedTimePeriodStart);
            session.setDashboardDateRangeEnd(selectedTimePeriodEnd);
        }

        List<MetricsAggregation> metricsForThisService = metricsAggregationRepo.getSummarizedMetricsByServiceIdAndDateRange(
                id, session.getDashboardDateRangeStart(), session.getDashboardDateRangeEnd());

        model.addAttribute("service", serviceApiRepository.findById(id).orElse(null));
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

    public Map<String, Map<String, Map<String, Map<String, Long>>>> get(long id, LocalDate selectedTimePeriodStart, LocalDate selectedTimePeriodEnd) {
        List<MetricsAggregation> metricsForThisService = metricsAggregationRepo.getSummarizedMetricsByServiceId(id);
        return buildMapOfStatisticsPerApiConsumer(metricsForThisService);
    }

    private Map<String, Map<String, Map<String, Map<String, Long>>>> buildMapOfStatisticsPerApiConsumer(List<MetricsAggregation> metricsForThisService) {
        Map<String, Map<String, Map<String, Map<String, Long>>>> statsPerApiConsumer = new TreeMap<>();

        metricsForThisService.forEach(metric -> {
            String userKey = metric.getApiUser().replaceAll(" ", "_");
            statsPerApiConsumer.putIfAbsent(userKey, new TreeMap<>());
            Map<String, Map<String, Map<String, Long>>> statsPerRequestMethod = statsPerApiConsumer.get(userKey);
            statsPerRequestMethod.putIfAbsent(metric.getRequestMethod(), new TreeMap<>());
            Map<String, Map<String, Long>> statsPerResponseCode = statsPerRequestMethod.get(metric.getRequestMethod());

            if (metric.getType().equals(MetricType.RESPONSE) || metric.getType().equals(MetricType.EMPTY_RESPONSE)) {
                statsPerResponseCode.putIfAbsent(metric.getHttpResponseCode().toString(), new TreeMap<>());
                Map<String, Long> statsPerPath = statsPerResponseCode.get(metric.getHttpResponseCode().toString());
                statsPerPath.merge(metric.getPath(), metric.getCount(), Long::sum);

                // Total per response key, summing up over all paths
                statsPerPath.merge(TOTAL_MAP_KEY, metric.getCount(), Long::sum);

            } else if (metric.getType().equals(MetricType.AUTHORIZED_REQUEST)) {
                // This is the request count, so it is also "the total count of all responses"
                statsPerResponseCode.putIfAbsent(TOTAL_MAP_KEY, new TreeMap<>());
                Map<String, Long> statsPerPath = statsPerResponseCode.get(TOTAL_MAP_KEY);
                statsPerPath.merge(metric.getPath(), metric.getCount(), Long::sum);
                // Total per Method, summing up over all paths and all response codes
                statsPerPath.merge(TOTAL_MAP_KEY, metric.getCount(), Long::sum);
            }
        });
        return statsPerApiConsumer;
    }


    private Map<String, Long> buildMapOfTotalsPerApiConsumer(List<MetricsAggregation> metricsForThisService) {
        Map<String, Long> totalsPerConsumer = new TreeMap<>();

        metricsForThisService.forEach(metric -> {
            if (metric.getType().equals(MetricType.AUTHORIZED_REQUEST)) {
                String userKey = metric.getApiUser().replaceAll(" ", "_");
                totalsPerConsumer.merge(userKey, metric.getCount(), Long::sum);
            }
        });
        return totalsPerConsumer;
    }

    private Map<String, Long> buildMapOfTotalsPerResponseCode(List<MetricsAggregation> metricsForThisService) {

        Map<String, Long> totalsPerResponseCode = new TreeMap<>();
        metricsForThisService.forEach(metric -> {
            if (metric.getType().equals(MetricType.RESPONSE) || metric.getType().equals(MetricType.EMPTY_RESPONSE))
                totalsPerResponseCode.merge(metric.getHttpResponseCode().toString(), metric.getCount(), Long::sum);
        });
        return totalsPerResponseCode;
    }

    private Map<String, Map<String, Map<String, Long>>> buildMapOfStatisticsPerResponseCode(List<MetricsAggregation> metricsForThisService) {
        Map<String, Map<String, Map<String, Long>>> statsPerResponseCode = new TreeMap<>();

        metricsForThisService.stream()
                // consider only response metrics
                .filter(metric -> metric.getType().equals(MetricType.RESPONSE) || metric.getType().equals(MetricType.EMPTY_RESPONSE))
                .forEach(metric -> {
                    statsPerResponseCode.putIfAbsent(metric.getHttpResponseCode().toString(), new TreeMap<>());
                    Map<String, Map<String, Long>> statsPerPath = statsPerResponseCode.get(metric.getHttpResponseCode().toString());
                    statsPerPath.putIfAbsent(metric.getRequestMethod(), new TreeMap<>());
                    Map<String, Long> statsPerRequestMethod = statsPerPath.get(metric.getRequestMethod());
                    statsPerRequestMethod.merge(metric.getPath(), metric.getCount(), Long::sum);
                    // Total per response key, summing up over all paths
                    statsPerRequestMethod.merge(TOTAL_MAP_KEY, metric.getCount(), Long::sum);
                });
        return statsPerResponseCode;
    }

    public Map<String, DoughnutChart> generateUsageStatisticsDoughnutCharts(long id) {
        Map<String, DoughnutChart> returnMap = new TreeMap<>();

        Color[] doughnutChartColors = {
                Color.LIGHT_BLUE, Color.LIGHT_GRAY, Color.LIGHT_SALMON,
                Color.RED, Color.AZURE, Color.BLACK,
                Color.GREEN, Color.GREEN_YELLOW, Color.DARK_OLIVE_GREEN};

        List<MetricsAggregation> metricsForThisService = metricsAggregationRepo.getSummarizedMetricsByServiceIdAndDateRange(
                id, session.getDashboardDateRangeStart(), session.getDashboardDateRangeEnd());

        // stats per user: build doughnut charts for each combination of user-name + method
        buildMapOfStatisticsPerApiConsumer(metricsForThisService).forEach((consumer, statsPerMethodMap) -> statsPerMethodMap.forEach((method, statsPerResponseCodeMap) -> {
            DoughnutDataset dataset = new DoughnutDataset()
                    .setLabel("HTTP response codes")
                    .addBackgroundColors(doughnutChartColors)
                    .setBorderWidth(2);
            DoughnutData data = new DoughnutData().addDataset(dataset);

            statsPerResponseCodeMap.forEach((responseCode, noOfCallsPerPathMap) -> {
                if (!responseCode.equals(TOTAL_MAP_KEY)) {
                    data.addLabel(method + " -> " + responseCode);
                    dataset.addData(noOfCallsPerPathMap.entrySet().stream()
                            .filter(callsPerPath -> !TOTAL_MAP_KEY.equals(callsPerPath.getKey()))
                            .mapToLong(Map.Entry::getValue).sum()
                    );
                }
            });
            String consumerInfo = String.format("CONSUMER_%s_%s", consumer, method);
            returnMap.put(consumerInfo, new DoughnutChart(data));
        }));

        // stats per response code: build doughnut charts for each combination of responseCode + method
        buildMapOfStatisticsPerResponseCode(metricsForThisService).forEach((responseCode, statsPerMethodMap) ->
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
                    String statusInfo = String.format("STATUS_%s_%s", responseCode, method);
                    returnMap.put(statusInfo, new DoughnutChart(data));
                }));
        return returnMap;
    }

    public ServiceApi getByIdRest(long id) {
        log.info("getById " + id + " proxy:" + serviceApiRepository.findById(id).orElse(null));
        return serviceApiRepository.findById(id).orElse(null);
    }

    public Iterable<Proxy> getProxiesByServiceRest(long id) {
        log.info("getProxiesByServiceRest " + id);
        return proxyRepository.customSearchByServiceApiId(id);
    }

    public Iterable<ServiceApi> deleteRest(long id) {
        // TODO: Consider the idea to change by a Custom Query or Delete on Cascade
        ServiceApi serviceApi = serviceApiRepository.findById(id).orElse(null);
        if (serviceApi != null) {
            serviceApi.setDeletedWhen(new Date());
            serviceApiRepository.save(serviceApi);

            Iterable<Proxy> proxies = proxyRepository.customSearchByServiceApiId(id);
            for (Proxy proxy : proxies) {
                proxy.getServiceApis().remove(serviceApi);
                proxyRepository.save(proxy);
            }
        }
        return serviceListPageRest();
    }

    public ModelAndView showToLoggedInConsumerTheListOfPublicServicesByOtherProviders() {
        ModelAndView mav = new ModelAndView();

        List<ApiKey> userApiKeys = apiKeyRepository.findByLoggedInAPIConsumer();
        List<Long> idListOfServicesUserCanAccess = userApiKeys.stream().map(ApiKey::getServiceApi).map(ServiceApi::getId).collect(Collectors.toList());

        mav.addObject("loggedInUser", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        mav.addObject("idsOfServicesUserCanAccess", idListOfServicesUserCanAccess);
        mav.addObject("services", serviceApiRepository.findByServiceAccessPermissionPolicyAndDeletedWhenIsNull(ServiceAccessPermissionPolicy.PUBLIC));
        mav.setViewName(ADMIN_SERVICES_LIST_VIEW_FOR_CONSUMER);
        return mav;
    }

    public ModelAndView createOwnApiKey(Long selectedServiceId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());

        // get selected service
        ServiceApi selectedService = serviceApiRepository.findById(selectedServiceId).orElse(null);

        // create API key for logged in user and selected service
        log.debug("creating new api key for user {} and service {}", user, selectedService);
        createApiKeyAction.setServiceApi(selectedService);
        createApiKeyAction.setUser(user);
        createApiKeyAction.execute();

        // show the updated list of public services to the logged in consumer user
        return showToLoggedInConsumerTheListOfPublicServicesByOtherProviders();
    }
}
