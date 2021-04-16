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
import eu.coatrack.api.ServiceApi;
import eu.coatrack.proxy.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.AuthenticationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiKeyAuthTokenVerifierTest {

    private final String someValidApiKeyValue = "ca716b82-745c-4f6d-a38b-ff8fe140ffd1";
    private ApiKeyAuthToken apiKeyAuthToken;
    private ApiKeyAuthTokenVerifier apiKeyAuthTokenVerifier;
    private ApiKey apiKey;

    private LocalApiKeyManager localApiKeyManagerMock;
    private ApiKeyFetcher apiKeyFetcherMock;
    private ApiKeyVerifier apiKeyVerifierMock;

    @BeforeEach
    public void setup(){
        apiKey = createSampleApiKeyForTesting();

        apiKeyAuthTokenVerifier = createApiKeyAuthTokenVerifier();

        // Create an auth token for a valid api key without any granted authorities.
        apiKeyAuthToken = new ApiKeyAuthToken(someValidApiKeyValue, null);
        apiKeyAuthToken.setAuthenticated(false);
    }

    private ApiKey createSampleApiKeyForTesting() {
        ServiceApi serviceApi = new ServiceApi();
        serviceApi.setUriIdentifier("weather-data-service");

        ApiKey localApiKey = new ApiKey();
        localApiKey.setKeyValue(someValidApiKeyValue);
        localApiKey.setServiceApi(serviceApi);

        return localApiKey;
    }

    private ApiKeyAuthTokenVerifier createApiKeyAuthTokenVerifier() {
        localApiKeyManagerMock = mock(LocalApiKeyManager.class);
        apiKeyFetcherMock = mock(ApiKeyFetcher.class);
        apiKeyVerifierMock = mock(ApiKeyVerifier.class);

        ApiKeyAuthTokenVerifier localApiKeyAuthTokenVerifier = new ApiKeyAuthTokenVerifier(
                localApiKeyManagerMock,
                apiKeyFetcherMock,
                apiKeyVerifierMock
        );

        return localApiKeyAuthTokenVerifier;
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
        extendApiKeyFetcherMock_SetExpectedResponse(ResultOfApiKeyRequestToAdmin.API_KEY);
        extendApiKeyVerifierMock_ShallApiKeyBeValid(true);

        assertTrue(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken).isAuthenticated());
    }

    @Test
    public void invalidApiKeyFromAdminShouldBeRejected() throws ApiKeyFetchingFailedException {
        extendApiKeyFetcherMock_SetExpectedResponse(ResultOfApiKeyRequestToAdmin.API_KEY);
        extendApiKeyVerifierMock_ShallApiKeyBeValid(false);

        assertThrows(AuthenticationException.class, () -> apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void nullApiKeyReceivedFromAdminShouldBeRejected() throws ApiKeyFetchingFailedException {
        extendApiKeyFetcherMock_SetExpectedResponse(ResultOfApiKeyRequestToAdmin.NULL);
        extendApiKeyVerifierMock_ShallApiKeyBeValid(false);

        assertThrows(AuthenticationException.class, () -> apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void apiKeyNotFoundInLocalApiKeyListShouldBeRejected() throws ApiKeyFetchingFailedException {
        extendApiKeyFetcherMock_SetExpectedResponse(ResultOfApiKeyRequestToAdmin.EXCEPTION);
        extendApiKeyManagerMock_ShallDeadlineBeExceeded(false);
        extendApiKeyManagerMock_ShallApiKeyBeFoundInLocalApiKeyList(false);

        assertThrows(AuthenticationException.class, () -> apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void apiKeyAuthorizedByLocalApiKeyListAndShouldBeAuthorized() throws ApiKeyFetchingFailedException {
        extendApiKeyFetcherMock_SetExpectedResponse(ResultOfApiKeyRequestToAdmin.EXCEPTION);
        extendApiKeyManagerMock_ShallDeadlineBeExceeded(false);
        extendApiKeyManagerMock_ShallApiKeyBeFoundInLocalApiKeyList(true);
        extendApiKeyVerifierMock_ShallApiKeyBeValid(true);

        assertTrue(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken).isAuthenticated());
    }

    @Test
    public void exceedingTheDeadlineShouldRejectRequest() throws ApiKeyFetchingFailedException {
        extendApiKeyFetcherMock_SetExpectedResponse(ResultOfApiKeyRequestToAdmin.EXCEPTION);
        extendApiKeyManagerMock_ShallDeadlineBeExceeded(true);

        assertThrows(AuthenticationException.class, () -> apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void invalidApiKeyFromLocalApiKeyListAndShouldNotBeAuthorized() throws ApiKeyFetchingFailedException {
        extendApiKeyFetcherMock_SetExpectedResponse(ResultOfApiKeyRequestToAdmin.EXCEPTION);
        extendApiKeyManagerMock_ShallDeadlineBeExceeded(false);
        extendApiKeyManagerMock_ShallApiKeyBeFoundInLocalApiKeyList(true);
        extendApiKeyVerifierMock_ShallApiKeyBeValid(false);

        assertThrows(AuthenticationException.class, () -> apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }


    //Behavior of the mock objects

    //apiKeyFetcherMock
    private void extendApiKeyFetcherMock_SetExpectedResponse(ResultOfApiKeyRequestToAdmin resultOfApiKeyRequestToAdmin)
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

    enum ResultOfApiKeyRequestToAdmin {
        NULL, API_KEY, EXCEPTION
    }

    //apiKeyVerifierMock
    private void extendApiKeyVerifierMock_ShallApiKeyBeValid(boolean shallApiKeyBeValid) {
        when(apiKeyVerifierMock.isApiKeyValid(apiKey)).thenReturn(shallApiKeyBeValid);
    }

    //localApiKeyManagerMock
    private void extendApiKeyManagerMock_ShallApiKeyBeFoundInLocalApiKeyList(boolean isFoundInLocalApiKeyList) {
        ApiKey expectedReturnValue = isFoundInLocalApiKeyList ? apiKey : null;
        when(localApiKeyManagerMock.getApiKeyEntityByApiKeyValue(apiKey.getKeyValue())).thenReturn(expectedReturnValue);
    }

    private void extendApiKeyManagerMock_ShallDeadlineBeExceeded(boolean isDeadlineExceeded) {
        when(localApiKeyManagerMock.isMaxDurationOfOfflineModeExceeded()).thenReturn(!isDeadlineExceeded);
    }
}
