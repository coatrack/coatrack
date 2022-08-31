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
import eu.coatrack.admin.service.report.ApiUsageDTO;
import eu.coatrack.admin.service.report.ReportService;
import eu.coatrack.api.ApiUsageReport;
import eu.coatrack.api.DataTableView;
import eu.coatrack.api.ServiceApi;
import eu.coatrack.api.User;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;
import java.util.List;

import static eu.coatrack.admin.utils.DateUtils.parseDateStringOrGetTodayIfNull;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(path = "/admin/reports")
public class ReportController {

    public static final String REPORT_VIEW = "admin/reports/report";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceApiRepository serviceApiRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

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
            @PathVariable("isOnlyPaidCalls") boolean considerOnlyPaidCalls
    ) {
        ModelAndView response = new ModelAndView(REPORT_VIEW);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {

            ApiUsageDTO report = getApiUsageDTO(dateFrom, dateUntil, selectedServiceId, selectedApiConsumerUserId, considerOnlyPaidCalls);

            User currentUser = userRepository.findByUsername(auth.getName());
            List<ServiceApi> servicesProvided = serviceApiRepository.findByDeletedWhen(null);
            List<User> totalConsumers = reportService.getServiceConsumers(servicesProvided);
            List<String> idsPayedPerCall = reportService.getPayPerCallServicesIds(servicesProvided);

            // generell data
            response.addObject("users", totalConsumers);
            response.addObject("selectedServiceId", selectedServiceId);
            response.addObject("selectedApiConsumerUserId", selectedApiConsumerUserId);
            response.addObject("services", servicesProvided);
            response.addObject("payPerCallServicesIds", idsPayedPerCall);
            response.addObject("exportUser", currentUser);

            // data regarding report
            response.addObject("isOnlyPaidCalls", report.isConsiderOnlyPaidCalls()); // TODO delete
            response.addObject("isReportForConsumer", report.isForConsumer()); // TODO delete
            response.addObject("dateFrom", report.getFrom()); // TODO delete
            response.addObject("dateUntil", report.getUntil()); // TODO delete
            response.addObject("serviceApiSelectedForReport", report.getService()); // TODO delete
            response.addObject("consumerUserSelectedForReport", report.getConsumer()); // TODO delete
        } else {
            response.addObject("error", "Request is not authenticated! Please log in.");
        }

        return response;
    }

    @RequestMapping(value = "/consumer", method = GET)
    public ModelAndView showGenerateReportPageForServiceConsumer() {
        return searchReportsByServicesConsumed(null, null, -1L, false);
    }

    // deleted @PathVariable("selectedApiConsumerUserId") Long selectedApiConsumerUserId, because it is not used
    @RequestMapping(value = "/consumer/{dateFrom}/{dateUntil}/{selectedServiceId}/{isOnlyPaidCalls}", method = RequestMethod.GET)
    public ModelAndView searchReportsByServicesConsumed(
            @PathVariable("dateFrom") String dateFromString,
            @PathVariable("dateUntil") String dateUntilString,
            @PathVariable("selectedServiceId") Long selectedServiceId,
            @PathVariable("isOnlyPaidCalls") boolean considerOnlyPaidCalls
    ) {
        ModelAndView response = new ModelAndView(REPORT_VIEW);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            ApiUsageDTO report = getApiUsageDTO(dateFromString, dateUntilString, selectedServiceId, -1L, considerOnlyPaidCalls);

            User currentUser = userRepository.findByUsername(auth.getName());
            List<ServiceApi> servicesFromUser = serviceApiRepository.findByApiKeyList(apiKeyRepository.findByLoggedInAPIConsumer());
            List<String> payPerCallServicesIds = reportService.getPayPerCallServicesIds(servicesFromUser);
            List<User> totalConsumers = reportService.getServiceConsumers(servicesFromUser);

            response.addObject("selectedServiceId", selectedServiceId);
            response.addObject("selectedApiConsumerUserId", currentUser.getId());
            response.addObject("services", servicesFromUser);
            response.addObject("payPerCallServicesIds", payPerCallServicesIds);
            response.addObject("exportUser", currentUser);
            response.addObject("users", totalConsumers);

            response.addObject("isOnlyPaidCalls", report.isConsiderOnlyPaidCalls()); // TODO to delete
            response.addObject("isReportForConsumer", report.isForConsumer()); // TODO to delete
            response.getModel().put("dateFrom", report.getFrom()); // TODO delete
            response.getModel().put("dateUntil", report.getUntil()); // TODO delete
            response.addObject("serviceApiSelectedForReport", report.getService()); // TODO delete
        } else {
            response.addObject("error", "Request is not authenticated! Please log in.");
        }
        return response;
    }

    private ApiUsageDTO getApiUsageDTO(String dateFrom, String dateUntil, Long selectedServiceId, Long apiConsumerId, boolean considerOnlyPaidCalls)  {
        Date from = parseDateStringOrGetTodayIfNull(dateFrom);
        Date until = parseDateStringOrGetTodayIfNull(dateUntil);
        ServiceApi selectedService = serviceApiRepository.findById(selectedServiceId).orElse(null);
        User selectedConsumer = userRepository.findById(apiConsumerId).orElse(null);
        return new ApiUsageDTO(selectedService, selectedConsumer, from, until, considerOnlyPaidCalls, false);
    }

    @RequestMapping(value = "/apiUsage/{dateFrom}/{dateUntil}/{selectedServiceId}/{apiConsumerId}/{onlyPaidCalls}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public DataTableView<ApiUsageReport> reportApiUsage(
            @PathVariable("dateFrom") String dateFrom,
            @PathVariable("dateUntil") String dateUntil,
            @PathVariable("selectedServiceId") Long selectedServiceId,
            @PathVariable("apiConsumerId") Long apiConsumerId,
            @PathVariable("onlyPaidCalls") boolean considerOnlyPaidCalls
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ApiUsageDTO apiUsageDTO = null;
        if (auth != null) {
            apiUsageDTO = getApiUsageDTO(dateFrom, dateUntil, selectedServiceId, apiConsumerId, considerOnlyPaidCalls);
        }
        return reportService.reportApiUsage(apiUsageDTO);
    }
}
