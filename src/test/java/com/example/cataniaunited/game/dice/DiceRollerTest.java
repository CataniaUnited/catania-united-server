package com.example.cataniaunited.game.dice;

import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class DiceRollerTest {

    private DiceRoller diceRoller;

    @BeforeEach
    void setUp() {
        diceRoller = new DiceRoller();
    }

    @Test
    void testRollDiceReturnsValidTotal() {
        DiceRoller methodDiceRoller = new DiceRoller();

        ObjectNode result = methodDiceRoller.rollDice();

        int dice1 = result.get("dice1").asInt();
        int dice2 = result.get("dice2").asInt();
        int total = result.get("total").asInt();

        assertTrue(dice1 >= 1 && dice1 <= 6, "Dice1 value must be between 1 and 6");
        assertTrue(dice2 >= 1 && dice2 <= 6, "Dice2 value must be between 1 and 6");
        assertEquals(dice1 + dice2, total, "Total should equal dice1 + dice2");
    }

    @Test
    void testRollDiceMultipleTimesProducesValidResults() {
        for (int i = 0; i < 100; i++) {
            ObjectNode result = diceRoller.rollDice();
            int dice1 = result.get("dice1").asInt();
            int dice2 = result.get("dice2").asInt();
            int total = result.get("total").asInt();

            assertTrue(dice1 >= 1 && dice1 <= 6, "Dice1 on iteration " + i + " should be between 1 and 6. Was: " + dice1);
            assertTrue(dice2 >= 1 && dice2 <= 6, "Dice2 on iteration " + i + " should be between 1 and 6. Was: " + dice2);
            assertEquals(dice1 + dice2, total, "Total on iteration " + i + " should equal dice1 + dice2.");
            assertTrue(total >= 2 && total <= 12, "Total on iteration " + i + " should be between 2 and 12. Was: " + total);
        }
    }

    @Test
    void testAddSubscriberIncreasesSubscriberCountAndNotifiesNewSubscriber() {
        Tile mockTileSubscriber = Mockito.mock(Tile.class);

        diceRoller.rollDice();
        verify(mockTileSubscriber, never()).update(anyInt());

        diceRoller.addSubscriber(mockTileSubscriber);

        ObjectNode result = diceRoller.rollDice();
        int total = result.get("total").asInt();

        verify(mockTileSubscriber, times(1)).update(total);
    }

    @Test
    void testRemoveSubscriberDecreasesSubscriberCountAndStopsNotifyingRemovedSubscriber() {
        Tile mockTileSubscriber1 = Mockito.mock(Tile.class);
        Tile mockTileSubscriber2 = Mockito.mock(Tile.class);

        diceRoller.addSubscriber(mockTileSubscriber1);
        diceRoller.addSubscriber(mockTileSubscriber2);

        ObjectNode result1 = diceRoller.rollDice();
        int total1 = result1.get("total").asInt();
        verify(mockTileSubscriber1, times(1)).update(total1);
        verify(mockTileSubscriber2, times(1)).update(total1);

        diceRoller.removeSubscriber(mockTileSubscriber1);

        ObjectNode result2 = diceRoller.rollDice();
        int total2 = result2.get("total").asInt();

        verify(mockTileSubscriber1, times(1)).update(anyInt());
        verify(mockTileSubscriber1, times(1)).update(total1);
        verify(mockTileSubscriber1, never()).update(total2);

        verify(mockTileSubscriber2, times(1)).update(total2);
        verify(mockTileSubscriber2, times(2)).update(anyInt());
    }

    @Test
    void testNotifySubscribersCallsUpdateOnAllRegisteredSubscribers() {
        Tile mockTile1 = Mockito.mock(Tile.class);
        Tile mockTile2 = Mockito.mock(Tile.class);
        Tile mockTile3 = Mockito.mock(Tile.class);

        diceRoller.addSubscriber(mockTile1);
        diceRoller.addSubscriber(mockTile2);
        diceRoller.addSubscriber(mockTile3);

        ObjectNode result = diceRoller.rollDice();
        int total = result.get("total").asInt();

        verify(mockTile1, times(1)).update(total);
        verify(mockTile2, times(1)).update(total);
        verify(mockTile3, times(1)).update(total);
    }

    @Test
    void testNotifySubscribersWithNoSubscribersDoesNotThrowException() {
        assertDoesNotThrow(() -> {
            diceRoller.rollDice();
        }, "Rolling dice with no subscribers should not throw an exception.");
    }

    @Test
    void testRemoveNonExistentSubscriberDoesNotThrowExceptionAndDoesNotAffectOthers() {
        Tile mockTile1 = Mockito.mock(Tile.class);
        Tile nonExistentMockTile = Mockito.mock(Tile.class);

        diceRoller.addSubscriber(mockTile1);

        assertDoesNotThrow(() -> diceRoller.removeSubscriber(nonExistentMockTile), "Removing a non-existent subscriber should not throw an exception.");

        ObjectNode result = diceRoller.rollDice();
        int total = result.get("total").asInt();
        verify(mockTile1, times(1)).update(total);
    }

    @Test
    void testAddSameSubscriberMultipleTimesOnlyAddsItOnceForNotifications() {
        Tile mockTile = Mockito.mock(Tile.class);

        diceRoller.addSubscriber(mockTile);
        diceRoller.addSubscriber(mockTile);

        ObjectNode result = diceRoller.rollDice();
        int total = result.get("total").asInt();

        verify(mockTile, times(2)).update(total);
    }
}
