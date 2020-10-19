package com.kount.authnode;

//TODO Why is DevicePersistenceException in this repository?
public class DevicePersistenceException extends Exception {

    /**
     * Creates a new exception object with the provided message.
     *
     * @param message The exception message.
     */
    public DevicePersistenceException(String message) {
        super(message);
    }

    /**
     * Creates a new exception object with the provided message and cause.
     *
     * @param message The exception message.
     * @param cause The cause of this exception.
     */
    public DevicePersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
