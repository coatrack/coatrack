package security.ApiKeyFetcherTest;

import eu.coatrack.proxy.security.ApiKeyFetchingFailedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

public class NullApiKeyValueTest extends AbstractApiKeyFetcherTestSetup {

    @Test
    public void nullApiKeyValueShouldBeAnsweredWithNull() throws ApiKeyFetchingFailedException {
        assertNull(apiKeyFetcher.requestApiKeyFromAdmin(null));
    }

}
