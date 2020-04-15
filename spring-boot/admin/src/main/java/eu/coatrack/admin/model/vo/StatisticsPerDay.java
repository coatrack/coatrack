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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * @author gr-hovest(at)atb-bremen.de
 */
public class StatisticsPerDay {

    Date date;
    long noOfCalls;

    public StatisticsPerDay(Date date, long noOfCalls) {
        this.date = date;
        this.noOfCalls = noOfCalls;
    }

    public Date getDate() {
        return date;
    }

    public LocalDate getLocalDate() {
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        } else {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
    }

    public long getNoOfCalls() {
        return noOfCalls;
    }
}
