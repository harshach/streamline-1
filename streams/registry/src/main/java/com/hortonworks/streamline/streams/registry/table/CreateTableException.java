package com.hortonworks.streamline.streams.registry.table;

public class CreateTableException extends Exception {
    public CreateTableException() {
    }

    public CreateTableException(String message) {
        super(message);
    }

    public CreateTableException(String message, Throwable cause) {
        super(message, cause);
    }

    public CreateTableException(Throwable cause) {
        super(cause);
    }

    public CreateTableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
