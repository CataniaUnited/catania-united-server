package com.example.cataniaunited.game.board;

public class Tile {
    final TileType type;
    final int value = 0; // To set later

    public Tile(TileType type){
        this.type = type;
    }

    public TileType getType() {
        return type;
    }

    public int getValue() {
        return value;
    }
}
