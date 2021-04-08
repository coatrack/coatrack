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
import eu.coatrack.proxy.security.ApiKeyVerifier;
import eu.coatrack.proxy.security.LocalApiKeyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ApiKeyVerifierTest {

    private final String someValidApiKeyValue = "ca716b82-745c-4f6d-a38b-ff8fe140ffd1";
    private ApiKey apiKey;
    private ApiKeyVerifier apiKeyVerifier;
    private LocalApiKeyManager localApiKeyManagerMock;

    private final long
            oneMinuteInMillis = 1000 * 60;

    private final Timestamp
            now = new Timestamp(System.currentTimeMillis()),
            oneMinuteAfterNow = new Timestamp(now.getTime() + oneMinuteInMillis),
            oneMinuteBeforeNow = new Timestamp(now.getTime() - oneMinuteInMillis);

    @BeforeEach
    public void createAnAcceptingDefaultSetup(){
        apiKey = createValidApiKey();
        localApiKeyManagerMock = createWorkingLocalApiKeyManagerMock();
        apiKeyVerifier = new ApiKeyVerifier(localApiKeyManagerMock);
    }

    private ApiKey createValidApiKey() {
        ApiKey apiKeyToBeCreated = new ApiKey();
        apiKeyToBeCreated.setDeletedWhen(null);
        apiKeyToBeCreated.setKeyValue(someValidApiKeyValue);
        apiKeyToBeCreated.setValidUntil(oneMinuteAfterNow);
        return apiKeyToBeCreated;
    }

    private LocalApiKeyManager createWorkingLocalApiKeyManagerMock() {
        LocalApiKeyManager mock = mock(LocalApiKeyManager.class);
        when(mock.findApiKeyFromLocalApiKeyList(apiKey.getKeyValue())).thenReturn(apiKey);
        when(mock.wasLatestUpdateOfLocalApiKeyListWithinDeadline()).thenReturn(true);
        return mock;
    }

    @Test
    public void validDefaultApiKeyShouldBeAccepted(){
        assertTrue(apiKeyVerifier.isApiKeyAuthorizedToAccessItsService(apiKey.getKeyValue()));
        assertTrue(apiKeyVerifier.isApiKeyValid(apiKey));
    }

    @Test
    public void apiKeyIsNotFoundInLocalApiKeyListAndShouldBeRejected(){
        reset(localApiKeyManagerMock);
        when(localApiKeyManagerMock.findApiKeyFromLocalApiKeyList(apiKey.getKeyValue())).thenReturn(null);

        assertFalse(apiKeyVerifier.isApiKeyAuthorizedToAccessItsService(apiKey.getKeyValue()));
    }

    @Test
    public void nullArgumentsShouldBeDenied(){
        assertFalse(apiKeyVerifier.isApiKeyAuthorizedToAccessItsService(null));
        assertFalse(apiKeyVerifier.isApiKeyValid(null));
    }

    @Test
    public void deletedApiKeyShouldBeDenied(){
        apiKey.setDeletedWhen(oneMinuteBeforeNow);

        assertFalse(apiKeyVerifier.isApiKeyAuthorizedToAccessItsService(apiKey.getKeyValue()));
        assertFalse(apiKeyVerifier.isApiKeyValid(apiKey));
    }

    @Test
    public void expiredApiKeyShouldBeDenied(){
        apiKey.setValidUntil(oneMinuteBeforeNow);

        assertFalse(apiKeyVerifier.isApiKeyAuthorizedToAccessItsService(apiKey.getKeyValue()));
        assertFalse(apiKeyVerifier.isApiKeyValid(apiKey));
    }

    @Test
    public void localApiKeyListWasNotUpdatedWithinDeadlineAndApiKeyShouldThereforeBeRejected(){
        reset(localApiKeyManagerMock);
        when(localApiKeyManagerMock.wasLatestUpdateOfLocalApiKeyListWithinDeadline()).thenReturn(false);
        when(localApiKeyManagerMock.findApiKeyFromLocalApiKeyList(apiKey.getKeyValue())).thenReturn(apiKey);

        assertFalse(apiKeyVerifier.isApiKeyAuthorizedToAccessItsService(apiKey.getKeyValue()));
    }
}
