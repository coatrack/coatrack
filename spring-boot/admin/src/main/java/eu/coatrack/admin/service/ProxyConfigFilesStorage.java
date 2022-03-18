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
public class ProxyConfigFilesStorage {

    @Value("${proxy.config.files.folder}")
    private String proxyConfigFilesFolderLocation;

    @Value("${ygg.admin.api-base-url-for-gateway}")
    private String adminApiBaseUrlForGateway;

    @PostConstruct
    private void emptyProxyConfigFilesFolderIfExists() throws IOException {
        Path proxyConfigFilesFolderPath = Paths.get(proxyConfigFilesFolderLocation);
        if (Files.exists(proxyConfigFilesFolderPath)){
            FileUtils.deleteDirectory(proxyConfigFilesFolderPath.toFile());
        }
        Files.createDirectory(proxyConfigFilesFolderPath);
    }

    public void addProxy(Proxy proxy) throws IOException {
        File proxyConfigFilesFolder = new File(proxyConfigFilesFolderLocation);
        if (!proxyConfigFilesFolder.exists())
            if (!proxyConfigFilesFolder.mkdirs())
                throw new IOException("Could not create directory " + proxyConfigFilesFolderLocation);

        PrintWriter writer = new PrintWriter(proxyConfigFilesFolderLocation + "/ygg-proxy-" + proxy.getId() + ".yml", "UTF-8");
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

    public void deleteProxy(Proxy proxy) throws FileNotFoundException, FileCouldNotBeDeletedException {
        File proxyConfigToBeDeleted = new File(proxyConfigFilesFolderLocation + "/ygg-proxy-" + proxy.getId() + ".yml", "UTF-8");
        if (!proxyConfigToBeDeleted.exists())
            throw new FileNotFoundException("Tried to delete the configuration for proxy " + proxy.getId()
                    + ", but there is no according file.");
        else {
            if (proxyConfigToBeDeleted.delete())
                log.debug("Proxy {} was successfully deleted.", proxy.getId());
            else
                throw new FileCouldNotBeDeletedException("Configuration file of proxy " + proxy.getId() + " could not be deleted.");
        }
    }

}
