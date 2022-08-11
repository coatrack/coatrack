package eu.coatrack.admin.service.report;

import eu.coatrack.api.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static eu.coatrack.api.MetricType.RESPONSE;
import static eu.coatrack.api.ServiceAccessPaymentPolicy.MONTHLY_FEE;
import static eu.coatrack.api.ServiceAccessPaymentPolicy.WELL_DEFINED_PRICE;

public class ReportMockFactory {


    public static ServiceApi getServiceApi(long id, ServiceAccessPaymentPolicy accessPaymentPolicy, double monthlyFee) {
        ServiceApi serviceDummy = new ServiceApi();
        serviceDummy.setId(id);
        serviceDummy.setServiceAccessPaymentPolicy(accessPaymentPolicy);
        serviceDummy.setEntryPoints(getEntryPoints());
        serviceDummy.setOwner(getUser(0L, "Owner, simple"));
        serviceDummy.setMonthlyFee(monthlyFee);
        return serviceDummy;
    }

    public static List<ServiceApi> getServiceList() {
        return Arrays.asList(
                getServiceApi(1L, MONTHLY_FEE, 500L),
                getServiceApi(2L, WELL_DEFINED_PRICE, 1000L)
        );
    }

    @Deprecated
    public static List getMetricResultListAsObjectArray() {
        return Arrays.asList(
                new Object[]{"Peter", 1L, RESPONSE, 1L, "/a", "GET"},
                new Object[]{"Tiffany", 2L, RESPONSE, 2L, "/b", "GET"},
                new Object[]{"Bobo", 3L, RESPONSE, 3L, "/c", "GET"}
        );
    }

    public static List<MetricResult> getMetricResultList() {
        return Arrays.asList(
                new MetricResult("Peter", 1L, RESPONSE, 1L, "/a", "GET"),
                new MetricResult("Tiffany", 2L, RESPONSE, 2L, "/b", "GET"),
                new MetricResult("Bobo", 3L, RESPONSE, 3L, "/c", "GET")
        );
    }

    public static List<EntryPoint> getEntryPoints() {
        EntryPoint entryPointA = new EntryPoint(0L, "entryPointA", "/a", "GET", 1.0, 0);
        EntryPoint entryPointB = new EntryPoint(1L, "entryPointB", "/b", "GET", 2.0, 1);
        EntryPoint entryPointC = new EntryPoint(2L, "entryPointC", "/c", "GET", 3.0, 2);

        return Arrays.asList(entryPointA, entryPointB, entryPointC);
    }

    public static ApiUsageDTO getApiUsageDTO(ServiceAccessPaymentPolicy accessPaymentPolicy) {
        Date from = getDateFromString("25-06-2022");
        Date until = new Date();
        ApiUsageDTO apiUsageDTO = new ApiUsageDTO(
                getServiceApi(1L, accessPaymentPolicy, 100.0),
                getUser(1L, "Consumer, simple"),
                from,
                until,
                false,
                false
        );
        return apiUsageDTO;
    }

    public static List<ApiUsageReport> getApiUsageReports() {
        return Arrays.asList(
                new ApiUsageReport("ReportA", 1L, 100.0, 100.0),
                new ApiUsageReport("ReportB", 1L, 100.0, 100.0),
                new ApiUsageReport("ReportC", 1L, 100.0, 100.0)
        );
    }


    private static User getUser(long id, String name) {
        User userDummy = new User();
        userDummy.setId(id);
        userDummy.setUsername(name);
        return userDummy;
    }


    public static Date getDateFromString(String dateString) {
        Date date;
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
            date = formatter.parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return date;
    }

    public static Map<EntryPoint, Long> getEntryPointMap() {
        Map<EntryPoint, Long> callsPerEntryPoint = new TreeMap<>();
        List<EntryPoint> entryPoints = getEntryPoints();
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
