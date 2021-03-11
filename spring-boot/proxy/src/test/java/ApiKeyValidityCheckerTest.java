import eu.coatrack.api.ApiKey;
import eu.coatrack.proxy.security.ApiKeyValidityChecker;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ApiKeyValidityCheckerTest {

    private final String someValidValue = "someValidValue";

    private ApiKey apiKey;
    private List<String> apiKeyValueList;
    private ResponseEntity<ApiKey> responseEntity;
    private ApiKeyValidityChecker checker = new ApiKeyValidityChecker();

    private final long oneDayInMillis = 1000 * 60 * 60 *24;
    private Timestamp today = new Timestamp(System.currentTimeMillis());
    private Timestamp tomorrow = new Timestamp(today.getTime() + oneDayInMillis);
    private Timestamp yesterday = new Timestamp(today.getTime() - oneDayInMillis);

    @BeforeEach
    public void buildAWorkingSetup(){
        buildUpApiKey();
        buildUpChecker();
        buildUpResponseEntity();
    }

    private void buildUpApiKey() {
        apiKey = new ApiKey();
        apiKey.setKeyValue(someValidValue);
        apiKey.setDeletedWhen(null);
        apiKey.setValidUntil(tomorrow);
    }

    private void buildUpChecker() {
        checker.setLastApiKeyValueListUpdate(new Timestamp(System.currentTimeMillis()));
        apiKeyValueList = new ArrayList<>();
        apiKeyValueList.add(someValidValue);
        checker.setApiKeyList(apiKeyValueList);
    }

    private void buildUpResponseEntity() {
        responseEntity = new ResponseEntity<>(apiKey, HttpStatus.OK);
    }

    @Test
    public void isDefaultKeyAccepted(){
        assertTrue(checker.doesResultValidateApiKey(responseEntity, someValidValue));
    }

    @Test
    public void testIfDeletedKeyIsDenied(){
        apiKey.setDeletedWhen(yesterday);
        assertFalse(checker.doesResultValidateApiKey(responseEntity, someValidValue));
    }

    @Test
    public void testIfExpiredKeyIsDenied(){
        apiKey.setValidUntil(yesterday);
        assertFalse(checker.doesResultValidateApiKey(responseEntity, someValidValue));
    }

    @Test
    public void testIfAKeyWhichWasRejectedByAdminIsDenied(){
        apiKey = null;
        responseEntity = new ResponseEntity<>(apiKey, HttpStatus.OK);
        assertFalse(checker.doesResultValidateApiKey(responseEntity, someValidValue));
    }

    @Test
    public void testIfApiKeyIsAcceptedSinceItIsInTheLocalApiKeyList(){
        responseEntity = null;
        assertTrue(checker.doesResultValidateApiKey(responseEntity, someValidValue));
    }

    @Test
    public void testIfApiKeyIsDeniedBecauseOfEmptyLocalApiKeyList(){
        responseEntity = null;
        checker.setApiKeyList(new ArrayList<>());
        assertFalse(checker.doesResultValidateApiKey(responseEntity, someValidValue));
    }

    @Test
    public void testIfApiKeyIsDeniedBecauseAdminIsNotReachableForLongerThanOneHour(){
        responseEntity = null;
        checker.setLastApiKeyValueListUpdate(yesterday);
        assertFalse(checker.doesResultValidateApiKey(responseEntity, someValidValue));
    }
}
