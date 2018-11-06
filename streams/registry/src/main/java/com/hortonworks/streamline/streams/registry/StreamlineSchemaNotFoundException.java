package com.hortonworks.streamline.streams.registry;

public class StreamlineSchemaNotFoundException extends Exception {
    public StreamlineSchemaNotFoundException() {
    }

    public StreamlineSchemaNotFoundException(String message) {
        super(message);
    }

    public StreamlineSchemaNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public StreamlineSchemaNotFoundException(Throwable cause) {
        super(cause);
    }

    public StreamlineSchemaNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
