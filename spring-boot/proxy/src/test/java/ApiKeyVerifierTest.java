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
