package eu.coatrack.admin.service;

public class ProxyDockerComposeTemplateInitializationFailedException extends RuntimeException {
    public ProxyDockerComposeTemplateInitializationFailedException(Exception cause) {
        super(cause);
    }
}
