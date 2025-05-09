package com.example.cataniaunited.player;

import com.example.cataniaunited.Subscriber;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import io.netty.util.HashedWheelTimer;
import io.quarkus.websockets.next.WebSocketConnection;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class Player {

    private String username;
    private final String uniqueId;
    final String connectionId;

    HashMap<TileType, Integer> resources = new HashMap<>();

    public Player() {
        this.uniqueId = UUID.randomUUID().toString();
        this.username = "RandomPlayer_" + new Random().nextInt(10000);
        this.connectionId = null;

        for (TileType resource: TileType.values()){
            resources.put(resource, 0);
        }
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

    @Override
    public String toString() {
        return "Player{" +
                "username='" + username + '\'' +
                ", uniqueId='" + uniqueId + '\'' +
                ", connectionId='" + connectionId + '\'' +
                '}';
    }

    public void getResource(TileType resource, int amount) {
        Integer resourceCount = resources.get(resource);
        resources.put(resource, resourceCount+amount);
    }
}
