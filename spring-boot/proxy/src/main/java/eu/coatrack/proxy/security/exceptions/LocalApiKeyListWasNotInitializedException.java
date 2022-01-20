package eu.coatrack.proxy.security.exceptions;

/*-
 * #%L
 * coatrack-proxy
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

/**
 * Is thrown, when the local API key list shall be used but has not been initialized. The gateway
 * never received a list of API Keys from the CoatRack Web Application before and can therefore not
 * be used for that purpose.
 */
public class LocalApiKeyListWasNotInitializedException extends RuntimeException {
    public LocalApiKeyListWasNotInitializedException(String msg) {
        super(msg);
    }
}
