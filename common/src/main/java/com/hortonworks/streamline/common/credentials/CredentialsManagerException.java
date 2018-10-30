package com.hortonworks.streamline.common.credentials;

/**
 * Custom Exception thrown by retrieving credentials
 */
public class CredentialsManagerException extends RuntimeException {

    public CredentialsManagerException() {
    }

    /**
     * Cosntructor with message
     * @param message
     */
    public CredentialsManagerException(String message) {
        super(message);
    }

    /**
     * Constructor with message and throwable
     * @param message
     * @param cause
     */
    public CredentialsManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with throwable
     * @param cause
     */
    public CredentialsManagerException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with all params
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public CredentialsManagerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
