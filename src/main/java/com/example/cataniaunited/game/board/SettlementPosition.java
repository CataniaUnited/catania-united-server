package com.example.cataniaunited.game.board;

import com.example.cataniaunited.Subscriber;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.exception.IntersectionOccupiedException;
import com.example.cataniaunited.exception.NoAdjacentRoadException;
import com.example.cataniaunited.exception.SpacingRuleViolationException;
import com.example.cataniaunited.game.board.ports.Port;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.game.buildings.Building;
import com.example.cataniaunited.player.Player;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

// fixme the name position is misleading for what this class handles (everything regarding settlements eg., including resource drops)
/**
 * Represents a potential location for a settlement or city on the Catan game board.
 * It is an intersection point connected to tiles and roads.
 * Implements {@link Placable} for coordinate and JSON representation, and {@link Subscriber}
 * to receive resource notifications from adjacent tiles.
 */
public class SettlementPosition implements Placable, Subscriber<TileType> {

    private static final Logger logger = Logger.getLogger(SettlementPosition.class);

    Building building = null;
    List<Road> roads = new ArrayList<>(3); // Max 3 roads per settlement position
    ArrayList<Tile> tiles = new ArrayList<>(3); // Max 3 tiles per settlement position
    Port port = null;

    double[] coordinates = new double[2];

    final int id;

    /**
     * Constructs a new SettlementPosition with a unique identifier.
     *
     * @param id The unique ID for this settlement position.
     */
    public SettlementPosition(int id) {
        this.id = id;
    }

    /**
     * Gets the unique identifier of this settlement position.
     *
     * @return The ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns a string representation of the SettlementPosition.
     *
     * @return A string detailing the ID, coordinates, associated tiles, and roads.
     */
    @Override
    public String toString() {
        return String.format("SettlementPosition{" +
                "ID='" + id + '\'' +
                ", (%s; %s)" +
                ", tiles=" + tiles +
                ", roads=" + roads +
                '}', this.coordinates[0], this.coordinates[1]);
    }

    /**
     * Gets a list of neighboring settlement positions connected by roads.
     *
     * @return A list of {@link SettlementPosition} objects.
     */
    public List<SettlementPosition> getNeighbours() {
        return roads.stream().map(r -> r.getNeighbour(this)).toList();
    }

    /**
     * Gets a list of tiles adjacent to this settlement position.
     *
     * @return A copy of the list of {@link Tile} objects.
     */
    public List<Tile> getTiles() {
        return List.copyOf(tiles);
    }

    /**
     * Gets a list of roads connected to this settlement position.
     *
     * @return A copy of the list of {@link Road} objects.
     */
    public List<Road> getRoads() {
        return List.copyOf(roads);
    }

    /**
     * Associates a tile with this settlement position.
     * A settlement position can be associated with a maximum of 3 tiles.
     * If the tile is already associated, this method does nothing.
     * This settlement position also subscribes to the added tile for resource notifications.
     *
     * @param tileToAdd The {@link Tile} to add.
     * @throws IllegalStateException if attempting to add more than 3 tiles.
     */
    public void addTile(Tile tileToAdd) {
        // If already added, do nothing
        if (tiles.contains(tileToAdd)) {
            return;
        }

        if (tiles.size() >= 3) {
            throw new IllegalStateException("Cannot assign more than 3 Tiles to SettlementPosition " + id);
        }
        tiles.add(tileToAdd);
        tileToAdd.addSubscriber(this);
    }

    /**
     * Connects a road to this settlement position.
     * A settlement position can be connected to a maximum of 3 roads.
     * If the road is already connected, this method does nothing.
     *
     * @param road The {@link Road} to connect.
     * @throws IllegalStateException if attempting to connect more than 3 roads.
     */
    public void addRoad(Road road) {
        // If already added, do nothing
        if (roads.contains(road)) {
            return;
        }

        if (roads.size() >= 3) {
            throw new IllegalStateException("Cannot connect more than 3 Roads to SettlementPosition " + id);
        }
        this.roads.add(road);
    }

