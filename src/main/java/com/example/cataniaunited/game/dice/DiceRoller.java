package com.example.cataniaunited.game.dice;

import com.example.cataniaunited.Publisher;
import com.example.cataniaunited.Subscriber;
import com.example.cataniaunited.exception.GameException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;
import java.util.ArrayList;
import java.util.List;

public class DiceRoller implements Publisher<DiceRoller, Integer> {
    private static final Logger logger = Logger.getLogger(DiceRoller.class);
    private final List<Subscriber<DiceRoller, Integer>> subscribers = new ArrayList<>();
    private final Dice dice1 = new Dice();
    private final Dice dice2 = new Dice();

    public ObjectNode rollDice() {
        int dice1Value = dice1.roll();
        int dice2Value = dice2.roll();
        int total = dice1Value + dice2Value;

        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("dice1", dice1Value);
        result.put("dice2", dice2Value);
        result.put("total", total);

        logger.infof("Dice rolled: dice1=%d, dice2=%d, total=%d",
                dice1Value, dice2Value, total);

        notifySubscribers(total);
        return result;
    }

    @Override
    public void addSubscriber(Subscriber<DiceRoller, Integer> subscriber) {
        subscribers.add(subscriber);
    }

    @Override
    public void removeSubscriber(Subscriber<DiceRoller, Integer> subscriber) {
        subscribers.remove(subscriber);
    }

    @Override
    public void notifySubscribers(Integer total) {
        subscribers.forEach(sub -> sub.update(total));
    }
}