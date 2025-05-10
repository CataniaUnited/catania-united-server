package com.example.cataniaunited.player;

import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.websockets.next.WebSocketConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerTest {

    Player player;

    @BeforeEach
    void setUp(){
        player = new Player();
    }

    @Test
    void testDefaultConstructor() {
        assertTrue(player.getUsername().startsWith("RandomPlayer_"));
        Assertions.assertNotNull(player.getUniqueId(), "uniqueId should not be null");
        Assertions.assertFalse(player.getUniqueId().isEmpty(), "uniqueId should not be empty");
    }

    @Test
    void testCustomConstructor() {
        String customUsername = "Alice1";
        Player customPlayer = new Player(customUsername);
        assertEquals(customUsername, customPlayer.getUsername());
        Assertions.assertNotNull(customPlayer.getUniqueId(), "uniqueId should not be null");
    }

    @Test
    void testSetUsername() {
        Player customPlayer = new Player();
        String newUsername = "Bob";
        customPlayer.setUsername(newUsername);
        assertEquals(newUsername, customPlayer.getUsername());
    }

    @Test
    void testUniqueIdIsDifferentForEachPlayer() {
        Player player1 = new Player();
        Player player2 = new Player();
        Assertions.assertNotEquals(player1.getUniqueId(), player2.getUniqueId(),
                "Each Player should have a unique ID");
    }

    @Test
    void defaultConstructorInitializesResourcesToZeroAndConnectionToNull() {
        assertNull(player.getConnection(), "connection should be null for default constructor.");

        for (TileType type : TileType.values()) {
            if (type == TileType.WASTE){
                assertNull(player.resources.get(type), "Waste should be null");
                continue;
            }
            assertEquals(0, player.resources.get(type), "Initial resource count for " + type + " should be 0.");
        }
    }

    @Test
    void constructorWithUsernameInitializesConnectionIdAsNull() {
        Player customPlayer = new Player("TestUser");
        assertNull(customPlayer.getConnection(), "connection should be null for username-only constructor.");
    }

    @Test
    void constructorWithWebSocketConnectionInitializesCorrectly() {
        WebSocketConnection mockConnection = mock(WebSocketConnection.class);
        String expectedConnectionId = "connId_123";
        when(mockConnection.id()).thenReturn(expectedConnectionId);

        Player customPlayer = new Player(mockConnection);

        assertTrue(customPlayer.getUsername().startsWith("RandomPlayer_"), "Username should start with 'RandomPlayer_'.");
        assertNotNull(customPlayer.getUniqueId(), "uniqueId should not be null.");
        assertEquals(expectedConnectionId, customPlayer.getConnection().id(), "connectionId should match the mock connection's ID.");
    }

    @Test
    void constructorWithUsernameAndWebSocketConnectionInitializesCorrectly() {
        String customUsername = "BobWithConnection";
        WebSocketConnection mockConnection = mock(WebSocketConnection.class);
        String expectedConnectionId = "connId_456";
        when(mockConnection.id()).thenReturn(expectedConnectionId);

        Player customPlayer = new Player(customUsername, mockConnection);

        assertEquals(customUsername, customPlayer.getUsername());
        assertNotNull(customPlayer.getUniqueId(), "uniqueId should not be null.");
        assertEquals(expectedConnectionId, customPlayer.getConnection().id(), "connectionId should match the mock connection's ID.");
    }

    @Test
    void toStringContainsAllRelevantFields() {
        String username = "TestUserToString";
        WebSocketConnection mockConnection = mock(WebSocketConnection.class);
        String connectionId = "ws_conn_789";
        when(mockConnection.id()).thenReturn(connectionId);

        Player customPlayer = new Player(username, mockConnection);
        String uniqueId = customPlayer.getUniqueId();

        String expectedString = "Player{" +
                "username='" + username + '\'' +
                ", uniqueId='" + uniqueId + '\'' +
                ", connectionId='" + connectionId + '\'' +
                '}';
        assertEquals(expectedString, customPlayer.toString());
    }

    @Test
    void toStringHandlesNullConnectionId() {
        String username = "OfflineUserToString";
        Player customPlayer = new Player(username);
        String uniqueId = customPlayer.getUniqueId();

        String expectedString = "Player{" +
                "username='" + username + '\'' +
                ", uniqueId='" + uniqueId + '\'' +
                ", connectionId='null'" +
                '}';
        assertEquals(expectedString, customPlayer.toString());
    }

    @Test
    void getResourceAddsResourceCorrectlyForFirstTime() {
        TileType testResource = TileType.WHEAT;
        int amount = 5;

        player.getResource(testResource, amount);

        assertEquals(amount, (int) player.resources.get(testResource), "Resource count should be updated after getting resources.");
    }

    @Test
    void getResourceAddsToExistingResourceAmount() {
        TileType testResource = TileType.WOOD;
        int initialAmount = 3;
        int additionalAmount = 7;
        int expectedTotal = initialAmount + additionalAmount;

        player.getResource(testResource, initialAmount);
        player.getResource(testResource, additionalAmount);

        assertEquals(expectedTotal, (int) player.resources.get(testResource), "Resource count should be the sum of initial and additional amounts.");
    }

    @Test
    void getResourceWorksForAllTileTypes() {
        int amount = 2;
        for (TileType type : TileType.values()) {
            if (type == TileType.WASTE){
                assertNull(player.resources.get(type), "Waste should be null");
                continue;
            }
            player.getResource(type, amount);
            assertEquals(amount, (int) player.resources.get(type), "Resource count for " + type + " should be " + amount + " after first get.");
            player.getResource(type, amount);
            assertEquals(amount * 2, (int) player.resources.get(type), "Resource count for " + type + " should be " + (amount * 2) + " after second get.");
        }
    }


    @Test
    void getResourceJSONWithSomeResourcesReturnsCorrectJSON() {
        player.getResource(TileType.WHEAT, 3);
        player.getResource(TileType.SHEEP, 1);
        player.getResource(TileType.ORE, 0);

        ObjectNode json = player.getResourceJSON();
        assertNotNull(json);

        assertEquals(3, json.get(TileType.WHEAT.name()).asInt());
        assertEquals(1, json.get(TileType.SHEEP.name()).asInt());
        assertEquals(0, json.get(TileType.ORE.name()).asInt());

        assertTrue(json.has(TileType.WOOD.name()), "JSON should have WOOD field");
        assertEquals(0, json.get(TileType.WOOD.name()).asInt(), "WOOD count should be 0");
        assertTrue(json.has(TileType.CLAY.name()), "JSON should have CLAY field");
        assertEquals(0, json.get(TileType.CLAY.name()).asInt(), "CLAY count should be 0");


        assertNull(json.get(TileType.WASTE.name()), "JSON should not contain WASTE resource.");
        assertEquals(TileType.values().length - 1, json.size(), "JSON object should have one field for each non-WASTE resource type.");
    }

    @Test
    void getResourceJSONDoesNotIncludeWaste() {
        player.getResource(TileType.WHEAT, 2);
        player.getResource(TileType.WASTE, 5);

        ObjectNode json = player.getResourceJSON();
        assertNotNull(json);
        assertTrue(json.has(TileType.WHEAT.name()));
        assertFalse(json.has(TileType.WASTE.name()), "JSON should not include WASTE type.");
    }

    @Test
    void getResourceJSONDoesNotIncludeWasteEvenIfInList() {
        player.resources.put(TileType.WASTE, 256);
        player.getResource(TileType.WHEAT, 2);
        player.getResource(TileType.WASTE, 5);

        ObjectNode json = player.getResourceJSON();
        assertNotNull(json);
        assertTrue(json.has(TileType.WHEAT.name()));
        assertFalse(json.has(TileType.WASTE.name()), "JSON should not include WASTE type.");
    }

    @Test
    void getResourceWithWasteTypeDoesNotChangeResourceCounts() {
        player.getResource(TileType.WOOD, 3);
        int initialWoodCount = player.getResourceCount(TileType.WOOD);
        int initialWheatCount = player.getResourceCount(TileType.WHEAT);

        player.getResource(TileType.WASTE, 5);

        assertEquals(initialWoodCount, player.getResourceCount(TileType.WOOD), "Adding WASTE should not affect WOOD count.");
        assertEquals(initialWheatCount, player.getResourceCount(TileType.WHEAT), "Adding WASTE should not affect WHEAT count.");
        assertFalse(player.resources.containsKey(TileType.WASTE), "Player's internal resources map should not contain WASTE key.");
    }

    @Test
    void getResourceWithNullResourceTypeShouldIdeallyThrowExceptionOrHandleGracefully() {
        assertThrows(NullPointerException.class, () -> player.getResource(null, 5), "Getting a null resource type should throw an exception or be handled.");
    }
}
