package eu.coatrack.proxy.filters.pre;

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
import eu.coatrack.proxy.metrics.TemporaryMetricsAggregation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;

/**
 * Pre-filter logging the incoming request, which is forwarded to the target URL
 *
 * @author gr-hovest
 */
public class RequestLoggingFilter extends ZuulFilter {

    private static Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Autowired
    private MetricsCounterService metricsCounterService;

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
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

        log.debug(String.format("Received a %s request to %s", request.getMethod(), request.getRequestURL().toString()));

        String apiKeyValue = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        metricsCounterService.increment(new TemporaryMetricsAggregation(
                request,
                apiKeyValue,
                MetricType.AUTHORIZED_REQUEST,
                null));

        return null;
    }

}
