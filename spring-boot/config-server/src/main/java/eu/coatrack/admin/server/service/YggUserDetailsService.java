package eu.coatrack.admin.server.service;

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

import eu.coatrack.admin.server.repository.ConfigServerCredentialRepository;
import eu.coatrack.admin.server.principal.AdminPrincipal;
import eu.coatrack.admin.server.principal.UserPrincipal;
import eu.coatrack.config.ConfigServerCredential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/**
 *
 * @author perezdf
 */
@Service("userDetailsService")
public class YggUserDetailsService implements UserDetailsService {

    @Autowired
    ConfigServerCredentialRepository configServerCredentialRepository;

    @Value("${ygg.admin.config.access.user.name}")
    String name;

    @Value("${ygg.admin.config.access.user.password}")
    String password;

    @Override
    public UserDetails loadUserByUsername(String username) {

        UserDetails userDetails = null;

        if (username.equals(name)) {
            // The new password format requires to specify a password encoder. {noop} means, that no password
            // encoder shall be used. Therefore, the password is stored in clear text. See the docs:
            // https://spring.io/blog/2017/11/01/spring-security-5-0-0-rc1-released#password-storage-format
            userDetails = new AdminPrincipal(name, "{noop}" + password);
        } else {

            ConfigServerCredential credential = configServerCredentialRepository.findOneByName(username);

            if (credential != null) {

                userDetails = new UserPrincipal(credential.getName(), "{noop}" + credential.getPassword());
            }
        }
        return userDetails;
    }
}
