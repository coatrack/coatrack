package eu.coatrack.admin.model.repository;

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

import eu.coatrack.admin.model.vo.MetricsAggregation;
import eu.coatrack.admin.model.vo.StatisticsPerApiUser;
import eu.coatrack.admin.model.vo.StatisticsPerDay;
import eu.coatrack.admin.model.vo.StatisticsPerHttpStatusCode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

/**
 * @author perezdf
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class MetricsAggregationCustomRepositoryTest {

    @Autowired
    private MetricsAggregationCustomRepository repository;

    @Autowired
    MetricRepository metricRepository;

    /**
     * api provider username, needs to be in sync with sample data
     *
     * @see eu.coatrack.admin.DatabaseInitializer
     */
    @Value("${ygg.admin.database.sampleDataUsername:exampleCompanyInc}")
    private String sampleDataApiProviderUsername;

    private LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
    private LocalDate today = LocalDate.now();

    /*
    @Test
    public void getServiceApisByMetric() {
        List<Long> ids = repository.getIDsOfLatestMetricsPerSession(null, null, true, true, sampleDataApiProviderUsername);
        List<ServiceApi> result = repository.getServiceApisByMetric(ids.get(0));
        assertNotNull(result);
    }

    @Test
    public void getIDsOfLatestMetricsPerSessionTrueTrue() {
        List<Long> result = repository.getIDsOfLatestMetricsPerSession(null, null, true, true, sampleDataApiProviderUsername);
        assertThat(result.size()).isEqualTo(9);
    }

    @Test
    public void getIDsOfLatestMetricsPerSessionFindByProxyId() {
        List<Long> resultList = repository.getIDsOfLatestMetricsPerSession(null, null, false, true, sampleDataApiProviderUsername);
        System.out.println(resultList);
        Metric resultItem = metricRepository.findOne(resultList.get(0));

        resultList = repository.getIDsOfLatestMetricsPerSession(resultItem.getProxy().getId(), null, false, true, sampleDataApiProviderUsername);
        boolean found = false;
        for (Long id : resultList) {
            Metric item = metricRepository.findOne(id);
            if (resultItem.getProxy().getId().equals(item.getProxy().getId())) {
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    public void getIDsOfLatestMetricsPerSessionFindByType() {
        List<Long> resultList = repository.getIDsOfLatestMetricsPerSession(null, null, false, true, sampleDataApiProviderUsername);
        System.out.println(resultList);
        Metric resultItem = metricRepository.findOne(resultList.get(0));

        resultList = repository.getIDsOfLatestMetricsPerSession(null, resultItem.getType(), false, true, sampleDataApiProviderUsername);
        boolean found = false;
        for (Long id : resultList) {
            Metric item = metricRepository.findOne(id);
            if (resultItem.getType().equals(item.getType())) {
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    public void getIDsOfLatestMetricsPerSessionTrueFalse() {
        List<Long> result = repository.getIDsOfLatestMetricsPerSession(null, null, true, false, sampleDataApiProviderUsername);
        assertThat(result.size()).isEqualTo(9);
    }

    @Test
    public void getIDsOfLatestMetricsPerSessionFalseFalse() {
        List<Long> result = repository.getIDsOfLatestMetricsPerSession(null, null, false, false, sampleDataApiProviderUsername);
        assertThat(result.size()).isEqualTo(41);
    }

    @Test
    public void getSummarizedMetricsByProxyId() {
        List<Long> idResult = repository.getIDsOfLatestMetricsPerSession(null, null, false, false, sampleDataApiProviderUsername);
        Metric resultItem = metricRepository.findOne(idResult.get(0));

        List<MetricsAggregation> result = repository.getSummarizedMetricsByProxyId(resultItem.getProxy().getId());
        assertNotNull(result);
        for (MetricsAggregation item : result) {
            System.out.println("::." + item.getServiceApiName() + " " + item.getApiUser() + " " + item.getHttpResponseCode() + " " + item.getCount());
        }

    }
    */

    @Test
    public void getSummarizedMetricsPerUser() {

        List<StatisticsPerApiUser> result = repository.getStatisticsPerApiConsumerInDescendingOrderByNoOfCalls(thirtyDaysAgo, today, sampleDataApiProviderUsername);
        assertNotNull(result);
        assertThat(result.size()).isEqualTo(3);

    }

    @Test
    public void getTotalNumberOfLoggedApiCalls() {

        int result = repository.getTotalNumberOfLoggedApiCalls(LocalDate.now().minusYears(999), LocalDate.now().plusDays(1), sampleDataApiProviderUsername);
        assertTrue(result > 0);
    }

    // In the following there are tests for each statistic displayed on the GUIs main dashboard
    // when logging in as the user configured as "sampleDataApiProviderUsername", which is the api provider username in the sample dataset

    @Test
    public void getNoOfCallsToExampleCompanyIncApisThisWeekTest() {
        assertEquals(157, repository.getTotalNumberOfLoggedApiCallsThisWeek(sampleDataApiProviderUsername));
    }

    @Test
    public void getNoOfErrorsWhenCallingExampleCompanyIncApisThisWeek() {
        assertEquals(11, repository.getNumberOfErroneousApiCalls(
                LocalDate.now().minusDays(6),
                LocalDate.now(),
                sampleDataApiProviderUsername));
    }

    @Test
    public void getNoOfCallsToExampleCompanyIncApisThisWeekComparedToLastWeekTest() {
        assertEquals(15,
                repository.getTotalNumberOfLoggedApiCallsThisWeek(sampleDataApiProviderUsername)
                        - repository.getTotalNumberOfLoggedApiCallsLastWeek(sampleDataApiProviderUsername));
    }

    @Test
    public void getNumberOfConsumersOfExampleCompanyIncApisTest() {
        assertEquals(3, repository.getNumberOfApiCallers(thirtyDaysAgo, today, sampleDataApiProviderUsername));
    }

    @Test
    public void getStatisticsPerUserOfExampleCompanyIncApisTest() {
        List<StatisticsPerApiUser> statsPerApiUser = repository.getStatisticsPerApiConsumerInDescendingOrderByNoOfCalls(thirtyDaysAgo, today, sampleDataApiProviderUsername);
        assertNotNull(statsPerApiUser);
        assertEquals(3, statsPerApiUser.size());

        StatisticsPerApiUser statistics = statsPerApiUser.get(0);
        assertEquals("Richard Retailer", statistics.getUserName());
        assertEquals(161, statistics.getNoOfCalls());
        assertEquals(161 * 100 / 299, statistics.getPercentage());

        statistics = statsPerApiUser.get(1);
        assertEquals("Walt Wholesaler", statistics.getUserName());
        assertEquals(121, statistics.getNoOfCalls());
        assertEquals(121 * 100 / 299, statistics.getPercentage());

        statistics = statsPerApiUser.get(2);
        assertEquals("exampleApiUser", statistics.getUserName());
        assertEquals(17, statistics.getNoOfCalls());
        assertEquals(17 * 100 / 299, statistics.getPercentage());
    }

    @Test
    public void getDistributionOfHttpResponseCodesForExampleCompanyIncApisTest() {
        List<StatisticsPerHttpStatusCode> statsPerResponseCode = repository.getNoOfCallsPerHttpResponseCode(thirtyDaysAgo, today, sampleDataApiProviderUsername);
        assertNotNull(statsPerResponseCode);
        assertEquals(3, statsPerResponseCode.size());

        StatisticsPerHttpStatusCode statistics = statsPerResponseCode.get(0);
        assertEquals(200, statistics.getStatusCode().intValue());
        assertEquals(272, statistics.getNoOfCalls());

        statistics = statsPerResponseCode.get(1);
        assertEquals(404, statistics.getStatusCode().intValue());
        assertEquals(14, statistics.getNoOfCalls());

        statistics = statsPerResponseCode.get(2);
        assertEquals(500, statistics.getStatusCode().intValue());
        assertEquals(13, statistics.getNoOfCalls());
    }

    @Test
    public void getNoOfExampleCompanyIncApiCallsPerDayThisWeekTest() {
        List<StatisticsPerDay> statsPerDay = repository.getNoOfCallsPerDayForDateRange(today.minusDays(6), today, sampleDataApiProviderUsername);
        assertNotNull(statsPerDay);
        assertEquals(7, statsPerDay.size());

        assertEquals(12, statsPerDay.get(0).getNoOfCalls());
        assertEquals(10, statsPerDay.get(1).getNoOfCalls());
        assertEquals(9, statsPerDay.get(2).getNoOfCalls());
        assertEquals(32, statsPerDay.get(3).getNoOfCalls());
        assertEquals(31, statsPerDay.get(4).getNoOfCalls());
        assertEquals(28, statsPerDay.get(5).getNoOfCalls());
        assertEquals(35, statsPerDay.get(6).getNoOfCalls());
    }

    @Test
    public void getTotalNoOfCallsToExampleCompanyIncApis() {
        assertEquals(299, repository.getTotalNumberOfLoggedApiCalls(sampleDataApiProviderUsername));
    }

    @Test
    public void getTotalNoOfErrorsThatOccuredWhenCallingExampleCompanyIncApis() {
        assertEquals(27, repository.getTotalNumberOfErroneousApiCalls(sampleDataApiProviderUsername));
    }

    /**
     * Test for the proxy-specific statistics page
     */
    @Test
    public void getAggregatedStatisticsPerProxyTest() {
        List<MetricsAggregation> list = repository.getSummarizedMetricsByProxyId("aa11aa22-aa33-aa44-aa55-aa66aa77aa88");
        assertNotNull(list);
        assertEquals(30, list.size());

        assertEquals(10, list.stream().filter(m -> m.getHttpResponseCode() == null).count());

        assertEquals(15, summarizeMetricsCounterValuesByUsernameAndHttpResponseCode(list, "exampleApiUser", 200));
        assertEquals(1, summarizeMetricsCounterValuesByUsernameAndHttpResponseCode(list, "exampleApiUser", 404));
        assertEquals(1, summarizeMetricsCounterValuesByUsernameAndHttpResponseCode(list, "exampleApiUser", 500));
        assertEquals(141, summarizeMetricsCounterValuesByUsernameAndHttpResponseCode(list, "Richard Retailer", 200));
        assertEquals(9, summarizeMetricsCounterValuesByUsernameAndHttpResponseCode(list, "Richard Retailer", 404));
        assertEquals(11, summarizeMetricsCounterValuesByUsernameAndHttpResponseCode(list, "Richard Retailer", 500));
        assertEquals(116, summarizeMetricsCounterValuesByUsernameAndHttpResponseCode(list, "Walt Wholesaler", 200));
        assertEquals(4, summarizeMetricsCounterValuesByUsernameAndHttpResponseCode(list, "Walt Wholesaler", 404));
        assertEquals(1, summarizeMetricsCounterValuesByUsernameAndHttpResponseCode(list, "Walt Wholesaler", 500));
    }

    @Test
    public void getAggregatedStatisticsPerServiceTest() {
        List<MetricsAggregation> list1 = repository.getSummarizedMetricsByServiceId(1);
        assertNotNull(list1);
        assertEquals(24, list1.size());

        assertEquals(8, list1.stream().filter(m -> m.getHttpResponseCode() == null).count());

        assertEquals(11, summarizeMetricsCounterValuesByUsernameAndHttpResponseCode(list1, "exampleApiUser", 200));
        assertEquals(141, summarizeMetricsCounterValuesByUsernameAndHttpResponseCode(list1, "Richard Retailer", 200));
        assertEquals(9, summarizeMetricsCounterValuesByUsernameAndHttpResponseCode(list1, "Richard Retailer", 404));
        assertEquals(11, summarizeMetricsCounterValuesByUsernameAndHttpResponseCode(list1, "Richard Retailer", 500));
        assertEquals(116, summarizeMetricsCounterValuesByUsernameAndHttpResponseCode(list1, "Walt Wholesaler", 200));
        assertEquals(4, summarizeMetricsCounterValuesByUsernameAndHttpResponseCode(list1, "Walt Wholesaler", 404));
        assertEquals(1, summarizeMetricsCounterValuesByUsernameAndHttpResponseCode(list1, "Walt Wholesaler", 500));

        List<MetricsAggregation> list2 = repository.getSummarizedMetricsByServiceId(2);
        assertNotNull(list2);
        assertEquals(0, list2.size());

        List<MetricsAggregation> list3 = repository.getSummarizedMetricsByServiceId(3);
        assertNotNull(list3);
        assertEquals(6, list3.size());
        assertEquals(2, list3.stream().filter(m -> m.getHttpResponseCode() == null).count());
        assertEquals(4, summarizeMetricsCounterValuesByUsernameAndHttpResponseCode(list3, "exampleApiUser", 200));
        assertEquals(1, summarizeMetricsCounterValuesByUsernameAndHttpResponseCode(list3, "exampleApiUser", 404));
        assertEquals(1, summarizeMetricsCounterValuesByUsernameAndHttpResponseCode(list3, "exampleApiUser", 500));
    }


    private long summarizeMetricsCounterValuesByUsernameAndHttpResponseCode(List<MetricsAggregation> list, String username, int httpResponseCode) {
        return list.stream()
                .filter(m -> m.getApiUser().equals(username))
                .filter(m -> m.getHttpResponseCode() != null && m.getHttpResponseCode().intValue() == httpResponseCode)
                .mapToLong(m -> m.getCount())
                .sum();
    }

}
