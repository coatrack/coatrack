package eu.coatrack.admin.server.repository;

/*-
 * #%L
 * coatrack-config-server
 * %%
 * Copyright (C) 2013 - 2021 Corizon | Institut für angewandte Systemtechnik Bremen GmbH (ATB)
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
import eu.coatrack.config.ConfigServerCredential;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PostFilter;

@RepositoryRestResource(collectionResourceRel = "credentials", path = "credentials")
public interface ConfigServerCredentialRepository extends PagingAndSortingRepository<ConfigServerCredential, Long> {
    
    public ConfigServerCredential findOneByName(String name);

    public ConfigServerCredential findOneByNameAndResource(String name, String resource);

    @Query(value = "Select apiKey from ApiKey apiKey where apiKey.deletedWhen is null")
    @PostFilter("filterObject.user.username != null and filterObject.user.username == authentication.name")
    public List<ApiKey> findByLoggedInAPIConsumer();

}
