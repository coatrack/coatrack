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
import eu.coatrack.api.ApiKey;
import eu.coatrack.api.Metric;
import eu.coatrack.api.MetricType;
import eu.coatrack.api.Proxy;
import eu.coatrack.admin.service.report.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import java.util.Date;

@DataJpaTest()
@ContextConfiguration(classes = SpringSecurityTestConfig.class)
public class AdminControllerTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private MetricRepository metricRepository;

    @Autowired
    private ProxyRepository proxyRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ServiceApiRepository serviceApiRepository;

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
}
