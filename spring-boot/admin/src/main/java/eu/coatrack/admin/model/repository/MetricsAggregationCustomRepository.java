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
import eu.coatrack.api.Metric;
import eu.coatrack.api.MetricType;
import eu.coatrack.api.ServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.TemporalType;
import eu.coatrack.admin.model.vo.StatisticsPerService;

/**
 * Custom repository to get several aggregations of Metrics that are not
 * supported by Spring CRUD repositories.
 *
 * @author gr-hovest(at)atb-bremen.de
 */
@Repository
public class MetricsAggregationCustomRepository {

    Logger log = LoggerFactory.getLogger(MetricsAggregationCustomRepository.class);

    private final EntityManager entityManager;

    public MetricsAggregationCustomRepository(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public int getTotalNumberOfLoggedApiCalls() {
        return getTotalNumberOfLoggedApiCalls(null);
    }

    public int getTotalNumberOfLoggedApiCalls(String apiProviderUsername) {
        return getTotalNumberOfLoggedApiCalls(LocalDate.now().minusYears(100), LocalDate.now(), apiProviderUsername);
    }

    public int getTotalNumberOfLoggedApiCallsThisWeek() {
        return getTotalNumberOfLoggedApiCallsThisWeek(null);
    }

    public int getTotalNumberOfLoggedApiCallsThisWeek(String apiProviderUsername) {
        return getTotalNumberOfLoggedApiCalls(LocalDate.now().minusDays(6), LocalDate.now(), apiProviderUsername);
    }

    public int getTotalNumberOfLoggedApiCallsLastWeek() {
        return getTotalNumberOfLoggedApiCallsLastWeek(null);
    }

    public int getTotalNumberOfLoggedApiCallsLastWeek(String apiProviderUsername) {
        return getTotalNumberOfLoggedApiCalls(LocalDate.now().minusDays(13), LocalDate.now().minusDays(7),
                apiProviderUsername);
    }

    public int getTotalNumberOfLoggedApiCalls(LocalDate from, LocalDate until) {
        return getTotalNumberOfLoggedApiCalls(from, until, null);
    }

    public int getTotalNumberOfLoggedApiCalls(LocalDate from, LocalDate until, String apiProviderOwnerName) {
        return getTotalNumberOfLoggedApiCalls(from, until, apiProviderOwnerName, null);
    }

    public int getTotalNumberOfLoggedApiCalls(LocalDate from, LocalDate until, String apiProviderOwnerName,
            String apiProviderConsumerName) {

        int result = 0;

        /*
         * It is important to check if there metric id list because there is an
         * exception in case you are using postgres and that error is skipped if you are
         * using H2 database
         */
        List<Long> metricIdList = getIDsOfRelevantMetricsByMetricType(MetricType.AUTHORIZED_REQUEST,
                apiProviderOwnerName);

        if (!metricIdList.isEmpty()) {
            String queryString = "SELECT sum(metric.count) "
                    + " FROM  Metric metric "
                    + " WHERE metric.id IN :idsOfRelevantMetrics"
                    + " AND metric.dateOfApiCall BETWEEN :startDate AND :finishDate ";

            if (apiProviderOwnerName != null) {
                queryString = queryString + " AND metric.apiKey.serviceApi.owner.username = :apiProviderUsername";
            }

            if (apiProviderConsumerName != null) {
                queryString = queryString + " AND metric.apiKey.user.username = :apiProviderConsumerName";
            }

            Query query = entityManager.createQuery(queryString);

            query.setParameter("startDate", Date.valueOf(from));
            query.setParameter("finishDate", Date.valueOf(until));
            query.setParameter("idsOfRelevantMetrics", metricIdList);
            if (apiProviderOwnerName != null) {
                query.setParameter("apiProviderUsername", apiProviderOwnerName);
            }

            if (apiProviderConsumerName != null) {
                query.setParameter("apiProviderConsumerName", apiProviderConsumerName);
            }

            if (query != null && query.getSingleResult() != null) {
                result = ((Long) query.getSingleResult()).intValue();
            }
        }
        return result;
    }

    public int getTotalNumberOfErroneousApiCalls(String apiProviderUsername) {
        return getNumberOfErroneousApiCalls(LocalDate.now().minusYears(100), LocalDate.now(), apiProviderUsername);

    }

    public int getNumberOfErroneousApiCalls(LocalDate from, LocalDate until, String apiProviderUsername) {

        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Integer> query = builder.createQuery(Integer.class);
        final Root<Metric> fromTable = query.from(Metric.class);

        int numberOfErroneousApiCalls = 0;
        List<Long> idsList = getIDsOfRelevantMetricsWithHttpErrorCodes(apiProviderUsername);
        if (idsList != null && !idsList.isEmpty()) {

            query.select(builder.sum(fromTable.get("count")));
            query.where(builder.and(
                    fromTable.get("id").in(idsList),
                    builder.greaterThanOrEqualTo((javax.persistence.criteria.Expression) fromTable.get("dateOfApiCall"),
                            Date.valueOf(from)),
                    builder.lessThanOrEqualTo((javax.persistence.criteria.Expression) fromTable.get("dateOfApiCall"),
                            Date.valueOf(until))));
            Integer result = entityManager.createQuery(query).getSingleResult();
            // result can be null in case no metrics have been recorded in the defined time
            // period
            numberOfErroneousApiCalls = (result == null) ? 0 : result;
        }
        return numberOfErroneousApiCalls;
    }

    public long getNumberOfApiCallers(LocalDate from, LocalDate until, String apiProviderUserName) {

        Query query = entityManager.createQuery("SELECT distinct metric.apiKey.user.username "
                + " FROM  Metric metric "
                + " WHERE metric.apiKey.serviceApi.owner.username = :apiProviderUserName"
                + " AND metric.dateOfApiCall BETWEEN :fromDate AND :untilDate ");

        query.setParameter("apiProviderUserName", apiProviderUserName);
        query.setParameter("fromDate", Date.valueOf(from));
        query.setParameter("untilDate", Date.valueOf(until));

        List result = query.getResultList();

        int amount = -1;
        if (result != null) {
            amount = result.size();
        }

        return amount;
    }

    public List<StatisticsPerService> getConsumerStatisticsPerApiConsumerInDescendingOrderByNoOfCalls(
            LocalDate dateRangeStart,
            LocalDate dateRangeEnd,
            String apiProviderConsumername) {

        List<StatisticsPerService> statisticsPerServiceList = new ArrayList<>();
        List<Long> metricIdList = getConsumerIDsOfRelevantMetricsByMetricType(MetricType.AUTHORIZED_REQUEST,
                apiProviderConsumername);

        if (!metricIdList.isEmpty()) {

            int totalNoOfCalls = getTotalNumberOfLoggedApiCalls(dateRangeStart, dateRangeEnd, null,
                    apiProviderConsumername);
            Query query = entityManager.createQuery(
                    "SELECT metric.apiKey.serviceApi.name, SUM(metric.count) AS noOfCalls, SUM(metric.count)*100 / :totalNoOfCalls "
                            + " FROM  Metric metric "
                            + " WHERE metric.id IN :idsOfRelevantMetrics"
                            + " AND metric.dateOfApiCall BETWEEN :startDate AND :endDate"
                            + " GROUP BY metric.apiKey.serviceApi.name"
                            + " ORDER BY noOfCalls DESC");

            query.setParameter("totalNoOfCalls", new Integer(totalNoOfCalls).longValue());
            query.setParameter("idsOfRelevantMetrics", metricIdList);
            query.setParameter("startDate", Date.valueOf(dateRangeStart));
            query.setParameter("endDate", Date.valueOf(dateRangeEnd));

            List result = query.getResultList();

            for (Object itemObject : result) {
                Object[] item = (Object[]) itemObject;

                StatisticsPerService statisticsPerApiUser = new StatisticsPerService((String) item[0],
                        (Long) item[1],
                        (Long) item[2]);
                statisticsPerServiceList.add(statisticsPerApiUser);
            }
        }

        return statisticsPerServiceList;
    }

    public List<StatisticsPerApiUser> getStatisticsPerApiConsumerInDescendingOrderByNoOfCalls(
            LocalDate dateRangeStart,
            LocalDate dateRangeEnd,
            String apiProviderUsername) {

        List<StatisticsPerApiUser> statisticsPerApiUserList = new ArrayList<>();
        List<Long> metricIdList = getIDsOfRelevantMetricsByMetricType(MetricType.AUTHORIZED_REQUEST,
                apiProviderUsername);

        if (!metricIdList.isEmpty()) {

            int totalNoOfCalls = getTotalNumberOfLoggedApiCalls(apiProviderUsername);
            Query query = entityManager.createQuery(
                    "SELECT metric.apiKey.user.username, SUM(metric.count) AS noOfCalls, SUM(metric.count)*100 / :totalNoOfCalls "
                            + " FROM  Metric metric "
                            + " WHERE metric.id IN :idsOfRelevantMetrics"
                            + " AND metric.dateOfApiCall BETWEEN :startDate AND :endDate"
                            + " GROUP BY metric.apiKey.user.username"
                            + " ORDER BY noOfCalls DESC");

            query.setParameter("totalNoOfCalls", new Integer(totalNoOfCalls).longValue());
            query.setParameter("idsOfRelevantMetrics",
                    getIDsOfRelevantMetricsByMetricType(MetricType.AUTHORIZED_REQUEST, apiProviderUsername));
            query.setParameter("startDate", Date.valueOf(dateRangeStart));
            query.setParameter("endDate", Date.valueOf(dateRangeEnd));

            List result = query.getResultList();

            for (Object itemObject : result) {
                Object[] item = (Object[]) itemObject;

                StatisticsPerApiUser statisticsPerApiUser = new StatisticsPerApiUser((String) item[0],
                        (Long) item[1],
                        (Long) item[2]);
                statisticsPerApiUserList.add(statisticsPerApiUser);
            }
        }

        return statisticsPerApiUserList;
    }

    public List<StatisticsPerHttpStatusCode> getNoOfCallsPerHttpResponseCode(
            LocalDate dateRangeStart,
            LocalDate dateRangeEnd,
            String apiProviderUsername) {
        return getNoOfCallsPerHttpResponseCode(dateRangeStart, dateRangeEnd, apiProviderUsername, null);
    }

    public List<StatisticsPerHttpStatusCode> getNoOfCallsPerHttpResponseCode(
            LocalDate dateRangeStart,
            LocalDate dateRangeEnd,
            String apiProviderUsername, String apiProviderConsumerUsername) {

        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<StatisticsPerHttpStatusCode> query = builder.createQuery(StatisticsPerHttpStatusCode.class);
        final Root<Metric> from = query.from(Metric.class);

        if (getIDsOfRelevantMetricsJustForResponses(apiProviderUsername, apiProviderConsumerUsername).size() > 0) {
            query.multiselect(
                    from.get("httpResponseCode"),
                    builder.sum(from.get("count")));
            query.where(builder.and(
                    from.get("id")
                            .in(getIDsOfRelevantMetricsJustForResponses(apiProviderUsername,
                                    apiProviderConsumerUsername)),
                    builder.between(
                            (javax.persistence.criteria.Expression) from.get("dateOfApiCall"),
                            Date.valueOf(dateRangeStart),
                            Date.valueOf(dateRangeEnd))));
            query.groupBy(
                    from.get("httpResponseCode"));
            query.orderBy(builder.asc(
                    from.get("httpResponseCode")));

            return entityManager.createQuery(query).getResultList();
        } else {
            return new ArrayList<StatisticsPerHttpStatusCode>();
        }
    }

    public List<StatisticsPerDay> getNoOfCallsPerDayForDateRange(
            LocalDate dateRangeStart,
            LocalDate dateRangeEnd,
            String apiProviderUsername) {

        return getNoOfCallsPerDayForDateRange(dateRangeStart, dateRangeEnd, apiProviderUsername, null);

    }

    public List<StatisticsPerDay> getNoOfCallsPerDayForDateRange(
            LocalDate dateRangeStart,
            LocalDate dateRangeEnd,
            String apiProviderUsername, String apiProviderConsumerUsername) {

        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<StatisticsPerDay> query = builder.createQuery(StatisticsPerDay.class);
        final Root<Metric> from = query.from(Metric.class);

        List<StatisticsPerDay> statisticsList = null;

        List<Long> idList = getIDsOfRelevantMetricsByMetricType(MetricType.AUTHORIZED_REQUEST, apiProviderUsername,
                apiProviderConsumerUsername);
        if (idList != null && !idList.isEmpty()) {

            query.multiselect(
                    from.get("dateOfApiCall"),
                    builder.sum(from.get("count")));
            query.where(builder.and(
                    from.get("id").in(idList),
                    builder.between(
                            (javax.persistence.criteria.Expression) from.get("dateOfApiCall"),
                            Date.valueOf(dateRangeStart),
                            Date.valueOf(dateRangeEnd))));

            query.groupBy(
                    from.get("dateOfApiCall"));
            query.orderBy(builder.asc(
                    from.get("dateOfApiCall")));

            statisticsList = entityManager.createQuery(query).getResultList();
        } else {
            statisticsList = Collections.emptyList();
        }

        return (statisticsList != null) ? statisticsList : new ArrayList<>();
    }

    public List<ServiceApi> getServiceApisByMetric(long id) {

        Query query = entityManager.createQuery("SELECT metric.apiKey.serviceApi "
                + " FROM  Metric metric "
                + " WHERE  metric.id = :id");

        query.setParameter("id", id);

        return query.getResultList();

    }

    public List<MetricsAggregation> getSummarizedMetricsByServiceId(long serviceId) {
        return getSummarizedMetrics(serviceId, null, null, null);
    }

    public List<MetricsAggregation> getSummarizedMetricsByServiceIdAndDateRange(
            long serviceId,
            LocalDate fromDate,
            LocalDate untilDate) {

        return getSummarizedMetrics(serviceId, null, fromDate, untilDate);
    }

    public List<MetricsAggregation> getSummarizedMetricsByProxyId(String proxyId) {
        return getSummarizedMetrics(null, proxyId, null, null);
    }

    private List<MetricsAggregation> getSummarizedMetrics(
            Long serviceId, String proxyId,
            LocalDate fromDate,
            LocalDate untilDate) {

        Assert.isTrue(serviceId == null ^ proxyId == null,
                "this method expects that exactly one of serviceId or proxyId is not null, while the other perameter is null");

        List<Long> idsOfRelevantMetrics = (serviceId != null)
                ? getIDsOfRelevantMetricsByServiceId(serviceId, fromDate, untilDate)
                : getIDsOfRelevantMetricsByProxyID(proxyId, fromDate, untilDate);

        if (idsOfRelevantMetrics == null || idsOfRelevantMetrics.isEmpty()) {
            return Collections.emptyList();
        } else {
            String queryString = "SELECT new eu.coatrack.admin.model.vo.MetricsAggregation("
                    + "metric.apiKey.serviceApi.name, "
                    + "metric.apiKey.user.username, "
                    + "metric.type, "
                    + "metric.requestMethod, "
                    + "metric.path, "
                    + "metric.httpResponseCode, "
                    + "SUM(metric.count)"
                    + ") "
                    + "FROM  Metric metric "
                    + "WHERE metric.id IN :idsOfRelevantMetrics "
                    + "GROUP BY metric.apiKey.serviceApi.name, metric.apiKey.user.username, metric.type, metric.requestMethod, metric.path, metric.httpResponseCode "
                    + "ORDER BY metric.apiKey.serviceApi.name, metric.apiKey.user.username, metric.type, metric.requestMethod, metric.path, metric.httpResponseCode ";

            TypedQuery<MetricsAggregation> query = entityManager.createQuery(queryString, MetricsAggregation.class);
            query.setParameter("idsOfRelevantMetrics", idsOfRelevantMetrics);

            return query.getResultList();
        }
    }

    private List<Long> getIDsOfRelevantMetricsByServiceId(
            long serviceId,
            LocalDate fromDate,
            LocalDate untilDate) {

        return getIDsOfLatestMetricsPerSession(
                serviceId,
                null,
                fromDate,
                untilDate,
                null,
                false,
                false,
                null);
    }

    private List<Long> getIDsOfRelevantMetricsByProxyID(
            String proxyId,
            LocalDate fromDate,
            LocalDate untilDate) {

        return getIDsOfLatestMetricsPerSession(
                null,
                proxyId,
                fromDate,
                untilDate,
                null,
                false,
                false,
                null);
    }

    private List<Long> getIDsOfRelevantMetricsByMetricType(MetricType metricType, String apiProviderUsername) {
        return getIDsOfLatestMetricsPerSession(
                null,
                null,
                null,
                null,
                metricType,
                false,
                false,
                apiProviderUsername);
    }

    private List<Long> getIDsOfRelevantMetricsByMetricType(MetricType metricType, String apiProviderUsername,
            String apiProviderConsumername) {
        return getIDsOfLatestMetricsPerSession(
                null,
                null,
                null,
                null,
                metricType,
                false,
                false,
                apiProviderUsername, apiProviderConsumername);
    }

    private List<Long> getConsumerIDsOfRelevantMetricsByMetricType(MetricType metricType,
            String apiProviderConsumername) {
        return getIDsOfLatestMetricsPerSession(
                null,
                null,
                null,
                null,
                metricType,
                false,
                false,
                null, apiProviderConsumername);
    }

    private List<Long> getIDsOfRelevantMetricsWithHttpErrorCodes(String apiProviderUsername) {
        return getIDsOfLatestMetricsPerSession(
                null,
                null,
                null,
                null,
                null,
                true,
                false,
                apiProviderUsername);
    }

    private List<Long> getIDsOfRelevantMetricsWithHttpErrorCodes(String apiProviderOwnerUsername,
            String apiProviderConsumerUsername) {
        return getIDsOfLatestMetricsPerSession(
                null,
                null,
                null,
                null,
                null,
                true,
                false,
                apiProviderOwnerUsername, apiProviderConsumerUsername);
    }

    private List<Long> getIDsOfRelevantMetricsJustForResponses(String apiProviderUsername) {
        return getIDsOfLatestMetricsPerSession(
                null,
                null,
                null,
                null,
                null,
                false,
                true,
                apiProviderUsername);
    }

    private List<Long> getIDsOfRelevantMetricsJustForResponses(String apiProviderOwnerUsername,
            String apiProviderConsumerUsername) {
        return getIDsOfLatestMetricsPerSession(
                null,
                null,
                null,
                null,
                null,
                false,
                true,
                apiProviderOwnerUsername, apiProviderConsumerUsername);
    }

    private List<Long> getIDsOfLatestMetricsPerSession(
            Long serviceId,
            String proxyId,
            LocalDate fromDate,
            LocalDate untilDate,
            MetricType metricType,
            boolean onlyHttpErrorCodes,
            boolean onlyResponseMetrics,
            String apiProviderUsername) {
        return getIDsOfLatestMetricsPerSession(
                serviceId,
                proxyId,
                fromDate,
                untilDate,
                metricType,
                onlyHttpErrorCodes,
                onlyResponseMetrics,
                apiProviderUsername,
                null);
    }

    /**
     * Returns the database IDs of the latest metrics per counter session, i.e. in
     * case the proxies transmitted regular updates of the metrics, only the latest
     * values for each metric are returned. This way "double counting" is avoided.
     * <p>
     * In the future this double counting should already be avoided at metrics
     * transmission time, therefore this method should in any case stay PRIVATE, as
     * this is an internal behaviour of the metrics repository to be changed later.
     *
     * @param serviceId           - optional parameter to filter by service
     * @param proxyId             - optional parameter to filter by proxy
     * @param metricType          - optional parameter to filter by metric
     *                            metricType
     * @param onlyHttpErrorCodes  - if TRUE, only http codes that indicate errors
     *                            are taken into account
     * @param onlyResponseMetrics
     * @param apiProviderUsername
     * @return List of Metric database IDs, which are the latest per session and
     *         thus relevant for statistics calculation.
     */
    private List<Long> getIDsOfLatestMetricsPerSession(
            Long serviceId,
            String proxyId,
            LocalDate fromDate,
            LocalDate untilDate,
            MetricType metricType,
            boolean onlyHttpErrorCodes,
            boolean onlyResponseMetrics,
            String apiProviderUsername,
            String apiProviderConsumerName) {

        String selectString = "SELECT MAX(metric.id) FROM Metric metric INNER JOIN metric.proxy proxy ";

        String whereString = " WHERE  1 = 1 ";

        String groupByString = " GROUP BY "
                + "metric.proxy, "
                + "metric.apiKey, "
                + "metric.type, "
                + "metric.requestMethod, "
                + "metric.path, "
                + "metric.httpResponseCode, "
                + "metric.metricsCounterSessionID , "
                + "metric.dateOfApiCall";

        if (serviceId != null) {
            whereString = whereString + " AND metric.apiKey.serviceApi.id = :serviceId";
        }

        if (proxyId != null) {
            whereString = whereString + " AND proxy.id = :proxyId";
        }

        if (fromDate != null && untilDate != null) {
            whereString = whereString + " AND metric.dateOfApiCall BETWEEN :fromDate AND :untilDate ";
        }

        if (metricType != null) {
            whereString = whereString + " AND metric.type = :metricType";
        }

        if (onlyHttpErrorCodes) {
            whereString = whereString + " AND metric.httpResponseCode >= 400 ";
        }

        if (onlyResponseMetrics) {
            whereString = whereString + " AND metric.httpResponseCode IS NOT NULL";
        }

        if (apiProviderUsername != null) {
            whereString = whereString + " AND metric.apiKey.serviceApi.owner.username = :apiProviderUsername";
        }

        if (apiProviderConsumerName != null) {
            whereString = whereString + " AND metric.apiKey.user.username = :apiProviderConsumerName";
        }

        Query query = entityManager.createQuery(selectString + whereString + groupByString);

        if (fromDate != null && untilDate != null) {
            query.setParameter("fromDate", Date.valueOf(fromDate));
            query.setParameter("untilDate", Date.valueOf(untilDate));
        }

        if (metricType != null) {
            query.setParameter("metricType", metricType);
        }

        if (serviceId != null) {
            query.setParameter("serviceId", serviceId);
        }

        if (proxyId != null) {
            query.setParameter("proxyId", proxyId);
        }

        if (apiProviderUsername != null) {
            query.setParameter("apiProviderUsername", apiProviderUsername);
        }
        if (apiProviderConsumerName != null) {
            query.setParameter("apiProviderConsumerName", apiProviderConsumerName);
        }

        List result = query.getResultList();
        return result;
    }

    public List getUsageApiConsumer(MetricType metricType, Long serviceApiId, String apiProviderUsername,
            Long apiConsumer, java.util.Date startDate, java.util.Date endDate) {
        return getUsageApiConsumer(metricType, serviceApiId, apiProviderUsername, apiConsumer, startDate, endDate,
                false);
    }

    public List getUsageApiConsumer(MetricType metricType, Long serviceApiId, String apiProviderUsername,
            Long apiConsumer, java.util.Date startDate, java.util.Date endDate, boolean errorFiltering) {

        List result = null;

        List<Long> metricIdList = getIDsOfRelevantMetricsByMetricType(metricType, apiProviderUsername);

        if (!metricIdList.isEmpty()) {

            String select = "SELECT metric.apiKey.user.username, metric.apiKey.serviceApi.id,metric.type, SUM(metric.count) AS noOfCalls ,metric.path,metric.requestMethod  "
                    + " FROM  Metric metric "
                    + " WHERE metric.id IN :idsOfRelevantMetrics";

            select += " AND metric.dateOfApiCall BETWEEN :startDate AND :endDate";

            if (errorFiltering) {
                select += " AND metric.httpResponseCode < 400";
            }

            if (apiConsumer != null && apiConsumer != -1) {
                select += " AND metric.apiKey.user.id = :apiConsumer";
            }

            if (serviceApiId != null && serviceApiId != -1) {
                select += " AND metric.apiKey.serviceApi.id = :serviceId";
            }

            select += " GROUP BY metric.apiKey.user.username, metric.apiKey.serviceApi.id,metric.type,metric.path,metric.path,metric.requestMethod";

            Query query = entityManager.createQuery(select);

            query.setParameter("idsOfRelevantMetrics",
                    getIDsOfRelevantMetricsByMetricType(metricType, apiProviderUsername));

            query.setParameter("startDate", startDate, TemporalType.DATE);
            query.setParameter("endDate", endDate, TemporalType.DATE);

            if (apiConsumer != null && apiConsumer != -1) {
                query.setParameter("apiConsumer", apiConsumer);
            }
            if (serviceApiId != null && serviceApiId != -1) {
                query.setParameter("serviceId", serviceApiId);
            }

            result = query.getResultList();
        }

        return result;
    }

}
