package com.example.cataniaunited.exception.ui;

import com.example.cataniaunited.exception.GameException;

public class ResourcesNotDiscardedException extends GameException {

    private static final String DEFAULT_MESSAGE = "One or more players must discard resources before ending turn!";
    public ResourcesNotDiscardedException(){
        super(DEFAULT_MESSAGE);
    }
}
