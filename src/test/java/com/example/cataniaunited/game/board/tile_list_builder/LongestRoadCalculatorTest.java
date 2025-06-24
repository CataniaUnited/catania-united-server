package com.example.cataniaunited.game.board.tile_list_builder;

import com.example.cataniaunited.game.board.BuildingSite;
import com.example.cataniaunited.game.board.LongestRoadCalculator;
import com.example.cataniaunited.game.board.Road;
import com.example.cataniaunited.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LongestRoadCalculatorTest {

    private Player testPlayer;
    private List<BuildingSite> sites;

    @BeforeEach
    void setUp() {
        testPlayer = new Player("p1", null);
        sites = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            BuildingSite site = new BuildingSite(i);
            sites.add(site);
        }
    }

    private Road createPlayerRoad(BuildingSite site1, BuildingSite site2) {
        Road road = new Road(site1, site2, site1.getId() * 100 + site2.getId());
        try {
            road.setOwner(testPlayer);
        } catch (Exception e) { /* Failsafe */ }
        site1.addRoad(road);
        site2.addRoad(road);
        return road;
    }

    @Test
    void testSimpleStraightRoad() {
        List<Road> playerRoads = List.of(
                createPlayerRoad(sites.get(0), sites.get(1)),
                createPlayerRoad(sites.get(1), sites.get(2)),
                createPlayerRoad(sites.get(2), sites.get(3)),
                createPlayerRoad(sites.get(3), sites.get(4)),
                createPlayerRoad(sites.get(4), sites.get(5))
        );
        LongestRoadCalculator calculator = new LongestRoadCalculator();
        int length = calculator.calculateFor(playerRoads);
        assertEquals(5, length);
    }

    @Test
    void testBranchingRoad() {
        List<Road> playerRoads = List.of(
                createPlayerRoad(sites.get(0), sites.get(1)),
                createPlayerRoad(sites.get(1), sites.get(2)),
                createPlayerRoad(sites.get(1), sites.get(3)),
                createPlayerRoad(sites.get(2), sites.get(4))
        );
        LongestRoadCalculator calculator = new LongestRoadCalculator();
        int length = calculator.calculateFor(playerRoads);
        assertEquals(3, length, "Should find the longest branch, which is 3.");
    }

    @Test
    void testRoadWithCycle() {
        List<Road> playerRoads = List.of(
                createPlayerRoad(sites.get(0), sites.get(1)),
                createPlayerRoad(sites.get(1), sites.get(2)),
                createPlayerRoad(sites.get(2), sites.get(3)),
                createPlayerRoad(sites.get(3), sites.get(1)),
                createPlayerRoad(sites.get(0), sites.get(4))
        );
        LongestRoadCalculator calculator = new LongestRoadCalculator();
        int length = calculator.calculateFor(playerRoads);
        assertEquals(5, length, "The longest road with the added branch should be 5.");
    }

    @Test
    void testDisconnectedRoads() {
        List<Road> playerRoads = List.of(
                createPlayerRoad(sites.get(0), sites.get(1)),
                createPlayerRoad(sites.get(1), sites.get(2)),
                createPlayerRoad(sites.get(2), sites.get(3)),
                createPlayerRoad(sites.get(5), sites.get(6)),
                createPlayerRoad(sites.get(6), sites.get(7))
        );
        LongestRoadCalculator calculator = new LongestRoadCalculator();
        int length = calculator.calculateFor(playerRoads);
        assertEquals(3, length);
    }

    @Test
    void testNullRoadList() {
        LongestRoadCalculator calculator = new LongestRoadCalculator();
        int length = calculator.calculateFor(null);
        assertEquals(0, length, "Should return 0 when the road list is null.");
    }

    @Test
    void testEmptyRoadList() {
        List<Road> playerRoads = new ArrayList<>();
        LongestRoadCalculator calculator = new LongestRoadCalculator();
        int length = calculator.calculateFor(playerRoads);
        assertEquals(0, length);
    }
}