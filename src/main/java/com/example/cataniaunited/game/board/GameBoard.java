package com.example.cataniaunited.game.board;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.exception.InsufficientResourcesException;
import com.example.cataniaunited.game.Buildable;
import com.example.cataniaunited.game.board.tile_list_builder.StandardTileListBuilder;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.example.cataniaunited.game.board.tile_list_builder.TileListBuilder;
import com.example.cataniaunited.game.board.tile_list_builder.TileListDirector;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.game.buildings.Building;
import com.example.cataniaunited.game.buildings.City;
import com.example.cataniaunited.game.buildings.Settlement;
import com.example.cataniaunited.game.dice.DiceRoller;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

/**
 * Represents the Catan game board, including its tiles, settlement positions, and roads.
 * Manages the generation of the board layout and handles actions like placing buildings and roads.
 */
public class GameBoard {
    private static final Logger logger = Logger.getLogger(GameBoard.class);
    static final int DEFAULT_TILES_PER_PLAYER_GOAL = 6;
    static final int SIZE_OF_HEX = 10; // Size parameter for graphical representation of hexes
    final int sizeOfBoard; // Number of rings/layers of tiles from the center
    private final DiceRoller diceRoller;

    List<SettlementPosition> settlementPositionGraph;
    List<Tile> tileList;
    List<Road> roadList;

    /**
     * Constructs a new GameBoard based on the number of players.
     * Initializes tiles, settlement positions, roads, and the dice roller.
     *
     * @param playerCount The number of players in the game.
     * @throws IllegalArgumentException if playerCount is less than or equal to 1.
     * @throws IllegalStateException    if board generation fails.
     */
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

    /**
     * Calculates the appropriate size (number of rings) of the game board based on the player count.
     *
     * @param playerCount The number of players.
     * @return The calculated size of the board (number of rings).
     */
    static int calculateSizeOfBoard(int playerCount) {
        return switch (playerCount) {
            case 2, 3, 4 -> 3;
            case 5, 6 -> 4;
            case 7, 8 -> 5;
            default -> (int) Math.floor(Math.sqrt((double) (DEFAULT_TILES_PER_PLAYER_GOAL * playerCount - 1) / 3)) + 1;
        };
    }

    /**
     * Generates the list of tiles for the game board using a {@link TileListDirector} and a {@link StandardTileListBuilder}.
     */
    void generateTileList() {
        TileListBuilder tileBuilder = new StandardTileListBuilder();
        TileListDirector director = new TileListDirector(tileBuilder);
        director.constructStandardTileList(sizeOfBoard, SIZE_OF_HEX, true);
        tileList = tileBuilder.getTileList();
    }

    /**
     * Generates the graph structure of settlement positions and roads for the game board.
     * This method relies on the tile list having been generated first.
     *
     * @throws IllegalStateException if the tile list has not been generated.
     */
    void generateBoard() {
        if (this.tileList == null) {
            throw new IllegalStateException("Cannot generate board graph before tile list is generated.");
        }

        GraphBuilder graphBuilder = new GraphBuilder(tileList, sizeOfBoard);
        settlementPositionGraph = graphBuilder.generateGraph();
        roadList = graphBuilder.getRoadList();
    }

    /**
     * Places a settlement for a player at the specified position on the board.
     *
     * @param player     The {@link Player} placing the settlement.
     * @param color      The {@link PlayerColor} of the settlement.
     * @param positionId The ID of the {@link SettlementPosition} to place the settlement on.
     * @throws GameException if the placement is invalid (e.g., position occupied, rules violated).
     */
    public void placeSettlement(Player player, PlayerColor color, int positionId) throws GameException {
        placeBuilding(positionId, new Settlement(player, color));
    }

    /**
     * Places a city for a player at the specified position on the board, upgrading an existing settlement.
     *
     * @param player     The {@link Player} placing the city.
     * @param color      The {@link PlayerColor} of the city.
     * @param positionId The ID of the {@link SettlementPosition} to place the city on.
     * @throws GameException if the placement is invalid (e.g., no settlement to upgrade, rules violated).
     */
    public void placeCity(Player player, PlayerColor color, int positionId) throws GameException {
        placeBuilding(positionId, new City(player, color));
    }

