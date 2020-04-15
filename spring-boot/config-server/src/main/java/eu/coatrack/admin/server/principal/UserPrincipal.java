/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.coatrack.admin.server.principal;

import java.util.ArrayList;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

/**
 *
 * @author perezdf
 */
public class UserPrincipal extends AbstractYggPrincipal {

    public UserPrincipal(String user, String password) {
        super(user, password);

    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        GrantedAuthority grant = new GrantedAuthority() {
            @Override
            public String getAuthority() {

                return "ROLE_USER";
            }
        };
        Collection<GrantedAuthority> grantedCollection = new ArrayList<>();
        grantedCollection.add(grant);
        return grantedCollection;
    }

}
