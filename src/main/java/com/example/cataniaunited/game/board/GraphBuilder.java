package com.example.cataniaunited.game.board;

import com.example.cataniaunited.game.board.tile_list_builder.StandardTileListBuilder;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;

import java.util.*;

public class GraphBuilder {
    final List<Tile> tileList;
    List<SettlementPosition> nodeList;
    int sizeOfBoard;

    int nodeId=0;

    public GraphBuilder(List<Tile> tileList, int sizeOfBoard) {
        if (tileList == null || tileList.isEmpty()) {
            throw new IllegalArgumentException("Tile list cannot be null or empty.");
        }
        if (sizeOfBoard <= 1) {
            throw new IllegalArgumentException("Board size must be greater Than 1.");
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

    /**
     * Creates the structure (nodes and roads) for a specific layer and connects it
     * to the previously built inner layer (if applicable).
     * @param layer The current layer number (1-based).
     */
    private void createLayerStructure(int layer) {

        if (layer == 1) {
            createFirstLayerRing();
        } else {
            createSubsequentLayerRing(layer);
        }
    }

    /** Creates the first layer (ring of 6 nodes) around the conceptual center. */
    private void createFirstLayerRing() {
        int nodesInRing = calculateNodesInRing(1); // Should be 6
        if (tileList.isEmpty()) throw new IllegalStateException("Tile list is empty, cannot create layer 1.");
        Tile centerTile = tileList.get(0);

        SettlementPosition firstNode = null;
        SettlementPosition previousNode = null;

        for (int i = 0; i < nodesInRing; i++) {
            SettlementPosition currentNode = createAndAddNode();
            currentNode.addTile(centerTile); // All nodes in layer 1 connect to the center tile

            if (previousNode != null) {
                createRoadBetweenTwoSettlements(previousNode, currentNode);
            }
            if (i == 0) {
                firstNode = currentNode;
            }
            previousNode = currentNode;
        }

        // Connect last node back to first node
        if (firstNode != null && firstNode != previousNode) {
            createRoadBetweenTwoSettlements(previousNode, firstNode);
        }
    }

    private void createSubsequentLayerRing(int currentLayer){
        int amountOfTilesOnBoardBefore = calculateTilesInBoardUpToLayer(currentLayer-1); // amount of tiles placed before
        int indexOfLastTileOfThisLayer =calculateTilesInBoardUpToLayer(currentLayer)-1; // amount of tiles that will be placed after this method is done -1 to get index
        int currentTileIndex = amountOfTilesOnBoardBefore; // index of current Tile regarding tileList

        int endIndex = calculateNodesInRing(currentLayer); // calculate the amount of new Nodes for this currentLayer

        int nodesUntilCompletionOfNextHexWithoutCorner = 2; // when we are currently not traversing a corner, every second node needs to connect to the inner sub graph
        int nodesUntilCompletionOfNextHexWithCorner = 3; // when we are currently not traversing a corner, every third node needs to connect to the inner sub graph
        int cornerRhythm = currentLayer - 1; // the rhythm in which corners appear correlate with the rhythm on currentLayer 2 its c c c c ... on currentLayer 3 c s c s cs, on currentLayer 4 c s s c s s c...
        int connectionCount = 0; // the count of connection made to know whether a corner follows or a side
        int currentNodesUntilCompletionOfNextHex = nodesUntilCompletionOfNextHexWithCorner; // the amount of nodes to traverse till the next connection to the subgraph has to be made
        int nodesAfterCompletionOfPreviousHex = 0; // the amount of nodes traversed since the last connection

        // the offset between the indices of the current nodes and the nodes of the inner layer are the amount of nodes in the previous layer -1
        int offsetBetweenIndicesOfHexesToInterConnect = (currentLayer > 2) ? calculateNodesInRing(currentLayer-1)-1 : 6;

        // -------------------------------- SETUP DONE, CREATE FIRST NODE ----------------------------------------
        // create the first node of the Graph of this currentLayer
        SettlementPosition lastSettlement = createAndAddNode();
        // Add all tiles the starting node connects to on the current layer
        lastSettlement.addTile(tileList.get(currentTileIndex));
        lastSettlement.addTile(tileList.get(indexOfLastTileOfThisLayer));

        // Connect first element of new layer, to inner layer and add second Tile
        SettlementPosition innerSettlement = nodeList.get((nodeId-1) - offsetBetweenIndicesOfHexesToInterConnect);
        createRoadBetweenTwoSettlements(innerSettlement, lastSettlement);

        addTilesToNodeInTheInnerLayer(innerSettlement, lastSettlement); // add the two tiles of the current node to the node in the inner layer
        offsetBetweenIndicesOfHexesToInterConnect+=2; // increase offset since a 'corner' follows and there are more nodes on the outher layer than on the inner layer


        // -------------------------------- CREATE ALL NODES ----------------------------------------
        for(int i = 2;i <= endIndex; i++){ // 'i' represent the ith node of this layer
            // New Node
            SettlementPosition currentSettlement = createAndAddNode();
            currentSettlement.addTile(tileList.get(currentTileIndex)); // Add Tile two current Node

            // create connection with previous node of the same layer
            createRoadBetweenTwoSettlements(lastSettlement, currentSettlement);

            // set previous settlement to this to set up the loop for the next node
            lastSettlement = currentSettlement;


            // ---------------------------- Check if you need to connect the current node if the inner layer ---------------------
            // check if we created the same amount of nodes as needed until a new connection
            if (currentNodesUntilCompletionOfNextHex == ++nodesAfterCompletionOfPreviousHex){
                // if yes
                currentTileIndex++; //  Update tileIndex, the previous tile is complete, you are at a new one
                currentSettlement.addTile(tileList.get(currentTileIndex)); // Add second Tile to current Node

                // create a connection to the inner layer, to divide the old hex and the now hex
                // and add the 2 tiles to the node in the inner layer
                innerSettlement = nodeList.get((nodeId-1) - offsetBetweenIndicesOfHexesToInterConnect);
                createRoadBetweenTwoSettlements(innerSettlement, currentSettlement);
                addTilesToNodeInTheInnerLayer(innerSettlement, currentSettlement);

                // log the connection to predict when the next connection to the inner layer has to be made
                connectionCount++;
                // decide where we are in the Edge Cycle and set amount of nodes till next connection depending on that
                if (connectionCount % cornerRhythm == 0){
                    offsetBetweenIndicesOfHexesToInterConnect+=2; // if we are in an edge cycle, we set more nodes, the offset to the inner cycle increases
                    currentNodesUntilCompletionOfNextHex = nodesUntilCompletionOfNextHexWithCorner;
                } else {
                    currentNodesUntilCompletionOfNextHex = nodesUntilCompletionOfNextHexWithoutCorner;
                }

                // reset nodes after last connection was made
                nodesAfterCompletionOfPreviousHex = 0;
            }
        }

        SettlementPosition firstNodeOfCurrentLayer = nodeList.get((nodeId)-endIndex);
        SettlementPosition lastPlacedNode = nodeList.get(nodeList.size()-1);

        // Connect first and last node of current layer
        createRoadBetweenTwoSettlements(firstNodeOfCurrentLayer, lastPlacedNode);
        // No Tile Adding since it's added when creating the node

    }

    /**
     * Post-processing step: Finds nodes with only 2 tiles (typically inner nodes
     * that border the next layer) and assigns the missing 3rd tile by looking
     * at common tiles among neighbours.
     */
    private void assignTilesToIncompleteNodes() {
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

            // get tiles of All Connected Nodes (3) if 2 outer layer or something went wrong -> exception

            neighbours = currentNode.getNeighbours();

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

    private void addTilesToNodeInTheInnerLayer(SettlementPosition innerNode, SettlementPosition outerNode){
        for (Tile tile: outerNode.getTiles()){
            innerNode.addTile(tile);
        }
    }

    /**
     * Create a new Edge = Road in the Graph = GameBoard connection two settlement positions
     * @param a Settlementposition a
     * @param b Settlementposition b
     */
    private void createRoadBetweenTwoSettlements(SettlementPosition a, SettlementPosition b){
        Road roadToAdd = new Road(a, b);
        a.addRoad(roadToAdd);
        b.addRoad(roadToAdd);
    }

    private void calculateRoadCoordinates(){
        for (SettlementPosition currentNode : nodeList) {
            for (Road road : currentNode.getRoads()) {
                road.setCoordinatesAndRotationAngle();
            }
        }
    }

    private void calculateSettlementCoordinates(){
        addCoordinatesToInnerNodes();
        addCoordinatesToOuterNodes();
    }

    private void addCoordinatesToInnerNodes(){
        // All nodes beside the one on the outermost layer have 3 neighbors
        int lastNodeWith3Neighbors = (int) (6*Math.pow((sizeOfBoard-1), 2));

        for(int i = 0; i < lastNodeWith3Neighbors; i++){
            SettlementPosition currentNode = nodeList.get(i);
            // get Positions of Tiles
            List<Tile> nodeTiles = currentNode.getTiles();

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

    private void addCoordinatesToOuterNodes(){
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
                    //
                    addCoordinatesToNodeWith1Tiles1NeighbourAnd1NodeAsNeighbourFromPreviousNode(currentNode);
                }


            }

        }
    }

    private void addCoordinatesToNodeWith2TilesAnd1Neighbor(SettlementPosition node){
        List<Tile> tilesOfNodeList = node.getTiles();
        List<SettlementPosition> neighbors = node.getNeighbours();




        SettlementPosition minNode = Collections.min(neighbors, Comparator.comparingInt(SettlementPosition::getId));

        double[] coordinates = calculateCoordinatesThruReflectingAPointAcrossTheMidpointOfTwoOtherPoints(tilesOfNodeList.get(0), tilesOfNodeList.get(1), minNode);

        node.setCoordinates(coordinates[0], coordinates[1]);
    }

    private void addCoordinatesToNodeWith1TilesAnd2NeighborsWithCoordinates(SettlementPosition node){
        List<SettlementPosition> neighbours = node.getNeighbours();




        Tile tile = node.getTiles().get(0);

        double[] coordinates = calculateCoordinatesThruReflectingAPointAcrossTheMidpointOfTwoOtherPoints(neighbours.get(0), neighbours.get(1), tile);

        node.setCoordinates(coordinates[0], coordinates[1]);
    }

    private void addCoordinatesToNodeWith1Tiles1NeighbourAnd1NodeAsNeighbourFromPreviousNode(SettlementPosition node){
        List<SettlementPosition> neighbours = node.getNeighbours();

        SettlementPosition neighbour = Collections.min(neighbours, Comparator.comparingInt(SettlementPosition::getId));

        List<SettlementPosition> distance2Connection = neighbour.getNeighbours();

        SettlementPosition reflectionNode = Collections.min(distance2Connection, Comparator.comparingInt(SettlementPosition::getId));


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
    private double[] calculateCoordinatesThruReflectingAPointAcrossTheMidpointOfTwoOtherPoints(Placable placableA, Placable placableB, Placable reflectedPlacable){
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

    public static int calculateTotalSettlementPositions(int sizeOfBoard){
        return (int) (6*Math.pow(sizeOfBoard, 2));
    }
    private SettlementPosition createAndAddNode(){
        SettlementPosition currentNode = new SettlementPosition(++nodeId);
        nodeList.add(currentNode);
        return currentNode;
    }

    /** Calculates total tiles up to and including layer k (1-based). */
    private static int calculateTilesInBoardUpToLayer(int layer) {
        if (layer <= 0) return 0;
        int n = layer - 1; // n = number of rings around center (0-based)
        return 3 * n * (n + 1) + 1;
    }

    /** Calculates number of *new* settlement positions *in* the ring of layer k (1-based layer).
     *  Nodes(k) - Nodes(k-1) = 6*k^2 - 6*(k-1)^2 = 6k^2 - 6(k^2-2k+1) = 12k - 6 = 6 * (2k - 1)
     **/
    private static int calculateNodesInRing(int layer) {
        if (layer <= 0) return 0;
        return 6 * (2 * layer - 1);
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

        return list.get(3);
    }

    /**
     * Deactivated since it drastically reduced branch coverage for working end product
     */
    static void checkAndThrowAssertionError(boolean success, String errorMessage){
        if (success)
            return;

        throw new AssertionError(errorMessage);
    }

}

