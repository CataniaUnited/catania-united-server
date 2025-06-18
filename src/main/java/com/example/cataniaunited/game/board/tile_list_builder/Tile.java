package com.example.cataniaunited.game.board.tile_list_builder;

import com.example.cataniaunited.Publisher;
import com.example.cataniaunited.Subscriber;
import com.example.cataniaunited.game.board.BuildingSite;
import com.example.cataniaunited.game.board.Placable;
import com.example.cataniaunited.game.dice.DiceRoller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * A hexagonal tile on the Catan game board.
 * Each tile has a type (resource or desert), a production value (dice roll number),
 * coordinates, and an ID. It also acts as a {@link Publisher} to notify
 * adjacent {@link BuildingSite}s about resource production when Notified via
 * The DiceRoller to which it acts as a {@link Subscriber}.
 */
public class Tile implements Placable, Publisher<BuildingSite, TileType>, Subscriber<Integer> {
    final TileType type;
    int value = 0; // Production number (dice roll), 0 for DESERT or if not yet set

    double[] coordinates = new double[2];

    int id;
    boolean robber = false;

    List<BuildingSite> buildingSitesOfTile = new ArrayList<>(6);

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
     * @return The production value. Returns 0 if it's a DESERT tile or value not set.
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

    public boolean hasRobber(){
        return robber;
    }

    public void setRobber(boolean robber) {
        this.robber = robber;
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
        tileNode.put("robber", this.robber);

        ArrayNode coordsNode = mapper.createArrayNode();
        coordsNode.add(this.coordinates[0]); // Add x
        coordsNode.add(this.coordinates[1]); // Add y
        tileNode.set("coordinates", coordsNode);

        return tileNode;
    }

    /**
     * Adds a {@link BuildingSite} as a subscriber to this tile.
     * Subscribers (building sites) will be notified via their
     * {@link BuildingSite#update(TileType)} method when this tile produces its resource.
     *
     * @param subscriber The {@link BuildingSite} to add, which must implement {@link Subscriber}{@code <TileType>}.
     */
    @Override
    public void addSubscriber(BuildingSite subscriber){
        buildingSitesOfTile.add(subscriber);
    }

    /**
     * Removes a {@link BuildingSite} from this tile's list of subscribers.
     *
     * @param subscriber The {@link BuildingSite} to remove.
     */
    @Override
    public void removeSubscriber(BuildingSite subscriber){
        buildingSitesOfTile.remove(subscriber);
    }

    /**
     * Notifies all subscribed {@link BuildingSite}s that this tile has produced its resource.
     * The notification includes the {@link TileType} of the resource produced.
     * This typically occurs when a dice roll matches this tile's production value.
     *
     * @param resourceProduced The {@link TileType} of the resource this tile produces and is notifying about.
     */
    @Override
    public void notifySubscribers(TileType resourceProduced) {
        for (BuildingSite subscriber: buildingSitesOfTile){
            subscriber.update(resourceProduced);
        }
    }

    /**
     * Handles an update notification from a {@link com.example.cataniaunited.game.dice.DiceRoller},
     * indicating a dice roll total.
     * If this tile's production {@link #getValue() value} matches the {@code diceRollTotal}
     * and this tile is not a {@link TileType#DESERT} tile, it will
     * {@link #notifySubscribers(TileType) notify its subscribers} (building sites)
     * about the production of its specific {@link #getType() resource type}.
     *
     * @param diceRollTotal The integer value of the total dice roll.
     */
    @Override
    public void update(Integer diceRollTotal) {
        if (value == diceRollTotal && type != TileType.DESERT && !robber)
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
     * Gets the list of {@link BuildingSite BuildingSites} that are adjacent to this tile.
     *
     * @return A list of {@link BuildingSite} objects.
     */
    public List<BuildingSite> getBuildingSitesOfTile() {
        return buildingSitesOfTile;
    }
}