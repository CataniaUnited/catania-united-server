package com.example.cataniaunited.game.board;

import com.example.cataniaunited.game.board.ports.GeneralPort;
import com.example.cataniaunited.game.board.ports.Port;
import com.example.cataniaunited.game.board.ports.SpecificResourcePort;
import com.example.cataniaunited.game.board.tile_list_builder.StandardTileListBuilder;
import com.example.cataniaunited.game.board.tile_list_builder.Tile;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.example.cataniaunited.util.CatanBoardUtils;

import java.util.*;

// fixme many magic constants
/**
 * Builds the graph structure of the Catan game board, consisting of
 * {@link SettlementPosition} nodes and {@link Road} edges.
 * It uses a list of pre-generated {@link Tile} objects to determine
 * the layout and connections.
 */
public class GraphBuilder {
    final List<Tile> tileList;
    List<SettlementPosition> nodeList;
    List<Road> roadList;
    List<Port> portList;
    int sizeOfBoard;

    int nodeId=0;

    /**
     * Constructs a GraphBuilder with a list of tiles and the size of the board.
     *
     * @param tileList    The list of {@link Tile} objects to build the graph from.
     * @param sizeOfBoard The size of the board (number of rings/layers).
     * @throws IllegalArgumentException if tileList is null/empty, sizeOfBoard is too small,
     *                                  or tileList size doesn't match expected count for the board size.
     */
    public GraphBuilder(List<Tile> tileList, int sizeOfBoard) {
        if (tileList == null || tileList.isEmpty()) {
            throw new IllegalArgumentException("Tile list cannot be null or empty.");
        }
        if (sizeOfBoard <= 1) {
            throw new IllegalArgumentException("Board size must be greater Than 1.");
        }

        // Check if tileList has the expected number of tiles for the size
        int expectedTileCount = CatanBoardUtils.calculateAmountOfTilesForLayerK(sizeOfBoard);
        if (tileList.size() != expectedTileCount) {
            throw new IllegalArgumentException("Tile list size mismatch.");
        }

        this.tileList = tileList;
        this.sizeOfBoard = sizeOfBoard;

        // Initialize node list with expected capacity
        int totalAmountOfSettlementPositions = calculateTotalSettlementPositions(sizeOfBoard);
        this.nodeList = new ArrayList<>(totalAmountOfSettlementPositions);
        this.roadList = new ArrayList<>(totalAmountOfSettlementPositions);
    }

    /**
     * Generates the complete graph of SettlementPositions.
     * Orchestrates the building process layer by layer, assigns tiles to nodes,
     * and calculates coordinates for settlement positions and roads.
     * Additionally, adds Ports and calculates the coordinates of them
     *
     * @return The generated list of {@link SettlementPosition} nodes.
     */
    public List<SettlementPosition> generateGraph(){
        for (int level = 1; level <= sizeOfBoard; level++){ // for each level create the subgraph and snd continuously interconnect it with the inner layer
            createLayerStructure(level);
        }

        // Assign remaining tiles to nodes with only two initial tiles
        assignTilesToIncompleteNodes();

        // Calculate coordinates for settlement positions = nodes of the graph
        calculateSettlementCoordinates();


        // Calculate coordinates and angles for roads
        // if possible refactor to reduce runtime complexity back to O(n)
        calculateRoadCoordinates();

        // add ports
        addPorts();

        return nodeList;
    }

    /**
     * Retrieves the list of generated roads.
     * This should be called after {@link #generateGraph()} has completed.
     *
     * @return The list of {@link Road} objects.
     * @throws IllegalStateException if the graph has not been built yet.
     */
    public List<Road> getRoadList() {
        if (roadList == null  || roadList.isEmpty()) {
            throw new IllegalStateException("Build graph before accessing road list.");
        }
        return roadList;
    }

    /**
     * Creates the structure (nodes and roads) for a specific layer of the board
     * and connects it to the previously built inner layer, if applicable.
     *
     * @param layer The current layer number (1-based, where 1 is the innermost layer around the center).
     */
    private void createLayerStructure(int layer) {

        if (layer == 1) {
            createFirstLayerRing();
        } else {
            createSubsequentLayerRing(layer);
        }
    }

