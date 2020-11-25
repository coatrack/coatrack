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

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import eu.coatrack.admin.logic.CreateApiKeyAction;
import eu.coatrack.admin.model.repository.ApiKeyRepository;
import eu.coatrack.admin.model.repository.ProxyRepository;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.admin.service.GithubService;
import eu.coatrack.api.ApiKey;
import eu.coatrack.api.Proxy;
import eu.coatrack.api.ServiceApi;
import eu.coatrack.api.User;
import eu.coatrack.config.github.GithubUserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMethod;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author gr-hovest@atb-bremen.de silva@atb-bremen.de
 */
@Controller
@RequestMapping("/admin/api-keys")
public class AdminApiKeysController {

    private static final Logger log = LoggerFactory.getLogger(AdminApiKeysController.class);

    private static final String ADMIN_API_KEY_LIST_VIEW = "admin/api-keys/list";
    private static final String ADMIN_API_KEY_EDITOR = "admin/api-keys/edit";
    private static final String ADMIN_API_KEY_VIEW = "admin/api-keys/api-key";
    private static final String ADMIN_API_KEY_LIST_VIEW_FOR_CONSUMER = "admin/api-keys/consumer/list";

    private static final int NEW_MODE = 0;
    private static final int UPDATE_MODE = 1;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceApiRepository serviceApiRepository;

    @Autowired
    private GithubService githubService;

    @Autowired
    private ProxyRepository proxyRepository;

    @Autowired
    private CreateApiKeyAction createApiKeyAction;

    @RequestMapping(value = "", method = GET)
    public ModelAndView showApiKeyListbyLoggedInServiceApiOwner() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Iterable<ApiKey> apiKeys = apiKeyRepository.findAvailable();

        ModelAndView mav = new ModelAndView();
        mav.addObject("apiKeys", apiKeys);
        mav.setViewName(ADMIN_API_KEY_LIST_VIEW);

