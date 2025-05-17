package com.example.cataniaunited.player;

import com.example.cataniaunited.dto.MessageDTO;
import com.example.cataniaunited.dto.MessageType;
import com.example.cataniaunited.exception.GameException;
import com.example.cataniaunited.exception.InsufficientResourcesException;
import com.example.cataniaunited.game.board.tile_list_builder.TileType;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class PlayerTest {

    Player player;

    @BeforeEach
    void setUp() {
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
        assertNotEquals(player1.getUniqueId(), player2.getUniqueId(),
                "Each Player should have a unique ID");
    }

    @Test
    void defaultConstructorInitializesResourcesToZeroAndConnectionToNull() {
        assertNull(player.getConnection(), "connection should be null for default constructor.");

        for (TileType type : TileType.values()) {
            if (type == TileType.WASTE) {
                assertNull(player.resources.get(type), "Waste should be null");
                continue;
            }
            assertEquals(type.getInitialAmount(), player.resources.get(type), "Initial resource count for " + type + " should be 0.");
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
    void testToJsonIncludesUsernameAndVictoryPoints() {
        Player testUser = new Player("TestUser");
        testUser.addVictoryPoints(3);

        ObjectNode json = testUser.toJson();

        assertNotNull(json);
        assertEquals("TestUser", json.get("username").asText());
        assertEquals(3, json.get("victoryPoints").asInt());
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

        player.receiveResource(testResource, amount);

        assertEquals(testResource.getInitialAmount() + amount, (int) player.resources.get(testResource), "Resource count should be updated after getting resources.");
    }

    @Test
    void getResourceAddsToExistingResourceAmount() {
        TileType testResource = TileType.WOOD;
        int firstIncrease = 3;
        int secondIncrease = 7;
        int expectedTotal = testResource.getInitialAmount() + firstIncrease + secondIncrease;

        player.receiveResource(testResource, firstIncrease);
        player.receiveResource(testResource, secondIncrease);

        assertEquals(expectedTotal, (int) player.resources.get(testResource), "Resource count should be the sum of initial and additional amounts.");
    }

    @Test
    void getResourceWorksForAllTileTypes() {
        int amount = 2;
        for (TileType type : TileType.values()) {
            if (type == TileType.WASTE) {
                assertNull(player.resources.get(type), "Waste should be null");
                continue;
            }
            int ressourceAmount = player.resources.get(type);
            player.receiveResource(type, amount);
            assertEquals(ressourceAmount + amount, (int) player.resources.get(type), "Resource count for " + type + " should be " + amount + " after first get.");
            player.receiveResource(type, amount);
            assertEquals(ressourceAmount + amount * 2, (int) player.resources.get(type), "Resource count for " + type + " should be " + (amount * 2) + " after second get.");
        }
    }


    @Test
    void getResourceJSONWithSomeResourcesReturnsCorrectJSON() {
        player.receiveResource(TileType.WHEAT, 3);
        player.receiveResource(TileType.SHEEP, 1);
        player.receiveResource(TileType.ORE, 0);

        ObjectNode json = player.getResourceJSON();
        assertNotNull(json);

        assertEquals(TileType.WHEAT.getInitialAmount() + 3, json.get(TileType.WHEAT.name()).asInt());
        assertEquals(TileType.SHEEP.getInitialAmount() + 1, json.get(TileType.SHEEP.name()).asInt());
        assertEquals(TileType.ORE.getInitialAmount(), json.get(TileType.ORE.name()).asInt());

        assertTrue(json.has(TileType.WOOD.name()), "JSON should have WOOD field");
        assertEquals(TileType.WOOD.getInitialAmount(), json.get(TileType.WOOD.name()).asInt(), "WOOD count should be 0");
        assertTrue(json.has(TileType.CLAY.name()), "JSON should have CLAY field");
        assertEquals(TileType.CLAY.getInitialAmount(), json.get(TileType.CLAY.name()).asInt(), "CLAY count should be 0");


        assertNull(json.get(TileType.WASTE.name()), "JSON should not contain WASTE resource.");
        assertEquals(TileType.values().length - 1, json.size(), "JSON object should have one field for each non-WASTE resource type.");
    }

    @Test
    void getResourceJSONDoesNotIncludeWaste() {
        player.receiveResource(TileType.WHEAT, 2);
        player.receiveResource(TileType.WASTE, 5);

        ObjectNode json = player.getResourceJSON();
        assertNotNull(json);
        assertTrue(json.has(TileType.WHEAT.name()));
        assertFalse(json.has(TileType.WASTE.name()), "JSON should not include WASTE type.");
    }

    @Test
    void getResourceJSONDoesNotIncludeWasteEvenIfInList() {
        player.resources.put(TileType.WASTE, 256);
        player.receiveResource(TileType.WHEAT, 2);
        player.receiveResource(TileType.WASTE, 5);

        ObjectNode json = player.getResourceJSON();
        assertNotNull(json);
        assertTrue(json.has(TileType.WHEAT.name()));
        assertFalse(json.has(TileType.WASTE.name()), "JSON should not include WASTE type.");
    }

    @Test
    void getResourceWithWasteTypeDoesNotChangeResourceCounts() {
        int initialWoodCount = player.getResourceCount(TileType.WOOD);
        int initialWheatCount = player.getResourceCount(TileType.WHEAT);

        player.receiveResource(TileType.WASTE, 5);

        assertEquals(initialWoodCount, player.getResourceCount(TileType.WOOD), "Adding WASTE should not affect WOOD count.");
        assertEquals(initialWheatCount, player.getResourceCount(TileType.WHEAT), "Adding WASTE should not affect WHEAT count.");
        assertFalse(player.resources.containsKey(TileType.WASTE), "Player's internal resources map should not contain WASTE key.");
    }

    @Test
    void receiveResourceWithNullResourceTypeShouldDoNothing() {
        var previousResource = player.resources;
        player.receiveResource(null, 5);
        assertEquals(previousResource, player.resources);
    }

    @Test
    void removeResourceOfTypeWasteShouldDoNothing() throws GameException {
        var previousResource = player.resources;
        player.removeResource(TileType.WASTE, 5);
        assertEquals(previousResource, player.resources);
    }

    @Test
    void removeResourceOfNullShouldDoNothing() throws GameException {
        var previousResource = player.resources;
        player.removeResource(null, 5);
        assertEquals(previousResource, player.resources);
    }

    @Test
    void removeResourceShouldThrowExceptionIfResourcseAmountIsTooSmall() {
        int woodResource = player.resources.get(TileType.WOOD);
        assertThrows(InsufficientResourcesException.class, () -> player.removeResource(TileType.WOOD, woodResource + 1));
    }

    @Test
    void removeResourceShouldRemoveCorrectResourceAmount() throws GameException {
        int woodResource = 4;
        player.resources.put(TileType.WOOD, woodResource);
        player.removeResource(TileType.WOOD, 2);
        assertEquals(woodResource - 2, player.resources.get(TileType.WOOD));
    }

    @Test
    void testHashCode() {
        assertEquals(Objects.hashCode(player.getUniqueId()), player.hashCode());
    }

    @Test
    void sendMessage_successfulSend_invokesConnection() {
        WebSocketConnection conn = mock(WebSocketConnection.class);
        when(conn.sendText(any(MessageDTO.class)))
                .thenReturn(Uni.createFrom().voidItem());

        Player player = new Player(conn);

        MessageDTO dto = new MessageDTO();
        dto.setType(MessageType.CREATE_LOBBY);
        player.sendMessage(dto);

        verify(conn).sendText(dto);
    }

    @Test
    void sendMessage_failureDoesNotThrow_stillInvokesConnection() {
        WebSocketConnection conn = mock(WebSocketConnection.class);
        when(conn.sendText(any(MessageDTO.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("boom")));

        Player player = new Player(conn);

        MessageDTO dto = new MessageDTO();
        dto.setType(MessageType.CREATE_LOBBY);

        assertDoesNotThrow(() -> player.sendMessage(dto));
        verify(conn).sendText(dto);
    }

    @Test
    void sendMessage_withNoConnection_doesNothing() {
        Player p = new Player("someUser");
        MessageDTO dto = new MessageDTO();
        dto.setType(MessageType.CREATE_LOBBY);

        assertDoesNotThrow(() -> p.sendMessage(dto));
    }

    @Test
    void sendMessage_successfulSend_invokesSendText() {
        WebSocketConnection conn = mock(WebSocketConnection.class);
        when(conn.sendText(any(MessageDTO.class)))
                .thenReturn(Uni.createFrom().voidItem());

        Player p = new Player(conn);
        MessageDTO dto = new MessageDTO();
        dto.setType(MessageType.CREATE_LOBBY);

        p.sendMessage(dto);

        verify(conn, times(1)).sendText(dto);
    }

    @Test
    void sendMessage_whenSendFails_stillInvokesSendText_andSwallowsException() {
        WebSocketConnection conn = mock(WebSocketConnection.class);
        when(conn.sendText(any(MessageDTO.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("kaboom")));

        Player p = new Player(conn);
        MessageDTO dto = new MessageDTO();
        dto.setType(MessageType.CREATE_LOBBY);

        assertDoesNotThrow(() -> p.sendMessage(dto));

        verify(conn, times(1)).sendText(dto);
    }

    @Test
    void sendMessageShouldNotThrowExceptionIfSendTextFails() {
        WebSocketConnection mockConnection = mock(WebSocketConnection.class);
        RuntimeException simulatedException = new RuntimeException("Simulated network error during send");
        MessageDTO testMessage = new MessageDTO(MessageType.DICE_RESULT, JsonNodeFactory.instance.objectNode());

        when(mockConnection.sendText(any(MessageDTO.class)))
                .thenReturn(Uni.createFrom().failure(simulatedException));

        Player newPlayer = new Player(mockConnection);
        Uni<Void> sendUni = newPlayer.sendMessage(testMessage);
        sendUni.subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(RuntimeException.class, "Simulated network error during send")
                .assertSubscribed()
                .assertTerminated();

        verify(mockConnection).sendText(testMessage);
    }
}

