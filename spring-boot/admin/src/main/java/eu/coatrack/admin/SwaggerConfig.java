package eu.coatrack.admin;

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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
public class SwaggerConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage(("eu.coatrack.admin.controllers")))
                .paths(PathSelectors.ant("/public-api/**"))
                .build()
                .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
                "CoatRack Public API",
                "This CoatRack Public API enables the BAE/marketplace to do the following:\n" +
                        "   \u2022 Get information about the service(s) that a specific user created in CoatRack, so that this user can create an offer for that service in the marketplace\n"
                        +
                        "   \u2022 Inform CoatRack about the acquisition of a specific service by a specific user via the marketplace, so that CoatRack will give this user permission to access the service\n"
                        +
                        "   \u2022 Get service usage statistics from CoatRack, so that the marketplace can do billing/accounting for pay-per-use services\n",
                null,
                null,
                null, "Apache License, Version 2.0", "https://www.apache.org/licenses/LICENSE-2.0");
    }
}
