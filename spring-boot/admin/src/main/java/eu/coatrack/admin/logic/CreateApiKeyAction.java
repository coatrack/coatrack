/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.coatrack.admin.logic;

/*-
 * #%L
 * ygg-admin
 * %%
 * Copyright (C) 2013 - 2019 Corizon
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

import java.util.Date;
import java.util.UUID;
import eu.coatrack.admin.model.repository.ApiKeyRepository;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.api.ApiKey;
import eu.coatrack.api.ServiceApi;
import eu.coatrack.api.User;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author perezdf
 */
@Component
public class CreateApiKeyAction implements Action {

    private static final Logger log = LoggerFactory.getLogger(CreateApiKeyAction.class);

    ///////////////////////////
    //
    //  Repositories
    //
    ///////////////////////////
    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ServiceApiRepository serviceApiRepository;

    ///////////////////////////
    //
    //  I/O Parameters
    //
    ///////////////////////////
    private User user;

    private ServiceApi serviceApi;

    private ApiKey apiKey;

    @Override
    public void execute() {

        apiKey = new ApiKey();
        apiKey.setKeyValue(UUID.randomUUID().toString());
        apiKey.setCreated(new Date());
        apiKey.setValidUntil(DateUtils.addMonths(new Date(), 1));
        apiKey.setServiceApi(serviceApi);
        apiKey.setUser(user);

        log.debug("creating a new API key {}", apiKey);
        apiKey = apiKeyRepository.save(apiKey);

        // also set the relationship on the service API side
        serviceApiRepository.findOne(serviceApi.getId());
        serviceApi.getApiKeys().add(apiKey);
        serviceApiRepository.save(serviceApi);

    }

    ///////////////////////////
    //
    //  Getters/Setters
    //
    ///////////////////////////
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ServiceApi getServiceApi() {
        return serviceApi;
    }

    public void setServiceApi(ServiceApi serviceApi) {
        this.serviceApi = serviceApi;
    }

    public ApiKey getApiKey() {
        return apiKey;
    }

    public void setApiKey(ApiKey apiKey) {
        this.apiKey = apiKey;
    }
    

}
