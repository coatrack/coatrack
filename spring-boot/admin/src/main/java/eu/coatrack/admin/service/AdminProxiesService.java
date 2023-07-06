package eu.coatrack.admin.service;

import eu.coatrack.admin.controllers.AdminProxiesController;
import eu.coatrack.admin.logic.CreateProxyAction;
import eu.coatrack.admin.model.repository.MetricsAggregationCustomRepository;
import eu.coatrack.admin.model.repository.ProxyRepository;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.api.ApiKey;
import eu.coatrack.api.Proxy;
import eu.coatrack.api.User;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AdminProxiesService {

    private static final String ADMIN_PROXIES_LIST_VIEW = "admin/proxies/list";
    private static final String ADMIN_PROXY_VIEW = "admin/proxies/proxy";
    private static final String ADMIN_PROXY_EDITOR = "admin/proxies/edit";

    private static final String PROXY_DOWNLOAD_MIMETYPE = "application/octet-stream";
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProxyRepository proxyRepository;

    @Autowired
    private MetricsAggregationCustomRepository metricsAggregationRepository;

    @Autowired
    private ServiceApiRepository serviceApiRepository;

    @Autowired
    private CreateProxyAction createProxyAction;

    @Autowired
    private CustomProxyFileGeneratorService customProxyFileGenerator;

    @Autowired
    private GatewayConfigFilesService gatewayConfigFilesService;

    @Autowired
    private RestTemplate restTemplate;

    public ModelAndView proxyListPage() {
        ModelAndView mav = new ModelAndView();
        mav.addObject("proxies", proxyRepository.findAvailable());
        mav.setViewName(ADMIN_PROXIES_LIST_VIEW);
        return mav;
    }

    public Iterable<Proxy> proxyListPageRest() {
        log.info("proxyListPageRest: {}", proxyRepository.findAvailable());
        return proxyRepository.findAvailable();
    }

    public ModelAndView newProxyForm() {
        log.debug("New Form");
        Proxy p = new Proxy();

        ModelAndView mav = new ModelAndView();
        mav.addObject("proxy", p);
        mav.addObject("services", serviceApiRepository.findByDeletedWhen(null));
        mav.addObject("mode", 0);
        mav.setViewName(ADMIN_PROXY_EDITOR);
        return mav;
    }

    public ModelAndView updateProxyForm(String id) {
        log.debug("Update Form");
        Proxy proxy = proxyRepository.findById(id).orElse(null);

        ModelAndView mav = new ModelAndView();
        mav.addObject("proxy", proxy);
        mav.addObject("services", serviceApiRepository.findByDeletedWhen(null));
        mav.addObject("mode", 1);
        mav.setViewName(ADMIN_PROXY_EDITOR);
        return mav;
    }

    public ModelAndView addProxy(Proxy proxy, List<Long> selectedServices) throws IOException {
        log.debug("POST call to proxy/add: " + proxy.toString());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());

        proxy.setId(UUID.randomUUID().toString());

        createProxyAction.setProxy(proxy);
        createProxyAction.setUser(user);
        createProxyAction.setSelectedServices(selectedServices);
        createProxyAction.execute();

        gatewayConfigFilesService.addGatewayConfigFile(proxy);

        return proxyListPage();
    }

    public Proxy updateProxy(Proxy proxyStored, List<String> selectedServices) throws GitAPIException, IOException, URISyntaxException {

        if (proxyStored != null) {
            proxyStored.setDescription(proxyStored.getDescription());
            proxyStored.setName(proxyStored.getName());
            proxyStored.setPublicUrl(proxyStored.getPublicUrl());
            proxyStored.setPort(proxyStored.getPort());
            proxyStored.setServiceApis(new HashSet<>());

            if (selectedServices != null) {
                selectedServices.forEach(s -> log.debug("service-id:" + s));
                selectedServices.stream()
                        .map(Long::parseLong)
                        .map(id -> serviceApiRepository.findById(id).orElse(null))
                        .forEach(service -> proxyStored.getServiceApis().add(service));
            }
            proxyRepository.save(proxyStored);
            // transmit config changes to config server git repo

            transmitConfigChangesToGitConfigRepository(proxyStored);
            // call proxy so that it will get the latest config from git (by calling the config server)
            informProxyAboutUpdatedConfiguration(proxyStored);

        }
        return proxyStored;
    }

    public ModelAndView tryUpdateProxy(Proxy proxy, List<String> selectedServices) {
        log.debug("Update proxy: " + proxy.toString());

        Proxy proxyStored = proxyRepository.findById(proxy.getId()).orElse(null);
        ModelAndView result = null;
        if(proxyStored != null) {
            try {
                proxyStored = updateProxy(proxyStored, selectedServices);
                informProxyAboutUpdatedConfiguration(proxyStored); // call proxy so that it will get the latest config from git (by calling the config server)
            } catch (IOException | URISyntaxException | GitAPIException e) {
                log.error("Error when trying to transmit config to git repository: ", e);
                return updateFormWithErrorMessage(proxyStored, "updateProxyGitError");
            } catch (Exception e) {
                log.error("Error when trying to call 'refresh' on the proxy: ", e);
                String errorMessageForGUI = String.format(
                        "Refresh on '%s' (public url: '%s') failed: %s; ",
                        proxyStored.getName(), proxyStored.getPublicUrl(), e);
                return updateFormWithErrorMessage(proxyStored, "updateProxyRefreshError", errorMessageForGUI);
            }
        }
        return proxyListPage();
    }


    public String getProxyById(String id, Model model) {
        Proxy proxy = proxyRepository.findById(id).orElse(null);
        if(proxy != null) {
            model.addAttribute("proxy", proxy);
            model.addAttribute("metrics", metricsAggregationRepository.getSummarizedMetricsByProxyId(proxy.getId()));
        }
        return ADMIN_PROXY_VIEW;
    }

    public Proxy getProxyByIdRest(@PathVariable("id") String id) {
        Proxy proxy = proxyRepository.findById(id).orElse(null);
        log.info("getById " + id + " proxy:" + proxy);
        return proxy;
    }

    public void downloadFile(HttpServletResponse response, String id) throws IOException {
        log.debug("received request to download proxy with id: " + id);

        Proxy proxy = proxyRepository.findById(id).orElse(null);
        if(proxy != null) {
            File proxyDownloadFile = customProxyFileGenerator.getCustomJarForDownload(proxy);
            log.debug("providing file as download: " + proxyDownloadFile);

            response.setContentType(PROXY_DOWNLOAD_MIMETYPE);
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", proxyDownloadFile.getName()));
            response.setContentLength((int) proxyDownloadFile.length());

            InputStream inputStream = new BufferedInputStream(Files.newInputStream(proxyDownloadFile.toPath()));

            FileCopyUtils.copy(inputStream, response.getOutputStream());

            // Maybe we should consider the idea to build the URL and redirect
            inputStream.close();
            proxyDownloadFile.delete();
        }
    }

    public Iterable<Proxy> deleteRest(String id) throws IOException {
        Proxy proxy = proxyRepository.findById(id).orElse(null);
        if(proxy != null) {
            proxy.getServiceApis().clear();
            proxy.setDeletedWhen(new Date());
            proxyRepository.save(proxy);
            gatewayConfigFilesService.deleteGatewayConfigFile(proxy);
        }
        return proxyListPageRest();
    }
    private ModelAndView updateFormWithErrorMessage(Proxy proxyStored, String errorMessageKey) {
        return updateFormWithErrorMessage(proxyStored, errorMessageKey, null);
    }

    private ModelAndView updateFormWithErrorMessage(Proxy proxyStored, String errorMessageKey, String errorMessageText) {
        ModelAndView updateForm = updateProxyForm(proxyStored.getId());
        updateForm.addObject("errorMessageKey", errorMessageKey);

        if (errorMessageText != null && !errorMessageText.isEmpty())
            updateForm.addObject("errorMessageText", "Error Details: " + errorMessageText);

        return updateForm;
    }

    public void transmitConfigChangesToGitConfigRepository(Proxy updatedProxy) throws IOException {
        log.debug("deleting old proxy config from git repository for proxy {}", updatedProxy.getId());
        gatewayConfigFilesService.deleteGatewayConfigFile(updatedProxy);
        log.debug("writing new proxy config into git repository {}", updatedProxy);
        gatewayConfigFilesService.addGatewayConfigFile(updatedProxy);
    }

    public void informProxyAboutUpdatedConfiguration(Proxy updatedProxy) throws URISyntaxException {

        String refreshUrl = updatedProxy.getPublicUrl().trim();
        refreshUrl = refreshUrl.endsWith("/") ? refreshUrl + "refresh" : refreshUrl + "/refresh";
        refreshUrl = refreshUrl + "?" + ApiKey.API_KEY_REQUEST_PARAMETER_NAME + "=" + ApiKey.API_KEY_FOR_YGG_ADMIN_TO_ACCESS_PROXIES;

        log.debug("inform proxy {} about updated config - calling refresh at URL: {}", updatedProxy.getId(), refreshUrl);
        Object responseOfRefreshCall = restTemplate.postForObject(new URI(refreshUrl), null, Object.class);
        log.info("refresh call to proxy returned: {}", responseOfRefreshCall);
    }

}
