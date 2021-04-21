package security.apiKeyFetcherTests;

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
