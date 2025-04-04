package com.example.cataniaunited.game.board;
import com.example.cataniaunited.game.buildings.Building;

import java.util.ArrayList;
import java.util.List;

public class SettlementPosition {
    Building building = null;
    List<Road> roads = new ArrayList<>(3);
    ArrayList<Tile> tiles = new ArrayList<>();

    final int ID;


    public SettlementPosition(int id){
        ID = id;
    }

    public int getID() {
        return ID;
    }

    @Override
    public String toString() {
        return "SettlementPosition{" +
                ", ID='" + ID + '\'' +
                //"building=" + building +
                ", tiles=" + tiles +
                ", roads=" + roads +
                '}';
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
}
