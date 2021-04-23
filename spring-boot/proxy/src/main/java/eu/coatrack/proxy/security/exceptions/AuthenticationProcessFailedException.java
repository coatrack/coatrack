package eu.coatrack.proxy.security.exceptions;

import org.springframework.security.core.AuthenticationException;

public class AuthenticationProcessFailedException extends AuthenticationException {

    public AuthenticationProcessFailedException(String msg) {
        super(msg);
    }
}
