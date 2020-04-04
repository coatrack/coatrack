package eu.coatrack.admin.server.principal;

import java.util.ArrayList;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

/**
 *
 * @author perezdf
 */
public class AdminPrincipal extends AbstractYggPrincipal {

    public AdminPrincipal(String user, String password) {
        super(user, password);

    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        GrantedAuthority grant = new GrantedAuthority() {
            @Override
            public String getAuthority() {

                return "ROLE_ADMIN";
            }
        };
        Collection<GrantedAuthority> grantedCollection = new ArrayList<>();
        grantedCollection.add(grant);
        return grantedCollection;
    }

}
