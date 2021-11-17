package eu.coatrack.admin;

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

import eu.coatrack.admin.model.repository.*;
import eu.coatrack.api.*;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * @author gr-hovest(at)atb-bremen.de
 */
@Component
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    ProxyRepository proxyRepository;

    @Autowired
    ApiKeyRepository apiKeyRepository;

    @Autowired
    ServiceApiRepository serviceApiRepository;

    @Autowired
    MetricRepository metricRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EntryPointRepository entryPointRepository;

    @Autowired
    CreditAccountRepository creditAccountRepository;

    @Value("${ygg.admin.database.insertSampleDataOnStartup:false}")
    private boolean sampleDataShouldBeInserted;

    // api provider username can be configured as test user's github login name
    // use exampleCompanyInc as default value, e.g. for unit testing
    @Value("${ygg.admin.database.sample-data.username.api-provider:exampleCompanyInc}")
    private String sampleDataApiProviderUsername;

    // api consumer username can be configured as test user's github login name
    // use exampleApiUser as default value, e.g. for unit testing
    @Value("${ygg.admin.database.sample-data.username.api-consumer:exampleApiUser}")
    private String sampleDataApiConsumerUsername;

    @PostConstruct
    public void createPresentationData() {

        if (sampleDataShouldBeInserted) {

            log.info("Inserting sample data into the database");

            log.debug("sample data api provider username is {}", sampleDataApiProviderUsername);
            User apiProviderUser = createNewUser("WeatherSolutions Inc.", sampleDataApiProviderUsername, "Arthur", "Admin");

            User simonSupermarketUser = createNewUser("Another Supermarket Chain", "Simon Supermarket", "Simon", "Supermarket");
            User frankFarmerUser = createNewUser("Frank's Farm", sampleDataApiConsumerUsername, "Frank", "Farmer");
            User richardRetailerUser = createNewUser("My Supermarket Chain", "Richard Retailer", "Richard", "Retailer");
            User waltWholesalerUser = createNewUser("ACME Wholesale Inc.", "Walt Wholesaler", "Walt", "Wholesaler");

            ServiceApi serviceApiWeather = createNewServiceAPI(
                    apiProviderUser,
                    "Weather Information",
                    "weather-service",
                    "http://192.168.55.14:8086/weather-info",
                    ServiceAccessPermissionPolicy.PUBLIC,
                    ServiceAccessPaymentPolicy.WELL_DEFINED_PRICE,
                    new BigDecimal(2.0),
                    new BigDecimal(15.9),
                    "Service for customers to get information about the weather in a specific location. Define the location via request parameters 'lat' and 'long' (mandatory).");



            ServiceApi serviceApiHumidity = createNewServiceAPI(
                    apiProviderUser,
                    "Humidity by location",
                    "humidity-by-location",
                    "http://my-local-network-server:1234/humidity-info",
                    ServiceAccessPermissionPolicy.PUBLIC,
                    ServiceAccessPaymentPolicy.FOR_FREE,
                    null,
                    null,
                    "Users can use this service to obtain the humidity in a specific location. Request parameters 'lat' and 'long' are required to specify the location.");

            ServiceApi serviceApiWeatherStationsLocation = createNewServiceAPI(
                    apiProviderUser,
                    "Weather Station Locations",
                    "weather-station-locations",
                    "http://192.168.55.19:8087",
                    ServiceAccessPermissionPolicy.PERMISSION_NECESSARY,
                    ServiceAccessPaymentPolicy.FOR_FREE,
                    null,
                    null,
                    "Obtain the location of the weather stations. Requires weather station ID as request parameter 'station-id'.");

            Set<ServiceApi> collection = new HashSet<>();
            collection.add(serviceApiWeather);
            collection.add(serviceApiHumidity);

            Proxy proxyWeather = createNewProxy(
                    "aa11aa22-aa33-aa44-aa55-aa66aa77aa88",
                    "username-aa33-aa44-aa55-aa66aa77aa88",
                    "password-aa33-aa44-aa55-aa66aa77aa88",
                    "Weather Information Gateway",
                    collection,
                    "http://localhost:8088",
                    "Gateway running in the Weather Solutions Inc. cloud", apiProviderUser);

            collection = new HashSet<>();
            collection.add(serviceApiWeatherStationsLocation);
            createNewProxy(
                    "bb11bb22-bb33-bb44-bb55-bb66bb77bb88",
                    "username-bb33-bb44-bb55-bb66bb77bb88",
                    "password-bb33-bb44-bb55-bb66bb77bb88",
                    "Weather Station Locations Service Gateway",
                    collection,
                    "http://localhost:8089/",
                    "Gateway running in the ACME Inc. data processing centre", apiProviderUser);

            ApiKey  simonSupermarketKeyServiceApiHumidity = createNewApiKey(
                    "ee11ee22-ee33-ee44-ee55-ee66ee77ee88",
                    simonSupermarketUser,
                    DateUtils.addDays(new Date(), -40), serviceApiHumidity);

            ApiKey frankFarmerKeyServiceApiWeather = createNewApiKey(
                    "ff11ff22-ff33-ff44-ff55-ff66ff77-cts",
                    frankFarmerUser,
                    DateUtils.addDays(new Date(), -25), serviceApiWeather);

            ApiKey frankFarmerKeyServiceApiLocation = createNewApiKey(
                    "ff11ff22-ff33-ff44-ff55-ff66ff77-onl",
                    frankFarmerUser,
                    DateUtils.addDays(new Date(), -25), serviceApiWeatherStationsLocation);

            ApiKey richardRetailerKey = createNewApiKey(
                    "rr11rr22-rr33-rr44-rr55-rr66rr77rr88",
                    richardRetailerUser,
                    DateUtils.addDays(new Date(), -9), serviceApiWeather);

            ApiKey richardRetailerKeyExpired = createNewApiKey(
                    "rr11rr22-rr33-rr44-rr55-rr66-expired",
                    richardRetailerUser,
                    DateUtils.addDays(new Date(), -100), serviceApiWeather);

            ApiKey waltWholesalerKey = createNewApiKey(
                    "ww11ww22-ww33-ww44-ww55-ww66ww77ww88",
                    waltWholesalerUser,
                    DateUtils.addDays(new Date(), -8), serviceApiWeather);

            String metricsCounterSessionID = UUID.randomUUID().toString();
            LocalDate today = LocalDate.now();

            // Frank Farmer
            addMetrics(proxyWeather,
                    frankFarmerKeyServiceApiLocation,
                    "POST",
                    "/order/large",
                    2,
                    1,
                    1,
                    metricsCounterSessionID,
                    today.minusDays(9));
            addMetrics(proxyWeather,
                   frankFarmerKeyServiceApiLocation,
                    "POST",
                    "/order/small",
                    2,
                    0,
                    0,
                    metricsCounterSessionID,
                    today.minusDays(8));
            addMetrics(proxyWeather,
                    frankFarmerKeyServiceApiWeather,
                    "GET",
                    "/historic",
                    2,
                    0,
                    0,
                    metricsCounterSessionID,
                    today.minusDays(4));
            addMetrics(proxyWeather,
                    frankFarmerKeyServiceApiWeather,
                    "GET",
                    "/current",
                    9,
                    0,
                    0,
                    metricsCounterSessionID,
                    today.minusDays(2));

            // Richard Retailer
            addMetrics(proxyWeather,
                    richardRetailerKey,
                    "GET",
                    "/historic",
                    82,
                    4,
                    8,
                    metricsCounterSessionID,
                    today.minusDays(12));
            addMetrics(proxyWeather,
                    richardRetailerKey,
                    "GET",
                    "/current",
                    8,
                    4,
                    0,
                    metricsCounterSessionID,
                    today.minusDays(6));
            addMetrics(proxyWeather,
                    richardRetailerKey,
                    "PUT",
                    "/current/modify",
                    9,
                    1,
                    0,
                    metricsCounterSessionID,
                    today.minusDays(5));
            addMetrics(proxyWeather,
                    richardRetailerKey,
                    "GET",
                    "/current",
                    7,
                    0,
                    0,
                    metricsCounterSessionID,
                    today.minusDays(4));
            addMetrics(proxyWeather,
                    richardRetailerKey,
                    "POST",
                    "/current/add",
                    11,
                    0,
                    1,
                    metricsCounterSessionID,
                    today.minusDays(3));
            addMetrics(proxyWeather,
                    richardRetailerKey,
                    "GET",
                    "/current",
                    8,
                    0,
                    0,
                    metricsCounterSessionID,
                    today.minusDays(2));
            addMetrics(proxyWeather,
                    richardRetailerKey,
                    "GET",
                    "/historic",
                    9,
                    0,
                    2,
                    metricsCounterSessionID,
                    today.minusDays(1));
            addMetrics(proxyWeather,
                    richardRetailerKey,
                    "GET",
                    "/historic",
                    7,
                    0,
                    0,
                    metricsCounterSessionID,
                    today);

            // Walt Wholesaler
            addMetrics(proxyWeather,
                    waltWholesalerKey,
                    "GET",
                    "/current",
                    40,
                    2,
                    0,
                    metricsCounterSessionID,
                    today.minusDays(9));
            addMetrics(proxyWeather,
                    waltWholesalerKey,
                    "GET",
                    "/historic",
                    18,
                    2,
                    0,
                    metricsCounterSessionID,
                    today.minusDays(3));
            addMetrics(proxyWeather,
                    waltWholesalerKey,
                    "GET",
                    "/current",
                    13,
                    0,
                    1,
                    metricsCounterSessionID,
                    today.minusDays(2));
            addMetrics(proxyWeather,
                    waltWholesalerKey,
                    "GET",
                    "/historic",
                    17,
                    0,
                    0,
                    metricsCounterSessionID,
                    today.minusDays(1));
            addMetrics(proxyWeather,
                    waltWholesalerKey,
                    "GET",
                    "/current",
                    28,
                    0,
                    0,
                    metricsCounterSessionID,
                    today);

            // put some credits into accounts for local testing
            createNewCreditAccount(
                    2150,
                    apiProviderUser
            );
            createNewCreditAccount(
                    1010,
                    simonSupermarketUser
            );
            createNewCreditAccount(
                    158,
                    frankFarmerUser
            );
            createNewCreditAccount(
                    220,
                    richardRetailerUser
            );
            createNewCreditAccount(
                    312,
                    waltWholesalerUser
            );
        }
    }

    private User createNewUser(String company, String username, String firstname, String familyname) {
        User user = new User();
        user.setCompany(company);
        user.setUsername(username);
        user.setFirstname(firstname);
        user.setLastname(familyname);
        user.setInitialized(Boolean.FALSE);
        user.setEmail("test@gmail.com");
        userRepository.save(user);
        return user;
    }

    private ApiKey createNewApiKey(
            String keyValue,
            User consumer,
            Date created,
            ServiceApi serviceApi) {

        ApiKey apiKey = new ApiKey();

        apiKey.setUser(consumer);
        apiKey.setCreated(created);
        apiKey.setValidUntil(DateUtils.addMonths(apiKey.getCreated(), 3));
        apiKey.setKeyValue(keyValue);
        apiKey.setServiceApi(serviceApi);

        apiKey = apiKeyRepository.save(apiKey);

        serviceApi = serviceApiRepository.findOne(serviceApi.getId());
        serviceApi.getApiKeys().add(apiKey);
        serviceApiRepository.save(serviceApi);

        return apiKey;
    }

    private Proxy createNewProxy(
            String uuid,
            String configServerName,
            String configServerPassword,
            String name,
            Set<ServiceApi> serviceApis,
            String publicUrl,
            String description, User user) {

        Proxy proxy = new Proxy();
        proxy.setId(uuid);
        proxy.setConfigServerName(configServerName);
        proxy.setConfigServerPassword(configServerPassword);
        proxy.setName(name);
        proxy.setServiceApis(serviceApis);
        proxy.setPublicUrl(publicUrl);
        proxy.setPort(8088);
        proxy.setDescription(description);
        proxy.setOwner(user);
        return proxyRepository.save(proxy);
    }

    private EntryPoint createEntrypoint(String name, String pathPattern, double pricePerCall, String httpMethod) {
        EntryPoint entryPoint = new EntryPoint();
        entryPoint.setName(name);
        entryPoint.setPathPattern(pathPattern);
        entryPoint.setPricePerCall(pricePerCall);
        entryPoint.setHttpMethod(httpMethod);
        return entryPoint;
    }

    private ServiceApi createNewServiceAPI(
            User apiProvider,
            String name,
            String uriIdentifier,
            String localUrl,
            ServiceAccessPermissionPolicy serviceAccessPermissionPolicy,
            ServiceAccessPaymentPolicy serviceAccessPaymentPolicy,
            BigDecimal pricePerCall,
            BigDecimal pricePerMonth,
            String description) {

        ServiceApi serviceApi = new ServiceApi();
        serviceApi.setOwner(apiProvider);
        serviceApi.setName(name);
        serviceApi.setUriIdentifier(uriIdentifier);
        serviceApi.setLocalUrl(localUrl);
        serviceApi.setServiceAccessPermissionPolicy(serviceAccessPermissionPolicy);
        serviceApi.setServiceAccessPaymentPolicy(serviceAccessPaymentPolicy);
        serviceApi.setDescription(description);

        List<EntryPoint> entryPoints = new ArrayList<>();

        if (serviceAccessPaymentPolicy.equals(ServiceAccessPaymentPolicy.WELL_DEFINED_PRICE)) {
            entryPoints.add(createEntrypoint("Read historical data", "/historic", 1.2, HttpMethod.GET.toString()));
            entryPoints.add(createEntrypoint("Read test data", "/test", 2, HttpMethod.GET.toString()));
            entryPoints.add(createEntrypoint("Read weather sample", "/weatherSample", 1.5, HttpMethod.GET.toString()));
        }
        serviceApi.setEntryPoints(entryPoints);

        return serviceApiRepository.save(serviceApi);
    }

    private void addMetrics(
            Proxy proxy,
            ApiKey apiKey,
            String requestMethod,
            String path,
            int countHttp200,
            int countHttp404,
            int countHttp500,
            String metricsCounterSessionID,
            LocalDate dateOfApiCall) {

        addMetric(proxy,
                apiKey,
                requestMethod,
                path,
                MetricType.AUTHORIZED_REQUEST,
                countHttp200 + countHttp404 + countHttp500,
                null,
                metricsCounterSessionID,
                dateOfApiCall);

        addMetric(proxy,
                apiKey,
                requestMethod,
                path,
                MetricType.RESPONSE,
                countHttp200,
                200,
                metricsCounterSessionID,
                dateOfApiCall);
        if (countHttp404 > 0) {
            addMetric(proxy,
                    apiKey,
                    requestMethod,
                    path + "/weatherData-id",
                    MetricType.RESPONSE,
                    countHttp404,
                    404,
                    metricsCounterSessionID,
                    dateOfApiCall);
        }
        if (countHttp500 > 0) {
            addMetric(proxy,
                    apiKey,
                    requestMethod,
                    path,
                    MetricType.RESPONSE,
                    countHttp500,
                    500,
                    metricsCounterSessionID,
                    dateOfApiCall);
        }

    }

    private void addMetric(
            Proxy proxy,
            ApiKey apiKey,
            String requestMethod,
            String path,
            MetricType type,
            int count,
            Integer httpResponseCode,
            String metricsCounterSessionID,
            LocalDate dateOfApiCall) {

        Metric metric = new Metric();
        metric.setProxy(proxy);

        metric.setRequestMethod(requestMethod);
        metric.setPath(path);
        metric.setMetricsCounterSessionID(metricsCounterSessionID);
        metric.setDateOfApiCall(java.sql.Date.valueOf(dateOfApiCall));
        metric.setType(type);
        metric.setCount(count);
        metric.setHttpResponseCode(httpResponseCode);
        metric.setProxy(proxy);
        metric.setApiKey(apiKey);
        metricRepository.save(metric);

        Metric returnMetric = metricRepository.findOne(metric.getId());
        Proxy proxies = returnMetric.getProxy();
        Set<ServiceApi> services = proxies.getServiceApis();
        //List<ApiKey> returnApiKeys = services.iterator().next().getApiKeys();
        //returnApiKeys.get(0).getId();
    }

    private void createNewCreditAccount(double balance, User user) {
        CreditAccount creditAccount = new CreditAccount();
        creditAccount.setBalance(balance);
        creditAccount.setUser(user);
        creditAccountRepository.save(creditAccount);
        user.setAccount(creditAccount);
        userRepository.save(user);
    }
}
