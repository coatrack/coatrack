package security.localApiKeyManagerTests;/*-
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
import eu.coatrack.proxy.security.ApiKeyFetchingFailedException;
import eu.coatrack.proxy.security.LocalApiKeyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ApiKeyFindingTest extends AbstractLocalApiKeyManagerSetup{

    @BeforeEach
    public void fillLocalApiKeyListWithListContainingValidApiKey() throws ApiKeyFetchingFailedException {
        super.setupLocalApiKeyManagerAndApiKeyList();
        when(apiKeyFetcherMock.requestLatestApiKeyListFromAdmin()).thenReturn(apiKeyList);
        localApiKeyManager.updateLocalApiKeyList();
    }

    @Test
    public void nullApiKeyValueShouldBeAnsweredWithNull(){
        assertNull(localApiKeyManager.getApiKeyEntityByApiKeyValue(null));
    }

    @Test
    public void apiKeyIsFoundInLocalApiKeyListAndThereforeReturned(){
        assertSame(apiKey, localApiKeyManager.getApiKeyEntityByApiKeyValue(apiKey.getKeyValue()));
    }

    @Test
    public void apiKeyIsNotFoundInLocalApiKeyListAndThereforeNullIsReturned() throws ApiKeyFetchingFailedException {
        List<ApiKey> apiKeyListNotContainingTheIncomingApiKey = createApiKeyListNotContainingTheIncomingApiKey();

        reset(apiKeyFetcherMock);
        when(apiKeyFetcherMock.requestLatestApiKeyListFromAdmin()).thenReturn(apiKeyListNotContainingTheIncomingApiKey);
        localApiKeyManager.updateLocalApiKeyList();

        assertNull(localApiKeyManager.getApiKeyEntityByApiKeyValue(apiKey.getKeyValue()));
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
    public void localApiKeyListShouldNotUpdateWhenApiKeyFetcherDeliversNull() throws ApiKeyFetchingFailedException {
        reset(apiKeyFetcherMock);
        when(apiKeyFetcherMock.requestLatestApiKeyListFromAdmin()).thenReturn(null);
        localApiKeyManager.updateLocalApiKeyList();

        assertSame(apiKey, localApiKeyManager.getApiKeyEntityByApiKeyValue(apiKey.getKeyValue()));
    }

    @Test
    public void latestUpdateOfLocalAPiKeyListWasWithinDeadline(){
        long deadlineIsOneMinuteAfterNow = 1;

        LocalApiKeyManager localApiKeyManager = new LocalApiKeyManager(apiKeyFetcherMock, deadlineIsOneMinuteAfterNow);
        localApiKeyManager.updateLocalApiKeyList();

        assertTrue(localApiKeyManager.isMaxDurationOfOfflineModeExceeded());
    }

    @Test
    public void latestUpdateOfLocalApiKeyListWasNotWithinDeadline(){
        long deadlineIsOneMinuteBeforeNow = -1;

        LocalApiKeyManager localApiKeyManager = new LocalApiKeyManager(apiKeyFetcherMock, deadlineIsOneMinuteBeforeNow);
        localApiKeyManager.updateLocalApiKeyList();

        assertFalse(localApiKeyManager.isMaxDurationOfOfflineModeExceeded());
    }
}