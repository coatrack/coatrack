import eu.coatrack.api.ApiKey;
import eu.coatrack.proxy.security.ApiKeyFetcher;
import eu.coatrack.proxy.security.LocalApiKeyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LocalApiKeyManagerTest {

    private final long oneHourInMinutes = 60;
    private final String someValidApiKeyValue = "ca716b82-745c-4f6d-a38b-ff8fe140ffd1";

    private ApiKey apiKey;
    private List<ApiKey> localApiKeyList;
    private ApiKeyFetcher apiKeyFetcherMock;
    private LocalApiKeyManager localApiKeyManager;

    @BeforeEach
    public void setup(){
        apiKey = new ApiKey();
        apiKey.setKeyValue(someValidApiKeyValue);

        localApiKeyList = new ArrayList<>();
        localApiKeyList.add(apiKey);

        apiKeyFetcherMock = mock(ApiKeyFetcher.class);
        when(apiKeyFetcherMock.requestLatestApiKeyListFromAdmin()).thenReturn(localApiKeyList);

        localApiKeyManager = new LocalApiKeyManager(apiKeyFetcherMock, oneHourInMinutes);
        localApiKeyManager.updateLocalApiKeyList();
    }

    @Test
    public void nullApiKeyValueShouldBeAnsweredWithNull(){
        assertNull(localApiKeyManager.findApiKeyFromLocalApiKeyList(null));
    }

    @Test
    public void apiKeyIsFoundInLocalApiKeyListAndThereforeReturned(){
        assertTrue(apiKey == localApiKeyManager.findApiKeyFromLocalApiKeyList(apiKey.getKeyValue()));
    }

    @Test
    public void apiKeyIsNotFoundInLocalApiKeyListAndThereforeNullIsReturned(){
        List<ApiKey> apiKeyListNotContainingTheIncomingApiKey = createApiKeyListNotContainingTheIncomingApiKey();

        reset(apiKeyFetcherMock);
        when(apiKeyFetcherMock.requestLatestApiKeyListFromAdmin()).thenReturn(apiKeyListNotContainingTheIncomingApiKey);
        localApiKeyManager.updateLocalApiKeyList();

        assertNull(localApiKeyManager.findApiKeyFromLocalApiKeyList(apiKey.getKeyValue()));
    }

    private List<ApiKey> createApiKeyListNotContainingTheIncomingApiKey() {
        List<ApiKey> listWithoutTheIncomingApiKey = new ArrayList<>();

        ApiKey wrongApiKey1 = new ApiKey();
        wrongApiKey1.setKeyValue("wrong value 1");
        listWithoutTheIncomingApiKey.add(wrongApiKey1);

        ApiKey wrongApiKey2 = new ApiKey();
        wrongApiKey2.setKeyValue("wrong value 2");
        listWithoutTheIncomingApiKey.add(wrongApiKey2);

        return listWithoutTheIncomingApiKey;
    }

    @Test
    public void latestUpdateOfLocalAPiKeyListWasWithinDeadline(){
        long deadlineIsOneMinuteAfterNow = 1;

        LocalApiKeyManager localApiKeyManager = new LocalApiKeyManager(apiKeyFetcherMock, deadlineIsOneMinuteAfterNow);
        localApiKeyManager.updateLocalApiKeyList();

        assertTrue(localApiKeyManager.wasLatestUpdateOfLocalApiKeyListWithinDeadline(apiKey));
    }

    @Test
    public void latestUpdateOfLocalApiKeyListWasNotWithinDeadline(){
        long deadlineIsOneMinuteBeforeNow = -1;

        LocalApiKeyManager localApiKeyManager = new LocalApiKeyManager(apiKeyFetcherMock, deadlineIsOneMinuteBeforeNow);
        localApiKeyManager.updateLocalApiKeyList();

        assertFalse(localApiKeyManager.wasLatestUpdateOfLocalApiKeyListWithinDeadline(apiKey));
    }
}
