package security.ApiKeyFetcherTest;

import eu.coatrack.api.ApiKey;
import eu.coatrack.proxy.security.ApiKeyFetchingFailedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

public class ApiKeyListFetchingTest extends AbstractApiKeyFetcherTestSetup {

    @AfterEach
    public void verifyRestTemplateMockCall(){
        verify(restTemplateMock).getForEntity(anyString(), eq(ApiKey[].class), anyString());
    }

    @Test
    public void nullApiKeyListResponseEntityShouldBeAnsweredWithNull() throws ApiKeyFetchingFailedException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class), anyString())).thenReturn(null);
        assertNull(apiKeyFetcher.requestLatestApiKeyListFromAdmin());
    }

    @Test
    public void exceptionAtApiKeyListFetchingShouldBeAnsweredWithException() {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class), anyString()))
                .thenThrow(new RestClientException("test"));
        assertThrows(ApiKeyFetchingFailedException.class, () -> apiKeyFetcher.requestLatestApiKeyListFromAdmin());
    }

    @Test
    public void validApiKeyListResponseEntityShouldContainApiKey() throws ApiKeyFetchingFailedException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class), anyString()))
                .thenReturn(new ResponseEntity<>(apiKeys, HttpStatus.OK));
        assertTrue(apiKeyFetcher.requestLatestApiKeyListFromAdmin().contains(apiKey));
    }

    @Test
    public void apiKeyListNotFoundByAdminShouldBeAnsweredWithNull() throws ApiKeyFetchingFailedException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class), anyString()))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));
        assertNull(apiKeyFetcher.requestLatestApiKeyListFromAdmin());
    }

    @Test
    public void badHttpStatusShouldBeAnsweredWithNull() throws ApiKeyFetchingFailedException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class), anyString()))
                .thenReturn(new ResponseEntity<>(apiKeys, HttpStatus.INTERNAL_SERVER_ERROR));
        assertNull(apiKeyFetcher.requestLatestApiKeyListFromAdmin());
    }

    @Test
    public void apiKeyListNotFoundByAdminAndBadHttpStatusShouldBeAnsweredWithNull() throws ApiKeyFetchingFailedException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class), anyString()))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR));
        assertNull(apiKeyFetcher.requestLatestApiKeyListFromAdmin());
    }

}
