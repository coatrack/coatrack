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

import eu.coatrack.proxy.security.exceptions.LocalApiKeyListWasNotInitializedException;
import eu.coatrack.proxy.security.exceptions.OfflineWorkingTimeExceedingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LocalApiKeyManager_NonInitializedLocalApiKeyListTest extends LocalApiKeyManager_AbstractTestSetupProvider {

    @Test
    public void accessingNonInitializedLocalApiKeyListShouldCauseException() {
        super.setupLocalApiKeyManagerWithoutInitializingLocalApiKeyList();
        assertThrows(LocalApiKeyListWasNotInitializedException.class,
                () -> localApiKeyManager.getApiKeyEntityFromLocalCache(apiKey.getKeyValue()));
    }

}
