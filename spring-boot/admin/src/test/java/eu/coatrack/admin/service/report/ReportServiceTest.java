package eu.coatrack.admin.service.report;

import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.api.ApiUsageReport;
import eu.coatrack.api.DataTableView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

import static eu.coatrack.admin.service.report.ReportDataFactory.*;
import static eu.coatrack.api.ServiceAccessPaymentPolicy.WELL_DEFINED_PRICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ReportServiceTest {

    // TODO serviceApi dependency can be moved up by 1 layer, there is no other usage
    @Deprecated
    private final ServiceApiRepository serviceApiRepository;
    private final ApiUsageCalculator apiUsageCalculator;

    @Autowired
    private ReportService reportService;

    public ReportServiceTest() {
        serviceApiRepository = mock(ServiceApiRepository.class);
        apiUsageCalculator = mock(ApiUsageCalculator.class);
        doReturn(getApiUsageReports()).when(apiUsageCalculator).calculateForSpecificService(any(ApiUsageDTO.class));

        reportService = new ReportService(serviceApiRepository, apiUsageCalculator);
    }

    @Test
    public void reportApiUsage() {
        DataTableView<ApiUsageReport> tableView = reportService.reportApiUsage(getApiUsageDTO(WELL_DEFINED_PRICE));

        assertEquals(3, tableView.getData().size());
    }

    @Test
    public void reportTotalRevenueForApiProvider() {
        double res = reportService.reportTotalRevenueForApiProvider(getServiceList(), new Date(), new Date());

        assertEquals(600.0, res);
    }
}
