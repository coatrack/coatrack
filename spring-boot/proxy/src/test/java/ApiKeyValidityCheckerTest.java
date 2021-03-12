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

    private final long oneHourInMillis = 1000 * 60 * 60;
    private final long oneDayInMillis = oneHourInMillis * 24;
    private final Timestamp now = new Timestamp(System.currentTimeMillis());
    private final Timestamp tomorrow = new Timestamp(now.getTime() + oneDayInMillis);
    private final Timestamp yesterday = new Timestamp(now.getTime() - oneDayInMillis);
    private final Timestamp twoHoursAgo = new Timestamp(now.getTime() - oneHourInMillis * 2);
    private final Timestamp halfAnHourAgo = new Timestamp(now.getTime() - oneHourInMillis / 2);

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
        apiKey.setDeletedWhen(halfAnHourAgo);
        assertFalse(checker.doesResultValidateApiKey(responseEntity, someValidValue));
    }

    @Test
    public void testIfExpiredKeyIsDenied(){
        apiKey.setValidUntil(yesterday);
        assertFalse(checker.doesResultValidateApiKey(responseEntity, someValidValue));
        apiKey.setValidUntil(halfAnHourAgo);
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
        activateTestOfLocalApiKeyList();
        assertTrue(checker.doesResultValidateApiKey(responseEntity, someValidValue));
    }

    @Test
    public void testIfApiKeyIsDeniedBecauseOfEmptyLocalApiKeyList(){
        activateTestOfLocalApiKeyList();
        checker.setApiKeyList(new ArrayList<>());
        assertFalse(checker.doesResultValidateApiKey(responseEntity, someValidValue));
    }

    @Test
    public void testIfApiKeyIsDeniedBecauseAdminIsNotReachableForMoreThanOneHour(){
        activateTestOfLocalApiKeyList();
        checker.setLastApiKeyValueListUpdate(yesterday);
        assertFalse(checker.doesResultValidateApiKey(responseEntity, someValidValue));
        checker.setLastApiKeyValueListUpdate(twoHoursAgo);
        assertFalse(checker.doesResultValidateApiKey(responseEntity, someValidValue));
    }

    @Test
    public void testIfApiKeyIsAcceptedBecauseAdminIsNotReachableForLessThanOneHour(){
        activateTestOfLocalApiKeyList();
        checker.setLastApiKeyValueListUpdate(halfAnHourAgo);
        assertTrue(checker.doesResultValidateApiKey(responseEntity, someValidValue));
    }

    public void activateTestOfLocalApiKeyList(){
        responseEntity = null;
    }
}
