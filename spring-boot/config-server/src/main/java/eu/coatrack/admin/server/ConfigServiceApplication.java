package eu.coatrack.admin.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableConfigServer
@SpringBootApplication
@EnableJpaRepositories("eu.coatrack.admin.server.repository")
@EntityScan(basePackages = {"eu.coatrack.api", "eu.coatrack.config"})
@ComponentScan(basePackages = {"eu.coatrack.admin.server.principal.entrypoint", "eu.coatrack.admin.server.service", "eu.coatrack.admin.server", "eu.coatrack.admin.server.config", "eu.coatrack.admin.server.service.filter"})
public class ConfigServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }
}
