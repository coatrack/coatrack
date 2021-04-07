import eu.coatrack.api.ApiKey;
import eu.coatrack.proxy.security.ApiKeyFetcher;
import eu.coatrack.proxy.security.ApiKeyFetchingException;
import eu.coatrack.proxy.security.UrlResourcesProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ApiKeyFetcherTest {

    private final String someApiKeyValue = "ca716b82-745c-4f6d-a38b-ff8fe140ffd1";

    private ApiKeyFetcher apiKeyFetcher;
    private RestTemplate restTemplateMock;
    private UrlResourcesProvider urlResourcesProviderMock;
    private ApiKey apiKey;
    private ApiKey[] apiKeys;

    private ResponseEntity<ApiKey[]> apiKeyListResponseEntity;

    @BeforeEach
    public void setup(){
        apiKey = new ApiKey();
        apiKeys = new ApiKey[1];
        apiKeys[0] = apiKey;

        apiKeyListResponseEntity = new ResponseEntity<>(apiKeys, HttpStatus.OK);

        restTemplateMock = mock(RestTemplate.class);
        urlResourcesProviderMock = mock(UrlResourcesProvider.class);

        apiKeyFetcher = new ApiKeyFetcher(restTemplateMock, urlResourcesProviderMock);

        when(urlResourcesProviderMock.getApiKeyListRequestUrl()).thenReturn(null);
        when(urlResourcesProviderMock.getApiKeyRequestUrl(anyString())).thenReturn(null);
    }

    //API key List fetching

    @Test
    public void nullApiKeyListResponseEntityShouldBeAnsweredWithException() {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class), any(Object.class))).thenReturn(null);
        assertThrows(ApiKeyFetchingException.class, () -> apiKeyFetcher.requestLatestApiKeyListFromAdmin());
    }

    @Test
    public void exceptionAtApiKeyListFetchingShouldBeAnsweredWithException() {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class), any(Object.class))).thenThrow(new RestClientException("test"));
        assertThrows(ApiKeyFetchingException.class, () -> apiKeyFetcher.requestApiKeyFromAdmin(someApiKeyValue));
    }

    /* This test is not finished yet, because I failed letting restTemplateMock return an object of type ResponseEntity<ApiKey[]>.
        Further tests for the remaining cases should be implemented.

    @Test
    public void validDefaultApiKeyListResponseEntityShouldBeReturned() throws ApiKeyFetchingException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey.class), any(Object.class)))
                .thenAnswer((Answer<ResponseEntity<ApiKey[]>>) apiKeyListResponseEntity);

        //doReturn(apiKeyListResponseEntity).when(restTemplateMock.getForEntity(anyString(), eq(ApiKey.class), any(Object.class)));

        //when(apiKeyListResponseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
        //when(apiKeyListResponseEntity.getBody()).thenReturn(apiKeys);

        assertTrue(apiKeyFetcher.requestLatestApiKeyListFromAdmin().contains(apiKey));
    }
     */


    //Single API key fetching

    @Test
    public void nullArgumentShouldBeAnsweredWithNull() throws ApiKeyFetchingException {
        assertNull(apiKeyFetcher.requestApiKeyFromAdmin(null));
    }

    @Test
    public void nullApiKeyResponseEntityShouldBeAnsweredWithException() {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey.class))).thenReturn(null);
        assertThrows(ApiKeyFetchingException.class, () -> apiKeyFetcher.requestApiKeyFromAdmin(someApiKeyValue));
    }

    @Test
    public void exceptionAtApiKeyFetchingShouldBeAnsweredWithException() {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey.class))).thenThrow(new RestClientException("test"));
        assertThrows(ApiKeyFetchingException.class, () -> apiKeyFetcher.requestLatestApiKeyListFromAdmin());
    }
}
