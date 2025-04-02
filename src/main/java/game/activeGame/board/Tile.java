package game.activeGame.board;

public class Tile {
    final TileType type;

    public Tile(TileType type){
        this.type = type;
    }

    public TileType getType() {
        return type;
    }
}
