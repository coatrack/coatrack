package eu.coatrack.proxy.security.exceptions;

import org.springframework.security.core.AuthenticationException;

/**
 * Is thrown when an incoming API key could not be matched with any API key of the local API key list.
 */
public class ApiKeyNotFoundInLocalApiKeyListException extends AuthenticationException {
    public ApiKeyNotFoundInLocalApiKeyListException(String message) {
        super(message);
    }
}