    /**
     * Sets the 2D coordinates of this settlement position on the game board.
     * Coordinates can only be set once (if they are currently 0,0).
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     */
    public void setCoordinates(double x, double y) {
        if (this.coordinates[0] == 0 && this.coordinates[1] == 0) {
            this.coordinates = new double[]{x, y};
        }
    }

    /**
     * Sets a building (settlement or city) on this settlement position.
     * Enforces game rules such as intersection occupancy, spacing rule, and adjacent road requirement.
     *
     * @param building The {@link Building} to place.
     * @throws GameException               if the placement violates game rules.
     * @throws IntersectionOccupiedException if another player's building is already present.
     * @throws SpacingRuleViolationException if the new building is too close to an existing one.
     * @throws NoAdjacentRoadException       if the player does not have an adjacent road for initial placement.
     */
    public void setBuilding(Building building) throws GameException {
        if (this.building != null && this.building.getPlayer() != building.getPlayer()) {
            logger.errorf("Placement of building not allowed -> intersection occupied: positionId = %s, playerId = %s", id, building.getPlayer().getUniqueId());
            throw new IntersectionOccupiedException();
        }

        //Only check on initial placement of building
        if(this.building == null) {
            /*
                The three intersections surrounding this settlement position MUST NOT have buildings on it,
                and there must be one owned road adjacent to this settlement position
            */
            boolean hasNoNeighbouringBuildings = getNeighbours().stream().allMatch(sp -> sp.getBuildingOwner() == null);
            if (!hasNoNeighbouringBuildings) {
                logger.errorf("Placement of building is not allowed -> spacing rule violated: positionId = %s, playerId = %s", id, building.getPlayer().getUniqueId());
                throw new SpacingRuleViolationException();
            }

            boolean atLeastOneOwnedRoad = getRoads().stream().anyMatch(road -> road.getOwner() != null && road.getOwner().equals(building.getPlayer()));
            if (!atLeastOneOwnedRoad) {
                logger.errorf("Placement of building is not allowed -> no owned road adjacent: positionId = %s, playerId = %s", id, building.getPlayer().getUniqueId());
                throw new NoAdjacentRoadException();
            }
        }

        this.building = building;
    }

    public void setPort(Port port) {
        if (this.port != null)
            return;

        this.port = port;
    }

    public Port getPort() {
        return port;
    }

    /**
     * Gets the player who owns the building on this settlement position.
     *
     * @return The {@link Player} owner, or null if no building is present.
     */
    public Player getBuildingOwner() {
        return this.building == null ? null : building.getPlayer();
    }

    /**
     * Gets the 2D coordinates of this settlement position.
     *
     * @return A clone of the double array representing the [x, y] coordinates.
     */
    public double[] getCoordinates() {
        return coordinates.clone();
    }

    /**
     * Converts the settlement position's state to a JSON representation.
     * Includes ID, building information (if any), and coordinates.
     *
     * @return An {@link ObjectNode} representing the settlement position in JSON format.
     */
    @Override
    public ObjectNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode settlementPositionNode = mapper.createObjectNode();

        settlementPositionNode.put("id", this.id);

        if (this.building != null) {
            settlementPositionNode.set("building", this.building.toJson()); // type of Building
        } else {
            settlementPositionNode.putNull("building");
        }

        ArrayNode coordsNode = mapper.createArrayNode();
        coordsNode.add(this.coordinates[0]); // Add x
        coordsNode.add(this.coordinates[1]); // Add y
        settlementPositionNode.set("coordinates", coordsNode);

        return settlementPositionNode;
    }

    /**
     * Handles updates received from subscribed tiles (typically resource notifications).
     * If a building is present on this position, it distributes the received resource
     * to the building's owner.
     *
     * @param resourceType The {@link TileType} of the resource produced.
     */
    @Override
    public void update(TileType resourceType) {
        if (building == null)
            return;

        building.distributeResourcesToPlayer(resourceType);
    }
}