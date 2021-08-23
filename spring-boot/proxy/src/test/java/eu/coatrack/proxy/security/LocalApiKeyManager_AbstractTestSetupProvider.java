package eu.coatrack.proxy.security;

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

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

public abstract class LocalApiKeyManager_AbstractTestSetupProvider {

    protected ApiKey apiKey;
    protected List<ApiKey> apiKeyList;
    protected ApiKeyFetcher apiKeyFetcherMock;
    protected LocalApiKeyManager localApiKeyManager;

    public void setupLocalApiKeyManagerWithoutInitializingLocalApiKeyList() {
        apiKey = new ApiKey();
        apiKey.setKeyValue("ca716b82-745c-4f6d-a38b-ff8fe140ffd1");

        apiKeyList = new ArrayList<>();
        apiKeyList.add(apiKey);

        apiKeyFetcherMock = mock(ApiKeyFetcher.class);
        long timeInMinutesTheGatewayWorksWithoutConnectionToAdmin = 60;
        localApiKeyManager = new LocalApiKeyManager(apiKeyFetcherMock,
                timeInMinutesTheGatewayWorksWithoutConnectionToAdmin);
    }

}
