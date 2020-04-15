package eu.coatrack.admin.server.service;

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

            userDetails = new AdminPrincipal(name, password);
        } else {

            ConfigServerCredential credential = configServerCredentialRepository.findOneByName(username);

            if (credential != null) {

                userDetails = new UserPrincipal(credential.getName(), credential.getPassword());
            }
        }
        return userDetails;
    }
}
