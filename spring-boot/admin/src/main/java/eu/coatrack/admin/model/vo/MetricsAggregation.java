package eu.coatrack.admin.model.vo;

/*-
 * #%L
 * coatrack-admin
 * %%
 * Copyright (C) 2013 - 2020 Corizon | Institut für angewandte Systemtechnik Bremen GmbH (ATB)
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

public class MetricsAggregation {

    private final String serviceApiName;
    private final String apiUser;
    private final MetricType type;
    private final String requestMethod;
    private final String path;
    private final Integer httpResponseCode;
    private final long count;

    public MetricsAggregation(String serviceApiName, String apiUser, MetricType type, String requestMethod, String path,
            Integer httpResponseCode, long count) {
        this.serviceApiName = serviceApiName;
        this.apiUser = apiUser;
        this.type = type;
        this.requestMethod = requestMethod;
        this.path = path;
        this.httpResponseCode = httpResponseCode;
        this.count = count;
    }

    public String getServiceApiName() {
        return serviceApiName;
    }

    public String getApiUser() {
        return apiUser;
    }

    public MetricType getType() {
        return type;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public String getPath() {
        return path;
    }

    public Integer getHttpResponseCode() {
        return httpResponseCode;
    }

    public long getCount() {
        return count;
    }

}
