package eu.coatrack.proxy.security;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This custom access decision voter checks if the API key that was
 * transmitted by the API consumer is valid for the service API that
 * the consumer is trying to access.
 * <p>
 * This check is done by comparing the service API's URI identifier
 * to the relevant part of the called URL.
 *
 * @author gr-hovest
 */
public class ServiceApiAccessRightsVoter implements AccessDecisionVoter<FilterInvocation> {

    public static final String ACCESS_SERVICE_AUTHORITY_PREFIX = "ACCESS_SERVICE_";

    private static final Pattern PATTERN_TO_GET_SERVICE_API_ID = Pattern.compile("^/?([^/?]+).*");
    private static final int MATCHER_GROUP_INDEX_OF_SERVICE_API_ID = 1;

    Logger log = LoggerFactory.getLogger(ServiceApiAccessRightsVoter.class);

    @Override
    public boolean supports(ConfigAttribute attribute) {
        log.debug("custom voter asked for config attribute {}", attribute);
        return true;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        log.debug("custom voter asked for class {}", clazz);
        return FilterInvocation.class.isAssignableFrom(clazz);
    }

    @Override
    public int vote(Authentication authentication, FilterInvocation filterInvocation, Collection<ConfigAttribute> attributes) {
        log.debug("authentication '{}' filter invocation '{}' attributes '{}'",
                authentication, filterInvocation, attributes.stream().map(a -> a.toString()).toString());

        HttpServletRequest httpServletRequest = filterInvocation.getHttpRequest();

        String servletPath = httpServletRequest.getServletPath();

        Matcher matcher = PATTERN_TO_GET_SERVICE_API_ID.matcher(servletPath);

        if (matcher.find()) {
            String serviceApiUriID = matcher.group(MATCHER_GROUP_INDEX_OF_SERVICE_API_ID);
            log.debug("matched servlet path '{}' with service uri identifier '{}'", matcher.group(0), serviceApiUriID);

            String authorityNeededToAccessThisServlet = ACCESS_SERVICE_AUTHORITY_PREFIX + serviceApiUriID;
            boolean accessToBeGranted = authentication.getAuthorities().stream()
                    .anyMatch(ga -> ga.getAuthority().equals(authorityNeededToAccessThisServlet));

            log.debug("custom voter checked if needed authority '{}' is granted to api caller, result is '{}'",
                    authorityNeededToAccessThisServlet, accessToBeGranted);
            return accessToBeGranted ? ACCESS_GRANTED : ACCESS_DENIED;
        }
        return ACCESS_ABSTAIN;
    }
}
