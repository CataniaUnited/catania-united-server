package com.example.cataniaunited.game.dice;

import com.example.cataniaunited.exception.GameException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DiceRollerTest {

    private DiceRoller diceRoller;

    @BeforeEach
    void setUp() {
        diceRoller = new DiceRoller();
    }

    @Test
    void testRollDiceReturnsValidValues() throws GameException {
        ObjectNode result = diceRoller.rollDice();

        int dice1 = result.get("dice1").asInt();
        int dice2 = result.get("dice2").asInt();
        int total = result.get("total").asInt();

        assertTrue(dice1 >= 1 && dice1 <= 6, "Dice1 value should be between 1 and 6");
        assertTrue(dice2 >= 1 && dice2 <= 6, "Dice2 value should be between 1 and 6");
        assertEquals(dice1 + dice2, total, "Total should be the sum of dice1 and dice2");
    }

    @Test
    void testInternalStateAfterRoll() throws GameException {
        diceRoller.rollDice();
        int val1 = diceRoller.getDice1Value();
        int val2 = diceRoller.getDice2Value();

        assertTrue(val1 >= 1 && val1 <= 6, "dice1Value should be between 1 and 6");
        assertTrue(val2 >= 1 && val2 <= 6, "dice2Value should be between 1 and 6");
    }

    @Test
    void testGetDiceObjectsNotNull() {
        assertNotNull(diceRoller.getDice1(), "getDice1 should not return null");
        assertNotNull(diceRoller.getDice2(), "getDice2 should not return null");
    }
}