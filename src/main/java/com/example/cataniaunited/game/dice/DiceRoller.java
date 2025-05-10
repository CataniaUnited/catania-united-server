package com.example.cataniaunited.game.dice;

import com.example.cataniaunited.Publisher;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;
import java.util.ArrayList;
import java.util.List;

public class DiceRoller implements Publisher<Tile, Integer> {
    private static final Logger logger = Logger.getLogger(DiceRoller.class);
    private final List<Tile> subscribers = new ArrayList<>();
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
    public void addSubscriber(Tile subscriber) {
        subscribers.add(subscriber);
    }

    @Override
    public void removeSubscriber(Tile subscriber) {
        subscribers.remove(subscriber);
    }

    @Override
    public void notifySubscribers(Integer total) {
        subscribers.forEach(s -> s.update(total));
    }
}