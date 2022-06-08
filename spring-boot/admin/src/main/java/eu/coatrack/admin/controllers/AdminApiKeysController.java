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
import java.text.ParseException;

import eu.coatrack.admin.service.AdminApiKeysService;
import eu.coatrack.api.ApiKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

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

@Slf4j
@Controller
@RequestMapping("/admin/api-keys")
public class AdminApiKeysController {

    @Autowired
    private AdminApiKeysService adminApiKeysService;

    @RequestMapping(value = "", method = GET)
    public ModelAndView showApiKeyListbyLoggedInServiceApiOwner() {
        return adminApiKeysService.showApiKeyListbyLoggedInServiceApiOwner();
    }

    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Iterable<ApiKey> apiKeyListPageRest() {
        return adminApiKeysService.apiKeyListPageRest();
    }

    @RequestMapping(value = "/formAdd", method = GET)
    public ModelAndView formAddNewApiKey() {
        return adminApiKeysService.formAddNewApiKey();
    }

    @RequestMapping(value = "{id}/formUpdate", method = GET)
    public ModelAndView formUpdateNewApiKey(@PathVariable("id") long id) {
        return adminApiKeysService.formUpdateNewApiKey(id);
    }

    @RequestMapping(value = "/add", method = POST)
    public ModelAndView addApiKey(@RequestParam(required = false) Long selectedServiceId,
                                  @RequestParam(required = false) String selectedUserId) throws IOException {
        return adminApiKeysService.addApiKey(selectedServiceId, selectedUserId);
    }

    @RequestMapping(value = "/update", method = POST)
    public ModelAndView postApiKey(@ModelAttribute ApiKey apiKey,
                                   @RequestParam(required = false) String selectedServiceId) {
        return adminApiKeysService.postApiKey(apiKey, selectedServiceId);
    }


    @RequestMapping(value = "{id}/extendValidity", method = POST, produces = "application/json")
    @ResponseBody
    public ApiKey extendValidity(@PathVariable("id") long id,
                                 @RequestParam(value = "nextValidDate", required = false) String nextValidDate) throws ParseException {
        return adminApiKeysService.extendValidity(id, nextValidDate);
    }

    @RequestMapping(value = "{id}", method = GET)
    public String getById(@PathVariable("id") long id, Model model) throws IOException {
        return adminApiKeysService.getById(id, model);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ApiKey getByIdRest(@PathVariable("id") long id) {
        return adminApiKeysService.getByIdRest(id);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseBody
    public Iterable<ApiKey> deleteRest(@PathVariable("id") long id) {
        return adminApiKeysService.deleteRest(id);
    }

    @RequestMapping(value = "/consumer/list", method = GET)
    public ModelAndView showApiKeyListForLoggedInApiConsumer() {
        return adminApiKeysService.showApiKeyListForLoggedInApiConsumer();
    }
}
