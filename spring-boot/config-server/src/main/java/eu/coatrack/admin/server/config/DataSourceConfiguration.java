package eu.coatrack.admin.server.config;

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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfiguration {

    @Primary
    @Bean
    public DataSource getDataSource(
            @Qualifier("first") DataSourceProperties first,
            @Qualifier("second") DataSourceProperties second)
    {
        final DataSource firstDataSource = first.initializeDataSourceBuilder().build();
        final DataSource secondDataSource = second.initializeDataSourceBuilder().build();
        try
        {
            firstDataSource.getConnection();
            return firstDataSource;
        }
        catch (Exception e)
        {
            return secondDataSource;
        }
    }

    @Primary
    @Bean("first")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties primaryDataSource()
    {
        final DataSourceProperties dataSourceProperties = new DataSourceProperties();
        dataSourceProperties.setUrl("jdbc:postgresql://localhost/ygg_config_server");
        dataSourceProperties.setDriverClassName("org.postgresql.Driver");
        dataSourceProperties.setUsername("postgres");
        dataSourceProperties.setPassword("postgres1234");
        return dataSourceProperties;
    }

    @Bean("second")
    public DataSourceProperties secondaryDataSource() {
        final DataSourceProperties dataSourceProperties = new DataSourceProperties();
        dataSourceProperties.setUrl("jdbc:h2:mem:testdb");
        dataSourceProperties.setDriverClassName("org.h2.Driver");
        dataSourceProperties.setUsername("root");
        dataSourceProperties.setPassword("root");
        return dataSourceProperties;
    }

}
