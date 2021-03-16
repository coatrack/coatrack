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

import eu.coatrack.proxy.security.LocalApiKeyListManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class LocalApiKeyRequesterTest {

    private final String someValidApiKeyValue = "someValidApiKeyValue";

    private ApiKey apiKey;
    private List<ApiKey> apiKeyList = new ArrayList<>();
    private LocalApiKeyListManager manager = new LocalApiKeyListManager();

    private final long oneHourInMillis = 1000 * 60 * 60;
    private final long oneDayInMillis = oneHourInMillis * 24;
    private final Timestamp now = new Timestamp(System.currentTimeMillis());
    private final Timestamp tomorrow = new Timestamp(now.getTime() + oneDayInMillis);
    private final Timestamp yesterday = new Timestamp(now.getTime() - oneDayInMillis);
    private final Timestamp halfAnHourAgo = new Timestamp(now.getTime() - oneHourInMillis / 2);

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
        apiKeyList = new ArrayList<>();
        apiKeyList.add(apiKey);
        manager.updateLocalApiKeyList(apiKeyList, LocalDateTime.now());
    }

    @Test
    public void isDefaultKeyAccepted(){
        assertTrue(manager.isApiKeyValidConsideringLocalApiKeyList(apiKey.getKeyValue()));
    }

    @Test
    public void testIfDeletedKeyIsDenied(){
        apiKey.setDeletedWhen(yesterday);
        assertFalse(manager.isApiKeyValidConsideringLocalApiKeyList(someValidApiKeyValue));
        apiKey.setDeletedWhen(halfAnHourAgo);
        assertFalse(manager.isApiKeyValidConsideringLocalApiKeyList(someValidApiKeyValue));
    }

    @Test
    public void testIfExpiredKeyIsDenied(){
        apiKey.setValidUntil(yesterday);
        assertFalse(manager.isApiKeyValidConsideringLocalApiKeyList(someValidApiKeyValue));
        apiKey.setValidUntil(halfAnHourAgo);
        assertFalse(manager.isApiKeyValidConsideringLocalApiKeyList(someValidApiKeyValue));
    }

    @Test
    public void testIfApiKeyIsAcceptedSinceItIsInTheLocalApiKeyList(){
        assertTrue(manager.isApiKeyValidConsideringLocalApiKeyList(someValidApiKeyValue));
    }

    @Test
    public void testIfApiKeyIsDeniedBecauseOfEmptyLocalApiKeyList(){
        manager.updateLocalApiKeyList(new ArrayList<>(), LocalDateTime.now());
        assertFalse(manager.isApiKeyValidConsideringLocalApiKeyList(someValidApiKeyValue));
    }

    @Test
    public void testIfApiKeyIsDeniedBecauseAdminIsNotReachableForMoreThanOneHour(){
        manager.updateLocalApiKeyList(apiKeyList, LocalDateTime.now().minusDays(1));
        assertFalse(manager.isApiKeyValidConsideringLocalApiKeyList(someValidApiKeyValue));
        manager.updateLocalApiKeyList(apiKeyList, LocalDateTime.now().minusHours(2));
        assertFalse(manager.isApiKeyValidConsideringLocalApiKeyList(someValidApiKeyValue));
    }

    @Test
    public void testIfApiKeyIsAcceptedBecauseAdminIsNotReachableForLessThanOneHour(){
        manager.updateLocalApiKeyList(apiKeyList, LocalDateTime.now().plusMinutes(30));
        assertTrue(manager.isApiKeyValidConsideringLocalApiKeyList(someValidApiKeyValue));
    }
}
