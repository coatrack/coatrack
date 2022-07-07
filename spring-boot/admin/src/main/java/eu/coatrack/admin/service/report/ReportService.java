package eu.coatrack.admin.service.report;

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
import eu.coatrack.api.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static eu.coatrack.api.ServiceAccessPaymentPolicy.WELL_DEFINED_PRICE;


@Slf4j
@Service
@AllArgsConstructor
public class ReportService {

    @Autowired
    private final ServiceApiRepository serviceApiRepository;

    @Autowired
    private ApiUsageCalculator apiUsageCalculator;

    public DataTableView<ApiUsageReport> reportApiUsage(ApiUsageDTO apiUsageDTO) {
        List<ApiUsageReport> apiUsageReports;

        if (apiUsageDTO != null && apiUsageDTO.service != null) {
            apiUsageReports = calculateApiUsageReportForSpecificService(apiUsageDTO);
        } else {
            apiUsageReports = new ArrayList<>();
        }

        DataTableView<ApiUsageReport> table = new DataTableView<>();
        table.setData(apiUsageReports);
        return table;
    }

    public List<ApiUsageReport> calculateApiUsageReportForSpecificService(ServiceApi service, long consumerId, Date from, Date until, boolean considerOnlyPaidCalls) {
        ApiUsageDTO apiUsageDTO = new ApiUsageDTO(
                service,
                null,
                from,
                until,
                considerOnlyPaidCalls,
                false
        );
        return calculateApiUsageReportForSpecificService(apiUsageDTO);
    }

    // TODO remove after refactoring admin controller, serviceApiRepository can be moved to AdminController
    @Deprecated
    public double reportTotalRevenueForApiProvider(String apiProviderUsername, LocalDate timePeriodStart, LocalDate timePeriodEnd) {
        Date from = java.sql.Date.valueOf(timePeriodStart);
        Date until = java.sql.Date.valueOf(timePeriodEnd);

        // TODO serviceApi dependency can be moved up by 1 layer, there is no other usage
        List<ServiceApi> offeredServices = serviceApiRepository.findByOwnerUsername(apiProviderUsername);
        return reportTotalRevenueForApiProvider(offeredServices, from, until);
    }

    public double reportTotalRevenueForApiProvider(List<ServiceApi> offeredServices, Date from, Date until) {
        List<ApiUsageReport> apiUsageReportsForAllOfferedServices = new ArrayList<>();

        for (ServiceApi service : offeredServices) {
            List<ApiUsageReport> calculatedApiUsage = calculateApiUsageReportForSpecificService(service, -1L, from, until, true);
            apiUsageReportsForAllOfferedServices.addAll(calculatedApiUsage);
        }
        double total = apiUsageReportsForAllOfferedServices.stream().mapToDouble(ApiUsageReport::getTotal).sum();
        return total;
    }

    private List<ApiUsageReport> calculateApiUsageReportForSpecificService(ApiUsageDTO apiUsageDTO) {
        return apiUsageCalculator.calculateForSpecificService(apiUsageDTO);
    }

    // TODO add to ServiceApiService
    @Deprecated
    public List<String> getPayPerCallServicesIds(List<ServiceApi> serviceApis) {
        List<String> payPerCallServicesIds = new ArrayList<>();
        if (!serviceApis.isEmpty()) {
            payPerCallServicesIds = serviceApis.stream().filter(serviceApi -> serviceApi.getServiceAccessPaymentPolicy().equals(WELL_DEFINED_PRICE)).map(ServiceApi::getId).map(String::valueOf).collect(Collectors.toList());
        }
        return payPerCallServicesIds;
    }


    // TODO add to ServiceApiService
    @Deprecated
    public List<User> getServiceConsumers(List<ServiceApi> servicesProvidedByUser) {
        return servicesProvidedByUser.stream()
                .flatMap(api -> api.getApiKeys().stream())
                .map(ApiKey::getUser)
                .distinct()
                .collect(Collectors.toList());
    }
}
