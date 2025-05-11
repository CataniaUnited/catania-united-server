package com.example.cataniaunited.game.board;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.Buildable;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.Map;

public class Road implements Placable, Buildable {
    Player owner;
    PlayerColor color;
    final SettlementPosition positionA;
    final SettlementPosition positionB;
    final int id;

    double[] coordinates = new double[2];
    double rotationAngle;

    public Road(SettlementPosition positionA, SettlementPosition positionB, int id) {
        this.positionA = positionA;
        this.positionB = positionB;
        this.id = id;
    }

    public void setOwner(Player owner) throws GameException {
        if (owner == null) {
            throw new GameException("Owner of road must not be null: roadId = %s", id);
        }

        if (this.owner != null) {
            throw new GameException("Road cannot be placed twice: roadId = %s, playerId = %s", id, owner.getUniqueId());
        }
        this.owner = owner;
    }

    public SettlementPosition getNeighbour(SettlementPosition currentSettlement) {
        if (this.positionA == currentSettlement) {
            return positionB;
        }
        if (this.positionB == currentSettlement) {
            return positionA;
        }
        return null;
    }

    public double[] getCoordinates() {
        return coordinates.clone();
    }

    public double getRotationAngle() {
        return rotationAngle;
    }

    public int getId() {
        return id;
    }

    public void setCoordinatesAndRotationAngle() {
        if (!Arrays.equals(coordinates, new double[]{0, 0})) {
            return; // position has already been set
        }

        double xMax;
        double yMax;
        double xMin;
        double yMin;
        double[] coordinatesOfPositions;
        coordinatesOfPositions = positionA.getCoordinates();
        xMax = coordinatesOfPositions[0];
        yMax = coordinatesOfPositions[1];
        xMin = xMax;
        yMin = yMax;

        if (coordinatesOfPositions[0] == 0 && coordinatesOfPositions[1] == 0) {  // position of Settlement A is not yet set
            return;
        }

        coordinatesOfPositions = positionB.getCoordinates();
        xMax += coordinatesOfPositions[0];
        xMin -= coordinatesOfPositions[0];
        yMax += coordinatesOfPositions[1];
        yMin -= coordinatesOfPositions[1];

        if (coordinatesOfPositions[0] == 0 && coordinatesOfPositions[1] == 0) {  // position of Settlement B is not yet set
            return;
        }

        this.coordinates = new double[]{xMax / 2, yMax / 2};
        this.rotationAngle = StrictMath.atan2(yMin, xMin); // No need to assert that since no Road will be placed on 0,0
    }

    public Player getOwner() {
        return owner;
    }

    public PlayerColor getColor() {
        return color;
    }

    public void setColor(PlayerColor color) throws GameException {
        if (this.color != null) {
            throw new GameException("Color of road cannot be changed twice: roadId = %s, color = %s", id, color.getHexCode());
        }
        this.color = color;
    }

    @Override
    public String toString() {
        return "Road:{owner: %s; (%s, %s); position: (%s); angle: %f}"
                .formatted(owner, positionA.getId(), positionB.getId(), Arrays.toString(coordinates), rotationAngle);
    }

    @Override
    public ObjectNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode roadNode = mapper.createObjectNode();
        roadNode.put("id", this.id);

        if (this.owner != null) {
            roadNode.put("owner", this.owner.getUniqueId());
        } else {
            roadNode.putNull("owner");
        }

        if (this.color != null) {
            roadNode.put("color", this.color.getHexCode());
        } else {
            roadNode.putNull("color");
        }

        ArrayNode coordsNode = mapper.createArrayNode();
        coordsNode.add(this.coordinates[0]); // Add x
        coordsNode.add(this.coordinates[1]); // Add y
        roadNode.set("coordinates", coordsNode);
        roadNode.put("rotationAngle", this.rotationAngle);
        return roadNode;
    }

    @Override
    public Map<TileType, Integer> getRequiredResources() {
        return Map.of(
                TileType.WOOD, 1,
                TileType.CLAY, 1);
    }
}
