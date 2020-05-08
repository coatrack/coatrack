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

import java.util.Date;
import java.util.List;

import eu.coatrack.api.ApiKey;
import eu.coatrack.api.ServiceApi;
import eu.coatrack.api.ServiceAccessPermissionPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PostFilter;

/**
 * @author gr-hovest(at)atb-bremen.de
 * @author silva@atb-bremen.de
 */
@RepositoryRestResource(collectionResourceRel = "services", path = "services")
public interface ServiceApiRepository extends PagingAndSortingRepository<ServiceApi, Long> {

    @PostFilter("filterObject.owner != null and filterObject.owner.username == authentication.name")
    @Override
    public Iterable<ServiceApi> findAll(Sort sort);

    @PostFilter("filterObject.owner != null and filterObject.owner.username == authentication.name")
    @Override
    public Page<ServiceApi> findAll(Pageable pgbl);

    @Query("SELECT serviceApi FROM ApiKey apiKey INNER JOIN apiKey.serviceApi serviceApi WHERE apiKey.keyValue = :apiKeyValue")
    public ServiceApi findByApiKeyValue(@Param("apiKeyValue") String apiKeyValue);

    @RestResource(path = "findByDeletedWhen")
    @PostFilter("filterObject.owner != null and filterObject.owner.username == authentication.name")
    public List<ServiceApi> findByDeletedWhen(@Param("seletedWhen") Date deletedWhen);

    @Query("select s from ServiceApi s where s.owner.username = :username")
    public List<ServiceApi> findByOwnerUsername(@Param(value = "username") String username);

    public List<ServiceApi> findByServiceAccessPermissionPolicyAndDeletedWhenIsNull(ServiceAccessPermissionPolicy accessPolicy);

    @Query("SELECT serviceApi FROM ApiKey apiKey INNER JOIN apiKey.serviceApi serviceApi WHERE apiKey IN :apiKeyList")
    public List<ServiceApi> findByApiKeyList(@Param("apiKeyList") List<ApiKey> apiKeyList);

    @Query("SELECT s FROM ServiceApi s WHERE s.uriIdentifier = :uriIdentifier AND s.owner.username = :serviceOwner")
    public ServiceApi findServiceApiByServiceOwnerAndUriIdentifier(@Param("serviceOwner") String serviceOwner, @Param("uriIdentifier") String uriIdentifier);
}
