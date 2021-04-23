package eu.coatrack.proxy.security;/*-
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
import eu.coatrack.api.ServiceApi;
import eu.coatrack.proxy.security.exceptions.ApiKeyFetchingFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.AuthenticationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiKeyAuthTokenVerifierTest {

    private ApiKeyAuthToken apiKeyAuthToken;
    private ApiKeyAuthTokenVerifier apiKeyAuthTokenVerifier;
    private ApiKey apiKey;

    private LocalApiKeyManager localApiKeyManagerMock;
    private ApiKeyFetcher apiKeyFetcherMock;
    private ApiKeyVerifier apiKeyVerifierMock;

    @BeforeEach
    public void setup() {
        apiKey = createSampleApiKeyForTesting();

        apiKeyAuthTokenVerifier = createMockedApiKeyAuthTokenVerifier();

        // Create an auth token for a valid api key without any granted authorities.
        apiKeyAuthToken = new ApiKeyAuthToken(apiKey.getKeyValue(), null);
        apiKeyAuthToken.setAuthenticated(false);
    }

    private ApiKey createSampleApiKeyForTesting() {
        ServiceApi serviceApi = new ServiceApi();
        serviceApi.setUriIdentifier("weather-data-service");

        ApiKey localApiKey = new ApiKey();
        localApiKey.setKeyValue("ca716b82-745c-4f6d-a38b-ff8fe140ffd1");
        localApiKey.setServiceApi(serviceApi);

        return localApiKey;
    }

    private ApiKeyAuthTokenVerifier createMockedApiKeyAuthTokenVerifier() {
        localApiKeyManagerMock = mock(LocalApiKeyManager.class);
        apiKeyFetcherMock = mock(ApiKeyFetcher.class);
        apiKeyVerifierMock = mock(ApiKeyVerifier.class);

        return new ApiKeyAuthTokenVerifier(
                localApiKeyManagerMock,
                apiKeyFetcherMock,
                apiKeyVerifierMock
        );
    }

    @Test
    public void nullArgumentShouldCauseException(){
        assertThrows(AuthenticationException.class, () -> apiKeyAuthTokenVerifier.authenticate(null));
    }

    @Test
    public void nullCredentialsInAuthTokenShouldCauseException(){
        ApiKeyAuthToken nullToken = new ApiKeyAuthToken(null, null);

        assertThrows(AuthenticationException.class, () -> apiKeyAuthTokenVerifier.authenticate(nullToken));
    }

    @Test
    public void validApiKeyFromAdminShouldBeAuthenticated() throws ApiKeyFetchingFailedException {
        addBehaviorToApiKeyFetcherMock_SetExpectedResponse(ResultOfApiKeyRequestToAdmin.API_KEY);
        addBehaviorToApiKeyVerifierMock_ShallGivenApiKeyBeConsideredValid(true);

        assertTrue(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken).isAuthenticated());
    }

    @Test
    public void invalidApiKeyFromAdminShouldCauseException() throws ApiKeyFetchingFailedException {
        addBehaviorToApiKeyFetcherMock_SetExpectedResponse(ResultOfApiKeyRequestToAdmin.API_KEY);
        addBehaviorToApiKeyVerifierMock_ShallGivenApiKeyBeConsideredValid(false);

        assertThrows(AuthenticationException.class, () -> apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void nullApiKeyReceivedFromAdminShouldCauseException() throws ApiKeyFetchingFailedException {
        addBehaviorToApiKeyFetcherMock_SetExpectedResponse(ResultOfApiKeyRequestToAdmin.NULL);
        addBehaviorToApiKeyVerifierMock_ShallGivenApiKeyBeConsideredValid(false);

        assertThrows(AuthenticationException.class, () -> apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void apiKeyNotFoundInLocalApiKeyListShouldCauseException() throws ApiKeyFetchingFailedException {
        addBehaviorToApiKeyFetcherMock_SetExpectedResponse(ResultOfApiKeyRequestToAdmin.EXCEPTION);
        addBehaviorToApiKeyManagerMock_ShallOfflineWorkingTimeBeExceeded(false);
        addBehaviorToApiKeyManagerMock_ShallApiKeyBeFoundInLocalApiKeyList(false);

        assertThrows(AuthenticationException.class, () -> apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void apiKeyAuthorizedByLocalApiKeyListAndShouldBeAuthorized() throws ApiKeyFetchingFailedException {
        addBehaviorToApiKeyFetcherMock_SetExpectedResponse(ResultOfApiKeyRequestToAdmin.EXCEPTION);
        addBehaviorToApiKeyManagerMock_ShallOfflineWorkingTimeBeExceeded(false);
        addBehaviorToApiKeyManagerMock_ShallApiKeyBeFoundInLocalApiKeyList(true);
        addBehaviorToApiKeyVerifierMock_ShallGivenApiKeyBeConsideredValid(true);

        assertTrue(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken).isAuthenticated());
    }

    @Test
    public void exceedingOfflineWorkingTimeShouldCauseException() throws ApiKeyFetchingFailedException {
        addBehaviorToApiKeyFetcherMock_SetExpectedResponse(ResultOfApiKeyRequestToAdmin.EXCEPTION);
        addBehaviorToApiKeyManagerMock_ShallOfflineWorkingTimeBeExceeded(true);

        assertThrows(AuthenticationException.class, () -> apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void invalidApiKeyFromLocalApiKeyListAndShouldCauseException() throws ApiKeyFetchingFailedException {
        addBehaviorToApiKeyFetcherMock_SetExpectedResponse(ResultOfApiKeyRequestToAdmin.EXCEPTION);
        addBehaviorToApiKeyManagerMock_ShallOfflineWorkingTimeBeExceeded(false);
        addBehaviorToApiKeyManagerMock_ShallApiKeyBeFoundInLocalApiKeyList(true);
        addBehaviorToApiKeyVerifierMock_ShallGivenApiKeyBeConsideredValid(false);

        assertThrows(AuthenticationException.class, () -> apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void tokenWithAdminsAccessKeyShouldBeAccepted() {
        apiKeyAuthToken = new ApiKeyAuthToken(ApiKey.API_KEY_FOR_YGG_ADMIN_TO_ACCESS_PROXIES, null);
        apiKeyAuthToken.setAuthenticated(false);

        assertTrue(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken).isAuthenticated());
    }

    //Behavior of the mock objects

    //apiKeyFetcherMock
    private void addBehaviorToApiKeyFetcherMock_SetExpectedResponse(ResultOfApiKeyRequestToAdmin resultOfApiKeyRequestToAdmin)
            throws ApiKeyFetchingFailedException {
        switch (resultOfApiKeyRequestToAdmin){
            case NULL:
                when(apiKeyFetcherMock.requestApiKeyFromAdmin(anyString())).thenReturn(null);
                break;
            case API_KEY:
                when(apiKeyFetcherMock.requestApiKeyFromAdmin(apiKey.getKeyValue())).thenReturn(apiKey);
                break;
            case EXCEPTION:
                when(apiKeyFetcherMock.requestApiKeyFromAdmin(anyString())).thenThrow(new ApiKeyFetchingFailedException("test"));
                break;
        }
    }

    private enum ResultOfApiKeyRequestToAdmin {
        NULL, API_KEY, EXCEPTION
    }

    //apiKeyVerifierMock
    private void addBehaviorToApiKeyVerifierMock_ShallGivenApiKeyBeConsideredValid(boolean shallApiKeyBeValid) {
        when(apiKeyVerifierMock.isApiKeyValid(apiKey)).thenReturn(shallApiKeyBeValid);
    }

    //localApiKeyManagerMock
    private void addBehaviorToApiKeyManagerMock_ShallApiKeyBeFoundInLocalApiKeyList(boolean isFoundInLocalApiKeyList) {
        ApiKey expectedReturnValue = isFoundInLocalApiKeyList ? apiKey : null;
        when(localApiKeyManagerMock.getApiKeyEntityByApiKeyValue(apiKey.getKeyValue())).thenReturn(expectedReturnValue);
    }

    private void addBehaviorToApiKeyManagerMock_ShallOfflineWorkingTimeBeExceeded(boolean isOfflineWorkingTimeExceeded) {
        when(localApiKeyManagerMock.isOfflineWorkingTimeExceeded()).thenReturn(isOfflineWorkingTimeExceeded);
    }
}
