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
    public void nullCredentialsShouldCauseException(){
        String nullCredentials = null;
        ApiKeyAuthToken token = new ApiKeyAuthToken(nullCredentials, null);

        assertThrows(SessionAuthenticationException.class, () -> apiKeyAuthTokenVerifier.authenticate(token));
    }

    @Test
    public void validApiKeyFromAdminShouldBeAuthenticated(){
        apiKeyRequestFromAdminWorked();
        apiKeyIsValid();

        assertTrue(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken).isAuthenticated());
    }

    @Test
    public void invalidApiKeyFromAdminShouldBeRejected(){
        apiKeyRequestFromAdminWorked();
        apiKeyIsInvalid();

        assertNull(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void nullApiKeyReceivedFromAdminShouldBeRejected(){
        apiKeyRequestFromAdminWorkedButKeyIsNull();

        assertNull(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void apiKeyNotFoundInLocalApiKeyListShouldBeRejected(){
        apiKeyRequestFromAdminFailed();
        apiKeyNotFoundInLocalApiKeyList();

        assertNull(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }

    @Test
    public void apiKeyAuthorizedByLocalApiKeyListAndShouldBeAuthorized(){
        apiKeyRequestFromAdminFailed();
        apiKeyWasFoundInLocalApiKeyList();
        apiKeyIsAuthorizedConsideringLocalApiKeyList();

        assertTrue(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken).isAuthenticated());
    }

    @Test
    public void apiKeyNotAuthorizedByLocalApiKeyListAndShouldNotBeAuthorized(){
        apiKeyRequestFromAdminFailed();
        apiKeyWasFoundInLocalApiKeyList();
        apiKeyIsUnauthorizedConsideringLocalApiKeyList();

        assertNull(apiKeyAuthTokenVerifier.authenticate(apiKeyAuthToken));
    }


    //Behavior of the mock objects

    //apiKeyFetcherMock
    private void apiKeyRequestFromAdminWorked() {
        when(apiKeyFetcherMock.requestApiKeyFromAdmin(apiKey.getKeyValue())).thenReturn(apiKey);
    }

    private void apiKeyRequestFromAdminWorkedButKeyIsNull() {
        when(apiKeyFetcherMock.requestApiKeyFromAdmin(apiKey.getKeyValue())).thenReturn(null);
    }

    private void apiKeyRequestFromAdminFailed() {
        when(apiKeyFetcherMock.requestApiKeyFromAdmin(anyString())).thenThrow(new RestClientException("test"));
    }

    //localApiKeyVerifierMock
    private void apiKeyIsValid() {
        when(localApiKeyVerifierMock.isApiKeyValid(apiKey)).thenReturn(true);
    }

    private void apiKeyIsInvalid() {
        when(localApiKeyVerifierMock.isApiKeyValid(apiKey)).thenReturn(false);
    }

    private void apiKeyIsUnauthorizedConsideringLocalApiKeyList() {
        when(localApiKeyVerifierMock.isApiKeyAuthorizedConsideringLocalApiKeyList(apiKey.getKeyValue())).thenReturn(false);
    }

    private void apiKeyIsAuthorizedConsideringLocalApiKeyList() {
        when(localApiKeyVerifierMock.isApiKeyAuthorizedConsideringLocalApiKeyList(apiKey.getKeyValue())).thenReturn(true);
    }

    //localApiKeyManagerMock
    private void apiKeyWasFoundInLocalApiKeyList() {
        when(localApiKeyManagerMock.findApiKeyFromLocalApiKeyList(apiKey.getKeyValue())).thenReturn(apiKey);
    }

    private void apiKeyNotFoundInLocalApiKeyList() {
        when(localApiKeyManagerMock.findApiKeyFromLocalApiKeyList(apiKey.getKeyValue())).thenReturn(null);
    }
}
