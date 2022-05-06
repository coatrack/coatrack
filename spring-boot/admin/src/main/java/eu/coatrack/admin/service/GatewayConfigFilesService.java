package eu.coatrack.admin.service;

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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import eu.coatrack.api.Proxy;
import eu.coatrack.api.ServiceApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 *
 * @author perezdf, ChristophBaier
 */

@Slf4j
@Service
public class GatewayConfigFilesService {

    @Value("${ygg.admin.gateway.config.files.folder}")
    private String gatewayConfigFilesFolderLocation;

    @Value("${ygg.admin.api-base-url-for-gateway}")
    private String adminApiBaseUrlForGateway;

    @Value("${ygg.admin.store-gateway-config-files-folder-in-project-directory}")
    private boolean isGatewayConfigFilesFolderInUsersHomeDirectory;

    @PostConstruct
    private void prepareEmptyGatewayConfigFileFolder() throws IOException {
        if (isGatewayConfigFilesFolderInUsersHomeDirectory){
            Path gatewayConfigFilesFolderPath = Paths.get(gatewayConfigFilesFolderLocation);
            if (Files.notExists(gatewayConfigFilesFolderPath)){
                Files.createDirectory(gatewayConfigFilesFolderPath);
            }
        }
    }

    public void addGatewayConfigFile(Proxy proxy) throws IOException {
        PrintWriter writer = new PrintWriter(gatewayConfigFilesFolderLocation + "/ygg-proxy-" + proxy.getId() + ".yml", "UTF-8");
        writer.println("proxy-id: " + proxy.getId());
        writer.println("ygg.admin.api-base-url: " + adminApiBaseUrlForGateway);
        if (proxy.getPort() != null) {
            writer.println("server.port: " + proxy.getPort());
        }
        for (ServiceApi service : proxy.getServiceApis()) {
            writer.println("zuul.routes." + service.getUriIdentifier() + ".url : " + service.getLocalUrl());
        }
        writer.println("zuul.host.connect-timeout-millis: 150000");
        writer.println("zuul.host.socket-timeout-millis: 150000");
        writer.close();
    }

    public void deleteGatewayConfigFile(Proxy proxy) throws IOException {
        log.debug("Config file of Gateway {} is being deleted.", proxy.getId());
        Path gatewayConfigFileToBeDeletedPath = Paths.get(gatewayConfigFilesFolderLocation + "/ygg-proxy-" + proxy.getId() + ".yml");
        if (Files.exists(gatewayConfigFileToBeDeletedPath)) {
            tryToDeleteGatewayConfigFile(gatewayConfigFileToBeDeletedPath);
        } else {
            throw new FileNotFoundException("Tried to delete the configuration file for proxy " + proxy.getId()
                    + ", but there is no according file + " + gatewayConfigFileToBeDeletedPath);
        }
    }

    private void tryToDeleteGatewayConfigFile(Path gatewayConfigFileToBeDeletedPath) throws IOException {
        try {
            Files.delete(gatewayConfigFileToBeDeletedPath);
            log.debug("Gateway config file was successfully deleted.");
        } catch (Exception e) {
            throw new FileCouldNotBeDeletedException("Configuration file of gateway could not be deleted.", e);
        }
    }

}
