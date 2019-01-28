package com.hortonworks.streamline.streams.registry.table;

public class DeployTableException extends Exception {
    public DeployTableException() {
    }

    public DeployTableException(String message) {
        super(message);
    }

    public DeployTableException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeployTableException(Throwable cause) {
        super(cause);
    }

    public DeployTableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
