package eu.coatrack.admin.logic;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author perezdf
 */
@Component
public class CreateServiceAction extends AbstractServiceAction implements Action {

    private static final Logger log = LoggerFactory.getLogger(CreateServiceAction.class);

    @Override
    public void execute() {

        assureThatServiceApiHasValidUriIdentifier();

        serviceApi.setOwner(user);

        serviceApi = serviceApiRepository.save(serviceApi);
    }

}
