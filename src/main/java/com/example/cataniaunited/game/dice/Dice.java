package com.example.cataniaunited.game.dice;

import org.jboss.logging.Logger;
import java.util.Random;

/**
 * Represents a single six-sided die.
 * Provides functionality to roll the die and get its current value.
 */
public class Dice {
    private static final Logger logger = Logger.getLogger(Dice.class);
    private final Random random = new Random();
    private int currentValue;

    /**
     * Rolls the die, generating a random value between 1 and 6 (inclusive).
     * The result is stored as the current value of the die.
     *
     * @return The outcome of the roll (an integer from 1 to 6).
     */
    public int roll() {
        currentValue = random.nextInt(6) + 1; // 1-6
        logger.infof("Dice rolled: %d", currentValue);
        return currentValue;
    }

    /**
     * Gets the current value of the die (the result of the last roll).
     *
     * @return The current value of the die.
     */
    public int getCurrentValue() {
        return currentValue;
    }
}