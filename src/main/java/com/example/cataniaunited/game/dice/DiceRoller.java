package com.example.cataniaunited.game.dice;

import com.example.cataniaunited.exception.GameException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DiceRoller {
    private static final Logger logger = Logger.getLogger(DiceRoller.class);
    private final Dice dice1 = new Dice();
    private final Dice dice2 = new Dice();
    private int dice1Value = 0;
    private int dice2Value = 0;

    public ObjectNode rollDice() throws GameException {
        dice1.roll();
        dice2.roll();

        this.dice1Value = dice1.getCurrentValue();
        this.dice2Value = dice2.getCurrentValue();

        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("dice1", getDice1Value());
        result.put("dice2", getDice2Value());
        result.put("total", getDice1Value() + getDice2Value());

        logger.infof("Dice rolled: dice1=%d, dice2=%d, total=%d",
                dice1Value, dice2Value, dice1Value + dice2Value);

        return result;
    }

    public Dice getDice1() {
        return dice1;
    }

    public Dice getDice2() {
        return dice2;
    }

    public int getDice1Value() {
        return dice1Value;
    }

    public int getDice2Value() {
        return dice2Value;
    }
}