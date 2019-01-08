package com.telen.sdk.ble.exceptions;

public class ConnectionNotEstablished extends Exception {
    private static final String TAG = ConnectionNotEstablished.class.getSimpleName();

    public ConnectionNotEstablished(String message) {
        super(message);
    }
}
