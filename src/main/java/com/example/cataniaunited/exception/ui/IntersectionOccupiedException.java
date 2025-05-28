package com.example.cataniaunited.exception.ui;

import com.example.cataniaunited.exception.GameException;

public class IntersectionOccupiedException extends GameException {

    private static final String DEFAULT_MESSAGE = "Intersection occupied!";

    public IntersectionOccupiedException() {
        super(DEFAULT_MESSAGE);
    }
}
