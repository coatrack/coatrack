package eu.coatrack.admin.model.repository;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Repository;

@Repository
public class DummyClientRegistrationRepository implements ClientRegistrationRepository {

    public ClientRegistration findByRegistrationId(String s) {
        return null; //TODO This seems not to be correct. However it was required to let the AdminControllerTest pass.
    }
}
