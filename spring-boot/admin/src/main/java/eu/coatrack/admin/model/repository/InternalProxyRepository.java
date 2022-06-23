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
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * This is an additional repository for internal use, without using filter method security like other repositories
 * would do for external access.
 * This method can be used within the Spring Security method "authenticate()" without causing a recursive
 * authentication call which could lead to a StackOverflowException.
 * @author Christoph Baier
 */

public interface InternalProxyRepository extends PagingAndSortingRepository<Proxy, String> {

    Optional<Proxy> findById(@Param("id") String id);

}
