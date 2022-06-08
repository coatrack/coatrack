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

import be.ceau.chart.DoughnutChart;
import be.ceau.chart.color.Color;
import be.ceau.chart.data.DoughnutData;
import be.ceau.chart.dataset.DoughnutDataset;
import eu.coatrack.admin.UserSessionSettings;
import eu.coatrack.admin.logic.CreateApiKeyAction;
import eu.coatrack.admin.logic.CreateServiceAction;
import eu.coatrack.admin.logic.UpdateServiceAction;
import eu.coatrack.admin.model.repository.*;
import eu.coatrack.admin.model.vo.MetricsAggregation;
import eu.coatrack.admin.service.AdminProxiesService;
import eu.coatrack.admin.service.AdminServicesService;
import eu.coatrack.api.*;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * @author gr-hovest(at)atb-bremen.de
 */
@Slf4j
@Controller
@RequestMapping(value = "/admin/services")
public class AdminServicesController {

    @Autowired
    AdminServicesService adminServicesService;

    
    @RequestMapping(value = "", method = GET)
    public ModelAndView serviceListPage() {
        return adminServicesService.serviceListPage();
    }

    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Iterable<ServiceApi> serviceListPageRest() {
        return adminServicesService.serviceListPageRest();
    }

    @GetMapping(value = "/formAdd")
    public ModelAndView newServiceForm() {
        return adminServicesService.newServiceForm();
    }
    
    @GetMapping(value = "{id}/servicecover")
    public ModelAndView updateServiceCoverForm(@PathVariable("id") long id) {
        return adminServicesService.updateServiceCoverForm(id);
    }

    @GetMapping(value = "{id}/formUpdate")
    public ModelAndView updateServiceForm(@PathVariable("id") long id) {
        return adminServicesService.updateServiceForm(id);
    }

    @PostMapping(value = "/add")
    public ModelAndView newServiceSubmit(@RequestBody ServiceApi serviceApi) {
        return adminServicesService.newServiceSubmit(serviceApi);
    }

    @PostMapping(value = "/update")
    public ModelAndView updateService(@RequestBody ServiceApi service) {
        return adminServicesService.tryUpdateService(service);
    }


    @RequestMapping(value = "{id}", method = GET)
    public String get(@PathVariable("id") long id, Model model,
                      @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodStart,
                      @RequestParam(value = "dateUntil", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodEnd) {

        return adminServicesService.get(id, model, selectedTimePeriodStart, selectedTimePeriodEnd);
    }

    @RequestMapping(value = "{id}/statisticsPerConsumer", method = GET, produces = "application/json")
    @ResponseBody
    public Map<String, Map<String, Map<String, Map<String, Long>>>> get(
            @PathVariable("id") long id,
            @RequestParam("dateFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodStart,
            @RequestParam("dateUntil") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodEnd) {

        return adminServicesService.get(id, selectedTimePeriodStart, selectedTimePeriodEnd);
    }


    @RequestMapping(value = "{id}/usageStatisticsDoughnutCharts", method = GET, produces = "application/json")
    @ResponseBody
    public Map<String, DoughnutChart> generateUsageStatisticsDoughnutCharts(@PathVariable("id") long id) {
        return adminServicesService.generateUsageStatisticsDoughnutCharts(id);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ServiceApi getByIdRest(@PathVariable("id") long id) {
        return adminServicesService.getByIdRest(id);
    }

    @RequestMapping(value = "{id}/proxies", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Iterable<Proxy> getProxiesByServiceRest(@PathVariable("id") long id) {
        return adminServicesService.getProxiesByServiceRest(id);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseBody
    public Iterable<ServiceApi> deleteRest(@PathVariable("id") long id) {
        return adminServicesService.deleteRest(id);
    }

    @RequestMapping(value = "/consumer/list", method = GET)
    public ModelAndView showToLoggedInConsumerTheListOfPublicServicesByOtherProviders() {
        return adminServicesService.showToLoggedInConsumerTheListOfPublicServicesByOtherProviders();
    }

    @RequestMapping(value = "consumer/subscribe", method = POST)
    @ResponseBody
    public ModelAndView createOwnApiKey(@RequestParam Long selectedServiceId) {
        return adminServicesService.createOwnApiKey(selectedServiceId);
    }

}
