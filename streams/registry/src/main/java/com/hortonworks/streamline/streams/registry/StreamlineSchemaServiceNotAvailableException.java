package com.hortonworks.streamline.streams.registry;

public class StreamlineSchemaServiceNotAvailableException extends RuntimeException {
    public StreamlineSchemaServiceNotAvailableException() {
    }

    public StreamlineSchemaServiceNotAvailableException(String message) {
        super(message);
    }

    public StreamlineSchemaServiceNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public StreamlineSchemaServiceNotAvailableException(Throwable cause) {
        super(cause);
    }

    public StreamlineSchemaServiceNotAvailableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
