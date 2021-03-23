package eu.coatrack.admin.security;

/*-
 * #%L
 * coatrack-admin
 * %%
 * Copyright (C) 2013 - 2020 Corizon | Institut für angewandte Systemtechnik Bremen GmbH (ATB)
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

import eu.coatrack.admin.model.repository.ProxyRepository;
import eu.coatrack.api.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.Assert;

import java.util.Collections;

/**
 * Authentication provider that checks gateway auth tokens, which have been
 * added to the security context by @{@link GatewayAuthFilter}, for their
 * validity.
 * <p>
 * In case the key is valid, the gateway is considered as authenticated and the
 * role YGG_GATEWAY is added to the security context.
 *
 * @author gr-hovest
 */
public class GatewayAuthProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(GatewayAuthProvider.class);

    private static final String YGG_GATEWAY_ROLE_NAME = "ROLE_YGG_GATEWAY";

    @Autowired
    private ProxyRepository proxyRepository;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        if (authentication instanceof GatewayAuthToken) {

            GatewayAuthToken authToken = (GatewayAuthToken) authentication;
            if (isAuthTokenValid(authToken)) {

                // add ROLE_YGG_GATEWAY as granted authority
                GatewayAuthToken yggGatewayAuthToken = new GatewayAuthToken(
                        (String) authToken.getCredentials(), authToken.getUser(), authToken.getPassword(),
                        Collections.singleton(new SimpleGrantedAuthority(YGG_GATEWAY_ROLE_NAME))
                );
                yggGatewayAuthToken.setAuthenticated(true);
                return yggGatewayAuthToken;
            } else {
                throw new BadCredentialsException(
                        String.format("Gateway's auth token '%s' is not valid", authToken));
            }
        } else {
            // This provider is not able to decide about authentication
            return null;
        }
    }

    private boolean isAuthTokenValid(GatewayAuthToken authToken) throws AuthenticationException {

        try {
            Assert.notNull(authToken.getCredentials(), "the gateway's api key value is NULL");
            Assert.isInstanceOf(String.class, authToken.getCredentials());
            String gatewayUUID = (String) authToken.getCredentials();
            Assert.hasText(gatewayUUID, "the gateway's api key value (UUID) does not contain any text");

            log.debug("checking gateway's APIKEY (UUID) value {}", gatewayUUID);

            // search proxy with given api key value, which is equivalent to the proxy's UUID
            Proxy proxy = proxyRepository.findOne(gatewayUUID);

            if (proxy != null) {
                log.debug("Proxy was found by gateway api key verifier: " + proxy);
                if (proxy.getCredentialName().equals(authToken.getUser()) && proxy.getConfigServerPassword().equals(authToken.getPassword())) {
                    return true;
                } else {
                    log.debug("Gateway's Credential doesnt match for apiKey/UUID" + gatewayUUID);
                    return false;
                }

            } else {
                log.debug("Gateway's API key value (UUID) is invalid: " + gatewayUUID);
                return false;
            }
        } catch (IllegalArgumentException e) {
            log.debug("API key parameter missing or invalid: " + e.getMessage());
            throw new BadCredentialsException("API key parameter missing or invalid: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return GatewayAuthToken.class.isAssignableFrom(authentication);
    }
}
