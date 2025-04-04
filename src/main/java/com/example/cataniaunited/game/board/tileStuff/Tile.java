package com.example.cataniaunited.game.board.tileStuff;

import com.example.cataniaunited.game.board.TileType;

public class Tile {
    final TileType type;
    final int value = 0; // To set later

    private double x;
    private double y;
    int id;

    public Tile(TileType type){
        this.type = type;
    }

    public TileType getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public void setCoordinates(double x, double y) {
        if (this.x == 0 && this.y == 0) {
            this.x = x;
            this.y = y;
        }
    }

    public double[] getCoordinates() {
        return new double[]{x, y};
    }

    @Override
    public String toString() {
        return String.format(
        "Tile{" +
                "id=" + id + "," +
                "coordinates=(%f, %f)" +
        '}', x, y);
    }
}
