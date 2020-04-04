package eu.coatrack.admin.logic;

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

import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.api.ServiceApi;
import eu.coatrack.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author perezdf
 */
@Component
public abstract class AbstractServiceAction implements Action {

    private static final Logger log = LoggerFactory.getLogger(AbstractServiceAction.class);

    ///////////////////////////
    //
    //  Repositories
    //
    ///////////////////////////
    @Autowired
    protected ServiceApiRepository serviceApiRepository;

    ///////////////////////////
    //
    //  I/O Parameters
    //
    ///////////////////////////
    protected User user;

    protected ServiceApi serviceApi;

    /**
     * This method assures that the URI identifier is valid and that there are no duplicates
     */
    protected void assureThatServiceApiHasValidUriIdentifier() {

        // start with already existing identifier, if any
        String uriIdentifier = serviceApi.getUriIdentifier();

        if (uriIdentifier == null || uriIdentifier.trim().isEmpty()) {
            // no identifier there --> take the name as basis
            uriIdentifier = serviceApi.getName();
        }

        // replace all non-alphanumeric characters by dashes and transform to lower-case
        uriIdentifier = uriIdentifier
                .trim()
                .toLowerCase()
                .replaceAll("\\W+", "-");

        // get all service APIs by this user and assure that there is no duplicate
        List<ServiceApi> servicesByThisUser = serviceApiRepository.findByOwnerUsername(user.getUsername());
        servicesByThisUser.remove(serviceApi);

        List<String> identifiersAlreadyInUse = servicesByThisUser.stream()
                .map(s -> s.getUriIdentifier())
                .collect(Collectors.toList());

        if (identifiersAlreadyInUse.contains(uriIdentifier)) {
            // the identifier is already in use and thus needs to be modified

            String candidateToCheckForDuplicate = uriIdentifier;
            for (int suffixNumber = 2;
                 identifiersAlreadyInUse.contains(candidateToCheckForDuplicate);
                 suffixNumber++) {
                // append number as suffix
                candidateToCheckForDuplicate = uriIdentifier + "-" + suffixNumber;
            }
            uriIdentifier = candidateToCheckForDuplicate;
        }
        serviceApi.setUriIdentifier(uriIdentifier);
    }

    @Override
    public abstract void execute();

    ///////////////////////////
    //
    //  Getters/Setters
    //
    ///////////////////////////
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ServiceApi getServiceApi() {
        return serviceApi;
    }

    public void setServiceApi(ServiceApi serviceApi) {
        this.serviceApi = serviceApi;
    }

}
