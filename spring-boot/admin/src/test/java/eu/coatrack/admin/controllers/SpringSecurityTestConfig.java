package eu.coatrack.admin.controllers;

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

import java.util.Arrays;

import eu.coatrack.admin.model.repository.DummyClientRegistrationRepository;
import eu.coatrack.admin.model.vo.YggUserPrincipal;
import eu.coatrack.admin.service.YggUserDetailsService;
import eu.coatrack.api.User;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@TestConfiguration
public class SpringSecurityTestConfig {

    @Bean
    @Primary
    public UserDetailsService userDetailsService() {

        User user = new User();
        user.setUsername("aa11aa22-aa33-aa44-aa55-aa66aa77aa88");

        UserDetails basicUser = new YggUserPrincipal(user);

        return new InMemoryUserDetailsManager(Arrays.asList(
                basicUser));
    }

    @Bean
    public ResourceServerProperties resourceServerProperties(){
        return new ResourceServerProperties();
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(){
        return new DummyClientRegistrationRepository();
    }
}
