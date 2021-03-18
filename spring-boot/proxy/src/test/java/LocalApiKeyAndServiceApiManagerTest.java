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

    private final String someValidApiKeyValue = "someValidApiKeyValue";

    private ApiKey apiKey;

    private final long
            oneHourInMillis = 1000 * 60 * 60,
            oneDayInMillis = oneHourInMillis * 24;

    private final Timestamp
            now = new Timestamp(System.currentTimeMillis()),
            tomorrow = new Timestamp(now.getTime() + oneDayInMillis),
            yesterday = new Timestamp(now.getTime() - oneDayInMillis),
            halfAnHourAgo = new Timestamp(now.getTime() - oneHourInMillis / 2);

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
    public void isDefaultKeyAccepted(){
        assertTrue(isApiKeyValidConsideringLocalApiKeyList(apiKey.getKeyValue()));
    }

    @Test
    public void testIfDeletedKeyIsDenied(){
        apiKey.setDeletedWhen(yesterday);
        assertFalse(isApiKeyValidConsideringLocalApiKeyList(someValidApiKeyValue));
        apiKey.setDeletedWhen(halfAnHourAgo);
        assertFalse(isApiKeyValidConsideringLocalApiKeyList(someValidApiKeyValue));
    }

    @Test
    public void testIfExpiredKeyIsDenied(){
        apiKey.setValidUntil(yesterday);
        assertFalse(isApiKeyValidConsideringLocalApiKeyList(someValidApiKeyValue));
        apiKey.setValidUntil(halfAnHourAgo);
        assertFalse(isApiKeyValidConsideringLocalApiKeyList(someValidApiKeyValue));
    }

    @Test
    public void testIfApiKeyIsAcceptedSinceItIsInTheLocalApiKeyList(){
        assertTrue(isApiKeyValidConsideringLocalApiKeyList(someValidApiKeyValue));
    }

    @Test
    public void testIfApiKeyIsDeniedBecauseOfEmptyLocalApiKeyList(){
        localApiKeyList = new ArrayList<>();
        assertFalse(isApiKeyValidConsideringLocalApiKeyList(someValidApiKeyValue));
    }

    @Test
    public void testIfApiKeyIsDeniedBecauseAdminIsNotReachableForMoreThanOneHour(){
        latestLocalApiKeyListUpdate = LocalDateTime.now().minusDays(1);
        assertFalse(isApiKeyValidConsideringLocalApiKeyList(someValidApiKeyValue));
        latestLocalApiKeyListUpdate = LocalDateTime.now().minusHours(2);
        assertFalse(isApiKeyValidConsideringLocalApiKeyList(someValidApiKeyValue));
    }

    @Test
    public void testIfApiKeyIsAcceptedBecauseAdminIsNotReachableForLessThanOneHour(){
        latestLocalApiKeyListUpdate = LocalDateTime.now().plusMinutes(30);
        assertTrue(isApiKeyValidConsideringLocalApiKeyList(someValidApiKeyValue));
    }
}
