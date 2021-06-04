package eu.coatrack.proxy.security;/*-
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
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public abstract class ApiKeyFetcher_AbstractTestSetup {

    protected static final String someApiKeyValue = "ca716b82-745c-4f6d-a38b-ff8fe140ffd1";

    protected ApiKeyFetcher apiKeyFetcher;
    protected RestTemplate restTemplateMock;
    protected UrlResourcesProvider urlResourcesProviderMock;
    protected ApiKey apiKey;
    protected ApiKey[] apiKeys;
    protected List<ApiKey> apiKeyList;

    @BeforeEach
    public void setup() {
        apiKey = new ApiKey();
        apiKey.setKeyValue("ca716b82-745c-4f6d-a38b-ff8fe140ffd1");

        apiKeys = new ApiKey[1];
        apiKeys[0] = apiKey;
        apiKeyList = Arrays.asList(apiKeys);

        restTemplateMock = mock(RestTemplate.class);
        urlResourcesProviderMock = createUrlResourcesProviderMock();

        apiKeyFetcher = new ApiKeyFetcher(restTemplateMock, urlResourcesProviderMock);
    }

    private UrlResourcesProvider createUrlResourcesProviderMock() {
        UrlResourcesProvider mock = mock(UrlResourcesProvider.class);
        when(mock.getApiKeyListRequestUrl()).thenReturn("test");
        when(mock.getApiKeyRequestUrl(anyString())).thenReturn("test");
        return mock;
    }
}