package com.example.cataniaunited.game.board;

import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.game.Buildable;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.player.Player;
import com.example.cataniaunited.player.PlayerColor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.Map;

/**
 * Represents a road on the Catan game board, connecting two {@link SettlementPosition}s.
 * Implements {@link Placable} for coordinate and JSON representation, and {@link Buildable}
 * for resource requirements.
 */
public class Road implements Placable, Buildable {
    Player owner;
    PlayerColor color;
    final SettlementPosition positionA;
    final SettlementPosition positionB;
    final int id;

    double[] coordinates = new double[2]; // Midpoint of the road
    double rotationAngle; // Angle for graphical representation

    /**
     * Constructs a new Road between two settlement positions.
     *
     * @param positionA The first {@link SettlementPosition} connected by the road.
     * @param positionB The second {@link SettlementPosition} connected by the road.
     * @param id        The unique identifier for this road.
     */
    public Road(SettlementPosition positionA, SettlementPosition positionB, int id) {
        this.positionA = positionA;
        this.positionB = positionB;
        this.id = id;
    }

    /**
     * Sets the owner of this road. A road can only be owned by one player.
     *
     * @param owner The {@link Player} to set as the owner.
     * @throws GameException if the owner is null or if the road already has an owner.
     */
    public void setOwner(Player owner) throws GameException {
        if (owner == null) {
            throw new GameException("Owner of road must not be null: roadId = %s", id);
        }

        if (this.owner != null) {
            throw new GameException("Road cannot be placed twice: roadId = %s, playerId = %s", id, owner.getUniqueId());
        }
        this.owner = owner;
    }

    /**
     * Gets the neighboring settlement position connected by this road, given one of the positions.
     *
     * @param currentSettlement The {@link SettlementPosition} from which to find the neighbor.
     * @return The other {@link SettlementPosition} connected by this road, or null if currentSettlement is not part of this road.
     */
    public SettlementPosition getNeighbour(SettlementPosition currentSettlement) {
        if (this.positionA == currentSettlement) {
            return positionB;
        }
        if (this.positionB == currentSettlement) {
            return positionA;
        }
        return null;
    }

    /**
     * Gets the coordinates (midpoint) of this road.
     *
     * @return A clone of the double array representing the [x, y] coordinates.
     */
    public double[] getCoordinates() {
        return coordinates.clone();
    }

    /**
     * Gets the rotation angle of this road for graphical representation.
     *
     * @return The rotation angle in radians.
     */
    public double getRotationAngle() {
        return rotationAngle;
    }

    /**
     * Gets the unique identifier of this road.
     *
     * @return The road ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Calculates and sets the midpoint coordinates and rotation angle of this road.
     * This is based on the coordinates of the two {@link SettlementPosition}s it connects.
     * This method will only calculate if coordinates have not been set yet (are [0,0]) and
     * if both connected settlement positions have their coordinates set.
     */
    public void setCoordinatesAndRotationAngle() {
        if (!Arrays.equals(coordinates, new double[]{0, 0})) {
            return; // position has already been set
        }

        // fixme reuse transform / position struct
        double xMax;
        double yMax;
        double xMin;
        double yMin;
        double[] coordinatesOfPositions;
        coordinatesOfPositions = positionA.getCoordinates();
        xMax = coordinatesOfPositions[0];
        yMax = coordinatesOfPositions[1];
        xMin = xMax;
        yMin = yMax;

        if (coordinatesOfPositions[0] == 0 && coordinatesOfPositions[1] == 0) {  // position of Settlement A is not yet set
            return;
        }

        coordinatesOfPositions = positionB.getCoordinates();
        xMax += coordinatesOfPositions[0];
        xMin -= coordinatesOfPositions[0];
        yMax += coordinatesOfPositions[1];
        yMin -= coordinatesOfPositions[1];

        if (coordinatesOfPositions[0] == 0 && coordinatesOfPositions[1] == 0) {  // position of Settlement B is not yet set
            return;
        }

        this.coordinates = new double[]{xMax / 2, yMax / 2};
        this.rotationAngle = StrictMath.atan2(yMin, xMin); // No need to assert that since no Road will be placed on 0,0
    }

    /**
     * Gets the owner of this road.
     *
     * @return The {@link Player} who owns the road, or null if unowned.
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * Gets the color of this road, typically matching the owner's color.
     *
     * @return The {@link PlayerColor} of the road.
     */
    public PlayerColor getColor() {
        return color;
    }

    /**
     * Sets the color of this road. The color can only be set once.
     *
     * @param color The {@link PlayerColor} to set.
     * @throws GameException if the road's color has already been set.
     */
    public void setColor(PlayerColor color) throws GameException {
        if (this.color != null) {
            throw new GameException("Color of road cannot be changed twice: roadId = %s, color = %s", id, color.getHexCode());
        }
        this.color = color;
    }

    @Override
    public String toString() {
        return "Road:{owner: %s; (%s, %s); position: (%s); angle: %f}"
                .formatted(owner, positionA.getId(), positionB.getId(), Arrays.toString(coordinates), rotationAngle);
    }

    /**
     * Converts the road's state to a JSON representation.
     * Includes ID, owner, color, coordinates, and rotation angle.
     *
     * @return An {@link ObjectNode} representing the road in JSON format.
     */
    @Override
    public ObjectNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode roadNode = mapper.createObjectNode();
        roadNode.put("id", this.id);

        if (this.owner != null) {
            roadNode.put("owner", this.owner.getUniqueId());
        } else {
            roadNode.putNull("owner");
        }

        if (this.color != null) {
            roadNode.put("color", this.color.getHexCode());
        } else {
            roadNode.putNull("color");
        }

        ArrayNode coordsNode = mapper.createArrayNode();
        coordsNode.add(this.coordinates[0]); // Add x
        coordsNode.add(this.coordinates[1]); // Add y
        roadNode.set("coordinates", coordsNode);
        roadNode.put("rotationAngle", this.rotationAngle);
        return roadNode;
    }

    /**
     * Gets the map of resource types and amounts required to build this road.
     *
     * @return A map where keys are {@link TileType} (WOOD, CLAY) and values are 1.
     */
    @Override
    public Map<TileType, Integer> getRequiredResources() {
        return Map.of(
                TileType.WOOD, 1,
                TileType.CLAY, 1);
    }
}