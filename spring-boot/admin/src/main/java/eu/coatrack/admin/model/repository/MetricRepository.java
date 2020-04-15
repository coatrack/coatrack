package eu.coatrack.admin.model.repository;

/*-
 * #%L
 * ygg-admin
 * %%
 * Copyright (C) 2013 - 2019 Corizon
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

import eu.coatrack.api.ApiKey;
import eu.coatrack.api.Metric;
import eu.coatrack.api.MetricType;
import eu.coatrack.api.Proxy;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Date;
import java.util.List;

/**
 * @author monezz
 */
@RepositoryRestResource(collectionResourceRel = "metrics", path = "metrics")
public interface MetricRepository extends PagingAndSortingRepository<Metric, Long> {

    @Query("select m from Metric m where m.proxy.id = :proxyId")
    public List<Metric> findMetrics(@Param(value = "proxyId") long proxyId);

    @Query("select m from Metric m where m.apiKey.user.username = :username   AND m.dateOfApiCall BETWEEN :from AND :until ")
    public List<Metric> retrieveByUserConsumer(@Param(value = "username") String username , @Param(value = "from") Date from, @Param(value = "until") Date until);
   

    public List<Metric> findByProxyAndTypeAndRequestMethodAndPathAndHttpResponseCodeAndMetricsCounterSessionIDAndDateOfApiCallAndApiKeyOrderByIdDesc(
            Proxy proxy,
            MetricType type,
            String requestMethod,
            String path,
            Integer httpResponseCode,
            String metricsCounterSessionID,
            Date dateOfApiCall,
            ApiKey apiKey
    );

}
