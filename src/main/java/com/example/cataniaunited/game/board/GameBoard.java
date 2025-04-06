package com.example.cataniaunited.game.board;

import com.example.cataniaunited.api.GameWebSocket;
import org.jboss.logging.Logger;

import java.util.List;

public class GameBoard {
    // TODO implement position for tiles and nodes
    private static final Logger logger = Logger.getLogger(GameWebSocket.class);
    private static final int DEFAULT_TILES_PER_PLAYER_GOAL = 6;
    private static final int SIZE_OF_HEX = 10;

    public GameBoard(int playerCount, boolean printLoop){

        int sizeOfBoard = switch (playerCount) {
            case 2, 3, 4 -> 3;
            case 5, 6 -> 4;
            case 7, 8 -> 5;
            default -> (int) Math.floor(Math.sqrt((double) (DEFAULT_TILES_PER_PLAYER_GOAL * playerCount - 1) / 3)) + 1;
        };

        long starttime = System.nanoTime();
        List<SettlementPosition> settlementPositionGraph =  generateBoard(sizeOfBoard);
        long endtime = System.nanoTime();
        //printMainLoop(settlementPositionGraph);
        logger.infof("Generated Board for %d players, with %d Levels in %fs\n".formatted(playerCount, sizeOfBoard,(endtime-starttime)*10e-10));

        if (printLoop)
            printMainLoop(settlementPositionGraph);
    }

    public List<SettlementPosition> generateBoard(int sizeOfBoard){
        TileListGenerator tileGenerator = new TileListGenerator(sizeOfBoard, SIZE_OF_HEX, true);
        List<Tile> tileList = tileGenerator.generateShuffledTileList();
        GraphBuilder graphBuilder = new GraphBuilder();
        return graphBuilder.generateGraph(sizeOfBoard, tileList);
    }



    /**
     * Print out some stats of the graph
     * @param graph graph to gather stats from
     */
    public void printMainLoop(List<SettlementPosition> graph){
        for(SettlementPosition node:graph){
            System.out.println(node);
        }
    }

}
