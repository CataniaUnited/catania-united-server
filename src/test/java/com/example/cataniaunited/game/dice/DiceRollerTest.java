package com.example.cataniaunited.game.dice;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DiceRollerTest {

    @Test
    void testRollDiceReturnsValidTotal() {
        DiceRoller diceRoller = new DiceRoller();

        ObjectNode result = diceRoller.rollDice();

        int dice1 = result.get("dice1").asInt();
        int dice2 = result.get("dice2").asInt();
        int total = result.get("total").asInt();

        assertTrue(dice1 >= 1 && dice1 <= 6, "Dice1 value must be between 1 and 6");
        assertTrue(dice2 >= 1 && dice2 <= 6, "Dice2 value must be between 1 and 6");
        assertEquals(dice1 + dice2, total, "Total should equal dice1 + dice2");
    }
}
