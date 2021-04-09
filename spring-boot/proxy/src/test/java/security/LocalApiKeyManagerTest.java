package security;/*-
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
import eu.coatrack.proxy.security.ApiKeyFetcher;
import eu.coatrack.proxy.security.ApiKeyFetchingException;
import eu.coatrack.proxy.security.LocalApiKeyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LocalApiKeyManagerTest {

    private ApiKey apiKey;
    private List<ApiKey> localApiKeyList;
    private ApiKeyFetcher apiKeyFetcherMock;
    private LocalApiKeyManager localApiKeyManager;

    @BeforeEach
    public void setup() throws ApiKeyFetchingException {
        apiKey = new ApiKey();
        apiKey.setKeyValue("ca716b82-745c-4f6d-a38b-ff8fe140ffd1");

        localApiKeyList = new ArrayList<>();
        localApiKeyList.add(apiKey);

        apiKeyFetcherMock = mock(ApiKeyFetcher.class);
        when(apiKeyFetcherMock.requestLatestApiKeyListFromAdmin()).thenReturn(localApiKeyList);

        long timeInMinutesTheGatewayWorksWithoutConnectionToAdmin = 60;
        localApiKeyManager = new LocalApiKeyManager(apiKeyFetcherMock, timeInMinutesTheGatewayWorksWithoutConnectionToAdmin);
        localApiKeyManager.updateLocalApiKeyList();
    }

    @Test
    public void nullApiKeyValueShouldBeAnsweredWithNull(){
        assertNull(localApiKeyManager.getApiKeyFromLocalApiKeyList(null));
    }

    @Test
    public void apiKeyIsFoundInLocalApiKeyListAndThereforeReturned(){
        assertTrue(apiKey == localApiKeyManager.getApiKeyFromLocalApiKeyList(apiKey.getKeyValue()));
    }

    @Test
    public void apiKeyIsNotFoundInLocalApiKeyListAndThereforeNullIsReturned() throws ApiKeyFetchingException {
        List<ApiKey> apiKeyListNotContainingTheIncomingApiKey = createApiKeyListNotContainingTheIncomingApiKey();

        reset(apiKeyFetcherMock);
        when(apiKeyFetcherMock.requestLatestApiKeyListFromAdmin()).thenReturn(apiKeyListNotContainingTheIncomingApiKey);
        localApiKeyManager.updateLocalApiKeyList();

        assertNull(localApiKeyManager.getApiKeyFromLocalApiKeyList(apiKey.getKeyValue()));
    }

    private List<ApiKey> createApiKeyListNotContainingTheIncomingApiKey() {
        List<ApiKey> listWithoutTheIncomingApiKey = new ArrayList<>();

        ApiKey wrongApiKey1 = new ApiKey();
        wrongApiKey1.setKeyValue("wrong value 1");
        listWithoutTheIncomingApiKey.add(wrongApiKey1);

        ApiKey wrongApiKey2 = new ApiKey();
        wrongApiKey2.setKeyValue("wrong value 2");
        listWithoutTheIncomingApiKey.add(wrongApiKey2);

        return listWithoutTheIncomingApiKey;
    }

    @Test
    public void latestUpdateOfLocalAPiKeyListWasWithinDeadline(){
        long deadlineIsOneMinuteAfterNow = 1;

        LocalApiKeyManager localApiKeyManager = new LocalApiKeyManager(apiKeyFetcherMock, deadlineIsOneMinuteAfterNow);
        localApiKeyManager.updateLocalApiKeyList();

        assertTrue(localApiKeyManager.wasLatestUpdateOfLocalApiKeyListWithinDeadline());
    }

    @Test
    public void latestUpdateOfLocalApiKeyListWasNotWithinDeadline(){
        long deadlineIsOneMinuteBeforeNow = -1;

        LocalApiKeyManager localApiKeyManager = new LocalApiKeyManager(apiKeyFetcherMock, deadlineIsOneMinuteBeforeNow);
        localApiKeyManager.updateLocalApiKeyList();

        assertFalse(localApiKeyManager.wasLatestUpdateOfLocalApiKeyListWithinDeadline());
    }
}
