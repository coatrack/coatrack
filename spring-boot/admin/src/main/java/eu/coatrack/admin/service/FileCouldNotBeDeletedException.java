package eu.coatrack.admin.service;

import java.io.IOException;

public class FileCouldNotBeDeletedException extends IOException {
    public FileCouldNotBeDeletedException(String message) {
        super(message);
    }
}
