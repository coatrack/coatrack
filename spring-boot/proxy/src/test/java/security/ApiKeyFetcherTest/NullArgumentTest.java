package security.ApiKeyFetcherTest;

import eu.coatrack.proxy.security.ApiKeyFetchingFailedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

public class NullArgumentTest extends AbstractApiKeyFetcherTestSetup {

    @Test
    public void nullResponseEntityShouldBeAnsweredWithNull() throws ApiKeyFetchingFailedException {
        assertNull(apiKeyFetcher.requestApiKeyFromAdmin(null));
    }

}
