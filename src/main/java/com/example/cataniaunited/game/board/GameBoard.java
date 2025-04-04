package com.example.cataniaunited.game.board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class GameBoard {
    // TODO implement position for tiles and nodes
    /*
        //Random Calculations might not be needed
        // double apothem = StrictMath.sqrt(3.0/2.0) * sizeOfHex;
        double distanceBetweenTheMidpointOfTwoHexes = StrictMath.sqrt(3)*sizeOfHex;
        //double anglesToGetToAnotherMidpoint = (k -> ((double)k/3 + (double)1/6)*StrictMath.PI);
        //double anglesToGetToSettlementPosition = k -> (k*StrictMath.PI)/3;

        //int y = r * StrictMath.sin(theta);
        //int r = r * StrictMath.cos(theta);
        //int r = StrictMath.sqrt(StrictMath.pow(x, 2) + StrictMath.pow(y,2));
        //theta = StrictMath.atan(y / x);
    */

    public GameBoard(int sizeOfBoard){
        long starttime = System.nanoTime();
        List<SettlementPosition> settlementPositionGraph =  generateBoard(sizeOfBoard);
        long endtime = System.nanoTime();
        printMainLoop(settlementPositionGraph);
        System.out.printf("Generated Board with %d Levels in %fs\n", sizeOfBoard,(endtime-starttime)*10e-9);
    }

    public List<SettlementPosition> generateBoard(int sizeOfBoard){
        List<Tile> tileList = generateShuffledTileList(sizeOfBoard);
        return generateGraph(sizeOfBoard, tileList);
    }

    public List<Tile> generateShuffledTileList(int sizeOfBoard){
        int rings = sizeOfBoard-1;
        int amountOfTilesOnBoard = (int) (3*Math.pow(rings, 2) + 3 * rings + 1);
        TileType[] tileTypes = TileType.values();

        List<Tile> tileList = new ArrayList<>(amountOfTilesOnBoard);
        tileList.add(new Tile(TileType.WASTE));

        for(int i = 1; i < amountOfTilesOnBoard; i++){
            TileType currentTileType = tileTypes[i % (tileTypes.length -1)];
            tileList.add(new Tile(currentTileType));
        }

        Collections.shuffle(tileList);
        for(int i = 0; i < tileList.size(); i++){
            tileList.get(i).id = i+1;
        }
        return tileList;
    }

    /**
     * @param sizeOfBoard specifies the amount of hexes next do each other on the row with the most amount of hexes
     */
    public List<SettlementPosition> generateGraph(int sizeOfBoard, List<Tile> tileList){
        int totalAmountOfSettlementPositions = (int) (6*Math.pow(sizeOfBoard, 2)); // the total amount of nodes in our Settlementposition Graph will be 6 * k^2
        List<SettlementPosition> nodeList = new ArrayList<>(totalAmountOfSettlementPositions);


        for (int level = 1; level <= sizeOfBoard; level++){ // for each level create the subgraph and snd continuously interconnect it with the inner layer
            createInterconnectedSubgraphOfLevelK(nodeList, tileList, level);
        }

        // Update Nodes with only two connections by Checking all Neighbours and adding Tile where two of the three neighbours connect to and you don't
        insertTilesIntoNodesThatHaveLessThan3Connections(nodeList, sizeOfBoard);
        return nodeList;
    }

    public void createInterconnectedSubgraphOfLevelK(List<SettlementPosition> nodeList, List<Tile> tileList, int level){
        // amount new vertices for level k = 6(2*(k-1) + 3)
        Function<Integer, Integer> calculateEndIndexForLayerK = (k) -> 6 * (2 * (k-2) + 3);
        Function<Integer, Integer> calculateAmountOfTilesForLayerK = (k) -> (k > 0) ? (int) (3*Math.pow((k-1), 2) + 3 * (k-1) + 1) : 0;
        // amount of new Tiles for this level (level k -> k-1 rings)

        int amountOfTilesOnBoardBefore = calculateAmountOfTilesForLayerK.apply(level-1); // amount of tiles placed before
        int indexOfLastTileOfThisLayer =calculateAmountOfTilesForLayerK.apply(level)-1; // amount of tiles that will be placed after this method is done -1 to get index
        int currentTileIndex = amountOfTilesOnBoardBefore; // index of current Tile regarding tileList

        int endIndex = calculateEndIndexForLayerK.apply(level); // calculate the amount of new Nodes for this level
        int id = nodeList.size();

        int nodesUntilCompletionOfNextHexWithoutCorner = 2; // when we are currently not traversing a corner, every second node needs to connect to the inner sub graph
        int nodesUntilCompletionOfNextHexWithCorner = 3; // when we are currently not traversing a corner, every third node needs to connect to the inner sub graph
        int cornerRhythm = level - 1; // the rhythm in which corners appear correlate with the rhythm on level 2 its c c c c ... on level 3 c s c s cs, on level 4 c s s c s s c...
        int connectionCount = 0; // the count of connection made to know whether a corner follows or a side
        int currentNodesUntilCompletionOfNextHex = nodesUntilCompletionOfNextHexWithCorner; // the amount of nodes to traverse till the next connection to the subgraph has to be made
        int nodesAfterCompletionOfPreviousHex = 0; // the amount of nodes traversed since the last connection

        // the offset between the indices of nodes and the nodes of the inner layer are the amount of nodes in the previous layer -1
        int offsetBetweenIndicesOfHexesToInterConnect = (level > 2) ? calculateEndIndexForLayerK.apply(level-1)-1 : 6;

        // create the first node of the Graph of this level
        SettlementPosition lastSettlement = new SettlementPosition(++id);
        nodeList.add(lastSettlement);
        // Add all tiles the starting node connects to on the current layer
        // Normally first and last, if only layer one, then only first layer (there is only one tile no level1)
        lastSettlement.addTile(tileList.get(currentTileIndex));

        if (level != 1){
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
            if (level != 1 && currentNodesUntilCompletionOfNextHex == ++nodesAfterCompletionOfPreviousHex){
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

    public void insertTilesIntoNodesThatHaveLessThan3Connections(List<SettlementPosition> nodeList, int layers){
        // Since we insert tiles of the outer layer into nodes of the inner layers, we don't need to check the last layer
        SettlementPosition currentNode;
        List<SettlementPosition> neighbours;
        List<Tile> currentTiles, neighbourTiles = new ArrayList<>();

        int amountOfNodesToCheck = (int) (6*Math.pow((layers-1), 2));
        for(int i = 0; i < amountOfNodesToCheck; i++){
            currentNode = nodeList.get(i);
            currentTiles = currentNode.getTiles(); // get amount of tiles of this node
            if (currentTiles.size() == 3){
                continue; // if == 3 continue
            }
            assert currentTiles.size() < 3; // if > 3 exception
            // else < 3
            // get tiles of All Connected Nodes (3) if 2 outer layer or something went wrong -> exception
            assert currentNode.getRoads().size() == 3;
            neighbours = currentNode.getNeighbours();
            assert neighbours.size() == 3;

            for(SettlementPosition neighbour: neighbours){
                neighbourTiles.addAll(neighbour.getTiles()); // add all Possible Tiles !!!including duplicates!!!
            }

            // add the tile that is connected to two of the three nodes but node to you.
            neighbourTiles.removeAll(currentTiles); // remove Tiles already appended to this node

            //get duplicated element left
            //System.out.println("Find duplicate");
            //System.out.println(neighbourTiles);
            Tile tileToAdd = findDuplicateTile(neighbourTiles);
            //System.out.println(tileToAdd);
            //System.out.println("\n\n\n\n\n");
            currentNode.addTile(tileToAdd);

            assert currentNode.getTiles().size() == 3;
            neighbourTiles.clear();

        }

    }

    /**
     * O(1) Implementation of findDuplicate for lists of size 4 due to frequent usage in other Methods,
     * avoiding using the O(n) Algorithm of repeatedly removing an element (internal O(n) implementation in ArrayList)
     * until only the once duplicate is contaminated.
     * @param list
     * @return returns the first Tile, that exists twice in the list
     */
    public static Tile findDuplicateTile(List<Tile> list) {
        assert list.size() == 5;
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
        assert list.get(3).equals(list.get(4));
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
