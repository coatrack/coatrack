package eu.coatrack.admin.controllers;

/*-
 * #%L
 * coatrack-admin
 * %%
 * Copyright (C) 2013 - 2020 Corizon | Institut für angewandte Systemtechnik Bremen GmbH (ATB)
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

import eu.coatrack.admin.model.repository.ApiKeyRepository;
import eu.coatrack.admin.model.repository.MetricRepository;
import eu.coatrack.admin.model.repository.ProxyRepository;
import eu.coatrack.api.ApiKey;
import eu.coatrack.api.Metric;
import eu.coatrack.api.MetricType;
import eu.coatrack.api.Proxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest(showSql = true)
@ContextConfiguration(classes = SpringSecurityTestConfig.class)
@WithUserDetails("aa11aa22-aa33-aa44-aa55-aa66aa77aa88")
public class MetricsControllerTest {

    @Autowired
    MetricsController metricsController;

    @Autowired
    MetricRepository metricRepository;

    @Autowired
    ProxyRepository proxyRepository;

    @Autowired
    ApiKeyRepository apiKeyRepository;

    private Metric testMetric;
    private Metric copyOfTestMetric;
    private Proxy someProxyFromDB;
    private ApiKey someApiKeyFromDB;

    @BeforeEach
    public void prepareTestData() {

        someProxyFromDB = proxyRepository.findAll().iterator().next();
        someApiKeyFromDB = apiKeyRepository.findAll().iterator().next();

        testMetric = new Metric();
        testMetric.setCount(21);
        testMetric.setPath("/test/path/no/one/else/has/used");
        testMetric.setDateOfApiCall(new Date());
        testMetric.setHttpResponseCode(404);
        testMetric.setMetricsCounterSessionID("abcde-abcde-" + new Date().getTime());
        testMetric.setRequestMethod("GET");
        testMetric.setType(MetricType.RESPONSE);

        copyOfTestMetric = createCopyOfMetricObject(testMetric);
    }

    private Metric createCopyOfMetricObject(Metric objectToCopy) {

        Metric copy = new Metric();

        copy.setCount(objectToCopy.getCount());
        copy.setPath(objectToCopy.getPath());
        copy.setDateOfApiCall(objectToCopy.getDateOfApiCall());
        copy.setHttpResponseCode(objectToCopy.getHttpResponseCode());
        copy.setMetricsCounterSessionID(objectToCopy.getMetricsCounterSessionID());
        copy.setRequestMethod(objectToCopy.getRequestMethod());
        copy.setType(objectToCopy.getType());

        return copy;
    }

    @Test
    public void storeNewMetricInDatabaseTest() {

        long noOfRowsBefore = metricRepository.count();

        long newRowDbId = metricsController.storeMetricAndProperlySetRelationships(
                someProxyFromDB.getId(),
                someApiKeyFromDB.getKeyValue(),
                testMetric
        );

        long noOfRowsAfterwards = metricRepository.count();

        assertEquals(noOfRowsBefore + 1, noOfRowsAfterwards);

        Metric metricFromDatabase = metricRepository.findById(newRowDbId).orElse(null);

        assertNotNull(metricFromDatabase);
        assertEquals(someProxyFromDB, metricFromDatabase.getProxy());
        assertEquals(someApiKeyFromDB, metricFromDatabase.getApiKey());
        assertEquals(testMetric.getCount(), metricFromDatabase.getCount());
        assertEquals(testMetric.getPath(), metricFromDatabase.getPath());
        assertEquals(testMetric.getDateOfApiCall(), metricFromDatabase.getDateOfApiCall());
        assertEquals(testMetric.getHttpResponseCode(), metricFromDatabase.getHttpResponseCode());
        assertEquals(testMetric.getMetricsCounterSessionID(), metricFromDatabase.getMetricsCounterSessionID());
        assertEquals(testMetric.getRequestMethod(), metricFromDatabase.getRequestMethod());
        assertEquals(testMetric.getType(), metricFromDatabase.getType());

        testMetric.setType(MetricType.RESPONSE);
    }

    @Test
    public void sameMetricWithUpdatedCounterTest() {

        long rowIdFirstMetric = metricsController.storeMetricAndProperlySetRelationships(
                someProxyFromDB.getId(),
                someApiKeyFromDB.getKeyValue(),
                testMetric
        );

        Metric testMetricCopyWithUpdatedCounter = copyOfTestMetric;
        int newCounterValue = testMetric.getCount() + 10;
        testMetricCopyWithUpdatedCounter.setCount(newCounterValue);

        long rowIdUpdatedMetric = metricsController.storeMetricAndProperlySetRelationships(
                someProxyFromDB.getId(),
                someApiKeyFromDB.getKeyValue(),
                testMetricCopyWithUpdatedCounter
        );

        // database entry should have been updated, therefore IDs should be identical
        assertEquals(rowIdFirstMetric, rowIdUpdatedMetric);

        Metric metricFromDatabase = metricRepository.findById(rowIdFirstMetric).orElse(null);

        assertNotNull(metricFromDatabase);

        // count value in DB should have been updated
        assertEquals(newCounterValue, metricFromDatabase.getCount());
    }

    private void twoMetricsThatShouldBeStoredSeparatelyTest(Metric firstMetric, Metric secondMetric) {

        long dbIdOfFirstMetric = metricsController.storeMetricAndProperlySetRelationships(
                someProxyFromDB.getId(),
                someApiKeyFromDB.getKeyValue(),
                firstMetric
        );

        long dbIdOfSecondMetric = metricsController.storeMetricAndProperlySetRelationships(
                someProxyFromDB.getId(),
                someApiKeyFromDB.getKeyValue(),
                secondMetric
        );

        // there should be a new database entry, so IDs should be different
        assertNotEquals(dbIdOfFirstMetric, dbIdOfSecondMetric);

        Metric firstMetricFromDb = metricRepository.findById(dbIdOfFirstMetric).orElse(null);
        Metric secondMetricFromDb = metricRepository.findById(dbIdOfSecondMetric).orElse(null);

        // value should have been updated
        assertEquals(firstMetric.getCount(), firstMetricFromDb.getCount());
        assertEquals(secondMetric.getCount(), secondMetricFromDb.getCount());

    }

    @Test
    public void modifyPathTest() {

        copyOfTestMetric.setPath(testMetric.getPath() + "/pathmodification");
        twoMetricsThatShouldBeStoredSeparatelyTest(testMetric, copyOfTestMetric);
    }

    @Test
    public void modifyDateTest() {

        copyOfTestMetric.setDateOfApiCall(Date.from(LocalDateTime.now().plusDays(1l).toInstant(ZoneOffset.UTC)));
        twoMetricsThatShouldBeStoredSeparatelyTest(testMetric, copyOfTestMetric);
    }

    @Test
    public void modifyResponseCodeTest() {

        copyOfTestMetric.setHttpResponseCode(testMetric.getHttpResponseCode() + 1);
        twoMetricsThatShouldBeStoredSeparatelyTest(testMetric, copyOfTestMetric);
    }

    @Test
    public void responseCodeNullTest() {

        copyOfTestMetric.setHttpResponseCode(null);
        twoMetricsThatShouldBeStoredSeparatelyTest(testMetric, copyOfTestMetric);
    }

    @Test
    public void modifySessionIdTest() {

        copyOfTestMetric.setMetricsCounterSessionID(testMetric.getMetricsCounterSessionID() + "-modified");
        twoMetricsThatShouldBeStoredSeparatelyTest(testMetric, copyOfTestMetric);
    }

    @Test
    public void modifyRequestMethodTest() {

        copyOfTestMetric.setRequestMethod(testMetric.getRequestMethod() + "MODIFIED");
        twoMetricsThatShouldBeStoredSeparatelyTest(testMetric, copyOfTestMetric);
    }

    @Test
    public void modifyTypeTest() {

        copyOfTestMetric.setType(MetricType.EMPTY_RESPONSE);
        twoMetricsThatShouldBeStoredSeparatelyTest(testMetric, copyOfTestMetric);
    }

    @Test
    public void twoMetricsExistingInDatabaseUpdateTheCorrectOneTest() {

        // an old metric in database, which should not be updated
        Metric outdatedDatabaseEntry = testMetric;
        outdatedDatabaseEntry.setProxy(someProxyFromDB);
        outdatedDatabaseEntry.setApiKey(someApiKeyFromDB);
        outdatedDatabaseEntry.setCount(1);

        // the current metric in database that SHOULD be updated
        Metric currentDatabaseEntry = createCopyOfMetricObject(testMetric);
        currentDatabaseEntry.setProxy(someProxyFromDB);
        currentDatabaseEntry.setApiKey(someApiKeyFromDB);
        currentDatabaseEntry.setCount(2);

        // the new metric, based on which the current should be updated
        Metric latestSubmittedMetricToUpdateCurrent = createCopyOfMetricObject(testMetric);
        latestSubmittedMetricToUpdateCurrent.setCount(3);

        outdatedDatabaseEntry = metricRepository.save(outdatedDatabaseEntry);
        currentDatabaseEntry = metricRepository.save(currentDatabaseEntry);

        // assure that we have two database entries that have been assigned a different ID
        assertNotEquals(currentDatabaseEntry.getId(), outdatedDatabaseEntry.getId());
        assertEquals(1, outdatedDatabaseEntry.getCount());
        assertEquals(2, currentDatabaseEntry.getCount());

        long updatedRowDbId = metricsController.storeMetricAndProperlySetRelationships(
                someProxyFromDB.getId(),
                someApiKeyFromDB.getKeyValue(),
                latestSubmittedMetricToUpdateCurrent
        );

        // assure that the current was updated and that the counts are correctly set
        assertEquals(currentDatabaseEntry.getId(), updatedRowDbId);

        Metric outdatedReloaded = metricRepository.findById(outdatedDatabaseEntry.getId()).orElse(null);
        Metric currentReloaded = metricRepository.findById(currentDatabaseEntry.getId()).orElse(null);

        assertEquals(1, outdatedReloaded.getCount());
        assertEquals(3, currentReloaded.getCount());

    }
}
