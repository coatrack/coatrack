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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;
import eu.coatrack.admin.model.repository.ProxyRepository;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.service.GitService;
import eu.coatrack.config.ConfigServerCredential;
import eu.coatrack.api.Proxy;
import eu.coatrack.api.User;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author perezdf
 */
@Component
public class CreateProxyAction implements Action {

    private static final Logger log = LoggerFactory.getLogger(CreateProxyAction.class);

    ///////////////////////////
    //
    //  Services
    //
    ///////////////////////////
    @Autowired
    private GitService gitService;

    @Autowired
    private RestTemplate restTemplate;

    ///////////////////////////
    //
    //  Repositories
    //
    ///////////////////////////
    @Autowired
    private ServiceApiRepository serviceApiRepository;

    @Autowired
    private ProxyRepository proxyRepository;

    ///////////////////////////
    //
    //  I/O Parameters
    //
    ///////////////////////////
    private User user;

    private List<Long> selectedServices;

    private Proxy proxy;

    private Exception ex;

    ///////////////////////////
    //
    //  Config Server Parameters
    //
    ///////////////////////////
    @Value("${spring.cloud.config.uri}")
    String config_server_credentials_url;

    @Value("${spring.cloud.config.username}")
    String config_server_admin_name;

    @Value("${spring.cloud.config.password}")
    String config_server_password;

    @Override
    public void execute() {

        try {

            if (selectedServices != null) {
                selectedServices.forEach(s -> log.debug("service-id:" + s));
                selectedServices.stream()
                        .map(idString -> idString)
                        .map(id -> serviceApiRepository.findOne(id))
                        .forEach(service -> proxy.getServiceApis().add(service));
            }
            proxy.setId(UUID.randomUUID().toString());

            proxy.setConfigServerPassword(UUID.randomUUID().toString());
            ConfigServerCredential configServerCredential = new ConfigServerCredential();
            configServerCredential.setPassword(proxy.getConfigServerPassword());
            configServerCredential.setResource("/ygg-proxy-" + proxy.getId() + "/default");

            restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(config_server_admin_name, config_server_password));

            ConfigServerCredential credentialGenerated = restTemplate.postForObject(config_server_credentials_url+"/credentials", configServerCredential, ConfigServerCredential.class);
            proxy.setConfigServerName(credentialGenerated.getName());

            proxy.setOwner(user);
            proxy = proxyRepository.save(proxy);

            gitService.init();

            gitService.addProxy(proxy);
            gitService.commit("Add new proxy with id:" + proxy.getId());

        } catch (GitAPIException | URISyntaxException | IOException exception) {
            this.ex = exception;
        }
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

    public List<Long> getSelectedServices() {
        return selectedServices;
    }

    public void setSelectedServices(List<Long> selectedServices) {
        this.selectedServices = selectedServices;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public Exception getEx() {
        return ex;
    }

    public void setEx(Exception ex) {
        this.ex = ex;
    }

}
