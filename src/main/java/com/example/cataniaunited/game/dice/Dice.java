package com.example.cataniaunited.game.dice;

import com.example.cataniaunited.Publisher;
import com.example.cataniaunited.Subscriber;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Dice implements Publisher<Dice, Integer> {
    private static final Logger logger = Logger.getLogger(Dice.class);
    private final List<Subscriber<Dice, Integer>> subscribers = new ArrayList<>();
    private final Random random = new Random();
    private int currentValue;

    public void roll() {
        currentValue = random.nextInt(6) + 1; // 1-6
        logger.infof("Dice rolled: %d", currentValue);
        notifySubscribers(currentValue);
    }

    public int getCurrentValue() {
        return currentValue;
    }

    @Override
    public void addSubscriber(Subscriber<Dice, Integer> subscriber) {
        subscribers.add(subscriber);
    }

    @Override
    public void removeSubscriber(Subscriber<Dice, Integer> subscriber) {
        subscribers.remove(subscriber);
    }

    @Override
    public void notifySubscribers(Integer value) {
        subscribers.forEach(sub -> sub.update(value));
    }
}