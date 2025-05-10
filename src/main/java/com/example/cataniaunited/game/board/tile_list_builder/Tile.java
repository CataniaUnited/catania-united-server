package com.example.cataniaunited.game.board.tile_list_builder;

import com.example.cataniaunited.Publisher;
import com.example.cataniaunited.Subscriber;
import com.example.cataniaunited.game.board.Placable;
import com.example.cataniaunited.game.board.SettlementPosition;
import com.example.cataniaunited.game.dice.DiceRoller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

public class Tile implements Placable, Publisher<SettlementPosition, TileType>, Subscriber<Integer> {
    final TileType type;
    int value = 0; // To set later

    double[] coordinates = new double[2];

    int id;

    List<SettlementPosition> settlementsOfTile = new ArrayList<>(6);

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

    @Override
    public void addSubscriber(SettlementPosition subscriber){
        settlementsOfTile.add(subscriber);
    }

    @Override
    public void removeSubscriber(SettlementPosition subscriber){
        settlementsOfTile.remove(subscriber);
    }

    @Override
    public void notifySubscribers(TileType notification) {
        for (SettlementPosition subscriber: settlementsOfTile){
            subscriber.update(notification);
        }
    }

    @Override
    public void update(Integer notification) {
        if (value == notification)
            notifySubscribers(type);
    }
    public void subscribeToDice(DiceRoller diceRoller) {
        diceRoller.addSubscriber(this);
    }


    public List<SettlementPosition> getSettlementsOfTile() {
        return settlementsOfTile;
    }
}