    /**
     * Creates the first layer (innermost ring of 6 nodes) around the conceptual center tile.
     * Each node in this layer is connected to the center tile and its adjacent nodes in the ring.
     * @throws IllegalStateException if the tile list is empty.
     */
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

    // fixme way too long and implicit-knowledge method
    /**
     * Creates a subsequent layer (ring of nodes) on the game board.
     * This method connects the new nodes to each other within the layer,
     * to relevant tiles, and to nodes in the inner, previously constructed layer.
     *
     * @param currentLayer The current layer number being constructed (greater than 1).
     */
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
     * Post-processing step after initial layer creation.
     * Finds {@link SettlementPosition} nodes that are associated with only two tiles
     * (typically inner nodes bordering an outer layer) and assigns them a third tile.
     * The third tile is determined by finding a common tile among the node's neighbors
     * that the node itself is not yet associated with.
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
                continue; // if == 3 continue // fixme why, also this comment does not add any information
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

    /**
     * Adds the tiles associated with an outer layer node to an adjacent inner layer node.
     * This helps ensure inner nodes are correctly associated with all three surrounding tiles.
     *
     * @param innerNode The {@link SettlementPosition} in the inner layer.
     * @param outerNode The {@link SettlementPosition} in the outer layer, connected to the innerNode.
     */
    private void addTilesToNodeInTheInnerLayer(SettlementPosition innerNode, SettlementPosition outerNode){
        for (Tile tile: outerNode.getTiles()){
            innerNode.addTile(tile);
        }
    }

    /**
     * Creates a new {@link Road} (edge) in the graph, connecting two {@link SettlementPosition} nodes.
     * Adds the road to both settlement positions and to the main road list.
     *
     * @param a The first {@link SettlementPosition}.
     * @param b The second {@link SettlementPosition}.
     */
    private void createRoadBetweenTwoSettlements(SettlementPosition a, SettlementPosition b){
        Road roadToAdd = new Road(a, b, roadList.size()+1);
        a.addRoad(roadToAdd);
        b.addRoad(roadToAdd);
        roadList.add(roadToAdd);
    }

    // TODO: Add Javadoc
    private void addPorts() {
        // We are only talking about the Outermost nodes
        int startingIndex = (int) (6 * Math.pow((sizeOfBoard - 1), 2));
        int numberOfCoastalSettlements = nodeList.size() - startingIndex;

        int currentIndex = startingIndex;
        int currentPortIndex = 0;
        int portCount;
        int rhythm;

        List<Port> ports = getPortsToPlace(numberOfCoastalSettlements);
        Collections.shuffle(ports);
        portCount = ports.size();

        // assign ports
        rhythm = Math.max(2, numberOfCoastalSettlements / portCount);

        while (currentPortIndex < portCount) {
            if (currentIndex + 1 >= nodeList.size()) {
                // Not enough space for this port
                break;
            }

            Port currentPort = ports.get(currentPortIndex);
            nodeList.get(currentIndex).setPort(currentPort);
            nodeList.get(currentIndex + 1).setPort(currentPort);
            currentPort.setAssociatedSettlements(nodeList.get(currentIndex), nodeList.get(currentIndex+1));
            currentPort.calculatePosition();

            currentIndex += rhythm;
            currentPortIndex++;
        }

        portList = ports;
    }

    // fixme long hard-to-read method
    private List<Port> getPortsToPlace(int numberOfCoastalSettlements) {
        List<Port> ports = new ArrayList<>();
        TileType[] resourceTypes = Arrays.stream(TileType.values()).filter(type -> type != TileType.WASTE).toArray(TileType[]::new);

        List<TileType> shuffledList = new ArrayList<>(Arrays.asList(resourceTypes));
        Collections.shuffle(shuffledList);

        resourceTypes = shuffledList.toArray(new TileType[0]);

        int targetPortCount = calculateTargetPortCount(numberOfCoastalSettlements);

        // --- Distribute Port Types ---
        int specificPortCount;
        int generalPortCount;

        if (targetPortCount <= 5) { // prioritize specific ports if few
            specificPortCount = targetPortCount;
            generalPortCount = 0;
        } else if (targetPortCount <= 11) { // Classic Boards: 5 specific, rest general
            specificPortCount = 5;
            generalPortCount = targetPortCount - specificPortCount;
        } else { // More than 11 ports (very large boards)
            // Maintain ratio from classic boards~45% general.
            generalPortCount = (int) Math.round(targetPortCount * 0.45);
            specificPortCount = targetPortCount - generalPortCount;
        }


        // Add general ports
        for (int i = 0; i < generalPortCount; i++) {
            ports.add(new GeneralPort());
        }

        // Add specific ports, cycling through types to distribute them
        for (int i = 0; i < specificPortCount; i++) {
            ports.add(new SpecificResourcePort(resourceTypes[i % resourceTypes.length]));
        }
        // This ensures that if specificPortCount is 7, you get WOOD,CLAY,SHEEP,WHEAT,ORE,WOOD,CLAY

        return ports;
    }

