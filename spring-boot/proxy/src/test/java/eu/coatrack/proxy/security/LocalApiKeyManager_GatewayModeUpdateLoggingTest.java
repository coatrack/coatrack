package eu.coatrack.proxy.security;

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
import eu.coatrack.proxy.security.exceptions.ApiKeyFetchingFailedException;
import eu.coatrack.proxy.security.exceptions.ApiKeyNotFoundInLocalApiKeyListException;
import eu.coatrack.proxy.security.exceptions.LocalApiKeyListWasNotInitializedException;
import eu.coatrack.proxy.security.exceptions.OfflineWorkingTimeExceedingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class LocalApiKeyManager_GatewayModeUpdateLoggingTest extends LocalApiKeyManager_AbstractTestSetupProvider {

    private final static String switchingToOnlineModeMessage = LocalApiKeyManager.switchingToOnlineModeMessage;
    private final static String switchingToOfflineModeMessage = LocalApiKeyManager.switchingToOfflineModeMessage;

    private LogEventStorage logEventStorage;

    @BeforeEach
    public void setup() {
        super.setupLocalApiKeyManagerWithoutInitializingLocalApiKeyList();

        Logger log = (Logger) LoggerFactory.getLogger(LocalApiKeyManager.class);
        logEventStorage = new LogEventStorage();
        logEventStorage.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        log.setLevel(Level.DEBUG);
        log.addAppender(logEventStorage);
        logEventStorage.start();
    }

    @Test
    public void dontPerformAnyGatewayModeSwitchesAndExpectNoSwitchingLogMessages() {
        assertEquals(0, logEventStorage.getNumberOfLogEventsContaining(switchingToOnlineModeMessage));
        assertEquals(0, logEventStorage.getNumberOfLogEventsContaining(switchingToOfflineModeMessage));
    }

    @Test
    public void switchingToOnlineModeShouldBeLogged() {
        letLocalApiKeyManagerSwitchToOnlineMode();

        assertEquals(1, logEventStorage.getNumberOfLogEventsContaining(switchingToOnlineModeMessage));
        assertEquals(0, logEventStorage.getNumberOfLogEventsContaining(switchingToOfflineModeMessage));
    }

    @Test
    public void stayingInOfflineModeShouldNotBeLogged() {
        //Gateway starts in offline mode and does therefore not switch modes.
        letLocalApiKeyManagerSwitchToOfflineMode();

        assertEquals(0, logEventStorage.getNumberOfLogEventsContaining(switchingToOnlineModeMessage));
        assertEquals(0, logEventStorage.getNumberOfLogEventsContaining(switchingToOfflineModeMessage));
    }

    @Test
    public void testGatewayModeSwitching_on_off() {
        letLocalApiKeyManagerSwitchToOnlineMode();
        letLocalApiKeyManagerSwitchToOfflineMode();

        assertEquals(1, logEventStorage.getNumberOfLogEventsContaining(switchingToOnlineModeMessage));
        assertEquals(1, logEventStorage.getNumberOfLogEventsContaining(switchingToOfflineModeMessage));
    }

    @Test
    public void testGatewayModeSwitching_off_on() {
        letLocalApiKeyManagerSwitchToOfflineMode();
        letLocalApiKeyManagerSwitchToOnlineMode();

        assertEquals(1, logEventStorage.getNumberOfLogEventsContaining(switchingToOnlineModeMessage));
        assertEquals(0, logEventStorage.getNumberOfLogEventsContaining(switchingToOfflineModeMessage));
    }

    @Test
    public void testGatewayModeSwitching_on_on() {
        letLocalApiKeyManagerSwitchToOnlineMode();
        letLocalApiKeyManagerSwitchToOnlineMode();

        assertEquals(1, logEventStorage.getNumberOfLogEventsContaining(switchingToOnlineModeMessage));
        assertEquals(0, logEventStorage.getNumberOfLogEventsContaining(switchingToOfflineModeMessage));
    }

    @Test
    public void testGatewayModeSwitching_off_off() {
        letLocalApiKeyManagerSwitchToOfflineMode();
        letLocalApiKeyManagerSwitchToOfflineMode();

        assertEquals(0, logEventStorage.getNumberOfLogEventsContaining(switchingToOnlineModeMessage));
        assertEquals(0, logEventStorage.getNumberOfLogEventsContaining(switchingToOfflineModeMessage));
    }

    @Test
    public void localApiKeySearchShouldTriggerSwitchFromOnlineToOfflineMode() {
        letLocalApiKeyManagerSwitchToOnlineMode();
        localApiKeyManager.getApiKeyEntityFromLocalCache(apiKey.getKeyValue());

        assertEquals(1, logEventStorage.getNumberOfLogEventsContaining(switchingToOnlineModeMessage));
        assertEquals(1, logEventStorage.getNumberOfLogEventsContaining(switchingToOfflineModeMessage));
    }

    public void letLocalApiKeyManagerSwitchToOnlineMode() {
        reset(apiKeyFetcherMock);
        when(apiKeyFetcherMock.requestLatestApiKeyListFromAdmin()).thenReturn(apiKeyList);
        localApiKeyManager.refreshLocalApiKeyCacheWithApiKeysFromAdmin();
    }

    public void letLocalApiKeyManagerSwitchToOfflineMode() {
        reset(apiKeyFetcherMock);
        when(apiKeyFetcherMock.requestLatestApiKeyListFromAdmin()).thenThrow(new ApiKeyFetchingFailedException("test"));

        //This method fails due to the thrown ApiKeyFetchingException and causes the gateway to switch to offline mode.
        localApiKeyManager.refreshLocalApiKeyCacheWithApiKeysFromAdmin();
    }

    static class LogEventStorage extends ListAppender<ILoggingEvent> {
        public long getNumberOfLogEventsContaining(String phrase) {
            return list.stream().filter(logEntry -> logEntry.getMessage().contains(phrase)).count();
        }
    }

}
