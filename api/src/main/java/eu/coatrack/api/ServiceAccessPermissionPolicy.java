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
 * Policies supported by YGG related to service API access permission management
 *
 * @author gr-hovest
 */
public enum ServiceAccessPermissionPolicy {

    PERMISSION_NECESSARY("Restricted", "Restricted - Explicit permission is required to access this service API"),
    PUBLIC("Public", "Public - Everyone is allowed to access this service API");

    private final String displayStringShort;
    private final String displayStringVerbose;

    ServiceAccessPermissionPolicy(String displayStringShort, String displayStringVerbose) {
        this.displayStringShort = displayStringShort;
        this.displayStringVerbose = displayStringVerbose;
    }

    /**
     * Description of the policy, readable by end users.
     *
     * @return String to be displayed in GUI
     */
    public String getDisplayStringShort() {
        return displayStringShort;
    }

    public String getDisplayStringVerbose() {
        return displayStringVerbose;
    }
}
