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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.UUID;
import eu.coatrack.api.Proxy;
import eu.coatrack.api.ServiceApi;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author perezdf
 */
@Component
public class GitService {

    private static final Logger log = LoggerFactory.getLogger(GitService.class);
    private File resultDir;
    private Git git;

    @Value("${ygg.admin.gitService.url}")
    private String gitServiceUrl;
    
    @Value("${ygg.admin.gitService.user}")
    private String gitServiceUser;
    
    @Value("${ygg.admin.gitService.password}")
    private String gitServicePassword;

    @Value("${ygg.admin.api-base-url-for-gateway}")
    private String adminApiBaseUrlForGateway;

    @Value("${ygg.admin.server.url}")
    private String adminServerUrl;

    public void init() throws IOException, GitAPIException {
        

        String tmpDirStr = System.getProperty("java.io.tmpdir");
        if (tmpDirStr == null) {
            throw new IOException(
                    "System property 'java.io.tmpdir' does not specify a tmp dir");
        }

        File tmpDir = new File(tmpDirStr);
        if (!tmpDir.exists()) {
            boolean created = tmpDir.mkdirs();
            if (!created) {
                throw new IOException("Unable to create tmp dir " + tmpDir);
            }
        }

        resultDir = new File(tmpDir, "ygg-admin-" + UUID.randomUUID());
        if (resultDir.exists()) {
            throw new IOException(" attempts to generate a non-existent directory name failed, giving up");
        }
    
        boolean created = resultDir.mkdir();
        if (!created) {
            throw new IOException("Failed to create tmp directory");
        }

        // TODO parametrize
        git = Git.cloneRepository()
                .setURI(gitServiceUrl)
                .setDirectory(resultDir)
                .call();

    }

    public void addProxy(Proxy proxy) throws GitAPIException, URISyntaxException, FileNotFoundException, UnsupportedEncodingException {

        PrintWriter writer = new PrintWriter(resultDir + "/ygg-proxy-" + proxy.getId() + ".yml", "UTF-8");
        writer.println("proxy-id: " + proxy.getId());
        writer.println("ygg.admin.api-base-url: " + adminApiBaseUrlForGateway);
        writer.println("ygg.admin.server.url: " + adminServerUrl);
        if (proxy.getPort() != null) {
            writer.println("server.port: " + proxy.getPort());
        }

        for (ServiceApi service : proxy.getServiceApis()) {

            writer.println("zuul.routes." + service.getUriIdentifier() + ".url : " + service.getLocalUrl());
        }
        writer.println("zuul.host.connect-timeout-millis: 150000");
        writer.println("zuul.host.socket-timeout-millis: 150000");
        writer.close();

        AddCommand addCommand = git.add();
        addCommand.addFilepattern(".");
        addCommand.call();

    }

    public void deleteProxy(Proxy proxy) throws GitAPIException, URISyntaxException, FileNotFoundException, UnsupportedEncodingException {

        RmCommand deleteCommand = git.rm();
        deleteCommand.addFilepattern("ygg-proxy-" + proxy.getId() + ".yml");
        deleteCommand.call();

    }

    public void commit(String message) throws GitAPIException, URISyntaxException {

        CommitCommand command = git.commit();

        command.setMessage(message).call();

        // push to remote:
        PushCommand pushCommand = git.push();
        pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitServiceUser, gitServicePassword));
        // you can add more settings here if needed
        pushCommand.call();
        git.close();

    }

}