    private int calculateTargetPortCount(int numberOfCoastalSettlements) {
        int targetPortCount;
        int basePortsForLargeBoards = 11;
        int maximumPositionsPerPortForLargeBoards = 5;

        // --- Determine Target Port Count ---
        switch (sizeOfBoard) {
            case 1:
                return 0;
            case 2: // 7 tiles, 18 coastal settlements
                targetPortCount = 5;
                break;
            case 3: // Standard 3-4 player board (19 tiles, 30 coastal)
                targetPortCount = 9; // 4 general, 5 specific (classic rules)
                break;
            case 4: // Standard 5-6 player board (37 tiles, 42 coastal)
                targetPortCount = 11; // 6 general, 5 specific (classic rules)
                break;
            default:  // massive boards -> slower scaling (sqrt bases)
                int additionalPorts = (sizeOfBoard - 4) / 2; // Add 1 port for every 2 rings
                targetPortCount = basePortsForLargeBoards + additionalPorts;
                // Ensure growth is not too slow at least one port every few settlements
                int densityBasedMin = numberOfCoastalSettlements / maximumPositionsPerPortForLargeBoards;
                targetPortCount = Math.max(targetPortCount, Math.max(11, densityBasedMin));
                break;
        }
        return targetPortCount;
    }

    public List<Port> getPortList() {
        return portList;
    }

// ------------------- Coordinate Calculation ------------------------
    /**
     * Calculates and sets the coordinates and rotation angle for all roads in the road list.
     * This is done after settlement positions have their coordinates calculated.
     */
    private void calculateRoadCoordinates(){
        for (SettlementPosition currentNode : nodeList) {
            for (Road road : currentNode.getRoads()) {
                road.setCoordinatesAndRotationAngle();
            }
        }
    }

    /**
     * Calculates and sets the 2D coordinates for all {@link SettlementPosition} nodes.
     * Distinguishes between inner nodes (surrounded by 3 tiles) and outer nodes.
     */
    private void calculateSettlementCoordinates(){
        addCoordinatesToInnerNodes();
        addCoordinatesToOuterNodes();
    }

    /**
     * Calculates and sets coordinates for inner settlement positions.
     * These nodes are at the intersection of three tiles, and their
     * coordinates are the average of the coordinates of these three tiles.
     */
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

    // fixme unclear whats happening here, e.g., what is the "previous" method?
    /**
     * Calculates and sets coordinates for outer settlement positions.
     * The method of calculation depends on how many tiles the node is associated with
     * and the available coordinate information from its neighbors or associated tiles.
     */
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

    /**
     * Calculates coordinates for an outer node associated with two tiles and having one neighbor
     * whose coordinates are already known. The node's position is determined by reflecting
     * the known neighbor across the midpoint of the two associated tiles.
     *
     * @param node The {@link SettlementPosition} whose coordinates are to be calculated.
     */
    private void addCoordinatesToNodeWith2TilesAnd1Neighbor(SettlementPosition node){
        List<Tile> tilesOfNodeList = node.getTiles();
        List<SettlementPosition> neighbors = node.getNeighbours();

        SettlementPosition minNode = Collections.min(neighbors, Comparator.comparingInt(SettlementPosition::getId));

        double[] coordinates = calculateCoordinatesThruReflectingAPointAcrossTheMidpointOfTwoOtherPoints(tilesOfNodeList.get(0), tilesOfNodeList.get(1), minNode);

        node.setCoordinates(coordinates[0], coordinates[1]);
    }

