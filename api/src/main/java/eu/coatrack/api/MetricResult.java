package eu.coatrack.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class MetricResult {
    private String username;
    private long serviceId;
    private MetricType type;
    private long callsPerEntry;
    private String path;
    private String requestMethod;
}
