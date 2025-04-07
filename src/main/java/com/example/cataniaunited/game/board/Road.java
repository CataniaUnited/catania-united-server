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

        double x_max, y_max, x_min, y_min;
        double[] coordinates;
        coordinates = positionA.getCoordinates();
        x_max = coordinates[0];
        y_max = coordinates[1];
        x_min = x_max;
        y_min = y_max;

        if (coordinates[0] == 0 && coordinates[1] == 0){  // position of Settlement A is not yet set
            return;
        }

        coordinates = positionB.getCoordinates();
        x_max += coordinates[0];
        x_min -= coordinates[0];
        y_max += coordinates[1];
        y_min -= coordinates[1];

        if (coordinates[0] == 0 && coordinates[1] == 0){  // position of Settlement B is not yet set
            return;
        }

        this.coordinates = new double[]{x_max/2, y_max/2};
        this.rationAngle = StrictMath.atan2(y_min, x_min); // No need to assert that since no Road will be placed on 0,0

    }

    @Override
    public String toString() {
        return "Road:{owner: %s; (%s, %s); position: (%s); angle: %f}"
                .formatted(owner, positionA.getID(), positionB.getID(), Arrays.toString(coordinates), rationAngle);
    }
}
