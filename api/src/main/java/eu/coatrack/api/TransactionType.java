package eu.coatrack.api;

/*-
 * #%L
 * ygg-api
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

public enum TransactionType {
    DEPOSIT("Deposit"),
    DEPOSIT_IN_TRANSIT("Deposit in transit"),
    SERVICE_DEPOSIT("Service Deposit"),
    FEE("Fee"),
    WITHDRAWAL("Withdrawal");

    private final String displayString;

    TransactionType(String displayString) {
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
