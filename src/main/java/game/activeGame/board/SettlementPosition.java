package game.activeGame.board;

import game.activeGame.buildings.Building;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettlementPosition {
    Building building = null;
    List<Road> roads = new ArrayList<>(3);
    Tile[] tiles = new Tile[3];

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
}
