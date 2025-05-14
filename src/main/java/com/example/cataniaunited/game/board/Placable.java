package com.example.cataniaunited.game.board;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Interface for game elements that have a position on the game board
 * and can be represented as JSON.
 */
public interface Placable {
    /**
     * Gets the 2D coordinates of the placable item on the game board.
     *
     * @return A double array where index 0 is the x-coordinate and index 1 is the y-coordinate.
     *         Implementations should typically return a clone to prevent external modification.
     */
    double[] getCoordinates();

    /**
     * Converts the placable item's state to a JSON representation. So that it can be
     * shipped to the frontend via a {@link com.example.cataniaunited.dto.MessageDTO MessageDTO} Payload.
     *
     * @return An {@link ObjectNode} representing the item in JSON format.
     */
    ObjectNode toJson();
}