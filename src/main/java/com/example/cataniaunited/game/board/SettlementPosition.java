package com.example.cataniaunited.game.board;
import com.example.cataniaunited.game.board.tileStuff.Tile;
import com.example.cataniaunited.game.buildings.Building;

import java.util.ArrayList;
import java.util.List;

public class SettlementPosition {
    Building building = null;
    List<Road> roads = new ArrayList<>(3);
    ArrayList<Tile> tiles = new ArrayList<>();

    double[] coordinates = new double[2];

    final int ID;


    public SettlementPosition(int id){
        ID = id;
    }

    public int getID() {
        return ID;
    }

    @Override
    public String toString() {
        return String.format("SettlementPosition{" +
                ", ID='" + ID + '\'' +
                ", (%s; %s)" +
                //"building=" + building +
                ", tiles=" + tiles +
                ", roads=" + roads +
                '}', this.coordinates[0], this.coordinates[1]);
    }

    public void addTile(Tile tileToAdd){
        tiles.add(tileToAdd);
        assert tiles.size() <= 3: "Cant assign 4 Tiles to a single settlement Position";
    }

    public List<SettlementPosition> getNeighbours(){
        return roads.stream().map(r -> r.getNeighbour(this)).toList();
    }

    public ArrayList<Tile> getTiles() {
        return tiles;
    }

    public List<Road> getRoads() {
        return roads;
    }

    public void addRoad(Road road) {
        this.roads.add(road);
    }

    public void setCoordinates(double x, double y) {
        if (this.coordinates[0] == 0 && this.coordinates[1] == 0) {
            this.coordinates = new double[]{x, y};
        }
    }

    public double[] getCoordinates() {
        return coordinates;
    }
}
