package eu.coatrack.admin.service.report;

import eu.coatrack.admin.model.repository.MetricsAggregationCustomRepository;
import eu.coatrack.api.MetricType;
import eu.coatrack.api.ServiceAccessPaymentPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

import static eu.coatrack.admin.service.report.ReportMockFactory.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ApiUsageCounterTest {

    @Autowired
    private ApiUsageCounter apiUsageCounter;

    private MetricsAggregationCustomRepository metricsAggregationCustomRepository;

    public ApiUsageCounterTest() {
        metricsAggregationCustomRepository = mock(MetricsAggregationCustomRepository.class);

        doReturn(getMetricResultList()).when(metricsAggregationCustomRepository).getUsageApiConsumer(
                any(MetricType.class), anyLong(), anyString(), anyLong(), any(Date.class), any(Date.class)
        );

        doReturn(getMetricResultList()).when(metricsAggregationCustomRepository).getUsageApiConsumer(
                any(MetricType.class), anyLong(), anyString(), anyLong(), any(Date.class), any(Date.class), anyBoolean()
        );


        apiUsageCounter = new ApiUsageCounter(metricsAggregationCustomRepository);
    }

    @Test
    public void countMonthly() {
        ApiUsageDTO apiUsageDTO = ReportMockFactory.getApiUsageDTO(ServiceAccessPaymentPolicy.MONTHLY_FEE);
        CallCount result = apiUsageCounter.count(apiUsageDTO);
        assertFalse(result.isEmpty());
        assertEquals(6L, result.getMonthlyBilledCalls());
    }

    @Test
    public void countEntryPoint() {
        ApiUsageDTO apiUsageDTO = ReportMockFactory.getApiUsageDTO(ServiceAccessPaymentPolicy.WELL_DEFINED_PRICE);
        CallCount result = apiUsageCounter.count(apiUsageDTO);
        assertFalse(result.isEmpty());
        assertEquals(6L, result.getCallsByEntryPoint(getEntryPointDummys().get(0)));

    }


}
