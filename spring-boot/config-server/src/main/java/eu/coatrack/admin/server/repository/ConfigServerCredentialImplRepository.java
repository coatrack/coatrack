package eu.coatrack.admin.server.repository;

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
