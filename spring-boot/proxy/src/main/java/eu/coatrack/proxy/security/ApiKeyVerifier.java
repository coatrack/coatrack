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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * This class provides methods to determine the validity or authorization of an API key.
 *
 * @author Christoph Baier
 */

@Service
public class ApiKeyVerifier {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyVerifier.class);

    private final LocalApiKeyManager localApiKeyManager;

    public ApiKeyVerifier(LocalApiKeyManager localApiKeyManager) {
        this.localApiKeyManager = localApiKeyManager;
    }

    public boolean isApiKeyAuthorizedToAccessItsService(String apiKeyValue) {
        log.debug("Begin checking if the API key with the value {} is authorized using the local API key list.",
                apiKeyValue);

        if (!localApiKeyManager.wasLatestUpdateOfLocalApiKeyListWithinDeadline())
            return false;

        ApiKey apiKey = localApiKeyManager.findApiKeyFromLocalApiKeyList(apiKeyValue);
        return apiKey == null ? false : isApiKeyValid(apiKey);
    }

    public boolean isApiKeyValid(ApiKey apiKey) {
        return apiKey == null ? false : isApiKeyNotDeleted(apiKey) && isApiKeyNotExpired(apiKey);
    }

    private boolean isApiKeyNotDeleted(ApiKey apiKey) {
        return apiKey.getDeletedWhen() == null;
    }

    private boolean isApiKeyNotExpired(ApiKey apiKey) {
        return apiKey.getValidUntil().getTime() > System.currentTimeMillis();
    }
}
