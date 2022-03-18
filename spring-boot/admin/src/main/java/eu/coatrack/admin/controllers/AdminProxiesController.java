package eu.coatrack.admin.controllers;

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

import eu.coatrack.admin.model.repository.MetricsAggregationCustomRepository;
import eu.coatrack.admin.model.repository.ProxyRepository;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.service.GatewayConfigFilesStorage;
import eu.coatrack.api.ApiKey;
import eu.coatrack.api.Proxy;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import eu.coatrack.admin.logic.CreateProxyAction;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.admin.service.CustomProxyFileGeneratorService;
import eu.coatrack.api.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * @author Timon Veenstra <tveenstra@bebr.nl>
 */
@Controller
@RequestMapping(value = "/admin/proxies")
public class AdminProxiesController {

    private static final Logger log = LoggerFactory.getLogger(AdminProxiesController.class);

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
    private GatewayConfigFilesStorage gatewayConfigFilesStorage;

    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping(value = "", method = GET)
    public ModelAndView proxyListPage() {
        ModelAndView mav = new ModelAndView();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        mav.addObject("proxies", proxyRepository.findAvailable());
        mav.setViewName(ADMIN_PROXIES_LIST_VIEW);
        return mav;
    }

    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Iterable<Proxy> proxyListPageRest() throws IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("proxyListPageRest:" + proxyRepository.findAvailable());
        return proxyRepository.findAvailable();
    }

    @GetMapping(value = "/formAdd")
    public ModelAndView newProxyForm() {
        log.debug("New Form");

        ModelAndView mav = new ModelAndView();

        Proxy p = new Proxy();

        mav.addObject("proxy", p);
        mav.addObject("services", serviceApiRepository.findByDeletedWhen(null));
        mav.addObject("mode", 0);

        mav.setViewName(ADMIN_PROXY_EDITOR);
        return mav;
    }

    @GetMapping(value = "{id}/formUpdate")
    public ModelAndView updateProxyForm(@PathVariable("id") String id) {
        log.debug("Update Form");

        ModelAndView mav = new ModelAndView();

        Proxy proxy = proxyRepository.findById(id).orElse(null);

        mav.addObject("proxy", proxy);

        mav.addObject("services", serviceApiRepository.findByDeletedWhen(null));
        mav.addObject("mode", 1);

        mav.setViewName(ADMIN_PROXY_EDITOR);
        return mav;
    }

    @PostMapping(value = "/add")
    public ModelAndView addProxy(@ModelAttribute Proxy proxy,
            @RequestParam(required = false) List<Long> selectedServices) throws IOException, GitAPIException, URISyntaxException {
        log.debug("POST call to proxy/add: " + proxy.toString());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());

        proxy.setId(UUID.randomUUID().toString());

        createProxyAction.setProxy(proxy);
        createProxyAction.setUser(user);
        createProxyAction.setSelectedServices(selectedServices);
        createProxyAction.execute();

        gatewayConfigFilesStorage.addGatewayConfigFile(proxy);

        return proxyListPage();
    }

    @PostMapping(value = "/update")
    public ModelAndView updateProxy(@ModelAttribute Proxy proxy,
            @RequestParam(required = false) List<String> selectedServices) throws IOException, GitAPIException, URISyntaxException, Exception {
        log.debug("Update proxy: " + proxy.toString());

        Proxy proxyStored = proxyRepository.findById(proxy.getId()).orElse(null);
        proxyStored.setDescription(proxy.getDescription());
        proxyStored.setName(proxy.getName());
        proxyStored.setPublicUrl(proxy.getPublicUrl());
        proxyStored.setPort(proxy.getPort());

        proxyStored.setServiceApis(new HashSet<>());

        if (selectedServices != null) {
            selectedServices.forEach(s -> log.debug("service-id:" + s));
            selectedServices.stream()
                    .map(idString -> new Long(idString))
                    .map(id -> serviceApiRepository.findById(id).orElse(null))
                    .forEach(service -> proxyStored.getServiceApis().add(service));
        }
        proxyRepository.save(proxyStored);

        // transmit config changes to config server git repo
        try {
            transmitConfigChangesToGitConfigRepository(proxyStored);
        } catch (IOException | URISyntaxException | GitAPIException e) {
            log.error("Error when trying to transmit config to git repository: ", e);
            return updateFormWithErrorMessage(proxyStored, "updateProxyGitError");
        }

        // call proxy so that it will get the latest config from git (by calling the config server)
        try {
            informProxyAboutUpdatedConfiguration(proxyStored);
        } catch (Exception e) {
            log.error("Error when trying to call 'refresh' on the proxy: ", e);
            String errorMessageForGUI = String.format(
                    "Refresh on '%s' (public url: '%s') failed: %s; ",
                    proxyStored.getName(), proxyStored.getPublicUrl(), e);
            return updateFormWithErrorMessage(proxyStored, "updateProxyRefreshError", errorMessageForGUI);
        }

        return proxyListPage();
    }

    private ModelAndView updateFormWithErrorMessage(Proxy proxyStored, String errorMessageKey) {
        return updateFormWithErrorMessage(proxyStored, errorMessageKey, null);
    }

    private ModelAndView updateFormWithErrorMessage(Proxy proxyStored, String errorMessageKey, String errorMessageText) {
        ModelAndView updateForm = updateProxyForm(proxyStored.getId());
        updateForm.addObject("errorMessageKey", errorMessageKey);

        if (errorMessageText != null && !errorMessageText.isEmpty()) {
            updateForm.addObject("errorMessageText", "Error Details: " + errorMessageText);
        }
        return updateForm;
    }

    @RequestMapping(value = "{id}", method = GET)
    public String get(@PathVariable("id") String id, Model model) throws MalformedURLException, IOException {
        Proxy proxy = proxyRepository.findById(id).orElse(null);
        model.addAttribute("proxy", proxy);
        model.addAttribute("metrics", metricsAggregationRepository.getSummarizedMetricsByProxyId(proxy.getId()));
        return ADMIN_PROXY_VIEW;
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Proxy getByIdRest(@PathVariable("id") String id) throws IOException {
        Proxy proxy = proxyRepository.findById(id).orElse(null);
        log.info("getById " + id + " proxy:" + proxy);
        return proxy;
    }

    @RequestMapping(value = "{id}/download", method = RequestMethod.GET)
    public void downloadFile(HttpServletResponse response, @PathVariable("id") String id) throws IOException {
        log.debug("received request to download proxy with id " + id);

        Proxy proxy = proxyRepository.findById(id).orElse(null);
        File proxyDownloadFile = customProxyFileGenerator.getCustomJarForDownload(proxy);

        log.debug("providing file as download: " + proxyDownloadFile);

        response.setContentType(PROXY_DOWNLOAD_MIMETYPE);
        response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", proxyDownloadFile.getName()));
        response.setContentLength((int) proxyDownloadFile.length());

        InputStream inputStream = new BufferedInputStream(new FileInputStream(proxyDownloadFile));

        FileCopyUtils.copy(inputStream, response.getOutputStream());

        // Maybe we should consider the idea to build the URL and redirect
        inputStream.close();
        proxyDownloadFile.delete();
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseBody
    public Iterable<Proxy> deleteRest(@PathVariable("id") String id) throws IOException {
        Proxy proxy = proxyRepository.findById(id).orElse(null);
        proxy.getServiceApis().clear();
        proxy.setDeletedWhen(new Date());
        proxyRepository.save(proxy);
        gatewayConfigFilesStorage.deleteGatewayConfigFile(proxy);
        return proxyListPageRest();
    }

    public void transmitConfigChangesToGitConfigRepository(Proxy updatedProxy) throws IOException, GitAPIException, URISyntaxException {
        log.debug("deleting old proxy config from git repository for proxy {}", updatedProxy.getId());
        gatewayConfigFilesStorage.deleteGatewayConfigFile(updatedProxy);
        log.debug("writing new proxy config into git repository {}", updatedProxy);
        gatewayConfigFilesStorage.addGatewayConfigFile(updatedProxy);
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