        return mav;
    }

    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Iterable<ApiKey> apiKeyListPageRest() throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("apiKeyListPageRest:" + apiKeyRepository.findAvailable());
        Iterable<ApiKey> apiKeys = apiKeyRepository.findAvailable();
        return apiKeys;
    }

    @RequestMapping(value = "/formAdd", method = GET)
    public ModelAndView formAddNewApiKey() {
        log.debug("New Form");

        Iterable<ServiceApi> services = serviceApiRepository.findByDeletedWhen(null);
        Iterable<User> users = userRepository.findAll();

        ApiKey apiKey = new ApiKey();
        ServiceApi service = new ServiceApi();
        apiKey.setServiceApi(service);
        User user = new User();
        apiKey.setUser(user);

        ModelAndView mav = new ModelAndView();
        mav.addObject("apiKey", apiKey);
        mav.addObject("mode", NEW_MODE);
        mav.addObject("services", services);
        mav.addObject("users", users);
        mav.setViewName(ADMIN_API_KEY_EDITOR);

        return mav;
    }

    @RequestMapping(value = "{id}/formUpdate", method = GET)
    public ModelAndView formUpdateNewApiKey(@PathVariable("id") long id
    ) {
        log.debug("Update Form");

        Iterable<ServiceApi> services = serviceApiRepository.findByDeletedWhen(null);
        Iterable<User> users = userRepository.findAll();

        ModelAndView mav = new ModelAndView();
        mav.addObject("apiKey", apiKeyRepository.findOne(id));
        mav.addObject("mode", UPDATE_MODE);
        mav.addObject("services", services);
        mav.addObject("users", users);
        mav.setViewName(ADMIN_API_KEY_EDITOR);

        return mav;
    }

    @RequestMapping(value = "/add", method = POST)
    public ModelAndView addApiKey(
            @RequestParam(required = false) Long selectedServiceId,
            @RequestParam(required = false) String selectedUserId
    ) throws IOException {
        log.debug("POST call to api key/add:");

        User user = userRepository.findByUsername(selectedUserId);

        if (user == null) {
            List<GithubUserProfile> githubUser = githubService.findGithubUserProfileByUsername(selectedUserId);
            if (githubUser != null && !githubUser.isEmpty()) {
                
                // Create User
                user = new User();
                user.setUsername(githubUser.get(0).getLogin());
                user.setFirstname(githubUser.get(0).getName());
                user.setEmail(githubUser.get(0).getEmail());
                userRepository.save(user);
            }

        }

        ServiceApi selectedService = serviceApiRepository.findOne(selectedServiceId);

        createApiKeyAction.setServiceApi(selectedService);
        createApiKeyAction.setUser(user);
        createApiKeyAction.execute();

        ModelAndView mav = new ModelAndView();
        mav.setViewName(ADMIN_API_KEY_EDITOR);

        return showApiKeyListbyLoggedInServiceApiOwner();
    }

    @RequestMapping(value = "/update", method = POST)
    public ModelAndView postApiKey(@ModelAttribute ApiKey apiKey,
            @RequestParam(required = false) String selectedServiceId
    ) {
        log.debug("Update Api Key:" + apiKey.toString());
        ServiceApi selectedService = serviceApiRepository.findOne(Long.parseLong(selectedServiceId));

        ApiKey apiKeyStored = apiKeyRepository.findOne(apiKey.getId());

        apiKeyStored.setServiceApi(selectedService);

        apiKeyRepository.save(apiKeyStored);

        ModelAndView mav = new ModelAndView();
        mav.setViewName(ADMIN_API_KEY_EDITOR);

        return showApiKeyListbyLoggedInServiceApiOwner();
    }

    String[] parsePatterns = {"dd/MM/yyyy", "dd-MMM-yyyy", "dd.MM.yyyy", "dd/mm/yy"};

    @RequestMapping(value = "{id}/extendValidity", method = POST, produces = "application/json")
    @ResponseBody
    public ApiKey extendValidity(@PathVariable("id") long id,
            @RequestParam(value = "nextValidDate", required = false) String nextValidDate) throws ParseException {
        log.debug("Update Api Key:" + id + " with extendValidity until " + nextValidDate);

        ApiKey apiKeyStored = apiKeyRepository.findOne(id);

        apiKeyStored.setValidUntil(DateUtils.parseDate(nextValidDate, parsePatterns));

        apiKeyRepository.save(apiKeyStored);

        return apiKeyRepository.findOne(id);
    }

    @RequestMapping(value = "{id}", method = GET)
    public String getById(@PathVariable("id") long id, Model model) throws MalformedURLException, IOException {

        log.info("getById " + id + " api key/add:" + apiKeyRepository.findOne(id));
        model.addAttribute("apiKey", apiKeyRepository.findOne(id));
        return ADMIN_API_KEY_VIEW;
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ApiKey getByIdRest(@PathVariable("id") long id) throws IOException {
        log.info("getById " + id + " api key/add:" + apiKeyRepository.findOne(id));
        return apiKeyRepository.findOne(id);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseBody
    public Iterable<ApiKey> deleteRest(@PathVariable("id") long id) throws IOException {

        ApiKey apiKey = apiKeyRepository.findOne(id);
        apiKey.setDeletedWhen(new Date());
        apiKeyRepository.save(apiKey);

        return apiKeyListPageRest();
    }

    @RequestMapping(value = "/consumer/list", method = GET)
    public ModelAndView showApiKeyListForLoggedInApiConsumer() {

        // Get the ApiKeys for the services consumed by the Logged in User
        List <ApiKey> apiKeys = apiKeyRepository.findByLoggedInAPIConsumer();

        // Map the APiKeys to the specific Proxies URLs
        Map<String, List <String>> proxyURLsPerApiKey = new TreeMap<>();
        for (ApiKey apiKey : apiKeys) {
            String apiKeyString = apiKey.getKeyValue();
            List<String> proxiesUrlList = proxyRepository.customSearchForAllProxiesForGivenServiceApiId(apiKey.getServiceApi().getId())
                    .stream()
                    .filter(proxy -> proxy.getPublicUrl() != null)
                    .filter(proxy -> proxy.getPublicUrl() != "")
                    .map(Proxy::getPublicUrl)
                    .collect(Collectors.toList());

            proxyURLsPerApiKey.putIfAbsent(apiKeyString, proxiesUrlList);
        }

        ModelAndView mav = new ModelAndView();
        mav.addObject("apiKeys", apiKeys);
        mav.addObject("proxiesPerApiKey", proxyURLsPerApiKey);
        mav.setViewName(ADMIN_API_KEY_LIST_VIEW_FOR_CONSUMER);

        return mav;
    }
}
