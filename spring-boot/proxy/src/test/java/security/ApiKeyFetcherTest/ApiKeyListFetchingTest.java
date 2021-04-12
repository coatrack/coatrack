package security.ApiKeyFetcherTest;

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
        verify(restTemplateMock).getForEntity(anyString(), eq(ApiKey[].class));
    }

    @Test
    public void nullApiKeyListResponseEntityShouldBeAnsweredWithNull() throws ApiKeyFetchingFailedException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class))).thenReturn(null);
        assertNull(apiKeyFetcher.requestLatestApiKeyListFromAdmin());
    }

    @Test
    public void exceptionAtApiKeyListFetchingShouldBeAnsweredWithException() {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class)))
                .thenThrow(new RestClientException("test"));
        assertThrows(ApiKeyFetchingFailedException.class, () -> apiKeyFetcher.requestLatestApiKeyListFromAdmin());
    }

    @Test
    public void validApiKeyListResponseEntityShouldContainApiKey() throws ApiKeyFetchingFailedException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class)))
                .thenReturn(new ResponseEntity<>(apiKeys, HttpStatus.OK));
        assertTrue(apiKeyFetcher.requestLatestApiKeyListFromAdmin().contains(apiKey));
    }

    @Test
    public void apiKeyListNotFoundByAdminShouldBeAnsweredWithNull() throws ApiKeyFetchingFailedException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));
        assertNull(apiKeyFetcher.requestLatestApiKeyListFromAdmin());
    }

    @Test
    public void badHttpStatusShouldBeAnsweredWithNull() throws ApiKeyFetchingFailedException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class)))
                .thenReturn(new ResponseEntity<>(apiKeys, HttpStatus.INTERNAL_SERVER_ERROR));
        assertNull(apiKeyFetcher.requestLatestApiKeyListFromAdmin());
    }

    @Test
    public void apiKeyListNotFoundByAdminAndBadHttpStatusShouldBeAnsweredWithNull() throws ApiKeyFetchingFailedException {
        when(restTemplateMock.getForEntity(anyString(), eq(ApiKey[].class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR));
        assertNull(apiKeyFetcher.requestLatestApiKeyListFromAdmin());
    }

}
