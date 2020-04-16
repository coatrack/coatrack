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
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.api.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ContextConfiguration;

@RunWith(SpringRunner.class)
@DataJpaTest(showSql = true)
@ContextConfiguration(classes = SpringSecurityTestConfig.class)
public class AdminControllerTest {

    @Autowired
    ReportController reportController;

    @Autowired
    MetricRepository metricRepository;

    @Autowired
    ProxyRepository proxyRepository;

    @Autowired
    ApiKeyRepository apiKeyRepository;

    @Autowired
    ServiceApiRepository serviceApiRepository;

    private Metric testMetric;
    private Metric copyOfTestMetric;
    private Proxy someProxyFromDB;
    private ApiKey someApiKeyFromDB;

    @Before
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
    @WithUserDetails("edeka")
    public void test() {

        ServiceApi randomServiceApiFromDB = serviceApiRepository.findAll().iterator().next();
        reportController.calculateApiUsageReportForSpecificService(randomServiceApiFromDB, -1L, new Date(), new Date(), false);
    }

}
