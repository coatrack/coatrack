package eu.coatrack.admin.server.repository;

import java.util.List;
import eu.coatrack.api.ApiKey;
import eu.coatrack.config.ConfigServerCredential;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PostFilter;

@RepositoryRestResource(collectionResourceRel = "credentials", path = "credentials")
public interface ConfigServerCredentialRepository extends PagingAndSortingRepository<ConfigServerCredential, Long> {
    
    public ConfigServerCredential findOneByName(String name);

    public ConfigServerCredential findOneByNameAndResource(String name, String resource);

    @Query(value = "Select apiKey from ApiKey apiKey where apiKey.deletedWhen is null")
    @PostFilter("filterObject.user.username != null and filterObject.user.username == authentication.name")
    public List<ApiKey> findByLoggedInAPIConsumer();

}
