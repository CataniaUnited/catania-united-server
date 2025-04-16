package com.example.cataniaunited.game.board;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.example.cataniaunited.game.buildings.Building;

import java.util.ArrayList;
import java.util.List;

public class SettlementPosition implements Placable {
    Building building = null;
    List<Road> roads = new ArrayList<>(3);
    ArrayList<Tile> tiles = new ArrayList<>(3);

    double[] coordinates = new double[2];

    final int id;

    public SettlementPosition(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("SettlementPosition{" +
                "ID='" + id + '\'' +
                ", (%s; %s)" +
                ", tiles=" + tiles +
                ", roads=" + roads +
                '}', this.coordinates[0], this.coordinates[1]);
    }

    public List<SettlementPosition> getNeighbours() {
        return roads.stream().map(r -> r.getNeighbour(this)).toList();
    }

    public List<Tile> getTiles() {
        return List.copyOf(tiles);
    }

    public List<Road> getRoads() {
        return List.copyOf(roads);
    }

    public void addTile(Tile tileToAdd) {
        // If already added, do nothing
        if (tiles.contains(tileToAdd)) {
            return;
        }

        if (tiles.size() >= 3) {
            throw new IllegalStateException("Cannot assign more than 3 Tiles to SettlementPosition " + id);
        }
        tiles.add(tileToAdd);
    }

    public void addRoad(Road road) {
        // If already added, do nothing
        if (roads.contains(road)) {
            return;
        }

        if (roads.size() >= 3) {
            throw new IllegalStateException("Cannot connect more than 3 Roads to SettlementPosition " + id);
        }
        this.roads.add(road);
    }

    public void setCoordinates(double x, double y) {
        if (this.coordinates[0] == 0 && this.coordinates[1] == 0) {
            this.coordinates = new double[]{x, y};
        }
    }

    public void setBuilding(Building building) throws GameException {
        if (this.building != null && !this.building.getOwnerPlayerId().equals(building.getOwnerPlayerId())) {
            throw new GameException("Player mismatch when placing building: positionId = %s, playerId = %s", id, building.getOwnerPlayerId());
        }
        this.building = building;
    }

    public String getBuildingOwner(){
        return this.building == null ? null : building.getOwnerPlayerId();
    }

    public double[] getCoordinates() {
        return coordinates.clone();
    }
}
