package eu.coatrack.proxy.metrics;

/*-
 * #%L
 * coatrack-proxy
 * %%
 * Copyright (C) 2013 - 2021 Corizon | Institut für angewandte Systemtechnik Bremen GmbH (ATB)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
