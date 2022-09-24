package eu.coatrack.admin.datafactories;

import eu.coatrack.admin.service.report.ApiUsageDTO;
import eu.coatrack.admin.service.report.CallCount;
import eu.coatrack.api.*;
import lombok.Getter;

import java.util.*;

import static eu.coatrack.admin.utils.DateUtils.getDateFromString;
import static eu.coatrack.api.MetricType.RESPONSE;
import static eu.coatrack.api.ServiceAccessPaymentPolicy.MONTHLY_FEE;
import static eu.coatrack.api.ServiceAccessPaymentPolicy.WELL_DEFINED_PRICE;

@Getter
public class ReportDataFactory {

    public final static User consumer = getUser(1L, "Pete");

    public final static List<User> consumers = Arrays.asList(
            consumer,
            getUser(2L, "Hans"),
            getUser(3L, "Steffen")
    );

    public final static List<ServiceApi> serviceApis = Arrays.asList(
            getServiceApi(1L, MONTHLY_FEE, 500L),
            getServiceApi(2L, WELL_DEFINED_PRICE, 1000L)
    );

    @Deprecated
    public final static List<Object[]> metricResultListObjectArrays = Arrays.asList(
            new Object[]{"Peter", 1L, RESPONSE, 1L, "/a", "GET"},
            new Object[]{"Tiffany", 2L, RESPONSE, 2L, "/b", "GET"},
            new Object[]{"Bobo", 3L, RESPONSE, 3L, "/c", "GET"}
    );

    public final static List<MetricResult> metricResultList = Arrays.asList(
            new MetricResult("Peter", 1L, RESPONSE, 1L, "/a", "GET"),
            new MetricResult("Tiffany", 2L, RESPONSE, 2L, "/b", "GET"),
            new MetricResult("Bobo", 3L, RESPONSE, 3L, "/c", "GET")
    );

    public final static List<EntryPoint> entryPoints = Arrays.asList(
            new EntryPoint(0L, "entryPointA", "/a", "GET", 1.0, 0),
            new EntryPoint(1L, "entryPointB", "/b", "GET", 2.0, 1),
            new EntryPoint(2L, "entryPointC", "/c", "GET", 3.0, 2)
    );

    public final static List<ApiUsageReport> apiUsageReports = Arrays.asList(
            new ApiUsageReport("ReportA", 1L, 100.0, 100.0),
            new ApiUsageReport("ReportB", 1L, 100.0, 100.0),
            new ApiUsageReport("ReportC", 1L, 100.0, 100.0)
    );


    public final static List<String> payPerCallServiceIds = Arrays.asList("1", "2", "3");

    public final static long selectedServiceId = -1L;
    public final static long selectedApiConsumerUserId = -1L;
    public final static boolean considerOnlyPaidCalls = false;


    public static ServiceApi getServiceApi(long id, ServiceAccessPaymentPolicy accessPaymentPolicy, double monthlyFee) {
        ServiceApi serviceDummy = new ServiceApi();
        serviceDummy.setId(id);
        serviceDummy.setServiceAccessPaymentPolicy(accessPaymentPolicy);
        serviceDummy.setEntryPoints(entryPoints);
        serviceDummy.setOwner(getUser(0L, "Owner, simple"));
        serviceDummy.setMonthlyFee(monthlyFee);
        return serviceDummy;
    }
    public static ApiUsageDTO getApiUsageDTO(String fromString, ServiceAccessPaymentPolicy accessPaymentPolicy) {
        Date from = getDateFromString(fromString);
        ApiUsageDTO apiUsageDTO = new ApiUsageDTO(
                getServiceApi(1L, accessPaymentPolicy, 100.0),
                getUser(1L, "Consumer, simple"),
                from,
                new Date(), // TODO this can be a problem
                false,
                false
        );
        return apiUsageDTO;
    }

    public static User getUser(long id, String name) {
        User userDummy = new User();
        userDummy.setId(id);
        userDummy.setUsername(name);
        return userDummy;
    }

    public static Map<EntryPoint, Long> getEntryPointMap() {
        Map<EntryPoint, Long> callsPerEntryPoint = new TreeMap<>();
        callsPerEntryPoint.put(entryPoints.get(0), 3L);
        callsPerEntryPoint.put(entryPoints.get(1), 4L);
        callsPerEntryPoint.put(entryPoints.get(2), 7L);
        return callsPerEntryPoint;
    }

    public static CallCount getCallCount() {
        return new CallCount(
                1L,
                2L,
                3L,
                getEntryPointMap()
        );
    }
}
