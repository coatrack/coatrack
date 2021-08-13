package eu.coatrack.proxy.metrics;

import eu.coatrack.api.MetricType;

import javax.servlet.http.HttpServletRequest;

public class MetricsHolder {

    private final HttpServletRequest request;
    private final String apiKeyValue;
    private final MetricType metricType;
    private final Integer httpResponseCode;

    public HttpServletRequest getRequest() {
        return request;
    }

    public String getApiKeyValue() {
        return apiKeyValue;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public Integer getHttpResponseCode() {
        return httpResponseCode;
    }

    public MetricsHolder(HttpServletRequest request, String apiKeyValue, MetricType metricType, Integer httpResponseCode) {
        this.request = request;
        this.apiKeyValue = apiKeyValue;
        this.metricType = metricType;
        this.httpResponseCode = httpResponseCode;
    }
}
