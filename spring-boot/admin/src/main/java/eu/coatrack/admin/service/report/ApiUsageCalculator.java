package eu.coatrack.admin.service.report;

import eu.coatrack.api.ApiUsageReport;
import eu.coatrack.api.EntryPoint;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class ApiUsageCalculator {

    @Autowired
    private ApiUsageCounter counter;

    public List<ApiUsageReport> calculateForSpecificService(ApiUsageDTO apiUsageDTO) {
        List<ApiUsageReport> apiUsageReports = new ArrayList<>();
        CallCount count = counter.count(apiUsageDTO);

        if (count.hasCallsPerEntryPoint()) {
            for (EntryPoint entryPoint : apiUsageDTO.getService().getEntryPoints()) {
                String name = String.format("%s (%s %s)", entryPoint.getName(), entryPoint.getHttpMethod(), entryPoint.getPathPattern());
                long numberOfCalls = count.getCallsByEntryPoint(entryPoint);
                double price = BigDecimal.valueOf(entryPoint.getPricePerCall()).doubleValue();
                double total = BigDecimal.valueOf(entryPoint.getPricePerCall() * count.getCallsByEntryPoint(entryPoint) / 1000).doubleValue();
                apiUsageReports.add(new ApiUsageReport(name, numberOfCalls, price, total));
            }
        }

        if (count.getMonthlyBilledCalls() > 0) {
            int diffMonth = getMonthDifference(apiUsageDTO.getFrom(), apiUsageDTO.getUntil());
            ApiUsageReport apiUsageReportForMonthlyFlatrate = new ApiUsageReport(
                    "All Calls",
                    count.getMonthlyBilledCalls(),
                    apiUsageDTO.getService().getMonthlyFee(),
                    apiUsageDTO.getService().getMonthlyFee() * diffMonth
            );
            apiUsageReports.add(apiUsageReportForMonthlyFlatrate);
        }

        if (count.getNotMatchingCalls() > 0) {
            apiUsageReports.add(new ApiUsageReport("Other Calls", count.getNotMatchingCalls(), 0, 0));
        }

        if (count.getFreeCalls() > 0) {
            apiUsageReports.add(new ApiUsageReport("Free Calls", count.getFreeCalls(), 0, 0));
        }


        apiUsageReports.forEach(reportRow -> log.debug("row for report: " + reportRow));
        return apiUsageReports;
    }

    private static int getMonthDifference(Date from, Date until) {
        Calendar startCalendar = new GregorianCalendar();
        startCalendar.setTime(from);
        Calendar endCalendar = new GregorianCalendar();
        endCalendar.setTime(until);
        int diffYear = endCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR);
        int diffMonth = diffYear * 12 + endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH);
        return diffMonth;
    }

    public void setCounter(ApiUsageCounter counter) {
        this.counter = counter;
    }
}
