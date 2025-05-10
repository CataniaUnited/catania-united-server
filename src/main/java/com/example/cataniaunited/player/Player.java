package com.example.cataniaunited.player;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.websockets.next.WebSocketConnection;

import java.util.*;

public class Player {

    private String username;
    private final String uniqueId;
    final String connectionId;

    HashMap<TileType, Integer> resources = new HashMap<>();
    private int victoryPoints;

    public Player() {
        this.uniqueId = UUID.randomUUID().toString();
        this.username = "RandomPlayer_" + new Random().nextInt(10000);
        this.connectionId = null;
        initializeResources();
    }

    public Player(String username) {
        this.username = username;
        this.uniqueId = UUID.randomUUID().toString();
        this.connectionId = null;
        initializeResources();
    }

    public Player(WebSocketConnection connection) {
        this("RandomPlayer_" + new Random().nextInt(10000), connection);
    }

    public Player(String username, WebSocketConnection connection) {
        this.username = username;
        this.uniqueId = UUID.randomUUID().toString();
        this.connectionId = connection.id();
        initializeResources();
    }

    void initializeResources(){
        for (TileType resource: TileType.values()){
            if (resource == TileType.WASTE)
                continue; // No waste resource
            resources.put(resource, 0);
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

    public void addVictoryPoints(int victoryPoints) {
        this.victoryPoints += victoryPoints;
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
                ", connectionId='" + connectionId + '\'' +
                '}';
    }

    public void getResource(TileType resource, int amount) {
        if(resource == TileType.WASTE)
            return;

        Integer resourceCount = resources.get(resource);
        resources.put(resource, resourceCount+amount);
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
}
