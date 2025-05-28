package com.example.cataniaunited.game.board.ports;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Represents a 2D transformation, including position (x, y) and rotation.
 * Rotation is typically in radians.
 * This is an immutable data carrier.
 * For more explanation see docs of any game Engine
 */
public record Transform(double x, double y, double rotation) {
    public static final Transform ORIGIN = new Transform(0, 0, 0);

    /**
     * Gets the 2D coordinates (x, y) as a double array.
     * @return A new double array [x, y].
     */
    public double[] getCoordinatesArray() {
        return new double[]{x, y};
    }

    /**
     * Converts this Transform to a JSON representation.
     * @param nodeFactory The JsonNodeFactory to use for creating JSON nodes.
     * @return An ObjectNode representing this transform.
     */
    public ObjectNode toJson(JsonNodeFactory nodeFactory) {
        ObjectNode node = nodeFactory.objectNode();
        node.put("x", x); // Accessing fields directly (or via implicit x() getter)
        node.put("y", y);
        node.put("rotation", rotation);
        return node;
    }
}