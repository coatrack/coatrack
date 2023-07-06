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

import eu.coatrack.admin.service.AdminProxiesService;
import eu.coatrack.api.Proxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Date;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * @author Timon Veenstra <tveenstra@bebr.nl>
 */
@Controller
@RequestMapping(value = "/admin/proxies")
public class AdminProxiesController {

    @Autowired
    private AdminProxiesService adminProxiesService;

    @RequestMapping(value = "", method = GET)
    public ModelAndView proxyListPage() {
        return adminProxiesService.proxyListPage();
    }

    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Iterable<Proxy> proxyListPageRest() {
        return adminProxiesService.proxyListPageRest();
    }

    @GetMapping(value = "/formAdd")
    public ModelAndView newProxyForm() {
        return adminProxiesService.newProxyForm();
    }

    @GetMapping(value = "{id}/formUpdate")
    public ModelAndView updateProxyForm(@PathVariable("id") String id) {
        return adminProxiesService.updateProxyForm(id);
    }

    @PostMapping(value = "/add")
    public ModelAndView addProxy(@ModelAttribute Proxy proxy, @RequestParam(required = false) List<Long> selectedServices) throws IOException {
        return adminProxiesService.addProxy(proxy, selectedServices);
    }

    @PostMapping(value = "/update")
    public ModelAndView updateProxy(@ModelAttribute Proxy proxy, @RequestParam(required = false) List<String> selectedServices) {
        return adminProxiesService.tryUpdateProxy(proxy, selectedServices);
    }

    @RequestMapping(value = "{id}", method = GET)
    public String getProxyById(@PathVariable("id") String id, Model model) {
        return adminProxiesService.getProxyById(id, model);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Proxy getProxyByIdRest(@PathVariable("id") String id) {
        return adminProxiesService.getProxyByIdRest(id);
    }

    @RequestMapping(value = "{id}/download", method = RequestMethod.GET)
    public void downloadFile(HttpServletResponse response, @PathVariable("id") String id) throws IOException {
        adminProxiesService.downloadFile(response, id);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseBody
    public Iterable<Proxy> deleteRest(@PathVariable("id") String id) throws IOException {
        return adminProxiesService.deleteRest(id);
    }


}
