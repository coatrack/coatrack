package eu.coatrack.api;


public class MetricResult {
    private String username;
    private long serviceId;
    private MetricType type;
    private long callsPerEntry;
    private String path;
    private String requestMethod;

    public MetricResult(String username, long serviceId, MetricType metricType, long numberOfCalls, String path, String requestMethod) {
        this.username = username;
        this.serviceId = serviceId;
        this.type = metricType;
        this.callsPerEntry = numberOfCalls;
        this.path = path;
        this.requestMethod = requestMethod;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getServiceId() {
        return serviceId;
    }

    public void setServiceId(long serviceId) {
        this.serviceId = serviceId;
    }

    public MetricType getType() {
        return type;
    }

    public void setType(MetricType type) {
        this.type = type;
    }

    public long getCallsPerEntry() {
        return callsPerEntry;
    }

    public void setCallsPerEntry(long callsPerEntry) {
        this.callsPerEntry = callsPerEntry;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }
}
