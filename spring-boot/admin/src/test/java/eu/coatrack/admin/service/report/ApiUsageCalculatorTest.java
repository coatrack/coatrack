package eu.coatrack.admin.service.report;

import eu.coatrack.admin.factories.ReportDataFactory;
import eu.coatrack.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static eu.coatrack.admin.factories.ReportDataFactory.*;
import static eu.coatrack.admin.utils.DateUtils.getTodayMinusOneMonthAsString;
import static eu.coatrack.api.ServiceAccessPaymentPolicy.WELL_DEFINED_PRICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class ApiUsageCalculatorTest {
    private final ApiUsageCalculator apiUsageCalculator;

    private final ApiUsageCounter apiUsageCounter;

    public ApiUsageCalculatorTest() {
        apiUsageCounter = mock(ApiUsageCounter.class);

        doReturn(getCallCount()).when(apiUsageCounter).count(any(ApiUsageDTO.class));

        apiUsageCalculator = new ApiUsageCalculator(apiUsageCounter);
    }

    @Test
    public void calculateForSpecificService_whenWellDefined() {


        ApiUsageDTO apiUsageDTO = getApiUsageDTO(getTodayMinusOneMonthAsString(), WELL_DEFINED_PRICE);

        List<ApiUsageReport> result = apiUsageCalculator.calculateForSpecificService(apiUsageDTO);
        // well defined price
        assertEquals(0.003, result.get(0).getTotal());
        assertEquals(0.008, result.get(1).getTotal());
        assertEquals(0.021, result.get(2).getTotal());



    }
    @Test
    public void calculateForSpecificService_whenMonthlyPayed() {


        ApiUsageDTO apiUsageDTO = getApiUsageDTO(getTodayMinusOneMonthAsString(), WELL_DEFINED_PRICE);

        List<ApiUsageReport> result = apiUsageCalculator.calculateForSpecificService(apiUsageDTO);


        // monthly calls
        assertEquals(200.0, result.get(3).getTotal());


    }
    @Test
    public void calculateForSpecificService_whenFree() {

        ApiUsageDTO apiUsageDTO = getApiUsageDTO(getTodayMinusOneMonthAsString(), WELL_DEFINED_PRICE);

        List<ApiUsageReport> result = apiUsageCalculator.calculateForSpecificService(apiUsageDTO);

        // free calls are free by default
        assertEquals(0, result.get(4).getTotal());
    }
    @Test
    public void calculateForSpecificService_whenNotMatching() {

        ApiUsageDTO apiUsageDTO = getApiUsageDTO(getTodayMinusOneMonthAsString(), WELL_DEFINED_PRICE);

        List<ApiUsageReport> result = apiUsageCalculator.calculateForSpecificService(apiUsageDTO);

        // not matchting calls are free by default
        assertEquals(0, result.get(5).getTotal());
    }

}
