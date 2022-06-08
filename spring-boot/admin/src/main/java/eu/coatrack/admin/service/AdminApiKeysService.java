package eu.coatrack.admin.service;

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

import eu.coatrack.admin.logic.CreateApiKeyAction;
import eu.coatrack.admin.model.repository.ApiKeyRepository;
import eu.coatrack.admin.model.repository.ProxyRepository;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.api.ApiKey;
import eu.coatrack.api.Proxy;
import eu.coatrack.api.ServiceApi;
import eu.coatrack.api.User;
import eu.coatrack.config.github.GithubUserProfile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminApiKeysService {
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


    public ModelAndView showApiKeyListbyLoggedInServiceApiOwner() {
        Iterable<ApiKey> apiKeys = apiKeyRepository.findAvailable();

        ModelAndView mav = new ModelAndView();
        mav.addObject("apiKeys", apiKeys);
        mav.setViewName(ADMIN_API_KEY_LIST_VIEW);

        return mav;
    }

    public Iterable<ApiKey> apiKeyListPageRest() {
        log.info("apiKeyListPageRest:" + apiKeyRepository.findAvailable());
        return apiKeyRepository.findAvailable();
    }

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

    public ModelAndView formUpdateNewApiKey(long id) {
        log.debug("Update Form");

        Iterable<ServiceApi> services = serviceApiRepository.findByDeletedWhen(null);
        Iterable<User> users = userRepository.findAll();

        ModelAndView mav = new ModelAndView();
        mav.addObject("apiKey", apiKeyRepository.findById(id).orElse(null));
        mav.addObject("mode", UPDATE_MODE);
        mav.addObject("services", services);
        mav.addObject("users", users);
        mav.setViewName(ADMIN_API_KEY_EDITOR);

        return mav;
    }

    public ModelAndView addApiKey(Long selectedServiceId, String selectedUserId) throws IOException {
        log.debug("POST call to api key/add:");
        ModelAndView mav = new ModelAndView();
        ServiceApi selectedService;
        User user = userRepository.findByUsername(selectedUserId);

        if (user == null)
            user = createUserFromGithubById(selectedUserId);
        if(user != null)  {
            selectedService = serviceApiRepository.findById(selectedServiceId).orElse(null);
            if (selectedService != null) {
                createApiKeyAction.setServiceApi(selectedService);
                createApiKeyAction.setUser(user);
                createApiKeyAction.execute();
            } else {
                mav.addObject("error", "Service could not be found!");
            }
        } else
            mav.addObject("error", "user could not be found or created via github");

        mav.setViewName(ADMIN_API_KEY_EDITOR);
        return showApiKeyListbyLoggedInServiceApiOwner();
    }

    private User createUserFromGithubById(String userId) throws IOException {
        List<GithubUserProfile> githubUserList = githubService.findGithubUserProfileByUsername(userId);
        User user = null;
        if (githubUserList != null && !githubUserList.isEmpty()) {
            user = new User();
            user.setUsername(githubUserList.get(0).getLogin());
            user.setFirstname(githubUserList.get(0).getName());
            user.setEmail(githubUserList.get(0).getEmail());
            userRepository.save(user);
        }
        return user;
    }

    public ModelAndView postApiKey(ApiKey apiKey, String selectedServiceId) {
        log.debug("Update Api Key:" + apiKey.toString());
        ModelAndView mav = new ModelAndView();
        ServiceApi selectedService = serviceApiRepository.findById(Long.parseLong(selectedServiceId)).orElse(null);

        ApiKey apiKeyStored = apiKeyRepository.findById(apiKey.getId()).orElse(null);
        if (apiKeyStored != null && selectedService != null) {
            apiKeyStored.setServiceApi(selectedService);
            apiKeyRepository.save(apiKeyStored);
        } else {
            mav.addObject("error", "ServiceAPI or APIKey could not be found!");
        }
        mav.setViewName(ADMIN_API_KEY_EDITOR);
        return showApiKeyListbyLoggedInServiceApiOwner();
    }


    public ApiKey extendValidity(long id, String nextValidDate) throws ParseException {
        String[] parsePatterns = {"dd/MM/yyyy", "dd-MMM-yyyy", "dd.MM.yyyy", "dd/mm/yy"};
        log.debug("Update Api Key:" + id + " with extendValidity until " + nextValidDate);

        ApiKey apiKeyStored = apiKeyRepository.findById(id).orElse(null);

        if (apiKeyStored != null) {
            apiKeyStored.setValidUntil(DateUtils.parseDate(nextValidDate, parsePatterns));
            apiKeyRepository.save(apiKeyStored);
        }

        return apiKeyRepository.findById(id).orElse(null);
    }

    public String getById(long id, Model model) throws IOException {
        log.info("getById " + id + " api key/add:" + apiKeyRepository.findById(id).orElse(null));
        model.addAttribute("apiKey", apiKeyRepository.findById(id).orElse(null));
        return ADMIN_API_KEY_VIEW;
    }

    public ApiKey getByIdRest(long id) {
        log.info("getById " + id + " api key/add:" + apiKeyRepository.findById(id).orElse(null));
        return apiKeyRepository.findById(id).orElse(null);
    }

    public Iterable<ApiKey> deleteRest(long id) {
        ApiKey apiKey = apiKeyRepository.findById(id).orElse(null);
        if (apiKey != null) {
            apiKey.setDeletedWhen(new Date());
            apiKeyRepository.save(apiKey);
        }
        return apiKeyListPageRest();
    }

    public ModelAndView showApiKeyListForLoggedInApiConsumer() {

        // Get the ApiKeys for the services consumed by the Logged in User
        List<ApiKey> apiKeys = apiKeyRepository.findByLoggedInAPIConsumer();

        // Map the APiKeys to the specific Proxies URLs
        Map<String, List<String>> proxyURLsPerApiKey = new TreeMap<>();
        for (ApiKey apiKey : apiKeys) {
            String apiKeyString = apiKey.getKeyValue();
            List<String> proxiesUrlList = proxyRepository.customSearchForAllProxiesForGivenServiceApiId(apiKey.getServiceApi().getId())
                    .stream()
                    .map(Proxy::getPublicUrl)
                    .filter(Objects::nonNull)
                    .filter(publicUrl -> publicUrl.equals(""))
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
