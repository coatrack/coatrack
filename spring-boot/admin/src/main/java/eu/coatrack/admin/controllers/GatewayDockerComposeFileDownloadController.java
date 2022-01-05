package eu.coatrack.admin.controllers;

import eu.coatrack.admin.YggAdminApplication;
import eu.coatrack.admin.model.repository.ProxyRepository;
import eu.coatrack.api.Proxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Controller
public class GatewayDockerComposeFileDownloadController {

    @Autowired
    private ProxyRepository proxyRepository;

    @Value("spring.cloud.config.uri")
    private String springCloudConfigUri;

    @GetMapping("/admin/proxy-docker-config/{proxy-id}/download")
    public void downloadFile(HttpServletResponse response, @PathVariable("proxy-id") String proxyId) throws Exception {
        String templateContent = loadContentFromTemplateFile();
        String customizedContent = replacePlaceholdersWithCustomConfigValues(templateContent, proxyId);
        addContentToResponseAsDownloadableFile(response, customizedContent);
    }

    private String loadContentFromTemplateFile() throws URISyntaxException, IOException {
        URL resource = YggAdminApplication.class.getClassLoader().getResource("proxy-docker-compose-template.txt");
        assert resource != null;
        File file = new File(resource.toURI());

        byte[] encoded = Files.readAllBytes(file.toPath());
        return new String(encoded, StandardCharsets.UTF_8);
    }

    private String replacePlaceholdersWithCustomConfigValues(String templateContent, String proxyId) {
        Proxy proxy = proxyRepository.findById(proxyId);
        return templateContent
                .replace("<proxy-id>", proxyId)
                .replace("<config-server-uri>", springCloudConfigUri)
                .replace("<username>", proxy.getName())
                .replace("<password>", proxy.getConfigServerPassword());
    }

    private void addContentToResponseAsDownloadableFile(HttpServletResponse response, String customizedContent) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(customizedContent.getBytes(StandardCharsets.UTF_8));

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", "docker-compose.yml"));
        response.setContentLength(customizedContent.length());

        FileCopyUtils.copy(inputStream, response.getOutputStream());
        inputStream.close();
    }

}
