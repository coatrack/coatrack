package eu.coatrack.admin.service.report;

import eu.coatrack.api.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static eu.coatrack.api.MetricType.RESPONSE;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ReportDummyFactory {


    public static ServiceApi getServiceApiDummy(ServiceAccessPaymentPolicy accessPaymentPolicy) {
        ServiceApi serviceApiDummy = mock(ServiceApi.class);
        doReturn(accessPaymentPolicy).when(serviceApiDummy).getServiceAccessPaymentPolicy();
        doReturn(getEntryPointDummys()).when(serviceApiDummy).getEntryPoints();
        doReturn(getOwnerDummy()).when(serviceApiDummy).getOwner();
        return serviceApiDummy;
    }

    private static User getOwnerDummy() {
        User ownerDummy = mock(User.class);
        doReturn(0L).when(ownerDummy).getId();
        doReturn("Ownie, der Owner").when(ownerDummy).getUsername();
        return ownerDummy;
    }

    // Will produce typed list after refactoring MetricsAggregationCustomRepository
    @Deprecated
    public static List getMetricResultList() {
        Object[] metricA = new Object[]{"Peter", 1L, RESPONSE, 1L, "/test", "GET"};
        Object[] metricB = new Object[]{"Tiffany", 2L, RESPONSE, 2L, "/test", "GET"};
        Object[] metricC = new Object[]{"Bobo", 3L, RESPONSE, 3L, "/test", "GET"};
        List<Object[]> metricResultList = Arrays.asList(metricA, metricB, metricC);
        return metricResultList;
    }


    public static List<EntryPoint> getEntryPointDummys() {
        EntryPoint entryPointA = new EntryPoint(0L, "entryPointA", "/test", "GET", 1.0, 0);
        EntryPoint entryPointB = new EntryPoint(1L, "entryPointA", "/test", "GET", 2.0, 1);
        EntryPoint entryPointC = new EntryPoint(2L, "entryPointA", "/test", "GET", 3.0, 2);

        return Arrays.asList(entryPointA, entryPointB, entryPointC);
    }

    public static ApiUsageDTO getApiUsageDTODummy(ServiceAccessPaymentPolicy accessPaymentPolicy) {
        ApiUsageDTO apiUsageDTODummy = mock(ApiUsageDTO.class);
        Date from = getDateFromString("25-06-2022");
        Date until = new Date();

        doReturn(from).when(apiUsageDTODummy).getFrom();
        doReturn(until).when(apiUsageDTODummy).getUntil();
        doReturn(getConsumerDummy()).when(apiUsageDTODummy).getConsumer();
        doReturn(getServiceApiDummy(accessPaymentPolicy)).when(apiUsageDTODummy).getService();
        return apiUsageDTODummy;
    }

    private static User getConsumerDummy() {
        User consumer = mock(User.class);
        doReturn(666L).when(consumer).getId();
        return consumer;
    }

    public static Date getDateFromString(String dateString) {
        Date date = null;
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
            date = formatter.parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return date;
    }
}
