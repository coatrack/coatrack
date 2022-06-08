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
import eu.coatrack.admin.model.repository.MetricsAggregationCustomRepository;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.admin.service.ReportService;
import eu.coatrack.api.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static eu.coatrack.api.ServiceAccessPaymentPolicy.WELL_DEFINED_PRICE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Slf4j
@RestController
@RequestMapping(path = "/admin/reports")
public class ReportController {

    private static final String REPORT_VIEW = "admin/reports/report";

    @Autowired
    private ReportService reportService;


    @RequestMapping(value = "", method = RequestMethod.GET)
    public ModelAndView report() {
        return reportService.report();
    }

    @RequestMapping(value = "/{dateFrom}/{dateUntil}/{selectedServiceId}/{selectedApiConsumerUserId}/{isOnlyPaidCalls}", method = RequestMethod.GET)
    public ModelAndView report(
            @PathVariable("dateFrom") String dateFrom,
            @PathVariable("dateUntil") String dateUntil,
            @PathVariable("selectedServiceId") Long selectedServiceId,
            @PathVariable("selectedApiConsumerUserId") Long selectedApiConsumerUserId,
            @PathVariable("isOnlyPaidCalls") boolean isOnlyPaidCalls
    ) {


        ModelAndView mav = new ModelAndView();
        mav.setViewName(REPORT_VIEW);
        mav.addObject("services", serviceApiRepository.findByDeletedWhen(null));
        mav.addObject("users", serviceConsumers);
        mav.getModel().put("dateFrom", df.format(from));
        mav.getModel().put("dateUntil", df.format(until));
        mav.addObject("selectedServiceId", selectedServiceId);
        mav.addObject("selectedApiConsumerUserId", selectedApiConsumerUserId);
        mav.addObject("serviceApiSelectedForReport", (selectedServiceId == -1L) ? null : serviceApiRepository.findById(selectedServiceId).orElse(null));
        mav.addObject("consumerUserSelectedForReport", (selectedApiConsumerUserId == -1L) ? null : userRepository.findById(selectedApiConsumerUserId).orElse(null));
        mav.addObject("payPerCallServicesIds", payPerCallServicesIds);
        mav.addObject("exportUser", exportUser);
        mav.addObject("isReportForConsumer", false);
        mav.addObject("isOnlyPaidCalls", isOnlyPaidCalls);

        return mav;
        return ;
    }

    @RequestMapping(value = "/apiUsage/{dateFrom}/{dateUntil}/{selectedServiceId}/{apiConsumerId}/{onlyPaidCalls}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public DataTableView reportApiUsage(
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
        return reportService.showGenerateReportPageForServiceConsumer();
    }

    // deleted @PathVariable("selectedApiConsumerUserId") Long selectedApiConsumerUserId, because it is not used
    @RequestMapping(value = "/consumer/{dateFrom}/{dateUntil}/{selectedServiceId}/{selectedApiConsumerUserId}/{isOnlyPaidCalls}", method = RequestMethod.GET)
    public ModelAndView searchReportsByServicesConsumed(
            @PathVariable("dateFrom") String dateFrom,
            @PathVariable("dateUntil") String dateUntil,
            @PathVariable("selectedServiceId") Long selectedServiceId,
            @PathVariable("isOnlyPaidCalls") boolean isOnlyPaidCalls
    ) {


        ModelAndView mav = new ModelAndView();
        mav.setViewName(REPORT_VIEW);
        mav.addObject("services", servicesThatLoggedInUserHasAKeyFor);
        mav.getModel().put("dateFrom", df.format(dateFromDate));
        mav.getModel().put("dateUntil", df.format(dateUntilDate));
        mav.addObject("selectedServiceId", selectedServiceId);
        mav.addObject("selectedApiConsumerUserId", user.getId());
        mav.addObject("consumerUserSelectedForReport", user);
        mav.addObject("serviceApiSelectedForReport", (selectedServiceId == -1L) ? null : serviceApiRepository.findById(selectedServiceId).orElse(null));
        mav.addObject("payPerCallServicesIds", payPerCallServicesIds);
        mav.addObject("isReportForConsumer", true);
        mav.addObject("isOnlyPaidCalls", isOnlyPaidCalls);
        return mav;
    }
}
