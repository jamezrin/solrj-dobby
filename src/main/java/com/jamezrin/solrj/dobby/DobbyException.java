package com.jamezrin.solrj.dobby;

/**
 * Unchecked exception thrown when Dobby encounters an error during
 * type conversion, reflection, or configuration.
 */
public class DobbyException extends RuntimeException {

    public DobbyException(String message) {
        super(message);
    }

    public DobbyException(String message, Throwable cause) {
        super(message, cause);
    }
}
