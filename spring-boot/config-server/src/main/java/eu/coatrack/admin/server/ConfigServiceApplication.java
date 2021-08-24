package eu.coatrack.admin.server;

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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableConfigServer
@SpringBootApplication
@EnableJpaRepositories("eu.coatrack.admin.server.repository")
@EntityScan(basePackages = { "eu.coatrack.api", "eu.coatrack.config" })
@ComponentScan(basePackages = { "eu.coatrack.admin.server.principal.entrypoint", "eu.coatrack.admin.server.service",
        "eu.coatrack.admin.server", "eu.coatrack.admin.server.config", "eu.coatrack.admin.server.service.filter" })
public class ConfigServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }
}
