/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.coatrack.admin.server.principal;

import org.springframework.security.core.userdetails.UserDetails;

/**
 *
 * @author perezdf
 */
public abstract class AbstractYggPrincipal implements UserDetails {

    private final String user;
    private final String password;

    public AbstractYggPrincipal(String user, String password) {
        this.user = user;
        this.password = password;

    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.user;
    }
}
