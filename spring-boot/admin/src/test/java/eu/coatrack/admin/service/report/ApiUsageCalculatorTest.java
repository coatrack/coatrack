package eu.coatrack.admin.service.report;

import eu.coatrack.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static eu.coatrack.api.ServiceAccessPaymentPolicy.MONTHLY_FEE;
import static org.mockito.Mockito.*;

public class ApiUsageCalculatorTest {

    @Autowired
    private ApiUsageCalculator apiUsageCalculator;

    private ApiUsageCounter apiUsageCounter;

    public ApiUsageCalculatorTest() {
        apiUsageCounter = mock(ApiUsageCounter.class);
        doReturn(ReportMockFactory.getCallCountDummy()).when(apiUsageCounter).count(any(ApiUsageDTO.class));

        apiUsageCalculator = new ApiUsageCalculator(apiUsageCounter);
    }

    @Test
    public void calculateForSpecificService() {
        ApiUsageDTO apiUsageDTO = ReportMockFactory.getApiUsageDTO(MONTHLY_FEE);

        List<ApiUsageReport> result = apiUsageCalculator.calculateForSpecificService(apiUsageDTO);
        for(ApiUsageReport a : result)
            System.out.println(a.getName());
    }


}
