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

import eu.coatrack.admin.model.repository.ApiKeyRepository;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.admin.service.ReportService;
import eu.coatrack.api.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import java.util.List;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Slf4j
@RestController
@RequestMapping(path = "/admin/reports")
public class ReportController {

    private static final String REPORT_VIEW = "admin/reports/report";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceApiRepository serviceApiRepository;

    @Autowired ApiKeyRepository apiKeyRepository;

    @Autowired
    private ReportService reportService;

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ModelAndView report() {
        return report(null, null, -1L, -1L, false);
    }

    @RequestMapping(value = "/{dateFrom}/{dateUntil}/{selectedServiceId}/{selectedApiConsumerUserId}/{isOnlyPaidCalls}", method = RequestMethod.GET)
    public ModelAndView report(
            @PathVariable("dateFrom") String dateFrom,
            @PathVariable("dateUntil") String dateUntil,
            @PathVariable("selectedServiceId") Long selectedServiceId,
            @PathVariable("selectedApiConsumerUserId") Long selectedApiConsumerUserId,
            @PathVariable("isOnlyPaidCalls") boolean isOnlyPaidCalls
    ) {
        ModelAndView result = new ModelAndView();
        result.setViewName(REPORT_VIEW);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {

            User currentUser = userRepository.findByUsername(auth.getName());
            List<ServiceApi> servicesProvided = serviceApiRepository.findByDeletedWhen(null);
            Report report = reportService.getReport(dateFrom, dateUntil, selectedServiceId, selectedApiConsumerUserId, isOnlyPaidCalls);
            List<User> totalConsumers = reportService.getServiceConsumers(servicesProvided);
            List<String> idsPayedPerCall = reportService.getPayPerCallServicesIds(servicesProvided);

            // generell data
            result.addObject("users", totalConsumers);
            result.addObject("selectedServiceId", selectedServiceId);
            result.addObject("selectedApiConsumerUserId", selectedApiConsumerUserId);
            result.addObject("services", servicesProvided);
            result.addObject("payPerCallServicesIds", idsPayedPerCall);
            result.addObject("exportUser", currentUser);

            // data regarding report
            result.addObject("isOnlyPaidCalls", report.isOnlyPaidCalls()); // TODO delete
            result.addObject("isReportForConsumer", report.isForConsumer()); // TODO delete
            result.addObject("dateFrom", report.getFrom()); // TODO delete
            result.addObject("dateUntil", report.getUntil()); // TODO delete
            result.addObject("serviceApiSelectedForReport", report.getSelectedService()); // TODO delete
            result.addObject("consumerUserSelectedForReport", report.getSelectedConsumer()); // TODO delete
        } else {
            result.addObject("error", "Request is not authenticated! Please log in.");
        }

        return result;
    }

    @RequestMapping(value = "/apiUsage/{dateFrom}/{dateUntil}/{selectedServiceId}/{apiConsumerId}/{onlyPaidCalls}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public DataTableView<ApiUsageReport> reportApiUsage(
            @PathVariable("dateFrom") String dateFrom,
            @PathVariable("dateUntil") String dateUntil,
            @PathVariable("selectedServiceId") Long selectedServiceId,
            @PathVariable("apiConsumerId") Long apiConsumerId,
            @PathVariable("onlyPaidCalls") boolean onlyPaidCalls
    ) {
        return reportService.reportApiUsage(dateFrom, dateUntil, selectedServiceId, apiConsumerId, onlyPaidCalls);
    }


    @RequestMapping(value = "/consumer", method = GET)
    public ModelAndView showGenerateReportPageForServiceConsumer() {
        return searchReportsByServicesConsumed(null, null, -1L, false);
    }

    // deleted @PathVariable("selectedApiConsumerUserId") Long selectedApiConsumerUserId, because it is not used
    @RequestMapping(value = "/consumer/{dateFrom}/{dateUntil}/{selectedServiceId}/{selectedApiConsumerUserId}/{isOnlyPaidCalls}", method = RequestMethod.GET)
    public ModelAndView searchReportsByServicesConsumed(
            @PathVariable("dateFrom") String dateFrom,
            @PathVariable("dateUntil") String dateUntil,
            @PathVariable("selectedServiceId") Long selectedServiceId,
            @PathVariable("isOnlyPaidCalls") boolean isOnlyPaidCalls
    ) {



        Report report = reportService.getReport(dateFrom, dateUntil, selectedServiceId, -1L, isOnlyPaidCalls);

        ModelAndView result = new ModelAndView(REPORT_VIEW);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if(auth != null) {
            User currentUser = userRepository.findByUsername(auth.getName());
            List<ServiceApi> servicesFromUser = serviceApiRepository.findByApiKeyList(apiKeyRepository.findByLoggedInAPIConsumer());
            List<String> payPerCallServicesIds = reportService.getPayPerCallServicesIds(servicesFromUser);
            List<User> totalConsumers = reportService.getServiceConsumers(servicesFromUser);

            result.addObject("selectedServiceId", selectedServiceId);
            result.addObject("selectedApiConsumerUserId", currentUser.getId());
            result.addObject("services", servicesFromUser);
            result.addObject("payPerCallServicesIds", payPerCallServicesIds);
            result.addObject("exportUser", currentUser);
            result.addObject("users", totalConsumers);

            result.addObject("isOnlyPaidCalls", report.isOnlyPaidCalls());
            result.addObject("isReportForConsumer", report.isForConsumer());
            result.getModel().put("dateFrom", report.getFrom());
            result.getModel().put("dateUntil", report.getUntil());
            result.addObject("serviceApiSelectedForReport", report.getSelectedService());
        } else {
            result.addObject("error", "Request is not authenticated! Please log in.");
        }
        return result;
    }
}
