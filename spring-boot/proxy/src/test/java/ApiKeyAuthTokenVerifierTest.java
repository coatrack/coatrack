import eu.coatrack.api.ApiKey;
import eu.coatrack.api.ServiceApi;
import eu.coatrack.proxy.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiKeyAuthTokenVerifierTest {

    //Admins key verification functionality is not tested yet because it is unclear if this feature will outlast.

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
    public void validApiKeyFromAdminShouldBeAuthenticated(){
        shallApiKeyRequestFromAdminWork(true);
        shallApiKeyBeValid(true);

        assertTrue(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken).isAuthenticated());
    }

    @Test
    public void invalidApiKeyFromAdminShouldBeRejected(){
        shallApiKeyRequestFromAdminWork(true);
        shallApiKeyBeValid(false);

        assertNull(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void nullApiKeyReceivedFromAdminShouldBeRejected(){
        when(apiKeyFetcherMock.requestApiKeyFromAdmin(anyString())).thenReturn(null);
        shallApiKeyBeValid(false);

        assertNull(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void apiKeyNotFoundInLocalApiKeyListShouldBeRejected(){
        shallApiKeyRequestFromAdminWork(false);
        shallApiKeyBeFoundInLocalApiKeyList(false);

        assertNull(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void apiKeyAuthorizedByLocalApiKeyListAndShouldBeAuthorized(){
        shallApiKeyRequestFromAdminWork(false);
        shallApiKeyBeFoundInLocalApiKeyList(true);
        shallApiKeyBeAuthorizedConsideringLocalApiKeyList(true);

        assertTrue(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken).isAuthenticated());
    }

    @Test
    public void apiKeyNotAuthorizedByLocalApiKeyListAndShouldNotBeAuthorized(){
        shallApiKeyRequestFromAdminWork(false);
        shallApiKeyBeFoundInLocalApiKeyList(true);
        shallApiKeyBeAuthorizedConsideringLocalApiKeyList(false);

        assertNull(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }


    //Behavior of the mock objects

    //apiKeyFetcherMock
    private void shallApiKeyRequestFromAdminWork(boolean isApiKeyRequestFromAdminWorking) {
        if (isApiKeyRequestFromAdminWorking)
            when(apiKeyFetcherMock.requestApiKeyFromAdmin(apiKey.getKeyValue())).thenReturn(apiKey);
        else
            when(apiKeyFetcherMock.requestApiKeyFromAdmin(anyString())).thenThrow(new RestClientException("test"));
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
