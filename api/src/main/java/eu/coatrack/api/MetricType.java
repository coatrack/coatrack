package eu.coatrack.api;

/*-
 * #%L
 * coatrack-api
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

/**
 * Types of metrics that should be transmitted from YGG proxy to YGG admin.
 *
 * @author gr-hovest(at)atb-bremen.de
 */
public enum MetricType {

    FORBIDDEN_REQUEST("Requests that have not been authorized"),
    AUTHORIZED_REQUEST("Authorized requests forwarded to service API"),
    RESPONSE("Responses from service API"),
    EMPTY_RESPONSE("Empty or no responses from service API");

    private final String displayString;

    MetricType(String displayString) {
        this.displayString = displayString;
    }

    /**
     * Description of the type, readable by end users.
     *
     * @return String to be displayed in GUI
     */
    public String getDisplayString() {
        return displayString;
    }
}
