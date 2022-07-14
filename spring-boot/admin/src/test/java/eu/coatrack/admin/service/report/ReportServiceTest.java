package eu.coatrack.admin.service.report;

import eu.coatrack.admin.model.repository.MetricsAggregationCustomRepository;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.admin.service.ApiUsageCalculator;
import eu.coatrack.admin.service.ReportService;
import eu.coatrack.api.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ReportServiceTest {

    private ReportService reportService;

    private UserRepository userRepository;
    private ServiceApiRepository serviceApiRepository;
    private MetricsAggregationCustomRepository metricsAggregationCustomRepository;

    private User getUserDummy() {
        User userDummy = mock(User.class);
        doReturn(1L).when(userDummy).getId();
        doReturn("Pete Peterson").when(userDummy).getUsername();
        return userDummy;
    }

    private ServiceApi getServiceDummy() {
        ServiceApi serviceApiDummy = mock(ServiceApi.class);
        doReturn(1L).when(serviceApiDummy).getId();
        doReturn("SuperDuperService").when(serviceApiDummy).getName();
        return serviceApiDummy;
    }

    private List getMetricResultDummy() {
        List<Object[]> metricResultDummy = Arrays.asList(new Object[][]{
                { "Pete Peterson", 1L, MetricType.RESPONSE, 0L, "/test", "POST" }
        });
        return metricResultDummy;
    }

    public ReportServiceTest() {
        userRepository = mock(UserRepository.class);
        serviceApiRepository = mock(ServiceApiRepository.class);
        metricsAggregationCustomRepository = mock(MetricsAggregationCustomRepository.class);

        doReturn(Optional.of(getUserDummy())).when(userRepository).findById(1L);

        doReturn(Optional.of(getServiceDummy())).when(serviceApiRepository).findById(1L);
        doReturn(getMetricResultDummy()).when(metricsAggregationCustomRepository).getUsageApiConsumer(any(MetricType.class), anyLong(), anyString(), anyLong(), eq(null), eq(null), eq(true));

        //reportService = new ReportService(userRepository, serviceApiRepository, metricsAggregationCustomRepository);
    }


    @Test
    public void reportApiUsage() {
        ApiUsageCalculator apiUsageCalculator = mock(ApiUsageCalculator.class);
        //reportService.setApiUsageCalculator(apiUsageCalculator);
        ApiUsageDTO apiUsageDTO = new ApiUsageDTO(getServiceDummy(), getUserDummy(), null, null, false, false);
        DataTableView<ApiUsageReport> reportedUsage = reportService.reportApiUsage(apiUsageDTO);

    }

    @Test
    public void getPayPerCallServicesIds() {

    }

    @Test
    public void calculateTotalRevenueForApiProvider() {

    }

    @Test
    public void calculateApiUsageReportForSpecificService() {

    }


}
