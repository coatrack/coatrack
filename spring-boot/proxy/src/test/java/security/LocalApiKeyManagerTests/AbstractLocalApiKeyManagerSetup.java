package security.LocalApiKeyManagerTests;

import eu.coatrack.api.ApiKey;
import eu.coatrack.proxy.security.ApiKeyFetcher;
import eu.coatrack.proxy.security.LocalApiKeyManager;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

public class AbstractLocalApiKeyManagerSetup {

    protected ApiKey apiKey;
    protected List<ApiKey> apiKeyList;
    protected ApiKeyFetcher apiKeyFetcherMock;
    protected LocalApiKeyManager localApiKeyManager;

    public void setupLocalApiKeyManagerAndApiKeyList() {
        apiKey = new ApiKey();
        apiKey.setKeyValue("ca716b82-745c-4f6d-a38b-ff8fe140ffd1");

        apiKeyList = new ArrayList<>();
        apiKeyList.add(apiKey);

        apiKeyFetcherMock = mock(ApiKeyFetcher.class);
        long timeInMinutesTheGatewayWorksWithoutConnectionToAdmin = 60;
        localApiKeyManager = new LocalApiKeyManager(apiKeyFetcherMock, timeInMinutesTheGatewayWorksWithoutConnectionToAdmin);
    }
}
