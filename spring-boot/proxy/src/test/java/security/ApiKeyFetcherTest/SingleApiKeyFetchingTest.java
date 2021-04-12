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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SingleApiKeyFetchingTest extends AbstractApiKeyFetcherTestSetup {

    @AfterEach
    public void verifyRestTemplateMockCall(){
        verify(restTemplateMock).getForEntity(anyString(), eq(ApiKey.class));
    }

    @Test
    public void nullApiKeyResponseEntityShouldBeAnsweredWithNull() throws ApiKeyFetchingFailedException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey.class))).thenReturn(null);
        assertNull(apiKeyFetcher.requestApiKeyFromAdmin(someApiKeyValue));
    }

    @Test
    public void exceptionAtApiKeyFetchingShouldBeAnsweredWithException() {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey.class)))
                .thenThrow(new RestClientException("test"));
        assertThrows(ApiKeyFetchingFailedException.class, () -> apiKeyFetcher.requestApiKeyFromAdmin(someApiKeyValue));
    }

    @Test
    public void validApiKeyResponseEntityFromAdminShouldDeliverApiKey() throws ApiKeyFetchingFailedException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey.class)))
                .thenReturn(new ResponseEntity<>(apiKey, HttpStatus.OK));
        assertSame(apiKeyFetcher.requestApiKeyFromAdmin(apiKey.getKeyValue()), apiKey);
    }

    @Test
    public void nonOkHttpStatusShouldBeAnsweredWithNull() throws ApiKeyFetchingFailedException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey.class)))
                .thenReturn(new ResponseEntity<>(apiKey, HttpStatus.INTERNAL_SERVER_ERROR));
        assertNull(apiKeyFetcher.requestApiKeyFromAdmin(apiKey.getKeyValue()));
    }

    @Test
    public void apiKeyNotFoundByAdminShouldBeAnsweredWithNull() throws ApiKeyFetchingFailedException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));
        assertNull(apiKeyFetcher.requestApiKeyFromAdmin(apiKey.getKeyValue()));
    }

    @Test
    public void apiKeyNotFoundByAdminAndNonOkHttpStatusShouldBeAnsweredWithNull() throws ApiKeyFetchingFailedException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR));
        assertNull(apiKeyFetcher.requestApiKeyFromAdmin(apiKey.getKeyValue()));
    }

}
