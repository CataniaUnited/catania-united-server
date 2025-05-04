package com.example.cataniaunited.game.dice;

import com.example.cataniaunited.Subscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DiceTest {

    private Dice dice;

    @Mock
    private Subscriber<Dice, Integer> subscriber;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dice = new Dice();
    }

    @Test
    void testRoll() {
        dice.roll();
        int value = dice.getCurrentValue();
        assertTrue(value >= 1 && value <= 6);
    }

    @Test
    void testNotifySubscribers() {
        dice.addSubscriber(subscriber);
        dice.notifySubscribers(5);
        verify(subscriber).update(5);
    }

    @Test
    void testRemoveSubscriber() {
        dice.addSubscriber(subscriber);
        dice.removeSubscriber(subscriber);
        dice.notifySubscribers(5);
        verify(subscriber, never()).update(5);
    }

    @Test
    void testGetCurrentValue() {
        dice.roll();
        assertTrue(dice.getCurrentValue() >= 1 && dice.getCurrentValue() <= 6);
    }
}