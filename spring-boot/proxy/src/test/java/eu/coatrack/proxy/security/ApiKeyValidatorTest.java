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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApiKeyValidatorTest {

    private ApiKey apiKey;
    private ApiKeyValidator apiKeyValidator;

    private static final long oneMinuteInMillis = 1000 * 60;

    private final Timestamp now = new Timestamp(System.currentTimeMillis()),
            oneMinuteAfterNow = new Timestamp(now.getTime() + oneMinuteInMillis),
            oneMinuteBeforeNow = new Timestamp(now.getTime() - oneMinuteInMillis);

    @BeforeEach
    public void createAnAcceptingDefaultSetup() {
        apiKey = createValidApiKey();
        apiKeyValidator = new ApiKeyValidator();
    }

    private ApiKey createValidApiKey() {
        ApiKey apiKeyToBeCreated = new ApiKey();
        apiKeyToBeCreated.setDeletedWhen(null);
        apiKeyToBeCreated.setKeyValue("ca716b82-745c-4f6d-a38b-ff8fe140ffd1");
        apiKeyToBeCreated.setValidUntil(oneMinuteAfterNow);
        return apiKeyToBeCreated;
    }

    @Test
    public void validDefaultApiKeyShouldBeAccepted() {
        assertTrue(apiKeyValidator.isApiKeyValid(apiKey));
    }

    @Test
    public void nullArgumentsShouldBeDenied() {
        assertFalse(apiKeyValidator.isApiKeyValid(null));
    }

    @Test
    public void deletedApiKeyShouldBeDenied() {
        apiKey.setDeletedWhen(oneMinuteBeforeNow);
        assertFalse(apiKeyValidator.isApiKeyValid(apiKey));
    }

    @Test
    public void expiredApiKeyShouldBeDenied() {
        apiKey.setValidUntil(oneMinuteBeforeNow);
        assertFalse(apiKeyValidator.isApiKeyValid(apiKey));
    }
}
