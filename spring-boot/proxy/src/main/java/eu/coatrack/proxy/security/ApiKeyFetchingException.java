package eu.coatrack.proxy.security;

import java.net.ConnectException;

class ApiKeyFetchingException extends ConnectException {

    public ApiKeyFetchingException(String msg) {
        super(msg);
    }

    public ApiKeyFetchingException() {

    }
}