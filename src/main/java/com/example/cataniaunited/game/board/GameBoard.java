package com.example.cataniaunited.game.board;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.board.tile_list_builder.StandardTileListBuilder;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.example.cataniaunited.game.board.tile_list_builder.TileListBuilder;
import com.example.cataniaunited.game.board.tile_list_builder.TileListDirector;
import com.example.cataniaunited.game.buildings.Settlement;
import com.example.cataniaunited.game.dice.DiceRoller;
import com.example.cataniaunited.player.PlayerColor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;

import java.util.List;

public class GameBoard {
    private static final Logger logger = Logger.getLogger(GameBoard.class);
    static final int DEFAULT_TILES_PER_PLAYER_GOAL = 6;
    static final int SIZE_OF_HEX = 10;
    final int sizeOfBoard;
    private final DiceRoller diceRoller;

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

        this.diceRoller = new DiceRoller();
        subscribeTilesToDice();

        long endtime = System.nanoTime();

        // Something went wrong
        if (this.tileList == null || this.settlementPositionGraph == null || this.roadList == null) {
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

    public void placeSettlement(String playerId, PlayerColor color, int positionId) throws GameException {
        try {
            SettlementPosition settlementPosition = settlementPositionGraph.get(positionId - 1);
            settlementPosition.setBuilding(new Settlement(playerId, color));
        } catch (IndexOutOfBoundsException e) {
            throw new GameException("Settlement position not found: id = %s", positionId);
        }
    }

    public void placeRoad(String playerId, PlayerColor color, int roadId) throws GameException {
        try {
            Road road = roadList.get(roadId - 1);
            road.setOwnerPlayerId(playerId);
            road.setColor(color);
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

    public ObjectNode getJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode boardNode = mapper.createObjectNode();

        // Components of Json
        ArrayNode tilesNode = mapper.createArrayNode();
        ArrayNode positionsNode = mapper.createArrayNode();
        ArrayNode roadsNode = mapper.createArrayNode();

        // Add tiles
        for (Tile tile : this.tileList) {
            tilesNode.add(tile.toJson());
        }


        // Add Settlement positions
        for (SettlementPosition position : this.settlementPositionGraph) {
            positionsNode.add(position.toJson());
        }


        // Add roads
        for (Road road : this.roadList) {
            roadsNode.add(road.toJson());
        }

        // Add the arrays to the main board node
        boardNode.set("tiles", tilesNode);
        boardNode.set("settlementPositions", positionsNode);
        boardNode.set("roads", roadsNode);

        boardNode.put("ringsOfBoard", this.sizeOfBoard);
        boardNode.put("sizeOfHex", DEFAULT_TILES_PER_PLAYER_GOAL);

        return boardNode;
    }

    private void subscribeTilesToDice() {
        tileList.forEach(tile -> {
            tile.subscribeToDice(diceRoller.getDice1());
            tile.subscribeToDice(diceRoller.getDice2());
        });
    }

    public ObjectNode rollDice() throws GameException {
        return diceRoller.rollDice();
    }

}
