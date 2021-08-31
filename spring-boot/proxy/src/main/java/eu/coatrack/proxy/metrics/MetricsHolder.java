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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetricsHolder {

    private static final Logger log = LoggerFactory.getLogger(MetricsHolder.class);

    private static final Pattern PATTERN_TO_SPLIT_SERVLET_PATH = Pattern.compile("^/?([^/]+)(/.*)?");
    private static final int MATCHER_GROUP_INDEX_OF_SERVICE_API_ID = 1;
    private static final int MATCHER_GROUP_INDEX_OF_PATH = 2;

    private final HttpServletRequest request;
    private final String apiKeyValue;
    private final MetricType metricType;
    private final Integer httpResponseCode;

    private String requestMethod;
    private String serviceApiName;
    private String path;
    private Matcher matcher;

    public MetricsHolder(HttpServletRequest request, String apiKeyValue, MetricType metricType, Integer httpResponseCode) {
        this.request = request;
        this.apiKeyValue = apiKeyValue;
        this.metricType = metricType;
        this.httpResponseCode = httpResponseCode;
        initializeResidualFields();
    }

    private void initializeResidualFields() {
        requestMethod = request.getMethod();
        matcher = PATTERN_TO_SPLIT_SERVLET_PATH.matcher(request.getServletPath());

        if (matcher.find()) {
            // first element of the servlet path is the service api's name/id
            serviceApiName = matcher.group(MATCHER_GROUP_INDEX_OF_SERVICE_API_ID);
            initializePath();
        } else {
            log.warn("matcher {} did not match servlet path {}", matcher, request.getServletPath());
        }
    }

    private void initializePath() {
        // rest of the servlet path is the actual path that is called on the proxied service
        path = matcher.group(MATCHER_GROUP_INDEX_OF_PATH);
        log.debug("matched servlet path '{}' with service uri identifier '{}' and path '{}'",
                matcher.group(0), serviceApiName, path);
        if (path == null) {
            // if api consumer did not specific path, set as "/"
            path = "/";
        } else if (path.endsWith("/") && !path.equals("/")) {
            // remove trailing "/" as this is the same path from metrics point of view
            path = path.substring(0, path.length()-1);
        }
    }

    public Integer getHttpResponseCode() {
        return httpResponseCode;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public String getPath() {
        return path;
    }

    public String getApiKeyValue() {
        return apiKeyValue;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public CharSequence getServiceApiName() {
        return serviceApiName;
    }

    public String getRequestURI() {
        return request.getRequestURI();
    }
}
