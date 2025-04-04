package com.example.cataniaunited.game.board.tileStuff;

import com.example.cataniaunited.game.board.TileType;

public class Tile {
    final TileType type;
    final int value = 0; // To set later

    double[] coordinates = new double[2];
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
        if (this.coordinates[0] == 0 && this.coordinates[1] == 0) {
            this.coordinates = new double[]{x, y};
        }
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    @Override
    public String toString() {
        return String.format(
        "Tile{" +
                "id=" + id + "," +
                "coordinates=(%f, %f)" +
        '}', this.coordinates[0], this.coordinates[1]);
    }
}
