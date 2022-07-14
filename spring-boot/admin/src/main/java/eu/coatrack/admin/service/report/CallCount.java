package eu.coatrack.admin.service.report;

import eu.coatrack.api.EntryPoint;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.TreeMap;


@AllArgsConstructor
@NoArgsConstructor
public class CallCount {
    private long freeCalls = 0;
    private long notMatchingCalls = 0;
    private long monthlyBilledCalls = 0;
    private Map<EntryPoint, Long> callsPerEntryPoint;

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
        if(!hasEntryPoint(entryPoint))
            callsPerEntryPoint.put(entryPoint, 0L);
        callsPerEntryPoint.merge(entryPoint, count, Long::sum);
    }

    public long getCallsByEntryPoint(EntryPoint entryPoint) {
        return callsPerEntryPoint.get(entryPoint);
    }

    public Map<EntryPoint, Long> getCallsPerEntryPoint() {
        return callsPerEntryPoint;
    }

    public long getFreeCalls() {
        return freeCalls;
    }

    public long getNotMatchingCalls() {
        return notMatchingCalls;
    }

    public long getMonthlyBilledCalls() {
        return monthlyBilledCalls;
    }

    public boolean hasCallsPerEntryPoint() {
        return !callsPerEntryPoint.isEmpty();
    }

    private boolean hasEntryPoint(EntryPoint entryPoint) {
        return callsPerEntryPoint.containsKey(entryPoint);
    }

}
