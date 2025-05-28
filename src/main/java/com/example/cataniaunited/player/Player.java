package com.example.cataniaunited.player;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.exception.InsufficientResourcesException;
import com.example.cataniaunited.game.board.ports.Port;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.websockets.next.WebSocketConnection;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a player in the Catan game.
 * Manages player-specific information such as username, unique ID, WebSocket connection,
 * victory points, and resources.
 */
public class Player {

    private static final Logger LOG = Logger.getLogger(Player.class);

    private String username;
    private final String uniqueId;
    private final WebSocketConnection connection;
    private int victoryPoints = 0;
    HashMap<TileType, Integer> resources = new HashMap<>();

    final Set<Port> accessiblePorts = new HashSet<>();

    private static final Logger logger = Logger.getLogger(Player.class);

    /**
     * Default constructor. Creates a player with a random username and a unique ID.
     * The WebSocket connection will be null.
     */
    public Player() {
        this.uniqueId = UUID.randomUUID().toString();
        this.username = "RandomPlayer_" + new Random().nextInt(10000);
        this.connection = null;
        initializeResources();
    }

    /**
     * Constructs a player associated with a WebSocket connection.
     * Generates a random username and a unique ID.
     *
     * @param connection The {@link WebSocketConnection} for this player.
     */
    public Player(WebSocketConnection connection) {
        this.uniqueId = UUID.randomUUID().toString();
        this.username = "RandomPlayer_" + new Random().nextInt(10000);
        this.connection = connection;
        initializeResources();
    }

    /**
     * Constructs a player with a specified username.
     * Generates a unique ID. The WebSocket connection will be null.
     * Initializes resources.
     *
     * @param username The desired username for the player.
     */
    public Player(String username) {
        this.username = username;
        this.uniqueId = UUID.randomUUID().toString();
        this.connection = null;
        initializeResources();
    }

    /**
     * Constructs a player with a specified username and WebSocket connection.
     * Generates a unique ID.
     * Initializes resources.
     *
     * @param username   The desired username for the player.
     * @param connection The {@link WebSocketConnection} for this player.
     */
    public Player(String username, WebSocketConnection connection) {
        this.username = username;
        this.uniqueId = UUID.randomUUID().toString();
        this.connection = connection;
        initializeResources();
    }

    /**
     * Initializes the player's resources with default starting amounts for each {@link TileType}.
     */
    void initializeResources() {
        for (TileType resource : TileType.values()) {
            if (resource == TileType.WASTE)
                continue; // No waste resource
            resources.put(resource, resource.getInitialAmount());
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public int getVictoryPoints() {
        return victoryPoints;
    }

    public void addPort(Port port) {
        if (port == null) {
            throw new IllegalArgumentException("Port can't be null");
        }

        accessiblePorts.add(port);
    }

    /**
     * Adds a specified number of victory points to the player's total.
     *
     * @param victoryPoints The number of victory points to add.
     */
    public void addVictoryPoints(int victoryPoints) {
        this.victoryPoints += victoryPoints;
    }


    public WebSocketConnection getConnection() {
        return connection;
    }

    /**
     * Converts the player's basic information (username, victory points) to a JSON representation.
     *
     * @return An {@link ObjectNode} containing the player's username and victory points.
     */
    public ObjectNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode playerNode = mapper.createObjectNode();
        playerNode.put("username", this.username);
        playerNode.put("victoryPoints", this.victoryPoints);
        logger.infof("Serializing player %s with VP=%d", this.username, this.victoryPoints);
        return playerNode;
    }

    /**
     * Gets the count of a specific resource type held by the player.
     *
     * @param type The {@link TileType} of the resource.
     * @return The amount of the specified resource the player has. (0 if not tracked)
     */
    public int getResourceCount(TileType type) {
        return resources.getOrDefault(type, 0);
    }

    @Override
    public String toString() {
        return "Player{" +
                "username='" + username + '\'' +
                ", uniqueId='" + uniqueId + '\'' +
                ", connectionId='" + (connection != null ? connection.id() : "null") + '\'' +
                '}';
    }

    /**
     * Adds a specified amount of a resource to the player's
     * Does nothing if the resource type is null or WASTE.
     *
     * @param resource The {@link TileType} of the resource to receive.
     * @param amount   The amount of the resource to add.
     */
    public void receiveResource(TileType resource, int amount) {
        if (resource == null || resource == TileType.WASTE)
            return;

        int resourceCount = getResourceCount(resource);
        resources.put(resource, resourceCount + amount);
    }

    /**
     * Removes a specified amount of a resource from the player's inventory.
     * Does nothing if the resource type is null or WASTE.
     *
     * @param resource The {@link TileType} of the resource to remove.
     * @param amount   The amount of the resource to remove.
     * @throws GameException                  if the resource type is invalid.
     * @throws InsufficientResourcesException if the player does not have enough of the resource.
     */
    public void removeResource(TileType resource, int amount) throws GameException {
        if (resource == null || resource == TileType.WASTE)
            return;

        int resourceCount = getResourceCount(resource) - amount;
        if (resourceCount < 0) {
            throw new InsufficientResourcesException();
        }
        resources.put(resource, resourceCount);
    }

    /**
     * Gets a JSON representation of the player's current resource counts.
     * Excludes WASTE tiles.
     *
     * @return An {@link ObjectNode} where keys are resource type names (String) and values are their counts (Integer).
     */
    public ObjectNode getResourceJSON() {
        ObjectNode resourcesNode = JsonNodeFactory.instance.objectNode();
        for (Map.Entry<TileType, Integer> entry : this.resources.entrySet()) {
            if (entry.getKey() != TileType.WASTE) {
                resourcesNode.put(entry.getKey().name(), entry.getValue());
            }
        }
        return resourcesNode;
    }

    /**
     * Compares this Player object to another object for equality.
     * Two players are considered equal if their unique IDs are the same.
     *
     * @param o The object to compare with.
     * @return true if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // Optimization
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(uniqueId, player.uniqueId);
    }

    /**
     * Generates a hash code for this Player object.
     * Based on the player's unique ID.
     *
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(uniqueId);
    }
}
