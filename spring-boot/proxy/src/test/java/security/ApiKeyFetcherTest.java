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
import eu.coatrack.proxy.security.ApiKeyFetcher;
import eu.coatrack.proxy.security.ApiKeyFetchingFailedException;
import eu.coatrack.proxy.security.UrlResourcesProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @BeforeEach
    public void setup(){
        apiKey = new ApiKey();
        apiKey.setKeyValue(someApiKeyValue);

        apiKeys = new ApiKey[1];
        apiKeys[0] = apiKey;

        restTemplateMock = mock(RestTemplate.class);
        urlResourcesProviderMock = new UrlResourceProviderMock();
        apiKeyFetcher = new ApiKeyFetcher(restTemplateMock, urlResourcesProviderMock);
    }

    //API key List fetching

    @Test
    public void nullApiKeyListResponseEntityShouldBeAnsweredWithNull() throws ApiKeyFetchingFailedException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class), any(Object.class))).thenReturn(null);
        assertNull(apiKeyFetcher.requestLatestApiKeyListFromAdmin());
    }

    @Test
    public void exceptionAtApiKeyListFetchingShouldBeAnsweredWithException() {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class), anyString())).thenThrow(new RestClientException("test"));

        assertThrows(ApiKeyFetchingFailedException.class, () -> apiKeyFetcher.requestLatestApiKeyListFromAdmin());
    }

    @Test
    public void validDefaultApiKeyListResponseEntityShouldBeReturned() throws ApiKeyFetchingFailedException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class), anyString()))
                .thenReturn(new ResponseEntity<>(apiKeys, HttpStatus.OK));

        assertTrue(apiKeyFetcher.requestLatestApiKeyListFromAdmin().contains(apiKey));
    }

    @Test
    public void nullResponseBodyShouldBeAnsweredWithNull() throws ApiKeyFetchingFailedException {
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
    public void nullResponseBodyAndBadHttpStatusShouldBeAnsweredWithNull() throws ApiKeyFetchingFailedException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class), anyString()))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR));

        assertNull(apiKeyFetcher.requestLatestApiKeyListFromAdmin());
    }


    //Single API key fetching

    @Test
    public void nullArgumentShouldBeAnsweredWithNull() throws ApiKeyFetchingFailedException {
        assertNull(apiKeyFetcher.requestApiKeyFromAdmin(null));
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
    public void validApiKeyResponseFromAdminShouldBeAccepted() throws ApiKeyFetchingFailedException {
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

    class UrlResourceProviderMock extends UrlResourcesProvider {

        @Override
        public String getApiKeyRequestUrl(String someArgument) {
            return "test";
        }

        @Override
        public String getApiKeyListRequestUrl() {
            return "test";
        }

    }

}


