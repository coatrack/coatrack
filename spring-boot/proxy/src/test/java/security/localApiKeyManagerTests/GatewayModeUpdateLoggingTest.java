package security.localApiKeyManagerTests;

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
        when(apiKeyFetcherMock.requestLatestApiKeyListFromAdmin()).thenThrow(new ApiKeyFetchingFailedException("test"));
        localApiKeyManager.updateLocalApiKeyList();
    }

    class LogEventStorage extends ListAppender<ILoggingEvent> {
        public long getNumberOfEventsContaining(String phrase){
            return list.stream().filter(logEntry -> logEntry.getMessage().contains(phrase)).count();
        }
    }

}
