package com.example.cataniaunited.player;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.websockets.next.WebSocketConnection;

import java.util.Random;
import java.util.UUID;

public class Player {

    private String username;
    private final String uniqueId;
    private final String connectionId;
    private int victoryPoints;

    public Player() {
        this.uniqueId = UUID.randomUUID().toString();
        this.username = "RandomPlayer_" + new Random().nextInt(10000);
        this.connectionId = null;
    }

    public Player(String username) {
        this.username = username;
        this.uniqueId = UUID.randomUUID().toString();
        this.connectionId = null;
    }

    public Player(WebSocketConnection connection) {
        this("RandomPlayer_" + new Random().nextInt(10000), connection);
    }

    public Player(String username, WebSocketConnection connection) {
        this.username = username;
        this.uniqueId = UUID.randomUUID().toString();
        this.connectionId = connection.id();
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

    @Override
    public String toString() {
        return "Player{" +
                "username='" + username + '\'' +
                ", uniqueId='" + uniqueId + '\'' +
                ", connectionId='" + connectionId + '\'' +
                '}';
    }
}
