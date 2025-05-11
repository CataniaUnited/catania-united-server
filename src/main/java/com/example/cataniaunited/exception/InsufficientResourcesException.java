package com.example.cataniaunited.exception;

public class InsufficientResourcesException extends GameException {

    private static final String DEFAULT_MESSAGE = "Insufficient resources!";

    public InsufficientResourcesException() {
        super(DEFAULT_MESSAGE);
    }
}
