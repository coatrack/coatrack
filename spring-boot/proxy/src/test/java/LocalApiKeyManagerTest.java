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
import eu.coatrack.api.ApiKey;

import eu.coatrack.proxy.security.LocalApiKeyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class LocalApiKeyManagerTest extends LocalApiKeyManager {

    private final static long oneHourInMinutes = 60;

    private final String someValidApiKeyValue = "ca716b82-745c-4f6d-a38b-ff8fe140ffd1";
    private ApiKey apiKey;

    public LocalApiKeyManagerTest() {
        super(null, oneHourInMinutes);
    }

    @BeforeEach
    public void createAnAcceptingDefaultSetup(){
        apiKey = new ApiKey();
        apiKey.setKeyValue(someValidApiKeyValue);

        localApiKeyList = new ArrayList<>();
        localApiKeyList.add(apiKey);
        latestLocalApiKeyListUpdate = LocalDateTime.now();
    }

    @Test
    public void apiKeyFromLocalApiKeyListShouldBeAccepted(){
        assertTrue(isApiKeyAuthorizedConsideringLocalApiKeyList(someValidApiKeyValue));
    }

    @Test
    public void apiKeyNotContainedInLocalApiKeyListShouldBeDenied(){
        localApiKeyList = new ArrayList<>();
        assertFalse(isApiKeyAuthorizedConsideringLocalApiKeyList(someValidApiKeyValue));
    }

    @Test
    public void adminIsNotReachableForLessThanOneHourSoValidApiKeysShouldBeAccepted(){
        latestLocalApiKeyListUpdate = LocalDateTime.now().minusMinutes(oneHourInMinutes - 1);
        assertTrue(isApiKeyAuthorizedConsideringLocalApiKeyList(someValidApiKeyValue));
    }

    @Test
    public void adminIsNotReachableForMoreThanOneHourSoValidApiKeysShouldBeDenied(){
        latestLocalApiKeyListUpdate = LocalDateTime.now().minusMinutes(oneHourInMinutes + 1);
        assertFalse(isApiKeyAuthorizedConsideringLocalApiKeyList(someValidApiKeyValue));
    }
}
