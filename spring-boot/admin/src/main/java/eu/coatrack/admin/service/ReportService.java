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

import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.api.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static eu.coatrack.api.ServiceAccessPaymentPolicy.WELL_DEFINED_PRICE;


@Slf4j
@Service
@AllArgsConstructor
public class ReportService {

    private final static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final ServiceApiRepository serviceApiRepository;


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

    // TODO add to ServiceApiService
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
        List<ApiUsageReport> apiUsageReports;

        if (serviceApi != null) {
            apiUsageReports = calculateApiUsageReportForSpecificService(serviceApi, apiConsumerId, from, until, onlyPaidCalls);
        } else {
            apiUsageReports = new ArrayList<>();
        }

        DataTableView<ApiUsageReport> table = new DataTableView<>();
        table.setData(apiUsageReports);
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

        Date from = java.sql.Date.valueOf(timePeriodStart);
        Date until = java.sql.Date.valueOf(timePeriodEnd);

        List<ApiUsageReport> apiUsageReportsForAllOfferedServices = new ArrayList<>();
        List<ServiceApi> offeredServices = serviceApiRepository.findByOwnerUsername(apiProviderUsername);

        for (ServiceApi service : offeredServices) {
            List<ApiUsageReport> calculatedApiUsage = calculateApiUsageReportForSpecificService(service, -1L, from, until, true);
            apiUsageReportsForAllOfferedServices.addAll(calculatedApiUsage);
        }
        double total = apiUsageReportsForAllOfferedServices.stream().mapToDouble(ApiUsageReport::getTotal).sum();
        return total;
    }

    public List<ApiUsageReport> calculateApiUsageReportForSpecificService(ServiceApi service, Long apiConsumerId, Date from, Date until, boolean onlyPaidCalls) {
        // TODO needs to be typed, raw use for implicit types are no fun
        ApiUsageCalculator apiUsageCalculator = new ApiUsageCalculator(service, from, until);
        List<ApiUsageReport> apiUsageReports = apiUsageCalculator.calculateForSpecificService(apiConsumerId, onlyPaidCalls);
        return apiUsageReports;
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

    public UserRepository getUserRepository() {
        return userRepository;
    }


}
