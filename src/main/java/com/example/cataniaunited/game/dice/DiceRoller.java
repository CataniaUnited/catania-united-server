package com.example.cataniaunited.game.dice;

import com.example.cataniaunited.Publisher;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.logging.Log;
import org.jboss.logging.Logger;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the rolling of two dice for the Catan game.
 * It acts as a {@link Publisher} to notify subscribed {@link Tile}s
 * about the total result of the dice roll.
 */
public class DiceRoller implements Publisher<Tile, Integer> {
    private static final Logger logger = Logger.getLogger(DiceRoller.class);
    private final List<Tile> subscribers = new ArrayList<>();
    private final Dice dice1 = new Dice();
    private final Dice dice2 = new Dice();

    /**
     * Rolls both dice, calculates the total, and notifies all subscribed tiles.
     *
     * @return An {@link ObjectNode} containing the individual dice values and their total.
     *         Example: {"dice1": 3, "dice2": 4, "total": 7}
     */
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

    /**
     * Adds a {@link Tile} as a subscriber to this DiceRoller.
     * Subscribed tiles will be notified of dice roll results.
     *
     * @param subscriber The {@link Tile} to add.
     */
    @Override
    public void addSubscriber(Tile subscriber) {
        if(!isSubscribed(subscriber)){
            subscribers.add(subscriber);
        }
        Log.debug("Tile " + subscriber.getId() + "subscribed.");
    }

    /**
     * Removes a {@link Tile} from this DiceRoller's list of subscribers.
     *
     * @param subscriber The {@link Tile} to remove.
     */
    @Override
    public void removeSubscriber(Tile subscriber) {
        subscribers.remove(subscriber);
        Log.debug("Tile " + subscriber.getId() + "not subscribed anymore.");
    }

    /**
     * Notifies all subscribed {@link Tile}s about the total dice roll.
     *
     * @param total The total value of the two dice rolled.
     */
    @Override
    public void notifySubscribers(Integer total) {
        subscribers.forEach(s -> s.update(total));
    }

    public boolean isSubscribed(Tile tile) {
        return subscribers.contains(tile);
    }
}