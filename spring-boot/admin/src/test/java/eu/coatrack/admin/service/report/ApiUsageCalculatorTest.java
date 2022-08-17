package eu.coatrack.admin.service.report;

import eu.coatrack.admin.factories.ReportDataFactory;
import eu.coatrack.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static eu.coatrack.api.ServiceAccessPaymentPolicy.WELL_DEFINED_PRICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class ApiUsageCalculatorTest {
    private final ApiUsageCalculator apiUsageCalculator;

    private final ApiUsageCounter apiUsageCounter;

    public ApiUsageCalculatorTest() {
        apiUsageCounter = mock(ApiUsageCounter.class);
        doReturn(ReportDataFactory.getCallCount()).when(apiUsageCounter).count(any(ApiUsageDTO.class));

        apiUsageCalculator = new ApiUsageCalculator(apiUsageCounter);
    }

    @Test
    public void calculateForSpecificService() {
        ApiUsageDTO apiUsageDTO = ReportDataFactory.getApiUsageDTO(WELL_DEFINED_PRICE);

        List<ApiUsageReport> result = apiUsageCalculator.calculateForSpecificService(apiUsageDTO);
        // well defined price
        assertEquals(0.003, result.get(0).getTotal());
        assertEquals(0.008, result.get(1).getTotal());
        assertEquals(0.021, result.get(2).getTotal());

        // monthly calls
        assertEquals(200, result.get(3).getTotal());

        // free and not matchting calls are free by default
        assertEquals(0, result.get(4).getTotal());
        assertEquals(0, result.get(5).getTotal());
    }

}
