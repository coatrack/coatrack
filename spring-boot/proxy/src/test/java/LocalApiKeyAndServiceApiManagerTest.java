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

import eu.coatrack.proxy.security.LocalApiKeyAndServiceApiManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class LocalApiKeyAndServiceApiManagerTest extends LocalApiKeyAndServiceApiManager{

    private final String someValidApiKeyValue = "ca716b82-745c-4f6d-a38b-ff8fe140ffd1";
    private ApiKey apiKey;

    private final long
            oneHourInMillis = 1000 * 60 * 60,
            oneDayInMillis = oneHourInMillis * 24;

    private final Timestamp
            now = new Timestamp(System.currentTimeMillis()),
            tomorrow = new Timestamp(now.getTime() + oneDayInMillis),
            yesterday = new Timestamp(now.getTime() - oneDayInMillis),
            halfAnHourAgo = new Timestamp(now.getTime() - oneHourInMillis / 2);

    public LocalApiKeyAndServiceApiManagerTest() {
        super(null, 60);
    }

    @BeforeEach
    public void createAnAcceptingDefaultSetup(){
        buildUpApiKey();
        buildUpVerifier();
    }

    private void buildUpApiKey() {
        apiKey = new ApiKey();
        apiKey.setKeyValue(someValidApiKeyValue);
        apiKey.setDeletedWhen(null);
        apiKey.setValidUntil(tomorrow);
    }

    private void buildUpVerifier() {
        localApiKeyList = new ArrayList<>();
        localApiKeyList.add(apiKey);
        latestLocalApiKeyListUpdate = LocalDateTime.now();
    }

    @Test
    public void validDefaultKeyShouldBeAccepted(){
        assertTrue(isApiKeyAuthorizedToAccessItsServiceApiConsideringTheLocalApiKeyList(apiKey.getKeyValue()));
    }

    @Test
    public void deletedApiKeysShouldBeDenied(){
        apiKey.setDeletedWhen(yesterday);
        assertFalse(isApiKeyAuthorizedToAccessItsServiceApiConsideringTheLocalApiKeyList(someValidApiKeyValue));
        apiKey.setDeletedWhen(halfAnHourAgo);
        assertFalse(isApiKeyAuthorizedToAccessItsServiceApiConsideringTheLocalApiKeyList(someValidApiKeyValue));
    }

    @Test
    public void expiredApiKeysShouldBeDenied(){
        apiKey.setValidUntil(yesterday);
        assertFalse(isApiKeyAuthorizedToAccessItsServiceApiConsideringTheLocalApiKeyList(someValidApiKeyValue));
        apiKey.setValidUntil(halfAnHourAgo);
        assertFalse(isApiKeyAuthorizedToAccessItsServiceApiConsideringTheLocalApiKeyList(someValidApiKeyValue));
    }

    @Test
    public void apiKeyFromLocalApiKeyListShouldBeAccepted(){
        assertTrue(isApiKeyAuthorizedToAccessItsServiceApiConsideringTheLocalApiKeyList(someValidApiKeyValue));
    }

    @Test
    public void apiKeyNotContainedInLocalApiKeyListShouldBeDenied(){
        localApiKeyList = new ArrayList<>();
        assertFalse(isApiKeyAuthorizedToAccessItsServiceApiConsideringTheLocalApiKeyList(someValidApiKeyValue));
    }

    @Test
    public void adminIsNotReachableForMoreThanOneHourSoValidApiKeysShouldBeDenied(){
        latestLocalApiKeyListUpdate = LocalDateTime.now().minusDays(1);
        assertFalse(isApiKeyAuthorizedToAccessItsServiceApiConsideringTheLocalApiKeyList(someValidApiKeyValue));
        latestLocalApiKeyListUpdate = LocalDateTime.now().minusHours(2);
        assertFalse(isApiKeyAuthorizedToAccessItsServiceApiConsideringTheLocalApiKeyList(someValidApiKeyValue));
    }

    @Test
    public void adminIsNotReachableForLessThanOneHourSoValidApiKeysShouldBeAccepted(){
        latestLocalApiKeyListUpdate = LocalDateTime.now().plusMinutes(30);
        assertTrue(isApiKeyAuthorizedToAccessItsServiceApiConsideringTheLocalApiKeyList(someValidApiKeyValue));
    }
}
