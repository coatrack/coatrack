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
import be.ceau.chart.LineChart;
import be.ceau.chart.color.Color;
import be.ceau.chart.data.DoughnutData;
import be.ceau.chart.data.LineData;
import be.ceau.chart.dataset.DoughnutDataset;
import be.ceau.chart.dataset.LineDataset;
import be.ceau.chart.enums.PointStyle;
import be.ceau.chart.options.LineOptions;
import be.ceau.chart.options.scales.LinearScale;
import be.ceau.chart.options.scales.LinearScales;
import be.ceau.chart.options.ticks.LinearTicks;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.coatrack.admin.UserSessionSettings;
import eu.coatrack.admin.components.WebUI;
import eu.coatrack.admin.logic.CreateApiKeyAction;
import eu.coatrack.admin.logic.CreateProxyAction;
import eu.coatrack.admin.logic.CreateServiceAction;
import eu.coatrack.admin.model.repository.*;
import eu.coatrack.admin.model.vo.*;
import eu.coatrack.admin.service.GatewayHealthMonitorService;
import eu.coatrack.admin.service.report.ReportService;
import eu.coatrack.api.*;
import eu.coatrack.config.github.GithubEmail;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.SpringVersion;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * @author Timon Veenstra <tveenstra@bebr.nl>
 * @author Bruno Silva <silva@atb-bremen.de>
 */
