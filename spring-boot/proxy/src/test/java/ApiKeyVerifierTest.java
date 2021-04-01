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

import static eu.coatrack.proxy.security.ApiKeyVerifier.isApiKeyValid;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApiKeyVerifierTest {

    private ApiKey apiKey;

    private final long
            oneHourInMillis = 1000 * 60 * 60,
            oneDayInMillis = oneHourInMillis * 24;

    private final Timestamp
            now = new Timestamp(System.currentTimeMillis()),
            tomorrow = new Timestamp(now.getTime() + oneDayInMillis),
            yesterday = new Timestamp(now.getTime() - oneDayInMillis),
            halfAnHourAgo = new Timestamp(now.getTime() - oneHourInMillis / 2);

    @BeforeEach
    public void createAnAcceptingDefaultSetup(){
        apiKey = new ApiKey();
        apiKey.setDeletedWhen(null);
        apiKey.setValidUntil(tomorrow);
    }

    @Test
    public void validDefaultApiKeyShouldBeAccepted(){
        assertTrue(isApiKeyValid(apiKey));
    }

    @Test
    public void deletedApiKeysShouldBeDenied(){
        apiKey.setDeletedWhen(yesterday);
        assertFalse(isApiKeyValid(apiKey));
        apiKey.setDeletedWhen(halfAnHourAgo);
        assertFalse(isApiKeyValid(apiKey));
    }

    @Test
    public void expiredApiKeysShouldBeDenied(){
        apiKey.setValidUntil(yesterday);
        assertFalse(isApiKeyValid(apiKey));
        apiKey.setValidUntil(halfAnHourAgo);
        assertFalse(isApiKeyValid(apiKey));
    }
}
