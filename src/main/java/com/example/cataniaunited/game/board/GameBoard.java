package com.example.cataniaunited.game.board;

import com.example.cataniaunited.api.GameWebSocket;
import com.example.cataniaunited.game.board.tile_list_builder.StandardTileListBuilder;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.example.cataniaunited.game.board.tile_list_builder.TileListBuilder;
import com.example.cataniaunited.game.board.tile_list_builder.TileListDirector;
import org.jboss.logging.Logger;

import java.util.List;

public class GameBoard {
    private static final Logger logger = Logger.getLogger(GameWebSocket.class);
    static final int DEFAULT_TILES_PER_PLAYER_GOAL = 6;
    static final int SIZE_OF_HEX = 10;
    final int sizeOfBoard;

    List<SettlementPosition> settlementPositionGraph;
    List<Tile> tileList;



    public GameBoard(int playerCount){
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



        logger.infof("Generated Board for %d players, with %d Levels in %fs%n".formatted(playerCount, sizeOfBoard,(endtime-starttime)*10e-10));
    }

        static int calculateSizeOfBoard(int playerCount){
        return switch (playerCount) {
            case 2, 3, 4 -> 3;
            case 5, 6 -> 4;
            case 7, 8 -> 5;
            default -> (int) Math.floor(Math.sqrt((double) (DEFAULT_TILES_PER_PLAYER_GOAL * playerCount - 1) / 3)) + 1;
        };
    }

    void generateTileList(){
        TileListBuilder tileBuilder = new StandardTileListBuilder();
        TileListDirector director = new TileListDirector(tileBuilder);
        director.constructStandardTileList(sizeOfBoard, SIZE_OF_HEX, true);
        tileList = tileBuilder.getTileList();
    }

    void generateBoard(){
        if (this.tileList == null) {
            throw new IllegalStateException("Cannot generate board graph before tile list is generated.");
        }

        GraphBuilder graphBuilder = new GraphBuilder(tileList, sizeOfBoard);
        settlementPositionGraph = graphBuilder.generateGraph();
    }

    public List<SettlementPosition> getSettlementPositionGraph() {
        return settlementPositionGraph;
    }

    public List<Tile> getTileList() {
        return tileList;
    }

}
