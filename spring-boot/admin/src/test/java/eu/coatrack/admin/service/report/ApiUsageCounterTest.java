package eu.coatrack.admin.service.report;

import eu.coatrack.admin.factories.ReportDataFactory;
import eu.coatrack.admin.model.repository.MetricsAggregationCustomRepository;
import eu.coatrack.api.MetricType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

import static eu.coatrack.admin.factories.ReportDataFactory.*;
import static eu.coatrack.api.ServiceAccessPaymentPolicy.MONTHLY_FEE;
import static eu.coatrack.api.ServiceAccessPaymentPolicy.WELL_DEFINED_PRICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ApiUsageCounterTest {

    private final ApiUsageCounter apiUsageCounter;

    private final MetricsAggregationCustomRepository metricsAggregationCustomRepository;

    public ApiUsageCounterTest() {
        metricsAggregationCustomRepository = mock(MetricsAggregationCustomRepository.class);

        doReturn(getMetricResultListAsObjectArray()).when(metricsAggregationCustomRepository).getUsageApiConsumer(
                any(MetricType.class), anyLong(), anyString(), anyLong(), any(Date.class), any(Date.class)
        );

        doReturn(getMetricResultListAsObjectArray()).when(metricsAggregationCustomRepository).getUsageApiConsumer(
                any(MetricType.class), anyLong(), anyString(), anyLong(), any(Date.class), any(Date.class), anyBoolean()
        );


        apiUsageCounter = new ApiUsageCounter(metricsAggregationCustomRepository);
    }

    @Test
    public void countMonthly() {
        ApiUsageDTO apiUsageDTO = ReportDataFactory.getApiUsageDTO(MONTHLY_FEE);
        CallCount result = apiUsageCounter.count(apiUsageDTO);
        assertFalse(result.isEmpty());
        assertEquals(6L, result.getMonthlyBilledCalls());
    }

    @Test
    public void countEntryPoint() {
        ApiUsageDTO apiUsageDTO = getApiUsageDTO(WELL_DEFINED_PRICE);
        CallCount result = apiUsageCounter.count(apiUsageDTO);
        assertFalse(result.isEmpty());
        assertEquals(1L, result.getCallsByEntryPoint(getEntryPoints().get(0)));
        assertEquals(2L, result.getCallsByEntryPoint(getEntryPoints().get(1)));
        assertEquals(3L, result.getCallsByEntryPoint(getEntryPoints().get(2)));


    }


}
