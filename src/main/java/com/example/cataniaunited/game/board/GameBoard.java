package com.example.cataniaunited.game.board;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.tile_list_builder.StandardTileListBuilder;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.example.cataniaunited.game.board.tile_list_builder.TileListBuilder;
import com.example.cataniaunited.game.board.tile_list_builder.TileListDirector;
import com.example.cataniaunited.game.buildings.Settlement;
import org.jboss.logging.Logger;

import java.util.List;

public class GameBoard {
    private static final Logger logger = Logger.getLogger(GameBoard.class);
    static final int DEFAULT_TILES_PER_PLAYER_GOAL = 6;
    static final int SIZE_OF_HEX = 10;
    final int sizeOfBoard;

    List<SettlementPosition> settlementPositionGraph;
    List<Tile> tileList;
    List<Road> roadList;

    public GameBoard(int playerCount) {
        if (playerCount <= 1) {
            throw new IllegalArgumentException("Player count must be greater than 1.");
        }

        sizeOfBoard = calculateSizeOfBoard(playerCount);
        logger.infof("Generating Board for %d players, with %d Levels...%n", playerCount, sizeOfBoard);
        long starttime = System.nanoTime();

        generateTileList();
        generateBoard();

        long endtime = System.nanoTime();

        // Something went wrong
        if (this.tileList == null || this.settlementPositionGraph == null) {
            logger.errorf("Board generation failed for %d players.", playerCount);
            throw new IllegalStateException("Board generation resulted in null lists.");
        }

        logger.infof("Generated Board for %d players, with %d Levels in %fs%n".formatted(playerCount, sizeOfBoard, (endtime - starttime) * 10e-10));
    }

    static int calculateSizeOfBoard(int playerCount) {
        return switch (playerCount) {
            case 2, 3, 4 -> 3;
            case 5, 6 -> 4;
            case 7, 8 -> 5;
            default -> (int) Math.floor(Math.sqrt((double) (DEFAULT_TILES_PER_PLAYER_GOAL * playerCount - 1) / 3)) + 1;
        };
    }

    void generateTileList() {
        TileListBuilder tileBuilder = new StandardTileListBuilder();
        TileListDirector director = new TileListDirector(tileBuilder);
        director.constructStandardTileList(sizeOfBoard, SIZE_OF_HEX, true);
        tileList = tileBuilder.getTileList();
    }

    void generateBoard() {
        if (this.tileList == null) {
            throw new IllegalStateException("Cannot generate board graph before tile list is generated.");
        }

        GraphBuilder graphBuilder = new GraphBuilder(tileList, sizeOfBoard);
        settlementPositionGraph = graphBuilder.generateGraph();
        roadList = graphBuilder.getRoadList();
    }

    public void placeSettlement(String playerId, int positionId) throws GameException {
        try {
            SettlementPosition settlementPosition = settlementPositionGraph.get(positionId - 1);
            settlementPosition.setBuilding(new Settlement(playerId));
        } catch (IndexOutOfBoundsException e) {
            throw new GameException("Settlement position not found: id = %s", positionId);
        }
    }

    public void placeRoad(String playerId, int roadId) throws GameException {
        try {
            Road road = roadList.get(roadId - 1);
            road.setOwnerPlayerId(playerId);
        } catch (IndexOutOfBoundsException e) {
            throw new GameException("Road not found: id = %s", roadId);
        }
    }

    public List<SettlementPosition> getSettlementPositionGraph() {
        return settlementPositionGraph;
    }

    public List<Tile> getTileList() {
        return tileList;
    }

    public List<Road> getRoadList() {
        return roadList;
    }

}
