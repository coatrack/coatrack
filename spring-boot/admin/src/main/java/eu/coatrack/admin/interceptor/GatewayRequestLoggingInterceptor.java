package eu.coatrack.admin.interceptor;

/*-
 * #%L
 * coatrack-admin
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

@Component
public class GatewayRequestLoggingInterceptor extends HandlerInterceptorAdapter {

    private static final Logger log = LoggerFactory.getLogger(GatewayRequestLoggingInterceptor.class);

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {

        log.info(String.format(
                "Request (preHandle): the url called is '%s' and the headers are [%s]",
                request.getRequestURI(),
                generateStringOfRequestHeaders(request)));
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {

        log.info(String.format(
                "Response status '%s' after completion: the url called was '%s' and the headers were [%s] ",
                response.getStatus(),
                request.getRequestURI(),
                generateStringOfRequestHeaders(request)));
    }

    private String generateStringOfRequestHeaders(HttpServletRequest request) {
        StringBuilder headers = new StringBuilder();

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.append(String.format("'%s': '%s'; ", headerName, request.getHeader(headerName)));
        }
        return headers.toString();
    }
}