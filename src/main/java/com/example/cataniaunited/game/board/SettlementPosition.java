package com.example.cataniaunited.game.board;
import com.example.cataniaunited.game.buildings.Building;

import java.util.ArrayList;
import java.util.Arrays;
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
                "building=" + building +
                ", roads=" + roads +
                ", tiles=" + Arrays.toString(tiles) +
                ", ID='" + ID + '\'' +
                '}';
    }

    public void addTile(Tile tileToAdd){
        tiles.add(tileToAdd);
        assert tiles.size() <= 3: "Cant assign 4 Tiles to a single settlement Position";
    }
}
