package com.example.cataniaunited.exception.ui;

import com.example.cataniaunited.exception.GameException;

public class RobberNotPlacedException extends GameException {

    private static final String DEFAULT_MESSAGE = "Robber must be placed before ending turn!";

    public RobberNotPlacedException(){
        super(DEFAULT_MESSAGE);
    }
}
