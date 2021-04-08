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
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

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
        apiKeyAuthTokenVerifier = createApiKeyAuthTokenVerifier();

        // Create an auth token for a valid api key without any granted authorities.
        apiKeyAuthToken = new ApiKeyAuthToken(someValidApiKeyValue, null);
        apiKeyAuthToken.setAuthenticated(false);

        apiKey = createSampleApiKeyForTesting();
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

    private ApiKey createSampleApiKeyForTesting() {
        ServiceApi serviceApi = new ServiceApi();
        serviceApi.setUriIdentifier("weather-data-service");

        apiKey = new ApiKey();
        apiKey.setKeyValue(someValidApiKeyValue);
        apiKey.setServiceApi(serviceApi);

        return apiKey;
    }

    @Test
    public void nullArgumentShouldCauseException(){
        assertThrows(SessionAuthenticationException.class, () -> apiKeyAuthTokenVerifier.authenticate(null));
    }

    @Test
    public void nullCredentialsInAuthTokenShouldCauseException(){
        ApiKeyAuthToken token = new ApiKeyAuthToken(null, null);

        assertThrows(SessionAuthenticationException.class, () -> apiKeyAuthTokenVerifier.authenticate(token));
    }

    @Test
    public void validApiKeyFromAdminShouldBeAuthenticated() throws ApiKeyFetchingException {
        initializeAdminMockWithExpectedResponse(ResultOfApiKeyRequestToAdmin.API_KEY);
        initializeApiKeyVerifierMockShallApiKeyBeValid(true);

        assertTrue(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken).isAuthenticated());
    }

    @Test
    public void invalidApiKeyFromAdminShouldBeRejected() throws ApiKeyFetchingException {
        initializeAdminMockWithExpectedResponse(ResultOfApiKeyRequestToAdmin.API_KEY);
        initializeApiKeyVerifierMockShallApiKeyBeValid(false);

        assertNull(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void nullApiKeyReceivedFromAdminShouldBeRejected() throws ApiKeyFetchingException {
        initializeAdminMockWithExpectedResponse(ResultOfApiKeyRequestToAdmin.NULL);
        initializeApiKeyVerifierMockShallApiKeyBeValid(false);

        assertNull(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void apiKeyNotFoundInLocalApiKeyListShouldBeRejected() throws ApiKeyFetchingException {
        initializeAdminMockWithExpectedResponse(ResultOfApiKeyRequestToAdmin.EXCEPTION);
        shallApiKeyBeFoundInLocalApiKeyList(false);

        assertNull(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void apiKeyAuthorizedByLocalApiKeyListAndShouldBeAuthorized() throws ApiKeyFetchingException {
        initializeAdminMockWithExpectedResponse(ResultOfApiKeyRequestToAdmin.EXCEPTION);
        shallApiKeyBeFoundInLocalApiKeyList(true);
        initializeApiKeyVerifierMockShallApiKeyBeAuthorized(true);

        assertTrue(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken).isAuthenticated());
    }

    @Test
    public void apiKeyNotAuthorizedByLocalApiKeyListAndShouldNotBeAuthorized() throws ApiKeyFetchingException {
        initializeAdminMockWithExpectedResponse(ResultOfApiKeyRequestToAdmin.EXCEPTION);
        shallApiKeyBeFoundInLocalApiKeyList(true);
        initializeApiKeyVerifierMockShallApiKeyBeAuthorized(false);

        assertNull(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }


    //Behavior of the mock objects

    //apiKeyFetcherMock
    private void initializeAdminMockWithExpectedResponse(ResultOfApiKeyRequestToAdmin resultOfApiKeyRequestToAdmin) throws ApiKeyFetchingException {
        switch (resultOfApiKeyRequestToAdmin){
            case NULL:
                when(apiKeyFetcherMock.requestApiKeyFromAdmin(anyString())).thenReturn(null);
                break;
            case API_KEY:
                when(apiKeyFetcherMock.requestApiKeyFromAdmin(apiKey.getKeyValue())).thenReturn(apiKey);
                break;
            case EXCEPTION:
                when(apiKeyFetcherMock.requestApiKeyFromAdmin(anyString())).thenThrow(new ApiKeyFetchingException("test"));
                break;
        }
    }

    enum ResultOfApiKeyRequestToAdmin {
        NULL, API_KEY, EXCEPTION
    }

    //apiKeyVerifierMock
    private void initializeApiKeyVerifierMockShallApiKeyBeValid(boolean shallApiKeyBeValid) {
        when(apiKeyVerifierMock.isApiKeyValid(apiKey)).thenReturn(shallApiKeyBeValid);
    }

    private void initializeApiKeyVerifierMockShallApiKeyBeAuthorized(boolean shallApiKeyBeAuthorized) {
        when(apiKeyVerifierMock.isApiKeyAuthorizedToAccessItsService(apiKey.getKeyValue()))
                .thenReturn(shallApiKeyBeAuthorized);
    }

    //localApiKeyManagerMock
    private void shallApiKeyBeFoundInLocalApiKeyList(boolean isFoundInLocalApiKeyList) {
        ApiKey expectedReturnValue = isFoundInLocalApiKeyList ? apiKey : null;
        when(localApiKeyManagerMock.findApiKeyFromLocalApiKeyList(apiKey.getKeyValue())).thenReturn(expectedReturnValue);
    }
}
