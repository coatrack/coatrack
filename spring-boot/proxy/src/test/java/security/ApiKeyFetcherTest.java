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

        when(urlResourcesProviderMock.getApiKeyListRequestUrl()).thenReturn("");
        when(urlResourcesProviderMock.getApiKeyRequestUrl(anyString())).thenReturn("");
    }

    //API key List fetching

    @Test
    public void nullApiKeyListResponseEntityShouldBeAnsweredWithNull() throws ApiKeyFetchingException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class), any(Object.class))).thenReturn(null);
        assertNull(apiKeyFetcher.requestLatestApiKeyListFromAdmin());
    }

    /*
    This Test does not work.

    @Test
    public void exceptionAtApiKeyListFetchingShouldBeAnsweredWithException() {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class), any(Object.class))).thenThrow(new RestClientException("test"));
        assertThrows(ApiKeyFetchingException.class, () -> apiKeyFetcher.requestLatestApiKeyListFromAdmin());
    }

     This test is not finished yet, because I failed letting restTemplateMock return an object of type ResponseEntity<ApiKey[]>.
        Further tests for the remaining cases should be implemented.

    @Test
    public void validDefaultApiKeyListResponseEntityShouldBeReturned() throws ApiKeyFetchingException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class), anyString()))
                .thenReturn(apiKeyListResponseEntity);

        assertTrue(apiKeyFetcher.requestLatestApiKeyListFromAdmin().contains(apiKey));
    }*/


    //Single API key fetching

    @Test
    public void nullArgumentShouldBeAnsweredWithNull() throws ApiKeyFetchingException {
        assertNull(apiKeyFetcher.requestApiKeyFromAdmin(null));
    }

    @Test
    public void nullApiKeyResponseEntityShouldBeAnsweredWithNull() throws ApiKeyFetchingException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey.class))).thenReturn(null);
        assertNull(apiKeyFetcher.requestApiKeyFromAdmin(someApiKeyValue));
    }

    @Test
    public void exceptionAtApiKeyFetchingShouldBeAnsweredWithException() {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey.class))).thenThrow(new RestClientException("test"));
        assertThrows(ApiKeyFetchingException.class, () -> apiKeyFetcher.requestApiKeyFromAdmin(someApiKeyValue));
    }
}
