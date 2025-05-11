package com.example.cataniaunited.player;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.exception.InsufficientResourcesException;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.websockets.next.WebSocketConnection;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public class Player {

    private static final Logger LOG = Logger.getLogger(Player.class);

    private String username;
    private final String uniqueId;
    private final WebSocketConnection connection;
    private int victoryPoints = 0;
    HashMap<TileType, Integer> resources = new HashMap<>();

    public Player() {
        this.uniqueId = UUID.randomUUID().toString();
        this.username = "RandomPlayer_" + new Random().nextInt(10000);
        this.connection = null;
        initializeResources();
    }

    public Player(WebSocketConnection connection) {
        this.uniqueId = UUID.randomUUID().toString();
        this.username = "RandomPlayer_" + new Random().nextInt(10000);
        this.connection = connection;
        initializeResources();
    }

    public Player(String username) {
        this.username = username;
        this.uniqueId = UUID.randomUUID().toString();
        this.connection = null;
        initializeResources();
    }

    public Player(String username, WebSocketConnection connection) {
        this.username = username;
        this.uniqueId = UUID.randomUUID().toString();
        this.connection = connection;
        initializeResources();
    }

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

    public void setUsername(String u) {
        username = u;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public int getVictoryPoints() {
        return victoryPoints;
    }

    public void addVictoryPoints(int victoryPoints) {
        this.victoryPoints += victoryPoints;
    }


    public WebSocketConnection getConnection() {
        return connection;
    }


    public void sendMessage(MessageDTO dto) {
        if (connection == null) {
            LOG.warnf("No WS connection for player %s – message dropped!", uniqueId);
            return;
        }
        try {
            String json = new ObjectMapper().writeValueAsString(dto);
            connection.sendText(json)          // non-blocking
                    .subscribe().with(
                            v -> LOG.debugf("Sent to %s : %s", uniqueId, dto.getType()),
                            err -> LOG.errorf(err, "Failed to send to %s", uniqueId)
                    );
        } catch (Exception e) {
            LOG.errorf(e, "Failed to serialise DTO for player %s", uniqueId);
        }
    }

    public ObjectNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode playerNode = mapper.createObjectNode();
        playerNode.put("username", this.username);
        playerNode.put("victoryPoints", this.victoryPoints);
        return playerNode;
    }

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

    public void receiveResource(TileType resource, int amount) {
        if (resource == null || resource == TileType.WASTE)
            return;

        int resourceCount = getResourceCount(resource);
        resources.put(resource, resourceCount + amount);
    }

    public void removeResource(TileType resource, int amount) throws GameException {
        if (resource == null || resource == TileType.WASTE)
            return;

        int resourceCount = getResourceCount(resource) - amount;
        if (resourceCount < 0) {
            throw new InsufficientResourcesException();
        }
        resources.put(resource, resourceCount);
    }

    public ObjectNode getResourceJSON() {
        ObjectNode resourcesNode = JsonNodeFactory.instance.objectNode();
        for (Map.Entry<TileType, Integer> entry : this.resources.entrySet()) {
            if (entry.getKey() != TileType.WASTE) {
                resourcesNode.put(entry.getKey().name(), entry.getValue());
            }
        }
        return resourcesNode;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(uniqueId, player.uniqueId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uniqueId);
    }
}
