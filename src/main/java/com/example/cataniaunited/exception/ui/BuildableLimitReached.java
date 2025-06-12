package com.example.cataniaunited.exception.ui;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.Buildable;

public class BuildableLimitReached extends GameException {

    private static final String DEFAULT_MESSAGE = "You've reached the %s limit!";

    public BuildableLimitReached(Class<? extends Buildable> buildable) {
        super(DEFAULT_MESSAGE.formatted(buildable.getSimpleName()));
    }
}
