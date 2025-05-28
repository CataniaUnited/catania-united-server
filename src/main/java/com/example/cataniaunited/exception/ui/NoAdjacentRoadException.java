package com.example.cataniaunited.exception.ui;

import com.example.cataniaunited.exception.GameException;

public class NoAdjacentRoadException extends GameException {

    private static final String DEFAULT_MESSAGE = "No adjacent roads found";

    public NoAdjacentRoadException() {
        super(DEFAULT_MESSAGE);
    }
}
