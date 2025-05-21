package com.example.cataniaunited.exception.ui;

import com.example.cataniaunited.exception.GameException;

public class InsufficientResourcesException extends GameException {

    private static final String DEFAULT_MESSAGE = "Insufficient resources!";

    public InsufficientResourcesException() {
        super(DEFAULT_MESSAGE);
    }
}
