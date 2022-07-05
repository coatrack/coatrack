package eu.coatrack.admin.service.report;

import eu.coatrack.api.EntryPoint;
import eu.coatrack.api.ServiceApi;
import lombok.Getter;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class ApiUsageCounter {
    private long freeCalls = 0;
    private long notMatchingCalls = 0;
    private long monthlyBilledCalls = 0;

    private final Map<EntryPoint, AtomicLong> noOfCallsPerEntryPoint = new TreeMap<>();

    public ApiUsageCounter(ServiceApi serviceApi) {
        for (EntryPoint entryPoint : serviceApi.getEntryPoints()) {
            noOfCallsPerEntryPoint.put(entryPoint, new AtomicLong(0L));
        }
    }

    public void addFree(long count) {
       freeCalls += count;
    }

    public void addMonthlyBilled(long count) {
        monthlyBilledCalls += count;
    }

    public void addNotMatching(long count) {
        notMatchingCalls += count;
    }

    public void addForEntryPoint(EntryPoint entryPoint, long count) {
        noOfCallsPerEntryPoint.get(entryPoint).addAndGet(count);
    }

    public boolean hasCallsPerEntryPoint() {
        return !noOfCallsPerEntryPoint.isEmpty();
    }



}
