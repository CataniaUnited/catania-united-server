package com.example.cataniaunited.game.dice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DiceTest {
    private Dice dice;

    @BeforeEach
    void setUp() {
        dice = new Dice();
    }

    @Test
    void roll_shouldReturnValueBetween1And6() {
        for (int i = 0; i < 100; i++) {
            int value = dice.roll();
            assertTrue(value >= 1 && value <= 6);
        }
    }

    @Test
    void getCurrentValue_shouldReturnLastRolledValue() {
        dice.roll();
        int value = dice.getCurrentValue();
        assertTrue(value >= 1 && value <= 6);
    }
}