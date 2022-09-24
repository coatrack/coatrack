package eu.coatrack.admin.service.report;

import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.api.*;
import lombok.AllArgsConstructor;
import lombok.Setter;
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
@Setter
public class ReportService {
    // TODO serviceApi dependency can be moved up by 1 layer, there is no other usage
    @Deprecated
    @Autowired
    private final ServiceApiRepository serviceApiRepository;

    @Autowired
    private ApiUsageCalculator apiUsageCalculator;

    public DataTableView<ApiUsageReport> reportApiUsage(ApiUsageDTO apiUsageDTO) {
        List<ApiUsageReport> apiUsageReports;

        if (apiUsageDTO != null && apiUsageDTO.getService() != null) {
            // TODO replace calculateApiUsageReportForSpecificService(apiUsageDTO) with apiUsageCalculator.calculateForSpecificService(apiUsageDTO);
            apiUsageReports = calculateApiUsageReportForSpecificService(apiUsageDTO);
        } else {
            apiUsageReports = new ArrayList<>();
        }

        DataTableView<ApiUsageReport> table = new DataTableView<>();
        table.setData(apiUsageReports);
        return table;
    }

    public double reportTotalRevenueForApiProvider(List<ServiceApi> offeredServices, Date from, Date until) {
        List<ApiUsageReport> apiUsageReportsForAllOfferedServices = new ArrayList<>();

        for (ServiceApi service : offeredServices) {
            ApiUsageDTO apiUsageDTO = new ApiUsageDTO(service, null, from, until, true, false);
            List<ApiUsageReport> calculatedApiUsage = calculateApiUsageReportForSpecificService(apiUsageDTO);
            apiUsageReportsForAllOfferedServices.addAll(calculatedApiUsage);
        }
        double total = apiUsageReportsForAllOfferedServices.stream().mapToDouble(ApiUsageReport::getTotal).sum();
        return total;
    }


    // TODO remove after refactoring PublicApiController
    @Deprecated
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

    // TODO remove after refactoring PublicApiController
    @Deprecated
    private List<ApiUsageReport> calculateApiUsageReportForSpecificService(ApiUsageDTO apiUsageDTO) {
        return apiUsageCalculator.calculateForSpecificService(apiUsageDTO);
    }

    @Deprecated
    public double calculateTotalRevenueForApiProvider(String apiProviderUsername, LocalDate timePeriodStart, LocalDate timePeriodEnd) {
        Date from = java.sql.Date.valueOf(timePeriodStart);
        Date until = java.sql.Date.valueOf(timePeriodEnd);

        // TODO move to ServiceApiService after refactoring admin controller, serviceApiRepository can be moved to AdminController
        List<ServiceApi> offeredServices = serviceApiRepository.findByOwnerUsername(apiProviderUsername);
        return reportTotalRevenueForApiProvider(offeredServices, from, until);
    }

    @Deprecated
    public double reportTotalRevenueForApiProvider(String apiProviderUsername, LocalDate timePeriodStart, LocalDate timePeriodEnd) {
        Date from = java.sql.Date.valueOf(timePeriodStart);
        Date until = java.sql.Date.valueOf(timePeriodEnd);

        // TODO move to ServiceApiService after refactoring admin controller, serviceApiRepository can be moved to AdminController
        List<ServiceApi> offeredServices = serviceApiRepository.findByOwnerUsername(apiProviderUsername);
        return reportTotalRevenueForApiProvider(offeredServices, from, until);
    }



    // TODO move to ServiceApiService
    @Deprecated
    public List<String> getPayPerCallServicesIds(List<ServiceApi> serviceApis) {
        List<String> payPerCallServicesIds = new ArrayList<>();
        if (!serviceApis.isEmpty()) {
            payPerCallServicesIds = serviceApis.stream().filter(serviceApi -> serviceApi.getServiceAccessPaymentPolicy().equals(WELL_DEFINED_PRICE)).map(ServiceApi::getId).map(String::valueOf).collect(Collectors.toList());
        }
        return payPerCallServicesIds;
    }


    // TODO move to ServiceApiService
    @Deprecated
    public List<User> getServiceConsumers(List<ServiceApi> servicesProvidedByUser) {
        return servicesProvidedByUser.stream()
                .flatMap(api -> api.getApiKeys().stream())
                .map(ApiKey::getUser)
                .distinct()
                .collect(Collectors.toList());
    }
}
