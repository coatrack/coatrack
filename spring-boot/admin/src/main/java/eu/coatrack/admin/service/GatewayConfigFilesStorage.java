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
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 *
 * @author perezdf, ChristophBaier
 */

@Slf4j
@Component
public class GatewayConfigFilesStorage {

    @Value("${proxy.config.files.folder}")
    private String gatewayConfigFilesFolderLocation;

    @Value("${ygg.admin.api-base-url-for-gateway}")
    private String adminApiBaseUrlForGateway;

    @Value("${is-run-via-docker-compose-setup}")
    private boolean isDockerComposeSetup;

    @PostConstruct
    private void emptyProxyConfigFilesFolderIfExistsForNonDockerComposeDeployments() throws IOException {
        if (!isDockerComposeSetup){
            String userHomeDir = System.getenv("USERPROFILE");
            Path coatrackDir = Paths.get(userHomeDir + "/.coatrack");
            if (Files.notExists(coatrackDir)) {
                Files.createDirectory(coatrackDir);
            }

            Path gatewayConfigFilesFolderPath = Paths.get(gatewayConfigFilesFolderLocation);
            if (Files.exists(gatewayConfigFilesFolderPath)){
                FileUtils.deleteDirectory(gatewayConfigFilesFolderPath.toFile());
            }
            Files.createDirectory(gatewayConfigFilesFolderPath);
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
        Path gatewayConfigFileToBeDeletedPath = Paths.get(gatewayConfigFilesFolderLocation + "/ygg-proxy-" + proxy.getId() + ".yml");
        if (Files.exists(gatewayConfigFileToBeDeletedPath)) {
            tryToDeleteGatewayConfigFile(proxy, gatewayConfigFileToBeDeletedPath);
        } else {
            throw new FileNotFoundException("Tried to delete the configuration file for proxy " + proxy.getId()
                    + ", but there is no according file + " + gatewayConfigFileToBeDeletedPath);
        }
    }

    private void tryToDeleteGatewayConfigFile(Proxy proxy, Path gatewayConfigFileToBeDeletedPath) throws IOException {
        try {
            Files.delete(gatewayConfigFileToBeDeletedPath);
            log.debug("Gateway {} was successfully deleted.", proxy.getId());
        } catch (Exception e) {
            throw new FileCouldNotBeDeletedException("Configuration file of proxy " + proxy.getId() + " could not be deleted.", e);
        }
    }

}
