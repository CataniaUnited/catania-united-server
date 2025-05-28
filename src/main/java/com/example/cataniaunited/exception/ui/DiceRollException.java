package com.example.cataniaunited.exception.ui;

import com.example.cataniaunited.exception.GameException;

public class DiceRollException extends GameException {

    private static final String DEFAULT_MESSAGE = "Dice may only be rolled once per turn!";

    public DiceRollException() {
        super(DEFAULT_MESSAGE);
    }
}
