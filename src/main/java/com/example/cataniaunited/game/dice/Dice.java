package com.example.cataniaunited.game.dice;

import org.jboss.logging.Logger;
import java.util.Random;

public class Dice {
    private static final Logger logger = Logger.getLogger(Dice.class);
    private final Random random = new Random();
    private int currentValue;

    public int roll() {
        currentValue = random.nextInt(6) + 1; // 1-6
        logger.infof("Dice rolled: %d", currentValue);
        return currentValue;
    }

    public int getCurrentValue() {
        return currentValue;
    }
}