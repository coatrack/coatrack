package eu.coatrack.admin.controllers;

public class ProxyDockerComposeTemplateInitializationFailedException extends RuntimeException {
    public ProxyDockerComposeTemplateInitializationFailedException(Exception cause) {
        super(cause);
    }
}
