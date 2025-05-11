package com.example.cataniaunited.game.board;

import com.example.cataniaunited.Subscriber;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.exception.IntersectionOccupiedException;
import com.example.cataniaunited.exception.NoAdjacentRoadException;
import com.example.cataniaunited.exception.SpacingRuleViolationException;
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

public class SettlementPosition implements Placable, Subscriber<TileType> {

    private static final Logger logger = Logger.getLogger(SettlementPosition.class);

    Building building = null;
    List<Road> roads = new ArrayList<>(3);
    ArrayList<Tile> tiles = new ArrayList<>(3);

    double[] coordinates = new double[2];

    final int id;

    public SettlementPosition(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("SettlementPosition{" +
                "ID='" + id + '\'' +
                ", (%s; %s)" +
                ", tiles=" + tiles +
                ", roads=" + roads +
                '}', this.coordinates[0], this.coordinates[1]);
    }

    public List<SettlementPosition> getNeighbours() {
        return roads.stream().map(r -> r.getNeighbour(this)).toList();
    }

    public List<Tile> getTiles() {
        return List.copyOf(tiles);
    }

    public List<Road> getRoads() {
        return List.copyOf(roads);
    }

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

    public void setCoordinates(double x, double y) {
        if (this.coordinates[0] == 0 && this.coordinates[1] == 0) {
            this.coordinates = new double[]{x, y};
        }
    }

    public void setBuilding(Building building) throws GameException {
        if (this.building != null && this.building.getPlayer() != building.getPlayer()) {
            logger.errorf("Placement of building not allowed -> intersection occupied: positionId = %s, playerId = %s", id, building.getPlayer().getUniqueId());
            throw new IntersectionOccupiedException();
        }

        //Only check on initial placement of building
        if(this.building == null) {
            /*
                The three intersections surrounding this settlement position MUST NOT have buildings on it,
                and there may only be one road adjacent to this settlement position
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

    public Player getBuildingOwner() {
        return this.building == null ? null : building.getPlayer();
    }

    public double[] getCoordinates() {
        return coordinates.clone();
    }

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

    @Override
    public void update(TileType resourceType) {
        if (building == null)
            return;

        building.distributeResourcesToPlayer(resourceType);
    }
}
