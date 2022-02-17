package eu.coatrack.admin.controllers;

/*-
 * #%L
 * coatrack-admin
 * %%
 * Copyright (C) 2013 - 2022 Corizon | Institut für angewandte Systemtechnik Bremen GmbH (ATB)
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

import eu.coatrack.admin.service.ProxyDockerComposeFileProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;

@Controller
public class GatewayDockerComposeFileDownloadController {

    private static final Logger log = LoggerFactory.getLogger(GatewayDockerComposeFileDownloadController.class);

    @Autowired
    private ProxyDockerComposeFileProviderService proxyDockerComposeFileProviderService;

    @GetMapping("/admin/proxy-docker-config/{proxy-id}/download")
    public void downloadFile(HttpServletResponse response, @PathVariable("proxy-id") String proxyId) throws Exception {
        log.error("Starting to create custom docker compose file.");
        String customizedContent = proxyDockerComposeFileProviderService.provideDockerComposeFileContentOfProxy(proxyId);
        InputStream inputStream = new ByteArrayInputStream(customizedContent.getBytes(StandardCharsets.UTF_8));

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", "docker-compose.yml"));
        response.setContentLength(customizedContent.length());

        FileCopyUtils.copy(inputStream, response.getOutputStream());
        inputStream.close();
    }


}
