package eu.coatrack.admin.model.repository;

/*-
 * #%L
 * coatrack-api
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

import eu.coatrack.api.ApiKey;
import eu.coatrack.api.Metric;
import eu.coatrack.api.Proxy;
import eu.coatrack.api.ServiceApi;
import eu.coatrack.api.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * @author perezdf
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class MetricRepositoryTest {

    @Autowired
    MetricRepository metricRepository;

    @Autowired
    ApiKeyRepository apiKeyRepository;

    @Autowired
    ServiceApiRepository serviceApiRepository;

    @Autowired
    UserRepository userRepository;

    @Test
    public void save() {

        ApiKey apiKey = new ApiKey();
        apiKey.setKeyValue("anything");
        apiKeyRepository.save(apiKey);

        ServiceApi service = new ServiceApi();
        service.setName("something");
        service.setUriIdentifier("service");
        service.setLocalUrl("https://www.google.com");
        serviceApiRepository.save(service);

        User user = new User();
        user.setUsername("something");
        user.setFirstname("first");
        user.setLastname("Last");
        user.setCompany("company");
        user.setEmail("test@gmail.com");
        user.setInitialized(Boolean.FALSE);
        userRepository.save(user);

        apiKey.setServiceApi(service);
        apiKey.setUser(user);

        Proxy proxy = new Proxy();
        proxy.setName("something");

        Metric metric = new Metric();
        metric.setCount(1);
        metric.setApiKey(apiKey);
        //metric.setProxy(proxy);

        metricRepository.save(metric);

    }

}
