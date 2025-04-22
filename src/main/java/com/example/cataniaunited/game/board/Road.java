package com.example.cataniaunited.game.board;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.util.Util;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Arrays;

public class Road implements Placable {
    String ownerPlayerId;
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

    public void setOwnerPlayerId(String ownerPlayerId) throws GameException {
        if (Util.isEmpty(ownerPlayerId)) {
            throw new GameException("Owner Id of road must not be empty");
        }

        if (!Util.isEmpty(this.ownerPlayerId)) {
            throw new GameException("Road cannot be placed twice: roadId = %s, playerId = %s", id, ownerPlayerId);
        }
        this.ownerPlayerId = ownerPlayerId;
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

    public String getOwnerPlayerId() {
        return ownerPlayerId;
    }

    @Override
    public String toString() {
        return "Road:{owner: %s; (%s, %s); position: (%s); angle: %f}"
                .formatted(ownerPlayerId, positionA.getId(), positionB.getId(), Arrays.toString(coordinates), rotationAngle);
    }

    @Override
    public ObjectNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode roadNode = mapper.createObjectNode();

        roadNode.put("id", this.id);

        if (this.ownerPlayerId != null) {
            roadNode.put("owner", this.ownerPlayerId);
        } else {
            roadNode.putNull("owner");
        }


        ArrayNode coordsNode = mapper.createArrayNode();
        coordsNode.add(this.coordinates[0]); // Add x
        coordsNode.add(this.coordinates[1]); // Add y
        roadNode.set("coordinates", coordsNode);

        roadNode.put("rotationAngle", this.rotationAngle);

        return roadNode;
    }

}
