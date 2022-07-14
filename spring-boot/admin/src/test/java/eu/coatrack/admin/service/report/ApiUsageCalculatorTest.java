package eu.coatrack.admin.service.report;

import eu.coatrack.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.mockito.Mockito.*;

public class ApiUsageCalculatorTest {

    @Autowired
    private ApiUsageCalculator apiUsageCalculator;

    private ApiUsageCounter apiUsageCounter;

    private ServiceApi getServiceApiDummy() {
        ServiceApi serviceApiDummy = mock(ServiceApi.class);
        doReturn(getEntryPointDummys()).when(serviceApiDummy).getEntryPoints();
        return serviceApiDummy;
    }

    private List<EntryPoint> getEntryPointDummys() {
        return Arrays.asList(new EntryPoint(), new EntryPoint(), new EntryPoint());
    }

    private Map<EntryPoint, Long> getCallsPerEntryPointMap() {
        Map<EntryPoint, Long> callsPerEntryPoint = new TreeMap<>();
        List<EntryPoint> entryPoints = getEntryPointDummys();
        callsPerEntryPoint.put(entryPoints.get(0), 3L);
        callsPerEntryPoint.put(entryPoints.get(1), 4L);
        callsPerEntryPoint.put(entryPoints.get(2), 7L);
        return callsPerEntryPoint;
    }

    private CallCount getCallCountDummy() {
        return new CallCount(
                1L,
                2L,
                3L,
                getCallsPerEntryPointMap()
        );
    }

    private ApiUsageDTO getApiUsageDTODummy() throws ParseException {
        ApiUsageDTO apiUsageDTO = mock(ApiUsageDTO.class);
        doReturn(getServiceApiDummy()).when(apiUsageDTO).getService();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);

        String dateInString = "7-Jun-2022";
        Date from = formatter.parse(dateInString);
        Date until = new Date();
        doReturn(from).when(apiUsageDTO).getFrom();
        doReturn(until).when(apiUsageDTO).getUntil();
        return apiUsageDTO;
    }

    public ApiUsageCalculatorTest() {
        apiUsageCounter = mock(ApiUsageCounter.class);
        doReturn(getCallCountDummy()).when(apiUsageCounter).count(any(ApiUsageDTO.class));

        apiUsageCalculator = new ApiUsageCalculator(apiUsageCounter);
    }

    @Test
    public void calculateForSpecificService() throws Exception {
        ApiUsageDTO apiUsageDTO = getApiUsageDTODummy();

        List<ApiUsageReport> result = apiUsageCalculator.calculateForSpecificService(apiUsageDTO);
        for(ApiUsageReport a : result)
            System.out.println(a);
    }


}
