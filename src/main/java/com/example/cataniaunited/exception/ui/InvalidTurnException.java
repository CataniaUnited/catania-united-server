package com.example.cataniaunited.exception.ui;

import com.example.cataniaunited.exception.GameException;

public class InvalidTurnException extends GameException {

  private static final String DEFAULT_MESSAGE = "It is not your turn!";

    public InvalidTurnException() {
        super(DEFAULT_MESSAGE);
    }
}
