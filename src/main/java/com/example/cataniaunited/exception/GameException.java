package com.example.cataniaunited.exception;

public class GameException extends Exception {

    public GameException(String message) {
        super(message);
    }

    public GameException(String message, Object... args){
        super(String.format(message, args));
    }

}
