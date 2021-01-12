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
@Controller
@RequestMapping(value = "/admin/consumer")
public class AdminConsumerController {

    private static final Logger log = LoggerFactory.getLogger(AdminConsumerController.class);

    private static final String ADMIN_HOME_VIEW = "admin/dashboard";

    private static final String ADMIN_CONSUMER_HOME_VIEW = "admin/consumer_dashboard";

    @Autowired
    MetricsAggregationCustomRepository metricsAggregationCustomRepository;

    @Autowired
    MetricRepository metricRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceApiRepository serviceApiRepository;

    @Autowired
    UserController userController;

    @Autowired
    ReportController reportController;

    @Autowired
    WebUI webUI;

    @Autowired
    UserSessionSettings session;

    @RequestMapping(value = "", method = GET)
    public ModelAndView home(Model model) throws IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        ModelAndView mav = new ModelAndView();

        if (auth.isAuthenticated()) {

            if (userRepository.findByUsername(auth.getName()) != null) {

                boolean end = false;
                boolean found = false;

                List<ServiceApi> services = serviceApiRepository.findByOwnerUsername(auth.getName());

                // IT IS A CONSUMER USER
                mav.setViewName(ADMIN_CONSUMER_HOME_VIEW);

            }

        }

        return mav;

    }

    @RequestMapping(value = "/dashboard/userStatsDoughnutChart", method = GET, produces = "application/json")
    @ResponseBody
    public DoughnutChart generateUserStatisticsDoughnutChart(
            @RequestParam("dateFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodStart,
            @RequestParam("dateUntil") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodEnd) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        List<StatisticsPerService> userStatsList = metricsAggregationCustomRepository.getConsumerStatisticsPerApiConsumerInDescendingOrderByNoOfCalls(selectedTimePeriodStart, selectedTimePeriodEnd, auth.getName());
        DoughnutDataset dataset = new DoughnutDataset()
                .setLabel("API calls")
                .addBackgroundColors(Color.AQUA_MARINE, Color.LIGHT_BLUE, Color.LIGHT_SALMON, Color.LIGHT_BLUE, Color.GRAY)
                .setBorderWidth(2);
        userStatsList.forEach(stats -> dataset.addData(stats.getNoOfCalls()));
        if (userStatsList.size() > 0) {
            DoughnutData data = new DoughnutData()
                    .addDataset(dataset);
            userStatsList.forEach(stats -> data.addLabel(stats.getService()));

            return new DoughnutChart(data);
        } else {
            return new DoughnutChart();
        }
    }

    @RequestMapping(value = "/dashboard/statsPerDayLineChart", method = GET, produces = "application/json")
    @ResponseBody
    public LineChart generateStatsPerDayLineChart(
            @RequestParam("dateFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodStart,
            @RequestParam("dateUntil") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodEnd) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<StatisticsPerDay> statsList = metricsAggregationCustomRepository.getNoOfCallsPerDayForDateRange(
                selectedTimePeriodStart,
                selectedTimePeriodEnd, null,
                auth.getName());

        // create a map with entries for all days in the given date range
        Map<LocalDate, Long> callsPerDay = new TreeMap<>();
        long timePeriodDurationInDays = ChronoUnit.DAYS.between(selectedTimePeriodStart, selectedTimePeriodEnd);
        for (int i = 0; i <= timePeriodDurationInDays; i++) {
            // put "0" as default, in case no calls are registered in database
            callsPerDay.put(selectedTimePeriodStart.plusDays(i), 0l);
        }

        // add numbers from database, if any
        statsList.forEach(statisticsPerDay -> {
            callsPerDay.put(
                    statisticsPerDay.getLocalDate(),
                    statisticsPerDay.getNoOfCalls());
        });

        // create actual chart
        LineDataset dataset = new LineDataset()
                .setLabel("Total number of API calls per day")
                .setBackgroundColor(Color.LIGHT_YELLOW)
                .setBorderWidth(3);
        LineData data = new LineData()
                .addDataset(dataset);

        callsPerDay.forEach((date, noOfCalls) -> {
            data.addLabel(DateTimeFormatter.ISO_LOCAL_DATE.format(date));
            dataset.addData(noOfCalls)
                    .addPointStyle(PointStyle.CIRCLE)
                    .addPointBorderWidth(2)
                    .setLineTension(0f)
                    .setSteppedLine(false)
                    .addPointBackgroundColor(Color.LIGHT_YELLOW)
                    .addPointBorderColor(Color.LIGHT_GRAY);
        });
        LineOptions lineOptions = new LineOptions().setScales(new LinearScales().addyAxis(
                new LinearScale().setTicks(new LinearTicks().setBeginAtZero(true))));

        return new LineChart(data, lineOptions);
    }

    @RequestMapping(value = "/dashboard/httpResponseStatsChart", method = GET, produces = "application/json")
    @ResponseBody
    public DoughnutChart generateHttpResponseStatisticsDoughnutChart(
            @RequestParam("dateFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodStart,
            @RequestParam("dateUntil") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodEnd) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<StatisticsPerHttpStatusCode> statsList = metricsAggregationCustomRepository.getNoOfCallsPerHttpResponseCode(
                selectedTimePeriodStart,
                selectedTimePeriodEnd, null,
                auth.getName());
        if (statsList.size() > 0) {
            DoughnutDataset dataset = new DoughnutDataset()
                    .setLabel("HTTP response codes")
                    .addBackgroundColors(Color.LIGHT_BLUE, Color.LIGHT_GRAY, Color.LIGHT_SALMON, Color.AZURE, Color.BLACK)
                    .setBorderWidth(2);
            statsList.forEach(stats -> dataset.addData(stats.getNoOfCalls()));

            DoughnutData data = new DoughnutData()
                    .addDataset(dataset);
            statsList.forEach(stats -> data.addLabel(stats.getStatusCode().toString()));
            return new DoughnutChart(data);
        } else {
            return new DoughnutChart();
        }
    }

    @RequestMapping(value = "/dashboard/metricsByLoggedUserStatistics", method = GET, produces = "application/json")
    @ResponseBody
    private Iterable<Metric> loadCallsStatistics(
            @RequestParam("dateFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodStart,
            @RequestParam("dateUntil") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedTimePeriodEnd) {
        log.debug("List of Calls by the user during the date range from " + selectedTimePeriodStart + " and " + selectedTimePeriodEnd);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return metricRepository.retrieveByUserConsumer(auth.getName(), Date.valueOf(selectedTimePeriodStart), Date.valueOf(selectedTimePeriodEnd));
    }

}
