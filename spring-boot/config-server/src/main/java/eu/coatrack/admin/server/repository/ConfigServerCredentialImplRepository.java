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

import java.util.UUID;
import eu.coatrack.config.ConfigServerCredential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.hateoas.MediaTypes;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author perezdf
 */
@RepositoryRestController
public class ConfigServerCredentialImplRepository {

    @Autowired
    ConfigServerCredentialRepository repository;

    @RequestMapping(method = RequestMethod.POST, path = "/credentials", produces = MediaTypes.HAL_JSON_VALUE)
    @ResponseBody
    public ConfigServerCredential save(@RequestBody ConfigServerCredential s) {

        s.setName(UUID.randomUUID().toString());
        return repository.save(s);

    }
}
