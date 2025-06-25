package com.example.cataniaunited.game.board;

import com.example.cataniaunited.util.Util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LongestRoadCalculator {

    private int overallMaxLength;

    public int calculateFor(List<Road> allPlayerRoads) {
        if (Util.isEmpty(allPlayerRoads)) {
            return 0;
        }

        this.overallMaxLength = 0;

        for (Road road : allPlayerRoads) {
            Set<Road> visited = new HashSet<>();
            visited.add(road);
            findLongestPath(road.positionA, 1, visited, allPlayerRoads);

            visited = new HashSet<>();
            visited.add(road);
            findLongestPath(road.positionB, 1, visited, allPlayerRoads);
        }

        return this.overallMaxLength;
    }


    private void findLongestPath(BuildingSite currentNode, int currentLength, Set<Road> visited, List<Road> allPlayerRoads) {

        if (currentLength > this.overallMaxLength) {
            this.overallMaxLength = currentLength;
        }

        for (Road neighborRoad : currentNode.getRoads()) {
            if (allPlayerRoads.contains(neighborRoad) && !visited.contains(neighborRoad)) {
                Set<Road> newVisited = new HashSet<>(visited);
                newVisited.add(neighborRoad);

                BuildingSite nextNode = neighborRoad.getNeighbour(currentNode);
                if (nextNode != null) {
                    findLongestPath(nextNode, currentLength + 1, newVisited, allPlayerRoads);
                }
            }
        }
    }
}