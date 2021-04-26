package eu.coatrack.proxy.security.local_api_key_manager_tests;

import eu.coatrack.proxy.security.exceptions.LocalApiKeyListWasNotInitializedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class NonInitializedApiKeyListTest extends AbstractLocalApiKeyManagerSetup{

    @Test
    public void missingInitialUpdateOfLocalApiKeyListShouldCauseException(){
        super.setupLocalApiKeyManagerAndApiKeyList();
        assertThrows(LocalApiKeyListWasNotInitializedException.class, () -> localApiKeyManager.isOfflineWorkingTimeExceeded());
    }

}
