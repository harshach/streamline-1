package com.hortonworks.streamline.streams.security;

public class UserDoesnotExistException extends RuntimeException {
    public UserDoesnotExistException(String message) {
        super(message);
    }

    public UserDoesnotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
