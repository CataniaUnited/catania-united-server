package com.example.cataniaunited.exception.ui;

import com.example.cataniaunited.exception.GameException;

public class MissingRequiredStructuresException extends GameException {

    private static final String DEFAULT_MESSAGE = "Please place at least one road and one settlement before ending your turn!";

    public MissingRequiredStructuresException() {
        super(DEFAULT_MESSAGE);
    }
}
