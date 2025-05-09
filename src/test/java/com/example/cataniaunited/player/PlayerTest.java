package com.example.cataniaunited.player;

import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import io.quarkus.websockets.next.WebSocketConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerTest {

    @Test
    void testDefaultConstructor() {
        Player player = new Player();

        assertTrue(player.getUsername().startsWith("RandomPlayer_"));
        Assertions.assertNotNull(player.getUniqueId(), "uniqueId should not be null");
        Assertions.assertFalse(player.getUniqueId().isEmpty(), "uniqueId should not be empty");
    }

    @Test
    void testCustomConstructor() {
        String customUsername = "Alice1";
        Player player = new Player(customUsername);
        assertEquals(customUsername, player.getUsername());
        Assertions.assertNotNull(player.getUniqueId(), "uniqueId should not be null");
    }

    @Test
    void testSetUsername() {
        Player player = new Player();
        String newUsername = "Bob";
        player.setUsername(newUsername);
        assertEquals(newUsername, player.getUsername());
    }

    @Test
    void testUniqueIdIsDifferentForEachPlayer() {
        Player player1 = new Player();
        Player player2 = new Player();
        Assertions.assertNotEquals(player1.getUniqueId(), player2.getUniqueId(),
                "Each Player should have a unique ID");
    }

    @Test
    void defaultConstructorInitializesResourcesToZeroAndConnectionIdToNull() {
        Player player = new Player();
        assertNull(player.connectionId, "connectionId should be null for default constructor.");

        for (TileType type : TileType.values()) {
            assertEquals(0, player.resources.get(type), "Initial resource count for " + type + " should be 0.");
        }
    }

    @Test
    void constructorWithUsernameInitializesConnectionIdAsNull() {
        Player player = new Player("TestUser");
        assertNull(player.connectionId, "connectionId should be null for username-only constructor.");
    }

    @Test
    void constructorWithWebSocketConnectionInitializesCorrectly() {
        WebSocketConnection mockConnection = mock(WebSocketConnection.class);
        String expectedConnectionId = "connId_123";
        when(mockConnection.id()).thenReturn(expectedConnectionId);

        Player player = new Player(mockConnection);

        assertTrue(player.getUsername().startsWith("RandomPlayer_"), "Username should start with 'RandomPlayer_'.");
        assertNotNull(player.getUniqueId(), "uniqueId should not be null.");
        assertEquals(expectedConnectionId, player.connectionId, "connectionId should match the mock connection's ID.");
    }

    @Test
    void constructorWithUsernameAndWebSocketConnectionInitializesCorrectly() {
        String customUsername = "BobWithConnection";
        WebSocketConnection mockConnection = mock(WebSocketConnection.class);
        String expectedConnectionId = "connId_456";
        when(mockConnection.id()).thenReturn(expectedConnectionId);

        Player player = new Player(customUsername, mockConnection);

        assertEquals(customUsername, player.getUsername());
        assertNotNull(player.getUniqueId(), "uniqueId should not be null.");
        assertEquals(expectedConnectionId, player.connectionId, "connectionId should match the mock connection's ID.");
    }

    @Test
    void toStringContainsAllRelevantFields() {
        String username = "TestUserToString";
        WebSocketConnection mockConnection = mock(WebSocketConnection.class);
        String connectionId = "ws_conn_789";
        when(mockConnection.id()).thenReturn(connectionId);

        Player player = new Player(username, mockConnection);
        String uniqueId = player.getUniqueId();

        String expectedString = "Player{" +
                "username='" + username + '\'' +
                ", uniqueId='" + uniqueId + '\'' +
                ", connectionId='" + connectionId + '\'' +
                '}';
        assertEquals(expectedString, player.toString());
    }

    @Test
    void toStringHandlesNullConnectionId() {
        String username = "OfflineUserToString";
        Player player = new Player(username);
        String uniqueId = player.getUniqueId();

        String expectedString = "Player{" +
                "username='" + username + '\'' +
                ", uniqueId='" + uniqueId + '\'' +
                ", connectionId='null'" +
                '}';
        assertEquals(expectedString, player.toString());
    }

    @Test
    void getResourceAddsResourceCorrectlyForFirstTime() {
        Player player = new Player();
        TileType testResource = TileType.WHEAT;
        int amount = 5;

        player.getResource(testResource, amount);

        assertEquals(amount, (int) player.resources.get(testResource), "Resource count should be updated after getting resources.");
    }

    @Test
    void getResourceAddsToExistingResourceAmount() {
        Player player = new Player();
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
        Player player = new Player();
        int amount = 2;
        for (TileType type : TileType.values()) {
            player.getResource(type, amount);
            assertEquals(amount, (int) player.resources.get(type), "Resource count for " + type + " should be " + amount + " after first get.");
            player.getResource(type, amount);
            assertEquals(amount * 2, (int) player.resources.get(type), "Resource count for " + type + " should be " + (amount * 2) + " after second get.");
        }
    }
}
