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
 * Policies supported by YGG related to payment for service API usage
 *
 * @author gr-hovest
 */
public enum ServiceAccessPaymentPolicy {

    MONTHLY_FEE("Monthly fee", "Monthly fee"), WELL_DEFINED_PRICE("Pay per call", "Pay per call"),
    FOR_FREE("For free", "Access is for free");

    private final String displayStringShort;
    private final String displayStringVerbose;

    ServiceAccessPaymentPolicy(String displayStringShort, String displayStringVerbose) {
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
