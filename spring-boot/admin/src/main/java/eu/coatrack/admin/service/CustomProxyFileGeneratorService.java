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
import eu.coatrack.api.Proxy;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import org.apache.commons.io.FileUtils;

/**
 * Generates custom CoatRack proxy jars, which can then be downloaded and
 * installed on API provider side
 *
 * @author gr-hovest(at)atb-bremen.de
 */
@Service
public class CustomProxyFileGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(CustomProxyFileGeneratorService.class);

    // ****************  Config Properties  ****************  
    @Value("${ygg.proxy.executable-jar.template.url}")
    private String YGG_PROXY_FILENAME_ORIGINAL_PREFIX;

    @Value("${ygg.proxy.executable-jar.generated.path}")
    private String PATH_TO_GENERATED_YGG_PROXY_JAR;

    @Value("${mvn.pom.project.version}")
    private String MVN_POM_PROJECT_VERSION;

    @Value("${ygg.proxy.executable-jar.filename-custom.prefix}")
    private String YGG_PROXY_FILENAME_CUSTOM_PREFIX;

    @Value("${ygg.proxy.executable-jar.filename-custom.suffix}")
    private String YGG_PROXY_FILENAME_CUSTOM_SUFFIX;

    @Value("${ygg.proxy.generate-bootstrap-properties.spring.application.name.prefix}")
    private String CUSTOM_PROPERTIES_APPLICATION_NAME_PREFIX;

    @Value("${ygg.proxy.generate-bootstrap-properties.spring.cloud.config.uri}")
    private String CUSTOM_PROPERTIES_CLOUD_CONFIG_URI_VALUE;

    // ****************  Config Properties  ****************  
    // Please note: the following also works on Windows because the JAR file API seems to use "/" as separator on every system
    private static final String CUSTOM_CONFIG_FILE = "BOOT-INF/classes/bootstrap.properties";

    private static final String CUSTOM_PROPERTIES_APPLICATION_NAME_KEY = "spring.application.name";
    private static final String CUSTOM_PROPERTIES_CLOUD_CONFIG_URI_KEY = "spring.cloud.config.uri";

    private static final String CUSTOM_PROPERTIES_CREDENTIAL_ID = "spring.cloud.config.username";

    private static final String CUSTOM_PROPERTIES_CREDENTIAL_PASSWORD = "spring.cloud.config.password";

    /**
     * This method generates the proxy file from an Proxy Entity using a jar
     * template and overwriting specific properties
     *
     * @param proxy
     * @return
     * @throws java.net.MalformedURLException
     */
    public File getCustomJarForDownload(Proxy proxy) throws MalformedURLException, IOException {

        // ***
        // Retrieve proxy template
        // ***
        URL urlProxyOriginalExecutableFile = new URL(
                YGG_PROXY_FILENAME_ORIGINAL_PREFIX
                + MVN_POM_PROJECT_VERSION
                + ".jar");

        Path localTemplateProxyJar = Files.createTempFile("proxy-template", ".jar");

        FileUtils.copyURLToFile(urlProxyOriginalExecutableFile, localTemplateProxyJar.toFile());

        // ***
        // Create generate proxy file
        // ***
        String newCustomProxyFilename = YGG_PROXY_FILENAME_CUSTOM_PREFIX + MVN_POM_PROJECT_VERSION + "-" + proxy.getId() + YGG_PROXY_FILENAME_CUSTOM_SUFFIX;

        File newCustomProxyFile = new File(
                PATH_TO_GENERATED_YGG_PROXY_JAR + File.separator
                + newCustomProxyFilename);

        log.debug("generating new file '{}' based on original file '{}'", newCustomProxyFile, localTemplateProxyJar.toFile());

        // ***
        // Prepare jar
        // ***
        try {
            JarFile jarToCopy = new JarFile(localTemplateProxyJar.toFile());
            Enumeration<JarEntry> entriesInsideJarToCopy = jarToCopy.entries();

            JarInputStream originalJarInputStream = new JarInputStream(new FileInputStream(localTemplateProxyJar.toFile()));
            JarOutputStream customJarOutputStream = new JarOutputStream(new FileOutputStream(newCustomProxyFile));

            // copy all elements from the original CoatRack proxy jar, except for custom config file
            while (entriesInsideJarToCopy.hasMoreElements()) {

                // get meta data for this file from source jar
                JarEntry jarEntryToCopy = entriesInsideJarToCopy.nextElement();

                if (jarEntryToCopy.getName().equals(CUSTOM_CONFIG_FILE)) {
                    // this is the custom config file "bootstrap properties" --> to be replaced
                    log.debug("custom config file already existing in source, will be replaced: {}", jarEntryToCopy);

                    // Add meta data for new file
                    JarEntry jarEntryForNewConfigFile = new JarEntry(CUSTOM_CONFIG_FILE);
                    customJarOutputStream.putNextEntry(jarEntryForNewConfigFile);

                    // generate and add new properties file
                    Properties bootstrapPropsForGeneratedFile = generateCustomProperties(proxy);
                    log.debug("contents for new config file {} are {}", jarEntryForNewConfigFile, bootstrapPropsForGeneratedFile);
                    bootstrapPropsForGeneratedFile.store(customJarOutputStream, "Custom properties file generated by CoatRack");

                } else {
                    // another file --> to be copied to target jar

                    // add meta data
                    customJarOutputStream.putNextEntry(jarEntryToCopy);

                    // copy actual file
                    InputStream inputStreamForEntryToCopy = jarToCopy.getInputStream(jarEntryToCopy);
                    IOUtils.copy(inputStreamForEntryToCopy, customJarOutputStream);

                    customJarOutputStream.closeEntry();
                    IOUtils.closeQuietly(inputStreamForEntryToCopy);
                }
            }

            customJarOutputStream.flush();
            IOUtils.closeQuietly(customJarOutputStream);
            IOUtils.closeQuietly(originalJarInputStream);
        } catch (IOException e) {
            log.error("Error while generating proxy file", e);
        }
        return newCustomProxyFile;
    }

    /**
     * This method aims to fill the necessary bootstrap properties for the
     * generated proxy, enabling the proxy to get its config from the CoatRack
     * config server
     *
     * @param proxy
     * @return the bootstrap properties for the generated proxy
     */
    private Properties generateCustomProperties(Proxy proxy) {

        Properties bootstrapPropsForGeneratedFile = new Properties();
        bootstrapPropsForGeneratedFile.setProperty(
                CUSTOM_PROPERTIES_APPLICATION_NAME_KEY,
                CUSTOM_PROPERTIES_APPLICATION_NAME_PREFIX + proxy.getId());
        bootstrapPropsForGeneratedFile.setProperty(
                CUSTOM_PROPERTIES_CLOUD_CONFIG_URI_KEY,
                CUSTOM_PROPERTIES_CLOUD_CONFIG_URI_VALUE);

        bootstrapPropsForGeneratedFile.setProperty(
                CUSTOM_PROPERTIES_CREDENTIAL_ID,
                proxy.getConfigServerUsername());
        bootstrapPropsForGeneratedFile.setProperty(
                CUSTOM_PROPERTIES_CREDENTIAL_PASSWORD,
                proxy.getConfigServerPassword());

        bootstrapPropsForGeneratedFile.setProperty("management.security.enabled", "false");

        return bootstrapPropsForGeneratedFile;
    }

}
