package com.example.cataniaunited.exception.ui;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.Buildable;

public class BuildableLimitReachedException extends GameException {

    private static final String DEFAULT_MESSAGE = "You've reached the %s limit of %s!";

    public BuildableLimitReachedException(Buildable buildable) {
        super(DEFAULT_MESSAGE.formatted(buildable.getClass().getSimpleName(), buildable.getBuildLimit()));
    }
}
