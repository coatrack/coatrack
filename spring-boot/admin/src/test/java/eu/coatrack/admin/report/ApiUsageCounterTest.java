package eu.coatrack.admin.report;

import eu.coatrack.admin.datafactories.ReportDataFactory;
import eu.coatrack.admin.model.repository.MetricsAggregationCustomRepository;
import eu.coatrack.admin.service.report.ApiUsageCounter;
import eu.coatrack.admin.service.report.ApiUsageDTO;
import eu.coatrack.admin.service.report.CallCount;
import eu.coatrack.api.MetricType;
import org.junit.jupiter.api.Test;
import java.util.*;
import static eu.coatrack.admin.datafactories.ReportDataFactory.*;
import static eu.coatrack.admin.utils.DateUtils.getTodayMinusOneMonthAsString;
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

        doReturn(metricResultListObjectArrays).when(metricsAggregationCustomRepository).getUsageApiConsumer(
                any(MetricType.class), anyLong(), anyString(), anyLong(), any(Date.class), any(Date.class)
        );

        doReturn(metricResultListObjectArrays).when(metricsAggregationCustomRepository).getUsageApiConsumer(
                any(MetricType.class), anyLong(), anyString(), anyLong(), any(Date.class), any(Date.class), anyBoolean()
        );

        apiUsageCounter = new ApiUsageCounter(metricsAggregationCustomRepository);
    }

    @Test
    public void countMonthly() {
        ApiUsageDTO apiUsageDTO = ReportDataFactory.getApiUsageDTO(getTodayMinusOneMonthAsString(), MONTHLY_FEE);
        CallCount result = apiUsageCounter.count(apiUsageDTO);

        assertFalse(result.isEmpty());
        assertEquals(6L, result.getMonthlyBilledCalls());
    }

    @Test
    public void countEntryPoint() {
        ApiUsageDTO apiUsageDTO = getApiUsageDTO(getTodayMinusOneMonthAsString(), WELL_DEFINED_PRICE);
        CallCount result = apiUsageCounter.count(apiUsageDTO);

        assertFalse(result.isEmpty());
        assertEquals(1L, result.getCallsByEntryPoint(entryPoints.get(0)));
        assertEquals(2L, result.getCallsByEntryPoint(entryPoints.get(1)));
        assertEquals(3L, result.getCallsByEntryPoint(entryPoints.get(2)));
    }
}
