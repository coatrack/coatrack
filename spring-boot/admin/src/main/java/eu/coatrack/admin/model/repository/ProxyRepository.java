package eu.coatrack.admin.model.repository;

/*-
 * #%L
 * coatrack-api
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

import eu.coatrack.api.Proxy;
import java.util.List;
import java.util.Optional;

import eu.coatrack.api.ServiceApi;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;

/**
 *
 * @author monezz
 */
@RepositoryRestResource(collectionResourceRel = "proxies", path = "proxies")
public interface ProxyRepository extends PagingAndSortingRepository<Proxy, String> {

    @PostFilter("filterObject.owner != null and filterObject.owner.username == authentication.name")
    List<Proxy> findByName(@Param("name") String name);

    /*
    * PostAuthorize rule prevents unauthorized access to complete proxy info.
    * Access will only be allowed if the calls were made:
    * - either by the proxy with that same id (via API)
    * - or by the owner of the proxy (via GUI)
    */
    @PostAuthorize("#id == authentication.name or returnObject.get().owner.username == authentication.name")
    Optional<Proxy> findById(@Param("id") String id);

    @PostFilter("filterObject.owner != null and filterObject.owner.username == authentication.name")
    @Query("SELECT proxy FROM Proxy proxy JOIN proxy.serviceApis serviceApi WHERE :serviceApiId = serviceApi.id")
    Iterable<Proxy> customSearchByServiceApiId(@Param("serviceApiId") long serviceApi);

    @Query("SELECT proxy FROM Proxy proxy JOIN proxy.serviceApis serviceApi WHERE :serviceApiId = serviceApi.id")
    List<Proxy> customSearchForAllProxiesForGivenServiceApiId(@Param("serviceApiId") long serviceApi);

    @PostFilter("filterObject.owner != null and filterObject.owner.username == authentication.name")
    @RestResource(path = "findByDeletedWhen")
    @Query("SELECT proxy FROM Proxy proxy where proxy.deletedWhen is null")
    public List<Proxy> findAvailable();

}
