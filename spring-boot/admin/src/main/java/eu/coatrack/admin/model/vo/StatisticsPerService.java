package eu.coatrack.admin.model.vo;

/*-
 * #%L
 * ygg-admin
 * %%
 * Copyright (C) 2013 - 2019 Corizon
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

public class StatisticsPerService {

    String service;
    long noOfCalls;
    long percentage;

    public StatisticsPerService(String service, long noOfCalls, long percentage) {
        this.service = service;
        this.noOfCalls = noOfCalls;
        this.percentage = percentage;
    }

    public String getService() {
        return service;
    }

    public long getNoOfCalls() {
        return noOfCalls;
    }

    public long getPercentage() {
        return percentage;
    }
}
