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
import eu.coatrack.api.ServiceApi;
import eu.coatrack.proxy.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.web.client.RestClientException;

import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiKeyAuthTokenVerifierTest {

    //Verification via admin key is not tested yet because it is unclear if this feature will outlast.

    private final String someValidApiKeyValue = "ca716b82-745c-4f6d-a38b-ff8fe140ffd1";
    private ApiKeyAuthToken apiKeyAuthToken;
    private ApiKeyAuthTokenVerifier apiKeyAuthTokenVerifier;
    private ApiKey apiKey;

    private LocalApiKeyManager localApiKeyManagerMock;
    private ApiKeyFetcher apiKeyFetcherMock;
    private LocalApiKeyVerifier localApiKeyVerifierMock;

    @BeforeEach
    public void setup(){
        apiKeyAuthTokenVerifier = createApiKeyAuthTokenVerifier();

        apiKeyAuthToken = new ApiKeyAuthToken(someValidApiKeyValue, null);
        apiKeyAuthToken.setAuthenticated(false);

        apiKey = createApiKey();
    }

    private ApiKeyAuthTokenVerifier createApiKeyAuthTokenVerifier() {
        localApiKeyManagerMock = mock(LocalApiKeyManager.class);
        apiKeyFetcherMock = mock(ApiKeyFetcher.class);
        localApiKeyVerifierMock = mock(LocalApiKeyVerifier.class);

        ApiKeyAuthTokenVerifier localApiKeyAuthTokenVerifier = new ApiKeyAuthTokenVerifier(
                localApiKeyManagerMock,
                apiKeyFetcherMock,
                localApiKeyVerifierMock
        );

        return localApiKeyAuthTokenVerifier;
    }

    private ApiKey createApiKey() {
        ServiceApi serviceApi = new ServiceApi();
        serviceApi.setUriIdentifier("");

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
        String nullCredentials = null;
        ApiKeyAuthToken token = new ApiKeyAuthToken(nullCredentials, null);

        assertThrows(SessionAuthenticationException.class, () -> apiKeyAuthTokenVerifier.authenticate(token));
    }

    @Test
    public void validApiKeyFromAdminShouldBeAuthenticated() throws ConnectException {
        setResultOfApiKeyRequestToAdmin(ResultOfApiKeyRequestToAdmin.API_KEY);
        shallApiKeyBeValid(true);

        assertTrue(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken).isAuthenticated());
    }

    @Test
    public void invalidApiKeyFromAdminShouldBeRejected() throws ConnectException {
        setResultOfApiKeyRequestToAdmin(ResultOfApiKeyRequestToAdmin.API_KEY);
        shallApiKeyBeValid(false);

        assertNull(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void nullApiKeyReceivedFromAdminShouldBeRejected() throws ConnectException {
        setResultOfApiKeyRequestToAdmin(ResultOfApiKeyRequestToAdmin.NULL);
        shallApiKeyBeValid(false);

        assertNull(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void apiKeyNotFoundInLocalApiKeyListShouldBeRejected() throws ConnectException {
        setResultOfApiKeyRequestToAdmin(ResultOfApiKeyRequestToAdmin.EXCEPTION);
        shallApiKeyBeFoundInLocalApiKeyList(false);

        assertNull(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void apiKeyAuthorizedByLocalApiKeyListAndShouldBeAuthorized() throws ConnectException {
        setResultOfApiKeyRequestToAdmin(ResultOfApiKeyRequestToAdmin.EXCEPTION);
        shallApiKeyBeFoundInLocalApiKeyList(true);
        shallApiKeyBeAuthorizedConsideringLocalApiKeyList(true);

        assertTrue(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken).isAuthenticated());
    }

    @Test
    public void apiKeyNotAuthorizedByLocalApiKeyListAndShouldNotBeAuthorized() throws ConnectException {
        setResultOfApiKeyRequestToAdmin(ResultOfApiKeyRequestToAdmin.EXCEPTION);
        shallApiKeyBeFoundInLocalApiKeyList(true);
        shallApiKeyBeAuthorizedConsideringLocalApiKeyList(false);

        assertNull(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }


    //Behavior of the mock objects

    //apiKeyFetcherMock
    private void setResultOfApiKeyRequestToAdmin(ResultOfApiKeyRequestToAdmin resultOfApiKeyRequestToAdmin) throws ConnectException {
        switch (resultOfApiKeyRequestToAdmin){
            case NULL:
                when(apiKeyFetcherMock.requestApiKeyFromAdmin(anyString())).thenReturn(null);
                break;
            case API_KEY:
                when(apiKeyFetcherMock.requestApiKeyFromAdmin(apiKey.getKeyValue())).thenReturn(apiKey);
                break;
            case EXCEPTION:
                when(apiKeyFetcherMock.requestApiKeyFromAdmin(anyString())).thenThrow(new ConnectException("test"));
                break;
        }
    }

    enum ResultOfApiKeyRequestToAdmin {
        NULL, API_KEY, EXCEPTION
    }

    //localApiKeyVerifierMock
    private void shallApiKeyBeValid(boolean isApiKeyBeValid) {
        when(localApiKeyVerifierMock.isApiKeyValid(apiKey)).thenReturn(isApiKeyBeValid);
    }

    private void shallApiKeyBeAuthorizedConsideringLocalApiKeyList(boolean isAuthorized) {
        when(localApiKeyVerifierMock.isApiKeyAuthorizedConsideringLocalApiKeyList(apiKey.getKeyValue()))
                .thenReturn(isAuthorized);
    }

    //localApiKeyManagerMock
    private void shallApiKeyBeFoundInLocalApiKeyList(boolean isFoundInLocalApiKeyList) {
        if (isFoundInLocalApiKeyList)
            when(localApiKeyManagerMock.findApiKeyFromLocalApiKeyList(apiKey.getKeyValue())).thenReturn(apiKey);
        else
            when(localApiKeyManagerMock.findApiKeyFromLocalApiKeyList(apiKey.getKeyValue())).thenReturn(null);
    }
}
