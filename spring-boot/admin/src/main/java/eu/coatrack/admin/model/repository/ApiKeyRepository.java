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

import java.util.List;
import eu.coatrack.api.ApiKey;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PostFilter;

/**
 *
 * @author gr-hovest@atb-bremen.de
 */
@RepositoryRestResource(collectionResourceRel = "api-keys", path = "api-keys")
public interface ApiKeyRepository extends PagingAndSortingRepository<ApiKey, Long> {

    @RestResource(path = "findByKeyValue")
    public ApiKey findByKeyValue(@Param("keyValue") String keyValue);

    @RestResource(path = "findByDeletedWhen")
    @Query(value = "Select apiKey from ApiKey apiKey where apiKey.deletedWhen is null")
    @PostFilter("filterObject.serviceApi.owner != null and filterObject.serviceApi.owner.username == authentication.name")
    public List<ApiKey> findAvailable();

    @Query(value = "Select apiKey from ApiKey apiKey where apiKey.deletedWhen is null")
    @PostFilter("filterObject.user.username != null and filterObject.user.username == authentication.name")
    public List<ApiKey> findByLoggedInAPIConsumer();

    @Query(value = "Select apiKey from ApiKey apiKey where apiKey.serviceApi.id = :serviceId")
    @PostFilter("filterObject.user.username != null and filterObject.user.username == authentication.name")
    public List<ApiKey> findByLoggedInAPIConsumerAndServiceId(@Param(value = "serviceId") Long serviceId);

}
