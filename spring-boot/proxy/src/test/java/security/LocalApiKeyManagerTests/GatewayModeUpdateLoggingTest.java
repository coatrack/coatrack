package security.LocalApiKeyManagerTests;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import eu.coatrack.proxy.security.ApiKeyFetchingFailedException;
import eu.coatrack.proxy.security.LocalApiKeyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class GatewayModeUpdateLoggingTest extends AbstractLocalApiKeyManagerSetup{

    private final static String onlineModeMessage = "Switching to online mode.";
    private final static String offlineModeMessage = "Switching to offline mode.";

    private LogEventStorage logEventStorage;
    private Logger log;

    @BeforeEach
    public void setup(){
        super.setupLocalApiKeyManagerAndApiKeyList();

        log = (Logger) LoggerFactory.getLogger(LocalApiKeyManager.class);
        logEventStorage = new LogEventStorage();
        logEventStorage.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        log.setLevel(Level.DEBUG);
        log.addAppender(logEventStorage);
        logEventStorage.start();
    }

    @Test
    public void NoAction_ShouldNotSwitchToAnyMode() {
        assertEquals(0, logEventStorage.getNumberOfEventsContaining(onlineModeMessage));
        assertEquals(0, logEventStorage.getNumberOfEventsContaining(offlineModeMessage));
    }

    @Test
    public void ON_ShouldDisplaySwitchToOnlineModeOnce() throws ApiKeyFetchingFailedException {
        switchToOnlineMode();

        assertEquals(1, logEventStorage.getNumberOfEventsContaining(onlineModeMessage));
        assertEquals(0, logEventStorage.getNumberOfEventsContaining(offlineModeMessage));
    }

    @Test
    public void OFF_ShouldNotSwitchToAnyMode() throws ApiKeyFetchingFailedException {
        switchToOfflineMode();

        assertEquals(0, logEventStorage.getNumberOfEventsContaining(onlineModeMessage));
        assertEquals(0, logEventStorage.getNumberOfEventsContaining(offlineModeMessage));
    }

    @Test
    public void ON_OFF_ShouldDisplaySwitchToOnlineModeOnce() throws ApiKeyFetchingFailedException {
        switchToOnlineMode();
        switchToOfflineMode();

        assertEquals(1, logEventStorage.getNumberOfEventsContaining(onlineModeMessage));
        assertEquals(0, logEventStorage.getNumberOfEventsContaining(offlineModeMessage));
    }

    @Test
    public void ON_OFF_OFF_ShouldDisplaySwitchToOnlineAndOfflineModeOnce() throws ApiKeyFetchingFailedException {
        switchToOnlineMode();
        switchToOfflineMode();
        switchToOfflineMode();

        assertEquals(1, logEventStorage.getNumberOfEventsContaining(onlineModeMessage));
        assertEquals(1, logEventStorage.getNumberOfEventsContaining(offlineModeMessage));
    }

    @Test
    public void ON_OFF_ON_ShouldDisplaySwitchToOnlineModeOnce() throws ApiKeyFetchingFailedException {
        switchToOnlineMode();
        switchToOfflineMode();
        switchToOnlineMode();

        assertEquals(1, logEventStorage.getNumberOfEventsContaining(onlineModeMessage));
        assertEquals(0, logEventStorage.getNumberOfEventsContaining(offlineModeMessage));
    }

    public void switchToOnlineMode() throws ApiKeyFetchingFailedException {
        reset(apiKeyFetcherMock);
        when(apiKeyFetcherMock.requestLatestApiKeyListFromAdmin()).thenReturn(apiKeyList);
        localApiKeyManager.updateLocalApiKeyList();
    }

    public void switchToOfflineMode() throws ApiKeyFetchingFailedException {
        reset(apiKeyFetcherMock);
        when(apiKeyFetcherMock.requestLatestApiKeyListFromAdmin()).thenThrow(new ApiKeyFetchingFailedException());
        localApiKeyManager.updateLocalApiKeyList();
    }

    class LogEventStorage extends ListAppender<ILoggingEvent> {
        public long getNumberOfEventsContaining(String phrase){
            return list.stream().filter(logEntry -> logEntry.getMessage().contains(phrase)).count();
        }
    }

}