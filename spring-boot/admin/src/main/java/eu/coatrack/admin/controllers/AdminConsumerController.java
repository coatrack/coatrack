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
import be.ceau.chart.LineChart;
import be.ceau.chart.color.Color;
import be.ceau.chart.data.DoughnutData;
import be.ceau.chart.data.LineData;
import be.ceau.chart.dataset.DoughnutDataset;
import be.ceau.chart.dataset.LineDataset;
import be.ceau.chart.enums.PointStyle;
import be.ceau.chart.options.LineOptions;
import be.ceau.chart.options.scales.LinearScale;
import be.ceau.chart.options.scales.LinearScales;
import be.ceau.chart.options.ticks.LinearTicks;
import eu.coatrack.admin.UserSessionSettings;
import eu.coatrack.admin.components.WebUI;
import eu.coatrack.admin.model.repository.MetricsAggregationCustomRepository;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.admin.model.vo.StatisticsPerHttpStatusCode;
import eu.coatrack.admin.service.AdminConsumerService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import eu.coatrack.admin.model.repository.MetricRepository;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.model.vo.StatisticsPerDay;
import eu.coatrack.admin.model.vo.StatisticsPerService;
import eu.coatrack.api.Metric;
import eu.coatrack.api.ServiceApi;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * @author Timon Veenstra <tveenstra@bebr.nl>
 */
@Slf4j
@Controller
@RequestMapping(value = "/admin/consumer")
public class AdminConsumerController {
    @Autowired
    private AdminConsumerService adminConsumerService;


    @RequestMapping(value = "", method = GET)
    public ModelAndView home(Model model) throws IOException {
        return adminConsumerService.home(model);
    }

    @RequestMapping(value = "/dashboard/userStatsDoughnutChart", method = GET, produces = "application/json")
    @ResponseBody
    public DoughnutChart generateUserStatisticsDoughnutChart(
            @RequestParam("dateFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodStart,
            @RequestParam("dateUntil") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodEnd) {

        return adminConsumerService.generateUserStatisticsDoughnutChart(selectedTimePeriodStart, selectedTimePeriodEnd);
    }

    @RequestMapping(value = "/dashboard/statsPerDayLineChart", method = GET, produces = "application/json")
    @ResponseBody
    public LineChart generateStatsPerDayLineChart(
            @RequestParam("dateFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodStart,
            @RequestParam("dateUntil") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodEnd) {

       return adminConsumerService.generateStatsPerDayLineChart(selectedTimePeriodStart, selectedTimePeriodEnd);
    }

    @RequestMapping(value = "/dashboard/httpResponseStatsChart", method = GET, produces = "application/json")
    @ResponseBody
    public DoughnutChart generateHttpResponseStatisticsDoughnutChart(
            @RequestParam("dateFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodStart,
            @RequestParam("dateUntil") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodEnd) {

        return adminConsumerService.generateHttpResponseStatisticsDoughnutChart(selectedTimePeriodStart, selectedTimePeriodEnd);
    }

    @RequestMapping(value = "/dashboard/metricsByLoggedUserStatistics", method = GET, produces = "application/json")
    @ResponseBody
    private Iterable<Metric> loadCallsStatistics(
            @RequestParam("dateFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodStart,
            @RequestParam("dateUntil") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodEnd) {

        return adminConsumerService.loadCallsStatistics(selectedTimePeriodStart, selectedTimePeriodEnd);
    }

}
