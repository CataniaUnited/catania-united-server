package com.example.cataniaunited.game.board;

import com.example.cataniaunited.player.Player;

import java.util.Arrays;

public class Road implements Placable{
    Player owner;
    final SettlementPosition positionA;
    final SettlementPosition positionB;

    double[] coordinates = new double[2];
    double rationAngle;

    public Road(SettlementPosition positionA, SettlementPosition positionB){
        this.positionA = positionA;
        this.positionB = positionB;
    }

    public SettlementPosition getNeighbour(SettlementPosition currentSettlement){
        if (this.positionA == currentSettlement){
            return positionB;
        }
        if (this.positionB == currentSettlement){
            return positionA;
        }
        return null;
    }

    public double[] getCoordinates() {
        return coordinates.clone();
    }

    public double getRationAngle() {
        return rationAngle;
    }

    public void setCoordinatesAndRotationAngle(){
        if (!Arrays.equals(coordinates, new double[]{0, 0})){
            return; // position has already been set
        }

        double xMax, yMax, xMin, yMin;
        double[] coordinates;
        coordinates = positionA.getCoordinates();
        xMax = coordinates[0];
        yMax = coordinates[1];
        xMin = xMax;
        yMin = yMax;

        if (coordinates[0] == 0 && coordinates[1] == 0){  // position of Settlement A is not yet set
            return;
        }

        coordinates = positionB.getCoordinates();
        xMax += coordinates[0];
        xMin -= coordinates[0];
        yMax += coordinates[1];
        yMin -= coordinates[1];

        if (coordinates[0] == 0 && coordinates[1] == 0){  // position of Settlement B is not yet set
            return;
        }

        this.coordinates = new double[]{xMax/2, yMax/2};
        this.rationAngle = StrictMath.atan2(yMin, xMin); // No need to assert that since no Road will be placed on 0,0

    }

    @Override
    public String toString() {
        return "Road:{owner: %s; (%s, %s); position: (%s); angle: %f}"
                .formatted(owner, positionA.getID(), positionB.getID(), Arrays.toString(coordinates), rationAngle);
    }
}
