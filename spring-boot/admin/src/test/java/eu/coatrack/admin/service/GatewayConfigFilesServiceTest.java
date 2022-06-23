package eu.coatrack.admin.service;

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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import eu.coatrack.api.Proxy;
import eu.coatrack.api.ServiceApi;
import eu.coatrack.api.User;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author perezdf
 */
public class GatewayConfigFilesServiceTest {

    public GatewayConfigFilesServiceTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
    }

    @Autowired
    GatewayConfigFilesService gatewayConfigFilesService;
    
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    //Test
    public void testBasic() throws IOException, GitAPIException, URISyntaxException {
        Proxy proxy = new Proxy();
        proxy.setId(UUID.randomUUID().toString());

        User owner = new User();
        owner.setInitialized(Boolean.FALSE);
        
        
        ServiceApi service = new ServiceApi();
        service.setOwner(owner);
        service.setId(Calendar.getInstance().getTimeInMillis());
        service.setLocalUrl(" http://localhost:8083/crate-tracking2");
        Set<ServiceApi> services = new HashSet<ServiceApi>();
        services.add(service);
        proxy.setServiceApis(services);

        gatewayConfigFilesService.addGatewayConfigFile(proxy);
    }
}
