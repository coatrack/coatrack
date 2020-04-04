package eu.coatrack.admin.server.config;

import eu.coatrack.config.ConfigServerCredential;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;

/**
 *
 * @author perezdf
 */
@Configuration
public class AppRepositoryRestMvcConfiguration extends RepositoryRestMvcConfiguration {

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        config().exposeIdsFor(ConfigServerCredential.class);

    }

}
