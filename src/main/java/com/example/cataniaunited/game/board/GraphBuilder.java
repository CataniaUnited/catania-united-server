package com.example.cataniaunited.game.board;

import com.example.cataniaunited.api.GameWebSocket;
import com.example.cataniaunited.game.board.tileListBuilder.StandardTileListBuilder;
import com.example.cataniaunited.game.board.tileListBuilder.Tile;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class GraphBuilder {

    private static final Logger logger = Logger.getLogger(GameWebSocket.class);
    final List<Tile> tileList;
    List<SettlementPosition> nodeList;
    int sizeOfBoard;

    public GraphBuilder(List<Tile> tileList, int sizeOfBoard) {
        if (tileList == null || tileList.isEmpty()) {
            throw new IllegalArgumentException("Tile list cannot be null or empty.");
        }
        if (sizeOfBoard <= 0) {
            throw new IllegalArgumentException("Board size must be positive.");
        }

        // Check if tileList has the expected number of tiles for the size
        int expectedTileCount = StandardTileListBuilder.calculateAmountOfTilesForLayerK(sizeOfBoard);
        if (tileList.size() != expectedTileCount) {
            throw new IllegalArgumentException("Tile list size mismatch.");
        }

        this.tileList = tileList;
        this.sizeOfBoard = sizeOfBoard;

        // Initialize node list with expected capacity
        int totalAmountOfSettlementPositions = calculateTotalSettlementPositions(sizeOfBoard);
        this.nodeList = new ArrayList<>(totalAmountOfSettlementPositions);
    }

    /**
     * Generates the complete graph of SettlementPositions.
     * Orchestrates the building process layer by layer and performs post-processing.
     * @return The generated list of SettlementPosition nodes.
     */
    public List<SettlementPosition> generateGraph(){
        for (int level = 1; level <= sizeOfBoard; level++){ // for each level create the subgraph and snd continuously interconnect it with the inner layer
            createLayerStructure(level);
        }

        // Assign remaining tiles to nodes with only two initial tiles
        assignTilesToIncompleteNodes();

        // Calculate coordinates for settlement positions = nodes of the graph
        calculateSettlementCoordinates();


        // Calculate coordinates and angles for roads //TODO: refactor to reduce runtime complexity back to O(n)
        calculateRoadCoordinates();

        return nodeList;
    }

    public void createLayerStructure(int currentLayer){
        // amount new vertices for currentLayer k = 6(2*(k-1) + 3)
        Function<Integer, Integer> calculateEndIndexForLayerK = (k) -> 6 * (2 * (k-2) + 3);
        Function<Integer, Integer> calculateAmountOfTilesForLayerK = (k) -> (k > 0) ? (int) (3*Math.pow((k-1), 2) + 3 * (k-1) + 1) : 0;
        // amount of new Tiles for this currentLayer (currentLayer k -> k-1 rings)
        int amountOfTilesOnBoardBefore = calculateAmountOfTilesForLayerK.apply(currentLayer-1); // amount of tiles placed before
        int indexOfLastTileOfThisLayer =calculateAmountOfTilesForLayerK.apply(currentLayer)-1; // amount of tiles that will be placed after this method is done -1 to get index
        int currentTileIndex = amountOfTilesOnBoardBefore; // index of current Tile regarding tileList

        int endIndex = calculateEndIndexForLayerK.apply(currentLayer); // calculate the amount of new Nodes for this currentLayer
        int id = nodeList.size();

        int nodesUntilCompletionOfNextHexWithoutCorner = 2; // when we are currently not traversing a corner, every second node needs to connect to the inner sub graph
        int nodesUntilCompletionOfNextHexWithCorner = 3; // when we are currently not traversing a corner, every third node needs to connect to the inner sub graph
        int cornerRhythm = currentLayer - 1; // the rhythm in which corners appear correlate with the rhythm on currentLayer 2 its c c c c ... on currentLayer 3 c s c s cs, on currentLayer 4 c s s c s s c...
        int connectionCount = 0; // the count of connection made to know whether a corner follows or a side
        int currentNodesUntilCompletionOfNextHex = nodesUntilCompletionOfNextHexWithCorner; // the amount of nodes to traverse till the next connection to the subgraph has to be made
        int nodesAfterCompletionOfPreviousHex = 0; // the amount of nodes traversed since the last connection

        // the offset between the indices of nodes and the nodes of the inner layer are the amount of nodes in the previous layer -1
        int offsetBetweenIndicesOfHexesToInterConnect = (currentLayer > 2) ? calculateEndIndexForLayerK.apply(currentLayer-1)-1 : 6;

        // create the first node of the Graph of this currentLayer
        SettlementPosition lastSettlement = new SettlementPosition(++id);
        nodeList.add(lastSettlement);
        // Add all tiles the starting node connects to on the current layer
        // Normally first and last, if only layer one, then only first layer (there is only one tile no level1)
        lastSettlement.addTile(tileList.get(currentTileIndex));

        if (currentLayer != 1){
            // From the second layer on  you start between two tiles therefore the last tile needs to be added to the Settlementposition
            lastSettlement.addTile(tileList.get(indexOfLastTileOfThisLayer));

            // Connect first element of new layer, to inner layer and add second Tile
            SettlementPosition innerSettlement = nodeList.get((id-1) - offsetBetweenIndicesOfHexesToInterConnect);
            createRoadBetweenTwoSettlements(innerSettlement, lastSettlement);

            addTilesToNodeInTheInnerLayer(innerSettlement, lastSettlement);
            offsetBetweenIndicesOfHexesToInterConnect+=2;
        }

        // Generate nodes of current layer
        for(int i = 2;i <= endIndex; i++){ // 'i' represent the ith node of this layer
            // New Node
            SettlementPosition currentSettlement = new SettlementPosition(++id);
            nodeList.add(currentSettlement);
            currentSettlement.addTile(tileList.get(currentTileIndex)); // Add Tile two current Node

            // create connection with previous node of the same layer
            createRoadBetweenTwoSettlements(lastSettlement, currentSettlement);

            // set previous settlement to this to set up the loop for the next node
            lastSettlement = currentSettlement;


            // Do some black magic to connect the right nodes with the nodes from the inner layers
            // In the first layer, there are no inner layers; in all other layer we check if we created the same amount
            // of nodes as needed until a new connection
            if (currentLayer != 1 && currentNodesUntilCompletionOfNextHex == ++nodesAfterCompletionOfPreviousHex){
                // if yes
                currentTileIndex++; //  Update tileIndex
                currentSettlement.addTile(tileList.get(currentTileIndex)); // Add second Tile to current Node

                // and, create a road between them, log a new connection and add tile to inner Nodes
                SettlementPosition innerSettlement = nodeList.get((id-1) - offsetBetweenIndicesOfHexesToInterConnect);
                createRoadBetweenTwoSettlements(innerSettlement, currentSettlement);
                addTilesToNodeInTheInnerLayer(innerSettlement, currentSettlement);

                connectionCount++;
                // decide where we are in the Edge Cycle and set amount of nodes till next connection depending on that
                if (connectionCount % cornerRhythm == 0){
                    offsetBetweenIndicesOfHexesToInterConnect+=2;
                    currentNodesUntilCompletionOfNextHex = nodesUntilCompletionOfNextHexWithCorner;
                } else {
                    currentNodesUntilCompletionOfNextHex = nodesUntilCompletionOfNextHexWithoutCorner;
                }
                // reset nodes after last connection was made
                nodesAfterCompletionOfPreviousHex = 0;
            }
        }

        // Connect first and last node of current layer
        createRoadBetweenTwoSettlements(nodeList.get((id)-endIndex), nodeList.get(nodeList.size()-1));
        // No Tile Adding since it's done by creation
    }

    /**
     * Post-processing step: Finds nodes with only 2 tiles (typically inner nodes
     * that border the next layer) and assigns the missing 3rd tile by looking
     * at common tiles among neighbours.
     */
    void assignTilesToIncompleteNodes() {
        // Since we insert tiles of the outer layer into nodes of the inner layers, we don't need to check the last layer
        SettlementPosition currentNode;
        List<SettlementPosition> neighbours;
        List<Tile> currentTiles;

        // Only inner nodes can be added, because outer ones might not have 3 neighbours
        int amountOfNodesToCheck = (int) (6*Math.pow((sizeOfBoard-1), 2));

        for(int i = 0; i < amountOfNodesToCheck; i++){

            currentNode = nodeList.get(i);
            currentTiles = currentNode.getTiles(); // get amount of tiles of this node

            if (currentTiles.size() == 3){
                continue; // if == 3 continue
            }

            // else < 3
            customAssertion(currentTiles.size() == 2, "Node %s has more than three, or less than 2 connections to tiles".formatted(currentNode));

            // get tiles of All Connected Nodes (3) if 2 outer layer or something went wrong -> exception
            customAssertion(currentNode.getRoads().size() == 3, "Node %s should have 3 Road connections".formatted(currentNode));

            neighbours = currentNode.getNeighbours();

            customAssertion(neighbours.size() == 3, "Node %s should have 3 Neighbours connections".formatted(currentNode));

            // add the tile that is connected to two of the three nodes but node to you.
            List<Tile> neighbourTiles = new ArrayList<>();
            for(SettlementPosition neighbour: neighbours){
                neighbourTiles.addAll(neighbour.getTiles()); // add all Possible Tiles !!!including duplicates!!!
            }

            neighbourTiles.removeAll(currentTiles); // remove Tiles already appended to this node

            //get duplicated element left
            Tile tileToAdd = findDuplicateTile(neighbourTiles);

            currentNode.addTile(tileToAdd);

            neighbourTiles.clear();

        }

    }

    /**
     * O(1) Implementation of findDuplicate for lists of size 5 due to frequent usage in other Methods,
     * avoiding using the O(n) Algorithm of repeatedly removing an element (internal O(n) implementation in ArrayList)
     * until only the once duplicate is contaminated.
     * Used to find the tile that is common among neighbours but missing from the current node.
     * @param list List of size 5 that includes at least one duplicate
     * @return returns the first Tile, that exists twice in the list
     */
    public static Tile findDuplicateTile(List<Tile> list) {
        customAssertion(list.size() == 5, "findDuplicateTile only works with 5 tiles, from which one needs to be duplicate, %s".formatted(list));
        // First Element is Duplicate
        if (list.get(0).equals(list.get(1)) || list.get(0).equals(list.get(2)) || list.get(0).equals(list.get(3)) || list.get(0).equals(list.get(4))) {
            return list.get(0);
        }
        // Or Second Element is Duplicate
        if (list.get(1).equals(list.get(2)) || list.get(1).equals(list.get(3))|| list.get(1).equals(list.get(4))) {
            return list.get(1);
        }

        if (list.get(2).equals(list.get(3))|| list.get(2).equals(list.get(4))) {
            return list.get(2);
        }
        // or else third Element is Duplicate
        customAssertion(list.get(3).equals(list.get(4)), "findDuplicateTile only works with 5 tiles, from which one needs to be duplicate, %s".formatted(list));
        return list.get(3);
    }

    public void addTilesToNodeInTheInnerLayer(SettlementPosition innerNode, SettlementPosition outerNode){
        for (Tile tile: outerNode.getTiles()){
            innerNode.addTile(tile);
        }
    }

    /**
     * Create a new Edge = Road in the Graph = GameBoard connection two settlement positions
     * @param a Settlementposition a
     * @param b Settlementposition b
     */
    public void createRoadBetweenTwoSettlements(SettlementPosition a, SettlementPosition b){
        Road roadToAdd = new Road(a, b);
        a.addRoad(roadToAdd);
        b.addRoad(roadToAdd);
    }

    public static int calculateTotalSettlementPositions(int sizeOfBoard){
        return (int) (6*Math.pow(sizeOfBoard, 2));
    }

    public static void customAssertion(boolean success, String errorMessage){
        if (success)
            return;

        logger.error(errorMessage);
        throw new AssertionError(errorMessage);
    }

    void calculateRoadCoordinates(){
        for (SettlementPosition currentNode : nodeList) {
            for (Road road : currentNode.getRoads()) {
                road.setCoordinatesAndRotationAngle();
            }
        }
    }

    void calculateSettlementCoordinates(){
        addCoordinatesToInnerNodes();
        addCoordinatesToOuterNodes();
    }

    void addCoordinatesToInnerNodes(){
        if (sizeOfBoard == 1){ // If there is only one layer, there are no inner nodes
            return;
        }

        // All nodes beside the one on the outermost layer have 3 neighbors
        int lastNodeWith3Neighbors = (int) (6*Math.pow((sizeOfBoard-1), 2));

        for(int i = 0; i < lastNodeWith3Neighbors; i++){
            SettlementPosition currentNode = nodeList.get(i);
            // get Positions of Tiles
            List<Tile> nodeTiles = currentNode.getTiles();
            customAssertion(nodeTiles.size() == 3, "Node (%s) is supposed to have 3 Tiles Attached to it!".formatted("s"));
            // calculate middle Position
            double sumX = 0;
            double sumY = 0;
            double[] coordinates;
            for (Tile tile: nodeTiles){
                coordinates = tile.getCoordinates();
                sumX+=coordinates[0];
                sumY+=coordinates[1];
            }

            // set positiond
            currentNode.setCoordinates(sumX/3, sumY/3);

        }
    }

    void addCoordinatesToOuterNodes(){
        int startingIndex = (int) (6*Math.pow((sizeOfBoard-1), 2));

        for (int i = startingIndex; i < nodeList.size(); i++){
            SettlementPosition currentNode = nodeList.get(i);

            // To specify the point on the plane where our node needs to be placed we need 3 reference coordinates.
            // There are 3 Scenarios.
            // a) 3 Tiles (already completed in previous method)
            // b) 2 Tiles + one Neighbor with coordinates
            if (currentNode.getTiles().size() == 2){
                addCoordinatesToNodeWith2TilesAnd1Neighbor(currentNode);
            } else { // c) 1 Tile + 2 Neighbors with coordinates
                // We don't have two neighbours -> shit
                // c.1) check if next node has two neighbours and create them first
                if (i+1 < nodeList.size() && nodeList.get(i+1).getTiles().size() == 2){
                    addCoordinatesToNodeWith2TilesAnd1Neighbor(nodeList.get(i+1));
                    addCoordinatesToNodeWith1TilesAnd2NeighborsWithCoordinates(currentNode);
                    i++;
                } else if (i + 1 == nodeList.size()) {
                    // c.1.b this is the last node and the first node of this layer already got its coordinates
                    // therefore this node is the only one that already has 2 neighbors
                    addCoordinatesToNodeWith1TilesAnd2NeighborsWithCoordinates(currentNode);
                } else {
                    // c.2) previous node has two tiles do some fancy triangulation to
                    customAssertion(i > startingIndex, "First node has to have 2 Tiles");
                    addCoordinatesToNodeWith1Tiles1NeighbourAnd1NodeAsNeighbourFromPreviousNode(currentNode);
                }


            }

        }
    }

    void addCoordinatesToNodeWith2TilesAnd1Neighbor(SettlementPosition node){
        List<Tile> tileList = node.getTiles();
        List<SettlementPosition> neighbors = node.getNeighbours();

        customAssertion(tileList.size() == 2, "You need 2 tiles");
        customAssertion(!neighbors.isEmpty(), "You need at least one neighbour.");

        SettlementPosition minNode = Collections.min(neighbors, Comparator.comparingInt(SettlementPosition::getID));

        double[] coordinates = calculateCoordinatesThruReflectingAPointAcrossTheMidpointOfTwoOtherPoints(tileList.get(0), tileList.get(1), minNode);

        node.setCoordinates(coordinates[0], coordinates[1]);
    }

    void addCoordinatesToNodeWith1TilesAnd2NeighborsWithCoordinates(SettlementPosition node){
        List<SettlementPosition> neighbours = node.getNeighbours();

        customAssertion(node.getTiles().size() == 1, "You need one node for this method");
        customAssertion(neighbours.size() == 2, "You need 2 neighbours.");

        Tile tile = node.getTiles().get(0);

        double[] coordinates = calculateCoordinatesThruReflectingAPointAcrossTheMidpointOfTwoOtherPoints(neighbours.get(0), neighbours.get(1), tile);

        node.setCoordinates(coordinates[0], coordinates[1]);
    }

    void addCoordinatesToNodeWith1Tiles1NeighbourAnd1NodeAsNeighbourFromPreviousNode(SettlementPosition node){
        List<SettlementPosition> neighbours = node.getNeighbours();
        customAssertion(!neighbours.isEmpty(), "You need 1 neighbour.");
        SettlementPosition neighbour = Collections.min(neighbours, Comparator.comparingInt(SettlementPosition::getID));

        List<SettlementPosition> distance2Connection = neighbour.getNeighbours();
        customAssertion(distance2Connection.size() == 3, "PreviousNode needs 3 neighbours.");
        SettlementPosition reflectionNode = Collections.min(distance2Connection, Comparator.comparingInt(SettlementPosition::getID));

        customAssertion(node.getTiles().size() == 1, "You need one Tile for this method");
        Tile tile = node.getTiles().get(0);

        double[] coordinates = calculateCoordinatesThruReflectingAPointAcrossTheMidpointOfTwoOtherPoints(neighbour, tile, reflectionNode);

        node.setCoordinates(coordinates[0], coordinates[1]);
    }

    /**
     * Calculates the coordinates of a point C by reflecting point P across the midpoint M of points A and B.
     * Formula: C = M + (M - P) = 2*M - P = A + B - P
     * Where A, B, P are treated as vectors from the origin.
     * @param placableA Point A (provides coordinates)
     * @param placableB Point B (provides coordinates)
     * @param reflectedPlacable Point P (provides coordinates)
     * @return The calculated coordinates [x, y] for point C.
     */
    double[] calculateCoordinatesThruReflectingAPointAcrossTheMidpointOfTwoOtherPoints(Placable placableA, Placable placableB, Placable reflectedPlacable){
        double[] coordinates = new double[2];
        double[] placableCoordinates;

        placableCoordinates = placableA.getCoordinates();
        coordinates[0] += placableCoordinates[0];
        coordinates[1] += placableCoordinates[1];

        placableCoordinates = placableB.getCoordinates();
        coordinates[0] += placableCoordinates[0];
        coordinates[1] += placableCoordinates[1];


        // Reflect the Tile reflectionNode across the Midpoint; 2 midpoint - reflectionNode
        placableCoordinates = reflectedPlacable.getCoordinates();
        coordinates[0] -= placableCoordinates[0];
        coordinates[1] -= placableCoordinates[1];

        return coordinates;
    }

}
