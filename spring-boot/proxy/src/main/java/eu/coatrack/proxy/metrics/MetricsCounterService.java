package eu.coatrack.proxy.metrics;

/*-
 * #%L
 * coatrack-proxy
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

import eu.coatrack.api.Metric;
import eu.coatrack.api.MetricType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.sql.Date;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.coatrack.api.ApiKey;

public class MetricsCounterService {

    private static final Logger log = LoggerFactory.getLogger(MetricsCounterService.class);

    @Qualifier("counterService")
    @Autowired
    private CounterService counterService;

    private static final String PREFIX = "CUSTOM-METRICS";
    private static final String SEPARATOR = "___";

    private static final Pattern PATTERN_TO_SPLIT_SERVLET_PATH = Pattern.compile("^/?([^/]+)(/.*)?");
    private static final int MATCHER_GROUP_INDEX_OF_SERVICE_API_ID = 1;
    private static final int MATCHER_GROUP_INDEX_OF_PATH = 2;

    // this ID should be unique for each start of the proxy application, so that
    // CoatRack admin knows when counting was restarted
    private static final String counterSessionID = UUID.randomUUID().toString();

    public void increment(HttpServletRequest request, String apiKey, MetricType metricType, Integer httpResponseCode) {
        log.debug(String.format("incrementing metric '%s' for URI '%s' and api key %s",
                metricType,
                request.getRequestURI(),
                apiKey));

        String requestMethod = request.getMethod();

        String serviceApiName = null;
        String path = null;

        Matcher matcher = PATTERN_TO_SPLIT_SERVLET_PATH.matcher(request.getServletPath());

        if (matcher.find()) {
            // first element of the servlet path is the service api's name/id
            serviceApiName = matcher.group(MATCHER_GROUP_INDEX_OF_SERVICE_API_ID);
            // rest of the servlet path is the actual path that is called on the proxied
            // service
            path = matcher.group(MATCHER_GROUP_INDEX_OF_PATH);
            log.debug("matched servlet path '{}' with service uri identifier '{}' and path '{}'",
                    matcher.group(0), serviceApiName, path);
            if (path == null) {
                // if api consumer did not specifc path, set as "/"
                path = "/";
            } else if (path.endsWith("/") && !path.equals("/")) {
                // remove trailing "/" as this is the same path from metrics point of view
                path = path.substring(0, path.length() - 1);
            }
        } else {
            log.warn("matcher {} did not match servlet path {}", matcher, request.getServletPath());
        }

        LocalDate today = LocalDate.now();

        counterService.increment(new StringBuilder()
                .append(PREFIX) // [0]
                .append(SEPARATOR)
                .append(serviceApiName) // [1]
                .append(SEPARATOR)
                .append(requestMethod) // [2]
                .append(SEPARATOR)
                .append(apiKey) // [3]
                .append(SEPARATOR)
                .append(metricType) // [4]
                .append(SEPARATOR)
                .append(httpResponseCode) // [5]
                .append(SEPARATOR)
                .append(today) // [6]
                .append(SEPARATOR)
                .append(path) // [7]
                .toString());
    }

    public Metric filterAndTransformMetric(org.springframework.boot.actuate.metrics.Metric inputMetric) {

        String[] elements = inputMetric.getName().split(SEPARATOR);
        Assert.noNullElements(elements);

        // check if this is a custom metric
        if (elements[0].equals("counter." + PREFIX)) {
            log.debug("Metrics for transmission: " + inputMetric.getName());

            Metric outputMetric = new Metric();
            outputMetric.setRequestMethod(elements[2]);
            outputMetric.setCount(inputMetric.getValue().intValue());
            outputMetric.setMetricsCounterSessionID(counterSessionID);
            outputMetric.setType(MetricType.valueOf(elements[4]));

            // add a tmp api key object with just the key value, will later be mapped to
            // entity on CoatRack admin side
            ApiKey tmpApiKey = new ApiKey();
            tmpApiKey.setKeyValue(elements[3]);
            outputMetric.setApiKey(tmpApiKey);

            String httpStatusString = elements[5];
            Integer httpStatus = httpStatusString.equals("null") ? null : new Integer(httpStatusString);
            outputMetric.setHttpResponseCode(httpStatus);

            String dateString = elements[6];
            log.debug("date string is: {}", dateString);
            LocalDate dateOfApiUsage = LocalDate.parse(dateString);
            log.debug("date of api usage parsed: {}", dateOfApiUsage);
            outputMetric.setDateOfApiCall(Date.valueOf(dateOfApiUsage));

            outputMetric.setPath(elements[7]);

            return outputMetric;
        } else {
            // generic spring metric - not to be transmitted for now
            log.debug("Metrics not relevant for transmission: " + inputMetric);
            return null;
        }
    }
}
