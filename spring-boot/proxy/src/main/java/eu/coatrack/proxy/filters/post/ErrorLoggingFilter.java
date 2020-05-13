package eu.coatrack.proxy.filters.post;

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

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import eu.coatrack.proxy.metrics.MetricsCounterService;
import eu.coatrack.api.MetricType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Error-filter, which logs errors that occur when proxying the request to the target service
 *
 * @author gr-hovest
 */
@Component
public class ErrorLoggingFilter extends ZuulFilter {

    // this context key is to be set in case an error is logged by this filter,
    // to tell the ResponseFilter that no further logging is necessary
    public static final String CONTEXT_KEY_ALREADY_LOGGED_AS_ERROR = "YGG-LOGGED-AS-ERROR";
    public static final String CONTEXT_VALUE_TRUE = "TRUE";

    private static Logger log = LoggerFactory.getLogger(ErrorLoggingFilter.class);

    @Autowired
    private MetricsCounterService metricsCounterService;

    @Override
    public String filterType() {
        return FilterConstants.ERROR_TYPE;
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();

        log.debug(String.format("An error occured after proxying a %s request to %s",
                request.getMethod(), request.getRequestURL().toString()));

        HttpServletResponse servletResponse = ctx.getResponse();
        log.debug(String.format("Erroneous response status is: %s", servletResponse.getStatus()));

        if (ctx.getResponseBody() == null && ctx.getResponseDataStream() == null) {
            log.warn("Response body and data stream are null - assuming that service is unavailable (logging status 503)");

            String apiKeyValue = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
            metricsCounterService.increment(
                    request,
                    apiKeyValue,
                    MetricType.EMPTY_RESPONSE,
                    HttpStatus.SERVICE_UNAVAILABLE.value());
            // set context parameter to prevent ResponseLoggingFilter from "double-logging"
            ctx.set(CONTEXT_KEY_ALREADY_LOGGED_AS_ERROR, CONTEXT_VALUE_TRUE);
        }
        return null;
    }
}
