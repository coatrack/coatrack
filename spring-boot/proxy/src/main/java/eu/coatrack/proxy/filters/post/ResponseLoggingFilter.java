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
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Post-filter, which logs the response received from the target service
 *
 * @author gr-hovest
 */
public class ResponseLoggingFilter extends ZuulFilter {

    private static Logger log = LoggerFactory.getLogger(ResponseLoggingFilter.class);

    @Autowired
    private MetricsCounterService metricsCounterService;

    @Override
    public String filterType() {
        return FilterConstants.POST_TYPE;
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

        log.debug(
                String.format("Response to %s request to %s", request.getMethod(), request.getRequestURL().toString()));

        // check if this response was already logged as error
        Object contextParamLoggedAsError = ctx.get(ErrorLoggingFilter.CONTEXT_KEY_ALREADY_LOGGED_AS_ERROR);
        if (contextParamLoggedAsError != null
                && contextParamLoggedAsError.equals(ErrorLoggingFilter.CONTEXT_VALUE_TRUE)) {

            log.debug("This response was already logged by error filter, not to be logged by this response filter");
        } else {
            HttpServletResponse servletResponse = ctx.getResponse();
            log.debug(String.format("Response status is: %s", servletResponse.getStatus()));

            MetricType metricType = MetricType.RESPONSE;

            if (ctx.getResponseBody() == null && ctx.getResponseDataStream() == null) {
                log.warn("Response body and data stream are null");
                metricType = MetricType.EMPTY_RESPONSE;
            }
            String apiKeyValue = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
            metricsCounterService.increment(request, apiKeyValue, metricType, servletResponse.getStatus());
        }
        return null;
    }
}