    /**
     * Internal helper method to place a generic building (settlement or city) on the board.
     * Checks for required resources and updates the settlement position.
     *
     * @param positionId The ID of the settlement position.
     * @param building   The {@link Building} to be placed.
     * @throws GameException if resources are insufficient, position is invalid, or other rules are violated.
     */
    private void placeBuilding(int positionId, Building building) throws GameException {
        try {
            checkRequiredResources(building.getPlayer(), building);
            logger.debugf("Placing building: playerId = %s, positionId = %s, type = %s", building.getPlayer().getUniqueId(), positionId, building.getClass().getSimpleName());
            SettlementPosition settlementPosition = settlementPositionGraph.get(positionId - 1);
            settlementPosition.setBuilding(building);
            removeRequiredResources(building.getPlayer(), building);
        } catch (IndexOutOfBoundsException e) {
            throw new GameException("Settlement position not found: id = %s", positionId);
        }
    }

    /**
     * Places a road for a player at the specified road ID on the board.
     *
     * @param player The {@link Player} placing the road.
     * @param color  The {@link PlayerColor} of the road.
     * @param roadId The ID of the {@link Road} to be placed.
     * @throws GameException if the placement is invalid (e.g., road ID not found, rules violated).
     */
    public void placeRoad(Player player, PlayerColor color, int roadId) throws GameException {
        try {
            Road road = roadList.get(roadId - 1);
            checkRequiredResources(player, road);
            logger.debugf("Placing road: playerId = %s, roadId = %s", player.getUniqueId(), roadId);
            road.setOwner(player);
            road.setColor(color);
            removeRequiredResources(player, road);
        } catch (IndexOutOfBoundsException e) {
            throw new GameException("Road not found: id = %s", roadId);
        }
    }

    /**
     * Removes the required resources from a player's inventory for a buildable item.
     *
     * @param player    The {@link Player} whose resources are to be removed.
     * @param buildable The {@link Buildable} item for which resources are required.
     * @throws GameException if the player is null or an error occurs during resource removal.
     * @throws InsufficientResourcesException if the player does not have enough resources.
     */
    private void removeRequiredResources(Player player, Buildable buildable) throws GameException {
        if (player == null) {
            throw new GameException("Player must not be null");
        }

        for (Map.Entry<TileType, Integer> entry : buildable.getRequiredResources().entrySet()) {
            TileType tileType = entry.getKey();
            Integer amount = entry.getValue();
            logger.debugf("Removing resource of player: playerId = %s, tileType = %s, amount = %s", player.getUniqueId(), tileType, amount);
            player.removeResource(tileType, amount);
        }
    }

    /**
     * Checks if a player has the required resources to build a specific item.
     *
     * @param player    The {@link Player} to check.
     * @param buildable The {@link Buildable} item.
     * @throws GameException if the player is null.
     * @throws InsufficientResourcesException if the player does not have enough of any required resource.
     */
    private void checkRequiredResources(Player player, Buildable buildable) throws GameException {
        if (player == null) {
            throw new GameException("Player must not be null");
        }
        logger.debugf("Checking if player has required amount of resources: playerId = %s, requiredResources = %s",
                player.getUniqueId(), buildable.getRequiredResources());
        for (Map.Entry<TileType, Integer> entry : buildable.getRequiredResources().entrySet()) {
            TileType tileType = entry.getKey();
            Integer amount = entry.getValue();
            if (player.getResourceCount(tileType) < amount) {
                throw new InsufficientResourcesException();
            }
        }
    }

    /**
     * Gets the list of all settlement positions on the game board.
     *
     * @return A list of {@link SettlementPosition} objects.
     */
    public List<SettlementPosition> getSettlementPositionGraph() {
        return settlementPositionGraph;
    }

    /**
     * Gets the list of all tiles on the game board.
     *
     * @return A list of {@link Tile} objects.
     */
    public List<Tile> getTileList() {
        return tileList;
    }

    /**
     * Gets the list of all roads on the game board.
     *
     * @return A list of {@link Road} objects.
     */
    public List<Road> getRoadList() {
        return roadList;
    }

    /**
     * Generates a JSON representation of the current game board state.
     * Includes information about tiles, settlement positions, roads, board size, and hex size.
     *
     * @return An {@link ObjectNode} containing the game board's JSON structure.
     */
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
        boardNode.put("sizeOfHex", DEFAULT_TILES_PER_PLAYER_GOAL);// This seems wrong TODO: check

        return boardNode;
    }

    /**
     * Subscribes all tiles on the board to the dice roller.
     * This allows tiles to be notified when dice are rolled to distribute resources.
     */
    private void subscribeTilesToDice() {
        tileList.forEach(tile -> tile.subscribeToDice(diceRoller));
    }

    /**
     * Rolls the dice using the board's {@link DiceRoller}.
     * This will trigger resource distribution based on the roll.
     *
     * @return An {@link ObjectNode} containing the dice roll result (dice1, dice2, total).
     */
    public ObjectNode rollDice() {
        return diceRoller.rollDice();
    }
}