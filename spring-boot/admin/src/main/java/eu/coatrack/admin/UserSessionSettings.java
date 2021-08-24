package eu.coatrack.admin;

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

import java.io.Serializable;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UserSessionSettings implements Serializable {

    private LocalDate dashboardDateRangeStart;
    private LocalDate dashboardDateRangeEnd;

    public UserSessionSettings() {
        // default settings for new user session
        // dashboard to show statistics for last 7 days
        dashboardDateRangeEnd = LocalDate.now();
        dashboardDateRangeStart = dashboardDateRangeEnd.minusDays(6);
    }

    public LocalDate getDashboardDateRangeStart() {
        return dashboardDateRangeStart;
    }

    public void setDashboardDateRangeStart(LocalDate dashboardDateRangeStart) {
        this.dashboardDateRangeStart = dashboardDateRangeStart;
    }

    public LocalDate getDashboardDateRangeEnd() {
        return dashboardDateRangeEnd;
    }

    public void setDashboardDateRangeEnd(LocalDate dashboardDateRangeEnd) {
        this.dashboardDateRangeEnd = dashboardDateRangeEnd;
    }

    @Override
    public String toString() {
        return "UserSessionSettings{" +
                "dashboardDateRangeStart=" + dashboardDateRangeStart +
                ", dashboardDateRangeEnd=" + dashboardDateRangeEnd +
                '}';
    }
}
