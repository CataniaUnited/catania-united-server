package game.activeGame.board;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class GameBoard {
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
        List<SettlementPosition> settlementPositionGraph =  generateGraph(sizeOfBoard);
        long endtime = System.nanoTime();
        printMainLoop(settlementPositionGraph);
        System.out.printf("Generated Board with %d Levels in %fs\n", sizeOfBoard,(endtime-starttime)*10e-9);
    }

    /**
     * @param sizeOfBoard specifies the amount of hexes next do each other on the row with the most amount of hexes
     */
    public List<SettlementPosition> generateGraph(int sizeOfBoard){
        int totalAmountOfSettlementPositions = (int) (6*Math.pow(sizeOfBoard, 2)); // the total amount of nodes in our Settlementposition Graph will be 6 * k^2
        List<SettlementPosition> nodeList = new ArrayList<>(totalAmountOfSettlementPositions);


        for (int level = 1; level <= sizeOfBoard; level++){ // for each level create the subgraph and snd continuously interconnect it with the inner layer
            createInterconnectedSubgraphOfLevelk(nodeList, level);
        }
        return nodeList;
    }

    public void createInterconnectedSubgraphOfLevelk(List<SettlementPosition> nodeList, int level){
        // amount new vertices for level k = 6(2*(k-1) + 3)
        Function<Integer, Integer> calculateEndIndexForLayerK = (k) -> 6 * (2 * (k-2) + 3);

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

        // Connect first element of new layer, to inner layer
        if (level != 1){
            createRoadBetweenTwoSettlements(nodeList.get((id-1) - offsetBetweenIndicesOfHexesToInterConnect), lastSettlement);
            offsetBetweenIndicesOfHexesToInterConnect+=2;
        }

        // Generate nodes of current layer
        for(int i = 2;i <= endIndex; i++){ // i represents the i'th node of this layer
            // New Node
            SettlementPosition currentSettlement = new SettlementPosition(++id);
            nodeList.add(currentSettlement);

            // create connection with previous node of the same layer
            createRoadBetweenTwoSettlements(lastSettlement, currentSettlement);

            // set previous settlement to this to setup the loop for the next node
            lastSettlement = currentSettlement;


            // Do some black magic to connect the right nodes with the nodes from the inner layers
            // In the first layer, there are no inner layers; in all other layer we check if we created the same amount of nodes as needed until a new connection
            if (level != 1 && currentNodesUntilCompletionOfNextHex == ++nodesAfterCompletionOfPreviousHex){
                // if yes, create a road between them and log a new connectino
                createRoadBetweenTwoSettlements(nodeList.get((id-1) - offsetBetweenIndicesOfHexesToInterConnect), currentSettlement);
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
    }


    /**
     * Create a new Edge = Road in the Graph = GameBoard connection two settlementpositions
     * @param a Settlementposition a
     * @param b Settlementposition b
     */
    public void createRoadBetweenTwoSettlements(SettlementPosition a, SettlementPosition b){
        Road roadToAdd = new Road(a, b);
        a.roads.add(roadToAdd);
        b.roads.add(roadToAdd);
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
