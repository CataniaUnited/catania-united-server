package com.example.cataniaunited.game.board.tile_list_builder;

import com.example.cataniaunited.game.board.Placable;
import com.example.cataniaunited.Subscriber;
import com.example.cataniaunited.game.dice.Dice;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Tile implements Placable, Subscriber<Dice, Integer> {
    final TileType type;
    int value = 0; // To set later

    double[] coordinates = new double[2];

    int id;

    private boolean hasResource = false;

    public Tile(TileType type) {
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
        return coordinates.clone();
    }

    public void setId(int id) {
        if (this.id != 0) {
            return;
        }

        this.id = id;
    }

    public void setValue(int value) {
        if (this.value != 0) {
            return;
        }
        this.value = value;
    }

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

    public void subscribeToDice(Dice dice) {
        dice.addSubscriber(this);
    }

    @Override
    public void update(Integer diceValue) {
        if (this.value == diceValue && this.type != TileType.WASTE) {
            this.hasResource = true;
            // notify buildings here
        }
    }

    public boolean hasResource() {
        return hasResource;
    }

    public void resetResource() {
        this.hasResource = false;
    }
}