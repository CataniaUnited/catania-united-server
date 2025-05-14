package com.example.cataniaunited.game.board.tile_list_builder;

import com.example.cataniaunited.Publisher;
import com.example.cataniaunited.Subscriber;
import com.example.cataniaunited.game.board.Placable;
import com.example.cataniaunited.game.board.SettlementPosition;
import com.example.cataniaunited.game.dice.DiceRoller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * A hexagonal tile on the Catan game board.
 * Each tile has a type (resource or waste), a production value (dice roll number),
 * coordinates, and an ID. It also acts as a {@link Publisher} to notify
 * adjacent {@link SettlementPosition}s about resource production when Notified via
 * The DiceRoller to which it acts as a {@link Subscriber}.
 */
public class Tile implements Placable, Publisher<SettlementPosition, TileType>, Subscriber<Integer> {
    final TileType type;
    int value = 0; // Production number (dice roll), 0 for WASTE or if not yet set

    double[] coordinates = new double[2];

    int id;

    List<SettlementPosition> settlementsOfTile = new ArrayList<>(6);

    /**
     * Constructs a new Tile with a specified type.
     *
     * @param type The {@link TileType} of this tile.
     */
    public Tile(TileType type) {
        this.type = type;
    }

    /**
     * Gets the type of this tile.
     *
     * @return The {@link TileType}.
     */
    public TileType getType() {
        return type;
    }

    /**
     * Gets the production value (dice roll number) of this tile.
     *
     * @return The production value. Returns 0 if it's a WASTE tile or value not set.
     */
    public int getValue() {
        return value;
    }

    /**
     * Sets the 2D coordinates of this tile on the game board.
     * Coordinates can only be set once (if they are currently 0,0).
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     */
    public void setCoordinates(double x, double y) {
        if (this.coordinates[0] == 0 && this.coordinates[1] == 0) {
            this.coordinates = new double[]{x, y};
        }
    }

    /**
     * Gets the 2D coordinates of this tile.
     *
     * @return A clone of the double array representing the [x, y] coordinates.
     */
    public double[] getCoordinates() {
        return coordinates.clone();
    }

    /**
     * Sets the unique identifier for this tile.
     * The ID can only be set once (if it is currently 0).
     *
     * @param id The ID to set.
     */
    public void setId(int id) {
        if (this.id != 0) { // ID can only be set once
            return;
        }

        this.id = id;
    }

    /**
     * Sets the production value (dice roll number) for this tile.
     * The value can only be set once (if it is currently 0).
     *
     * @param value The production value to set.
     */
    public void setValue(int value) {
        if (this.value != 0) { // Value can only be set once
            return;
        }
        this.value = value;
    }

    /**
     * Gets the unique identifier of this tile.
     *
     * @return The tile ID.
     */
    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format(
                "Tile{" +
                        "id=" + id + "," +
                        "coordinates=(%f, %f)" +
                        '}', this.coordinates[0], this.coordinates[1]);
    }

    /**
     * Converts the tile's state to a JSON representation.
     * Includes ID, type, value, and coordinates.
     *
     * @return An {@link ObjectNode} representing the tile in JSON format.
     */
    @Override
    public ObjectNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode tileNode = mapper.createObjectNode();

        tileNode.put("id", this.id);
        tileNode.put("type", this.type.name());
        tileNode.put("value", this.value);

        ArrayNode coordsNode = mapper.createArrayNode();
        coordsNode.add(this.coordinates[0]); // Add x
        coordsNode.add(this.coordinates[1]); // Add y
        tileNode.set("coordinates", coordsNode);

        return tileNode;
    }

    /**
     * Adds a {@link SettlementPosition} as a subscriber to this tile.
     * Subscribers (settlement positions) will be notified when this tile produces resources.
     *
     * @param subscriber The {@link SettlementPosition} to add.
     */
    @Override
    public void addSubscriber(SettlementPosition subscriber){
        settlementsOfTile.add(subscriber);
    }

    /**
     * Removes a {@link SettlementPosition} from this tile's list of subscribers.
     *
     * @param subscriber The {@link SettlementPosition} to remove.
     */
    @Override
    public void removeSubscriber(SettlementPosition subscriber){
        settlementsOfTile.remove(subscriber);
    }

    /**
     * Notifies all subscribed {@link SettlementPosition}s that this tile has produced resources.
     * The notification includes the type of resource produced.
     *
     * @param notification The {@link TileType} of the resource produced.
     */
    @Override
    public void notifySubscribers(TileType notification) {
        for (SettlementPosition subscriber: settlementsOfTile){
            subscriber.update(notification);
        }
    }

    /**
     * Handles an update notification, from a {@link DiceRoller}, indicating a dice roll total.
     * If the tile's production value matches the dice roll total, it notifies its subscribers
     * (settlement positions) about resource production.
     *
     * @param notification The integer value of the total dice roll.
     */
    @Override
    public void update(Integer notification) {
        if (value == notification)
            notifySubscribers(type);
    }

    /**
     * Subscribes this tile to a {@link DiceRoller}.
     * This allows the tile to receive notifications about dice rolls.
     *
     * @param diceRoller The {@link DiceRoller} to subscribe to.
     */
    public void subscribeToDice(DiceRoller diceRoller) {
        diceRoller.addSubscriber(this);
    }


    /**
     * Gets the list of {@link SettlementPosition SettlementPositions} that are adjacent to this tile.
     *
     * @return A list of {@link SettlementPosition} objects.
     */
    public List<SettlementPosition> getSettlementsOfTile() {
        return settlementsOfTile;
    }
}