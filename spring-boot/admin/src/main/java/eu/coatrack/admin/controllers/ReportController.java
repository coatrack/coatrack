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

import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.service.ReportService;
import eu.coatrack.api.DataTableView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Slf4j
@RestController
@RequestMapping(path = "/admin/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Deprecated
    @Autowired
    private ServiceApiRepository serviceApiRepository;


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
        return reportService.tryReport(dateFrom, dateUntil, selectedServiceId, selectedApiConsumerUserId, isOnlyPaidCalls);
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

        return reportService.searchReportsByServicesConsumed(dateFrom, dateUntil, selectedServiceId, isOnlyPaidCalls);
    }
}
