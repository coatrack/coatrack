package eu.coatrack.admin.report;

import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.service.report.ApiUsageCalculator;
import eu.coatrack.admin.service.report.ApiUsageDTO;
import eu.coatrack.admin.service.report.ReportService;
import eu.coatrack.api.ApiUsageReport;
import eu.coatrack.api.DataTableView;
import org.junit.jupiter.api.Test;
import java.util.Date;
import static eu.coatrack.admin.datafactories.ReportDataFactory.*;
import static eu.coatrack.admin.utils.DateUtils.getTodayMinusOneMonthAsString;
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

    private final ReportService reportService;

    public ReportServiceTest() {
        serviceApiRepository = mock(ServiceApiRepository.class);
        apiUsageCalculator = mock(ApiUsageCalculator.class);

        doReturn(apiUsageReports).when(apiUsageCalculator).calculateForSpecificService(any(ApiUsageDTO.class));

        reportService = new ReportService(serviceApiRepository, apiUsageCalculator);
    }

    @Test
    public void reportApiUsage() {
        ApiUsageDTO apiUsageDTO = getApiUsageDTO(getTodayMinusOneMonthAsString(), WELL_DEFINED_PRICE);
        DataTableView<ApiUsageReport> tableView = reportService.reportApiUsage(apiUsageDTO);

        assertEquals(3, tableView.getData().size());
    }

    @Test
    public void reportTotalRevenueForApiProvider() {
        double res = reportService.reportTotalRevenueForApiProvider(serviceApis, new Date(), new Date());

        assertEquals(600.0, res);
    }
}
