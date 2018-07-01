package com.telen.ble.manager.exceptions;

public class CommandTimeoutException extends Exception {
    public CommandTimeoutException(String message) {
        super(message);
    }
}