    /**
     * Calculates coordinates for an outer node associated with one tile and having two neighbors
     * whose coordinates are already known. The node's position is determined by reflecting
     * the associated tile across the midpoint of the two known neighbors.
     *
     * @param node The {@link SettlementPosition} whose coordinates are to be calculated.
     */
    private void addCoordinatesToNodeWith1TilesAnd2NeighborsWithCoordinates(SettlementPosition node){
        List<SettlementPosition> neighbours = node.getNeighbours();

        Tile tile = node.getTiles().get(0);

        double[] coordinates = calculateCoordinatesThruReflectingAPointAcrossTheMidpointOfTwoOtherPoints(neighbours.get(0), neighbours.get(1), tile);

        node.setCoordinates(coordinates[0], coordinates[1]);
    }

    /**
     * Calculates coordinates for an outer node that is associated with one tile and has one direct neighbor
     * with known coordinates. It uses a "neighbor-of-a-neighbor" (distance 2 connection) as a third
     * reference point for triangulation. The node's position is determined by reflecting the
     * "neighbor-of-a-neighbor" across the midpoint of the direct neighbor and the associated tile.
     *
     * @param node The {@link SettlementPosition} whose coordinates are to be calculated.
     */
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
     * Calculates the coordinates of a point C by reflecting a point P (reflectedPlacable)
     * across the midpoint M of two other points A (placableA) and B (placableB).
     * The formula used is C = A + B - P, where A, B, and P are treated as vectors from the origin.
     *
     * @param placableA         A {@link Placable} object providing the coordinates for point A.
     * @param placableB         A {@link Placable} object providing the coordinates for point B.
     * @param reflectedPlacable A {@link Placable} object providing the coordinates for point P (the point to be reflected).
     * @return A double array `[x, y]` representing the calculated coordinates for point C.
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

    /**
     * Calculates the total number of settlement positions on a board of a given size.
     * The formula is 6 * (sizeOfBoard)^2.
     *
     * @param sizeOfBoard The size of the board (number of rings/layers).
     * @return The total number of settlement positions.
     */
    public static int calculateTotalSettlementPositions(int sizeOfBoard){
        return (int) (6*Math.pow(sizeOfBoard, 2));
    }

    /**
     * Creates a new {@link SettlementPosition} with a unique ID and adds it to the nodeList.
     *
     * @return The newly created {@link SettlementPosition}.
     */
    private SettlementPosition createAndAddNode(){
        SettlementPosition currentNode = new SettlementPosition(++nodeId);
        nodeList.add(currentNode);
        return currentNode;
    }

    /**
     * Calculates the total number of tiles on the board up to and including a specified layer.
     *
     * @param layer The layer number (1-based, where 1 is the center tile only).
     * @return The total number of tiles up to and including that layer. Returns 0 if layer is non-positive.
     */
    private static int calculateTilesInBoardUpToLayer(int layer) {
        if (layer <= 0) return 0;
        int n = layer - 1; // n = number of rings around center (0-based)
        return 3 * n * (n + 1) + 1;
    }

    /**
     * Calculates the number of new settlement positions *in* a specific ring (layer) of the board.
     * The formula is 6 * (2 * layer - 1).
     *
     * @param layer The layer number (1-based).
     * @return The number of settlement positions in that specific ring. Returns 0 if layer is non-positive.
     */
    private static int calculateNodesInRing(int layer) {
        if (layer <= 0) return 0;
        return 6 * (2 * layer - 1);
    }

    /**
     * Finds a duplicate {@link Tile} in a given list of tiles.
     * This is an O(1) implementation specifically optimized for lists of size 5,
     * assuming at least one duplicate exists.
     * Used to find a common tile among neighbors that is missing from the current node.
     *
     * @param list A list of 5 {@link Tile} objects, expected to contain at least one duplicate.
     * @return The first {@link Tile} found to be a duplicate. If the list structure assumption
     *         is violated or no duplicate is found within the checked comparisons,
     *         it may return an incorrect result or the last element checked.
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
     * Checks a boolean condition and throws an {@link AssertionError} if the condition is false.
     * This method is "Deprecated" since it drastically reduced branch coverage for the working end product.
     * And is only used in two tests
     *
     * @param success      The boolean condition to check.
     * @param errorMessage The message for the AssertionError if the condition is false.
     * @throws AssertionError if success is false.
     */
    @Deprecated
    static void checkAndThrowAssertionError(boolean success, String errorMessage){
        if (success)
            return;

        throw new AssertionError(errorMessage);
    }

}