@Controller
@RequestMapping(value = "/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private static final String ADMIN_HOME_VIEW = "admin/dashboard";

    @Value("${ygg.admin.gettingStarted.consumer.testService.provider.username}")
    private String gettingStartedTestServiceProvider;

    @Value("${ygg.proxy.server.port.defaultValue}")
    private int proxyServerDefaultPort;

    @Value("${ygg.admin.gettingStarted.consumer.testService.uriIdentifier}")
    private String gettingStartedTestServiceIdentifier;

    private static final String ADMIN_CONSUMER_HOME_VIEW = "admin/consumer_dashboard";
    private static final String ADMIN_WIZARD_VIEW = "admin/wizard/wizard";
    private static final String ADMIN_STARTPAGE = "admin/startpage";
    private static final String ADMIN_CONSUMER_WIZARD = "admin/consumer_wizard/wizard";
    private static final String ADMIN_PROFILE = "admin/profile/profile";
    private static final String GITHUB_API_USER = "https://api.github.com/user";
    private static final String GITHUB_API_EMAIL = GITHUB_API_USER + "/emails";
    private static final String GATEWAY_HEALTH_MONITOR_FRAGMENT = "admin/fragments/gateway_health_monitor :: gateway-health-monitor";
    private static final Map<Integer, Color> chartColorsPerHttpResponseCode;

    static {
        Map<Integer, Color> colorMap = new HashMap<>();
        colorMap.put(400, Color.ORANGE);
        colorMap.put(401, Color.SALMON);
        colorMap.put(403, Color.LIGHT_YELLOW);
        colorMap.put(404, new Color(255, 255, 102)); // yellow
        colorMap.put(500, Color.RED);
        colorMap.put(503, Color.ORANGE_RED);
        colorMap.put(504, Color.DARK_RED);
        chartColorsPerHttpResponseCode = Collections.unmodifiableMap(colorMap);
    }

    /* REPOSITORIES */
    @Autowired
    private MetricsAggregationCustomRepository metricsAggregationCustomRepository;

    @Autowired
    private MetricRepository metricRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ProxyRepository proxyRepository;

    @Autowired
    private ServiceApiRepository serviceApiRepository;

    @Autowired
    private CreateProxyAction createProxyAction;

    @Autowired
    private CreateServiceAction createServiceAction;

    @Autowired
    private CreateApiKeyAction createApiKeyAction;

    /* CONTROLLERS */
    @Autowired
    private UserController userController;

    @Autowired
    private ReportService reportService;

    @Autowired
    private WebUI webUI;

    @Autowired
    private UserSessionSettings session;

    @Autowired
    private GatewayHealthMonitorService gatewayHealthMonitorService;

    @RequestMapping(value = "/profiles", method = GET)
    public ModelAndView goProfiles(Model model) throws IOException {

        ModelAndView mav = new ModelAndView();

        mav.setViewName(ADMIN_PROFILE);
        return mav;
    }

    @RequestMapping(value = "", method = GET)
    public ModelAndView home(Model model, HttpServletRequest request) throws IOException {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        ModelAndView mav = new ModelAndView();

        if (auth.isAuthenticated()) {
            User user = userRepository.findByUsername(auth.getName());

            if (user != null) {

                boolean end = false;
                boolean found = false;

                List<ServiceApi> services = serviceApiRepository.findByOwnerUsername(auth.getName());
                if (services != null && !services.isEmpty()) {
                    mav.setViewName(ADMIN_HOME_VIEW);
                    // The user is already stored in our database
                    mav.addObject("stats", loadGeneralStatistics(session.getDashboardDateRangeStart(),
                            session.getDashboardDateRangeEnd()));
                    mav.addObject("userStatistics", getStatisticsPerApiConsumerInDescendingOrderByNoOfCalls(
                            session.getDashboardDateRangeStart(),
                            session.getDashboardDateRangeEnd()));
                } else {
                    if (!user.getInitialized()) {

                        // temporalFirstTimeFlag = false;
                        mav.setViewName(ADMIN_STARTPAGE);

                    } else {
                        // IT IS A CONSUMER USER
                        mav.setViewName(ADMIN_CONSUMER_HOME_VIEW);
                    }

                }

            } else {

                // The user is new for our database therefore we try to retrieve as much user
                // info is possible from Github
                OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) auth.getDetails();

                RestTemplate restTemplate = new RestTemplate();

                HttpHeaders headers = new HttpHeaders();
                headers.add("Authorization", "token " + details.getTokenValue());
                HttpEntity<String> githubRequest = new HttpEntity<String>(headers);

                ResponseEntity<String> userInfoResponse = restTemplate.exchange(GITHUB_API_USER, HttpMethod.GET,
                        githubRequest, String.class);
                String userInfo = userInfoResponse.getBody();

                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                Map<String, Object> userMap = objectMapper.readValue(userInfo,
                        new TypeReference<Map<String, Object>>() {
                });
                String email = (String) userMap.get("email");
                if (email == null || email.isEmpty()) {

                    ResponseEntity<String> userEmailsResponse = restTemplate.exchange(GITHUB_API_EMAIL, HttpMethod.GET,
                            githubRequest, String.class);
                    String userEmails = userEmailsResponse.getBody();
                    List<GithubEmail> emailsList = objectMapper.readValue(userEmails,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, GithubEmail.class));

                    Iterator<GithubEmail> it = emailsList.iterator();
                    boolean found = false;
                    if (emailsList.size() > 0) {
                        while (!found && it.hasNext()) {
                            GithubEmail githubEmail = it.next();
                            if (githubEmail.getVerified()) {
                                email = githubEmail.getEmail();
                                found = true;
                            }
                        }
                        if (!found) {
                            email = emailsList.get(0).getEmail();
                        }
                    }
                }

                user = new User();
                user.setUsername((String) userMap.get("login"));
                user.setFirstname((String) userMap.get("name"));
                user.setCompany((String) userMap.get("company"));

                if (email != null) {
                    user.setEmail(email);
                }

                mav.addObject("user", user);

                mav.setViewName("register");
            }

        } else {
            // User is not authenticated, it is quite weird according to the Github login
            // page but I prefer to prevent the case
            String springVersion = webUI.parameterizedMessage("home.spring.version", SpringBootVersion.getVersion(),
                    SpringVersion.getVersion());
            model.addAttribute("springVersion", springVersion);
            mav.setViewName("home");
        }
        return mav;
    }

    @RequestMapping(value = "/gettingstarted", method = GET)
    public ModelAndView gettingStartedWizard(Model model) throws IOException {

        ModelAndView mav = new ModelAndView();
        mav.setViewName(ADMIN_WIZARD_VIEW);
        return mav;
    }

    @RequestMapping(value = "/dashboard/statisticsPerApiConsumerInDescendingOrderByNoOfCalls", method = GET, produces = "application/json")
    @ResponseBody
    public List<StatisticsPerApiUser> getStatisticsPerApiConsumerInDescendingOrderByNoOfCalls(
            @RequestParam("dateFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodStart,
            @RequestParam("dateUntil") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodEnd) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        List<StatisticsPerApiUser> userStatistics = metricsAggregationCustomRepository
                .getStatisticsPerApiConsumerInDescendingOrderByNoOfCalls(selectedTimePeriodStart, selectedTimePeriodEnd,
                        auth.getName());

        return userStatistics;
    }

    @RequestMapping(value = "/dashboard/userStatsDoughnutChart", method = GET, produces = "application/json")
    @ResponseBody
    public DoughnutChart generateUserStatisticsDoughnutChart(
            @RequestParam("dateFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodStart,
            @RequestParam("dateUntil") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodEnd) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        List<StatisticsPerApiUser> userStatsList = metricsAggregationCustomRepository
                .getStatisticsPerApiConsumerInDescendingOrderByNoOfCalls(selectedTimePeriodStart, selectedTimePeriodEnd,
                        auth.getName());
        DoughnutDataset dataset = new DoughnutDataset().setLabel("API calls").addBackgroundColors(Color.AQUA_MARINE,
                Color.LIGHT_BLUE, Color.LIGHT_SALMON, Color.LIGHT_BLUE, Color.GRAY).setBorderWidth(2);
        userStatsList.forEach(stats -> dataset.addData(stats.getNoOfCalls()));
        if (userStatsList.size() > 0) {
            DoughnutData data = new DoughnutData().addDataset(dataset);
            userStatsList.forEach(stats -> data.addLabel(stats.getUserName()));

            return new DoughnutChart(data);
        } else {
            return new DoughnutChart();
        }
    }

    @RequestMapping(value = "/dashboard/httpResponseStatsChart", method = GET, produces = "application/json")
    @ResponseBody
    public DoughnutChart generateHttpResponseStatisticsDoughnutChart(
            @RequestParam("dateFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodStart,
            @RequestParam("dateUntil") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodEnd) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<StatisticsPerHttpStatusCode> statisticsPerHttpStatusCodeList = metricsAggregationCustomRepository
                .getNoOfCallsPerHttpResponseCode(selectedTimePeriodStart, selectedTimePeriodEnd, auth.getName());

        if (statisticsPerHttpStatusCodeList.size() > 0) {

            List<Color> chartColors = new ArrayList<>();
            for (int i = 0; i <= (statisticsPerHttpStatusCodeList.size()) - 1; i++) {
                int statusCode = statisticsPerHttpStatusCodeList.get(i).getStatusCode();
                Color colorForStatusCode = chartColorsPerHttpResponseCode.get(statusCode);

                if (colorForStatusCode == null) {
                    // there is no fixed color defined for this status code, set it based on the
                    // range
                    if (statusCode >= 200 && statusCode < 300) {
                        // lighter green
                        colorForStatusCode = new Color(0, 204, 0);
                    } else if (statusCode >= 300 && statusCode < 400) {
                        colorForStatusCode = Color.LIGHT_BLUE;
                    } else if (statusCode >= 404 && statusCode < 500) {
                        colorForStatusCode = Color.DARK_ORANGE;
                    } else if (statusCode >= 500 && statusCode < 600) {
                        // red
                        colorForStatusCode = new Color(255, 51, 51);
                    } else {
                        colorForStatusCode = Color.LIGHT_GRAY;
                    }
                }
                chartColors.add(colorForStatusCode);
            }

            DoughnutDataset dataset = new DoughnutDataset().setLabel("HTTP response codes")
                    .addBackgroundColors(chartColors.stream().toArray(Color[]::new)).setBorderWidth(2);
            statisticsPerHttpStatusCodeList.forEach(stats -> dataset.addData(stats.getNoOfCalls()));
            DoughnutData data = new DoughnutData().addDataset(dataset);
            statisticsPerHttpStatusCodeList.forEach(stats -> data.addLabel(stats.getStatusCode().toString()));
            return new DoughnutChart(data);
        } else {
            return new DoughnutChart();
        }
    }

    @RequestMapping(value = "/dashboard/statsPerDayLineChart", method = GET, produces = "application/json")
    @ResponseBody
    public LineChart generateStatsPerDayLineChart(
            @RequestParam("dateFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodStart,
            @RequestParam("dateUntil") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodEnd) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<StatisticsPerDay> statsList = metricsAggregationCustomRepository
                .getNoOfCallsPerDayForDateRange(selectedTimePeriodStart, selectedTimePeriodEnd, auth.getName());

        // create a map with entries for all days in the given date range
        Map<LocalDate, Long> callsPerDay = new TreeMap<>();
        long timePeriodDurationInDays = ChronoUnit.DAYS.between(selectedTimePeriodStart, selectedTimePeriodEnd);
        for (int i = 0; i <= timePeriodDurationInDays; i++) {
            // put "0" as default, in case no calls are registered in database
            callsPerDay.put(selectedTimePeriodStart.plusDays(i), 0l);
        }

        // add numbers from database, if any
        statsList.forEach(statisticsPerDay -> {
            callsPerDay.put(statisticsPerDay.getLocalDate(), statisticsPerDay.getNoOfCalls());
        });

        // create actual chart
        LineDataset dataset = new LineDataset().setLabel("Total number of API calls per day")
                .setBackgroundColor(Color.LIGHT_YELLOW).setBorderWidth(3);
        LineData data = new LineData().addDataset(dataset);

        callsPerDay.forEach((date, noOfCalls) -> {
            data.addLabel(DateTimeFormatter.ISO_LOCAL_DATE.format(date));
            dataset.addData(noOfCalls).addPointStyle(PointStyle.CIRCLE).addPointBorderWidth(2).setLineTension(0f)
                    .setSteppedLine(false).addPointBackgroundColor(Color.LIGHT_YELLOW)
                    .addPointBorderColor(Color.LIGHT_GRAY);
        });
        LineOptions lineOptions = new LineOptions().setScales(
                new LinearScales().addyAxis(new LinearScale().setTicks(new LinearTicks().setBeginAtZero(true))));

        return new LineChart(data, lineOptions);
    }

    @RequestMapping(value = "/dashboard/metricsByLoggedUserStatistics", method = GET, produces = "application/json")
    @ResponseBody
    private Iterable<Metric> loadCallsStatistics(
            @RequestParam("dateFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodStart,
            @RequestParam("dateUntil") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodEnd) {
        log.debug("List of Calls by the user during the date range from " + selectedTimePeriodStart + " and "
                + selectedTimePeriodEnd);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return metricRepository.retrieveByUserConsumer(auth.getName(), Date.valueOf(selectedTimePeriodStart),
                Date.valueOf(selectedTimePeriodEnd));
    }

    @RequestMapping(value = "/dashboard/generalStatistics", method = GET, produces = "application/json")
    @ResponseBody
    private GeneralStats loadGeneralStatistics(
            @RequestParam("dateFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodStart,
            @RequestParam("dateUntil") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodEnd) {

        log.debug("session before update is : {}", session);
        // store new selected time period in user session settings
        session.setDashboardDateRangeStart(selectedTimePeriodStart);
        session.setDashboardDateRangeEnd(selectedTimePeriodEnd);
        log.debug("session after update is : {}", session);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // YggUserPrincipal apiProviderUser = (YggUserPrincipal) auth.getPrincipal();

        String apiProviderUsername = auth.getName();

        long timePeriodDurationInDays = ChronoUnit.DAYS.between(selectedTimePeriodStart, selectedTimePeriodEnd);

        LocalDate previousTimePeriodEnd = selectedTimePeriodStart.minusDays(1);
        LocalDate previousTimePeriodStart = previousTimePeriodEnd.minusDays(timePeriodDurationInDays);

        int callsThisPeriod = metricsAggregationCustomRepository.getTotalNumberOfLoggedApiCalls(selectedTimePeriodStart,
                selectedTimePeriodEnd, apiProviderUsername);
        int callsPreviousPeriod = metricsAggregationCustomRepository
                .getTotalNumberOfLoggedApiCalls(previousTimePeriodStart, previousTimePeriodEnd, apiProviderUsername);

        GeneralStats stats = new GeneralStats();
        stats.dateFrom = selectedTimePeriodStart;
        stats.dateUntil = selectedTimePeriodEnd;

        stats.callsThisPeriod = callsThisPeriod;
        stats.callsDiff = callsThisPeriod - callsPreviousPeriod;

        stats.errorsThisPeriod = metricsAggregationCustomRepository
                .getNumberOfErroneousApiCalls(selectedTimePeriodStart, selectedTimePeriodEnd, apiProviderUsername);
        stats.errorsTotal = metricsAggregationCustomRepository.getNumberOfErroneousApiCalls(previousTimePeriodStart,
                previousTimePeriodEnd, apiProviderUsername);

        stats.users = metricsAggregationCustomRepository.getNumberOfApiCallers(selectedTimePeriodStart,
                selectedTimePeriodEnd, apiProviderUsername);
        stats.callsTotal = metricsAggregationCustomRepository.getTotalNumberOfLoggedApiCalls(selectedTimePeriodStart,
                selectedTimePeriodEnd, apiProviderUsername);

        stats.revenueTotal = reportService.reportTotalRevenueForApiProvider(auth.getName(),
                selectedTimePeriodStart, selectedTimePeriodEnd);

        return stats;
    }

    public static class GeneralStats {

        public LocalDate dateUntil;
        public LocalDate dateFrom;

        public int callsTotal;
        public int errorsTotal;
        public double revenueTotal;

        public int callsThisPeriod;
        public int errorsThisPeriod;
        public int callsDiff;
        public long users;
    }

    @PostMapping(value = "/serviceWizard")
    public ModelAndView serviceWizard(ServiceWizardForm wizard, BindingResult bindingResult, Model model)
            throws IOException, GitAPIException, URISyntaxException {

        // Create Service
        ServiceApi serviceApi = new ServiceApi();
        serviceApi.setName(wizard.getServiceName());
        serviceApi.setLocalUrl(wizard.getServiceUrl());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        User user = userRepository.findByUsername(auth.getName());
        serviceApi.setOwner(user);

        if (wizard.getServiceForFree()) {
            serviceApi.setServiceAccessPaymentPolicy(ServiceAccessPaymentPolicy.FOR_FREE);
        } else if (wizard.getMonthlyCharge()) {
            serviceApi.setServiceAccessPaymentPolicy(ServiceAccessPaymentPolicy.MONTHLY_FEE);
            serviceApi.setMonthlyFee(Double.valueOf(wizard.getServiceCost()));
        } else {
            serviceApi.setServiceAccessPaymentPolicy(ServiceAccessPaymentPolicy.WELL_DEFINED_PRICE);

            List<EntryPoint> entryPoints = new ArrayList<>();

            EntryPoint entryPoint = new EntryPoint();
            entryPoint.setHttpMethod("GET");
            entryPoint.setName("/");
            entryPoint.setPathPattern("/");
            entryPoint.setPricePerCall(Double.valueOf(wizard.getServiceCost()));

            entryPoints.add(entryPoint);
            serviceApi.setEntryPoints(entryPoints);

        }

        serviceApi.setServiceAccessPermissionPolicy(ServiceAccessPermissionPolicy.PERMISSION_NECESSARY);
        serviceApi.setDescription("Generated by the getting started wizard");

        createServiceAction.setUser(user);
        createServiceAction.setServiceApi(serviceApi);
        createServiceAction.execute();

        ServiceApi newServiceApi = createServiceAction.getServiceApi();

        // Create Proxy
        Proxy proxy = new Proxy();
        proxy.setPort(proxyServerDefaultPort);
        proxy.setName("Gateway for " + wizard.getServiceName());
        proxy.setOwner(user);
        proxy.setDescription("Gateway generated by the getting started wizard");
        List<Long> serviceIdList = new ArrayList();
        serviceIdList.add(newServiceApi.getId());
        createProxyAction.setProxy(proxy);
        createProxyAction.setUser(user);
        createProxyAction.setSelectedServices(serviceIdList);
        createProxyAction.execute();

        // Create an Api
        createApiKeyAction.setServiceApi(newServiceApi);
        createApiKeyAction.setUser(user);
        createApiKeyAction.execute();
        ApiKey apiKey = createApiKeyAction.getApiKey();

        // Update the User Flag
        user.setInitialized(Boolean.TRUE);
        userRepository.save(user);

        // Prepare response
        ServiceWizardResponse response = new ServiceWizardResponse();
        response.setApiKey(apiKey.getKeyValue());
        response.setMonthlyCharge(wizard.getMonthlyCharge());
        response.setPercallCharge(wizard.getPercallCharge());
        response.setProxyUrl("/admin/proxies/" + proxy.getId() + "/download");
        response.setServiceCost(wizard.getServiceCost());
        response.setServiceForFree(wizard.getServiceForFree());
        response.setServiceName(wizard.getServiceName());
        response.setUriIdentifier(newServiceApi.getUriIdentifier());
        response.setServiceUrl(wizard.getServiceUrl());
        response.setUserName(user.getUsername());

        ModelAndView mav = new ModelAndView();
        mav.addObject("wizardResponse", response);
        mav.setViewName("admin/wizard/serviceWizardResult");

        return mav;
    }

    // This is the controller for the Consumer Getting Started Wizard
    @RequestMapping(value = "/consumer/gettingstarted", method = GET)
    @ResponseBody
    public ModelAndView gettingStartedWizardForConsumer(Model model) throws IOException {

        // API key will be generated later in the wizard, put null as dummy value for
        // now
        ApiKey newApiKey = null;

        ServiceApi gettingStartedTestService = loadTestServiceForConsumerWizard();

        ModelAndView mav = new ModelAndView();
        mav.setViewName(ADMIN_CONSUMER_WIZARD);
        mav.addObject("testService", gettingStartedTestService);
        model.addAttribute("testApiKeys", newApiKey);
        return mav;
    }

    // Refresh the Test Api Key and the URL to call the service in the getting
    // started wizard for consumer
    @RequestMapping(value = "/consumer/gettingstarted/refreshApiKeys/{whichFragmentToLoad}", method = GET)
    public String refreshApiKeys(@PathVariable("whichFragmentToLoad") String whichFragmentToLoad, Model model) {

        ServiceApi gettingStartedTestService = loadTestServiceForConsumerWizard();
        List<ApiKey> apiKeysForTestService = apiKeyRepository
                .findByLoggedInAPIConsumerAndServiceId(gettingStartedTestService.getId());

        // Gets the last Getting Started Test Service APIKey, as the Getting Started for
        // Consumer can be used several times (there is size -1 because the array starts
        // in element 0)
        ApiKey newApiKey = apiKeysForTestService.get(apiKeysForTestService.size() - 1);

        // get the URL(s) for the test service proxy(s)
        Map<String, List<String>> proxyURLPerNewApiKey = new TreeMap<>();
        List<String> proxiesUrlList = proxyRepository
                .customSearchForAllProxiesForGivenServiceApiId(newApiKey.getServiceApi().getId()).stream()
                .filter(proxy -> proxy.getPublicUrl() != null).filter(proxy -> proxy.getPublicUrl() != "")
                .map(Proxy::getPublicUrl).collect(Collectors.toList());

        if (proxiesUrlList.size() > 1) {
            log.info(
                    "Please notice the list of proxies is larger than one, so for test consumer we will take into account only first one");
        }
        List<String> defaultProxyList = new ArrayList();
        if (!proxiesUrlList.isEmpty()) {
            defaultProxyList.add(proxiesUrlList.get(0));
        }

        proxyURLPerNewApiKey.putIfAbsent(newApiKey.getKeyValue(), defaultProxyList);

        model.addAttribute("testService", gettingStartedTestService);
        model.addAttribute("proxiesPerApiKey", proxyURLPerNewApiKey);
        model.addAttribute("apiKeys", newApiKey);

        // decide about which fragment to return
        String returnedFragment = (whichFragmentToLoad.equals("table"))
                // returns the ApiKeyTable Fragment
                ? "admin/fragments/api-keys/consumer/list :: apiKeyTable"
                // returns the Url Fragment
                : "admin/fragments/consumer_wizard/wizard :: gatewayCallURL";

        return returnedFragment;
    }

    private ServiceApi loadTestServiceForConsumerWizard() {
        // Getting the test service
        log.debug("trying to load test service for provider '{}' and uriIdentifier '{}'",
                gettingStartedTestServiceProvider, gettingStartedTestServiceIdentifier);
        ServiceApi testServiceApi = serviceApiRepository.findServiceApiByServiceOwnerAndUriIdentifier(
                gettingStartedTestServiceProvider, gettingStartedTestServiceIdentifier);
        log.debug("test service loaded: {}", testServiceApi);
        return testServiceApi;
    }

    @RequestMapping(value = "/dashboard/gateway-health-monitor", method = GET)
    @ResponseBody
    public ModelAndView getGatewayHealthMonitorGuiFragment() {
        ModelAndView mav = new ModelAndView();
        log.debug("client request for Gateway Health Monitor Data");
        GatewayHealthMonitorService.DataForGatewayHealthMonitor dataForGatewayHealthMonitor = gatewayHealthMonitorService
                .getGatewayHealthMonitorData();
        mav.addObject("gatewayHealthMonitorProxyData", dataForGatewayHealthMonitor);
        mav.setViewName(GATEWAY_HEALTH_MONITOR_FRAGMENT);
        return mav;
    }

    @RequestMapping(value = "/dashboard/gateway-health-monitor/notification-status", method = POST)
    @ResponseBody
    public void updateNotificationStatusOnGatewayHealthMonitor(@RequestParam String proxyId, @RequestParam boolean isMonitoringEnabled) {
        Proxy proxy = proxyRepository.findById(proxyId).orElse(null);
        proxy.setHealthMonitoringEnabled(isMonitoringEnabled);
        log.debug("Changing the monitoring status of proxy {} to {}", proxy.getName(), proxy.isHealthMonitoringEnabled());
        proxyRepository.save(proxy);
    }
}
