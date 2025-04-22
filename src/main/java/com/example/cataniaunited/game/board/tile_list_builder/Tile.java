package com.example.cataniaunited.game.board.tile_list_builder;

import com.example.cataniaunited.game.board.Placable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Tile implements Placable {
    final TileType type;
    int value = 0; // To set later

    double[] coordinates = new double[2];

    int id;

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

        if (this.type != null) {
            tileNode.put("type", this.type.name());
        } else {
            tileNode.putNull("type");
        }

        tileNode.put("value", this.value);

        ArrayNode coordsNode = mapper.createArrayNode();
        coordsNode.add(this.coordinates[0]); // Add x
        coordsNode.add(this.coordinates[1]); // Add y
        tileNode.set("coordinates", coordsNode);

        return tileNode;
    }
}
