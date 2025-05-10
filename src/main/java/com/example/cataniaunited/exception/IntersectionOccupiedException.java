package com.example.cataniaunited.exception;

public class IntersectionOccupiedException extends GameException {

    private static final String DEFAULT_MESSAGE = "Intersection occupied!";

    public IntersectionOccupiedException() {
        super(DEFAULT_MESSAGE);
    }
}
